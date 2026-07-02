package cz.heller.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import cz.heller.R
import cz.heller.core.security.BackupCrypto
import cz.heller.data.db.HellerDatabase
import cz.heller.data.settings.FioConnection
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/** Typ vybraného souboru při obnově — určuje, jestli je potřeba heslo. */
enum class BackupType { ENCRYPTED, LEGACY_PLAINTEXT, INVALID }

/**
 * Lokální šifrovaná záloha/obnova do/z uživatelského souboru (SAF).
 *
 * Nový formát ([BackupCrypto]) je **šifrovaný heslem** a obsahuje kromě SQLite databáze i
 * nastavení (měna) a **Fio napojení** (token v čitelné podobě uvnitř šifrovaného obsahu) — takže
 * obnova po odinstalaci vrátí i připojení k bance. Staré nešifrované SQLite zálohy jde stále
 * obnovit (bez hesla), jen neobsahují Fio token/nastavení.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: HellerDatabase,
    private val settings: SettingsRepository,
) {
    private val SQLITE_HEADER = "SQLite format 3"
    private val META_VERSION = 2

    private fun dbFile(): File = context.getDatabasePath(HellerDatabase.NAME)

    /** Zašifrovaná záloha do [uri] chráněná [password]. */
    suspend fun backupTo(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        // Sloučí WAL do hlavního souboru, ať je kopie konzistentní.
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val dbBytes = dbFile().readBytes()
        val metaBytes = buildMeta().toByteArray(Charsets.UTF_8)
        val payload = frame(metaBytes, dbBytes)
        val blob = BackupCrypto.encrypt(payload, password)
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException(context.getString(R.string.backup_error_open_target))
        out.use { it.write(blob) }
    }

    /** Zjistí typ vybraného souboru (bez modifikace) — kvůli rozhodnutí, zda ptát na heslo. */
    suspend fun peekType(uri: Uri): BackupType = withContext(Dispatchers.IO) {
        val head = context.contentResolver.openInputStream(uri)?.use { input ->
            // Stream může vrátit méně bajtů najednou — dočti spolehlivě prvních 16 B.
            val buf = ByteArray(16)
            var off = 0
            while (off < buf.size) {
                val n = input.read(buf, off, buf.size - off)
                if (n < 0) break
                off += n
            }
            buf.copyOf(off)
        } ?: return@withContext BackupType.INVALID
        when {
            BackupCrypto.isEncryptedBackup(head) -> BackupType.ENCRYPTED
            isSqlite(head) -> BackupType.LEGACY_PLAINTEXT
            else -> BackupType.INVALID
        }
    }

    /**
     * Obnoví ze zálohy [uri]. Šifrovaná záloha vyžaduje [password] (jinak/špatné heslo →
     * [cz.heller.core.security.WrongBackupPasswordException]). Stará SQLite záloha [password] ignoruje.
     */
    suspend fun restoreFrom(uri: Uri, password: CharArray?) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException(context.getString(R.string.backup_error_open_backup))

        if (BackupCrypto.isEncryptedBackup(bytes)) {
            requireNotNull(password) { context.getString(R.string.restore_wrong_password) }
            val payload = BackupCrypto.decrypt(bytes, password) // WrongBackupPasswordException při špatném hesle
            val (metaBytes, dbBytes) = unframe(payload)
            require(isSqlite(dbBytes)) { context.getString(R.string.backup_error_invalid) }
            writeDb(dbBytes)
            // Metadata (měna + Fio) jsou best-effort: DB už je obnovená, takže selhání re-šifrování
            // tokenu (KeyStore) nesmí shodit celou obnovu — appka se pak stejně musí restartovat.
            runCatching { applyMeta(String(metaBytes, Charsets.UTF_8)) }
        } else {
            // Zpětná kompatibilita: stará nešifrovaná SQLite záloha.
            require(isSqlite(bytes)) { context.getString(R.string.backup_error_invalid) }
            writeDb(bytes)
        }
    }

    /** Restartuje aplikaci (po obnově, aby se DB znovu otevřela čistě). */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    // --- interní ---

    private fun isSqlite(bytes: ByteArray): Boolean =
        bytes.size >= 16 && String(bytes.copyOfRange(0, 15), Charsets.US_ASCII) == SQLITE_HEADER

    private fun writeDb(dbBytes: ByteArray) {
        val db = dbFile()
        // Zapiš nejdřív do dočasného souboru (původní DB zůstane netknutá) a teprve po úplném
        // zápisu ji atomicky přejmenováním nahraď — když se zápis přeruší (pád/plný disk),
        // zůstane buď celá stará, nebo celá nová DB, nikdy poškozená.
        val tmp = File(db.path + ".restore")
        try {
            tmp.outputStream().use { it.write(dbBytes) }
            database.close()
            if (!tmp.renameTo(db)) {
                tmp.copyTo(db, overwrite = true) // fallback, kdyby rename nešel
                tmp.delete()
            }
            File(db.path + "-wal").delete()
            File(db.path + "-shm").delete()
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** Metadata (mimo DB): měna + Fio připojení s **dešifrovaným** tokenem. */
    private suspend fun buildMeta(): String {
        val fioArr = JSONArray()
        settings.fioConnections.first().forEach { c ->
            fioArr.put(
                JSONObject()
                    .put("token", c.token)
                    .put("accountId", c.accountId)
                    .put("lastSync", c.lastSyncMillis)
                    .put("fioAccountNumber", c.fioAccountNumber ?: JSONObject.NULL),
            )
        }
        return JSONObject()
            .put("version", META_VERSION)
            .put("currency", settings.currency.first())
            .put("fio", fioArr)
            .toString()
    }

    private suspend fun applyMeta(json: String) {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return
        o.optString("currency").takeIf { it.isNotBlank() }?.let { settings.setCurrency(it) }
        val arr = o.optJSONArray("fio") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { i ->
            val c = arr.optJSONObject(i) ?: return@mapNotNull null
            val token = c.optString("token")
            val accId = c.optString("accountId")
            if (token.isBlank() || accId.isBlank()) null
            else FioConnection(
                token = token,
                accountId = accId,
                lastSyncMillis = c.optLong("lastSync", 0L),
                fioAccountNumber = c.optString("fioAccountNumber").ifBlank { null },
            )
        }
        settings.restoreFioConnections(list)
    }

    /** [metaLen(4, big-endian)] ‖ meta ‖ db. */
    private fun frame(meta: ByteArray, db: ByteArray): ByteArray =
        ByteBuffer.allocate(4 + meta.size + db.size)
            .putInt(meta.size)
            .put(meta)
            .put(db)
            .array()

    private fun unframe(payload: ByteArray): Pair<ByteArray, ByteArray> {
        require(payload.size >= 4) { context.getString(R.string.backup_error_invalid) }
        val buf = ByteBuffer.wrap(payload)
        val metaLen = buf.int
        require(metaLen in 0..(payload.size - 4)) { context.getString(R.string.backup_error_invalid) }
        val meta = ByteArray(metaLen).also { buf.get(it) }
        val db = ByteArray(buf.remaining()).also { buf.get(it) }
        return meta to db
    }
}
