package cz.heller.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.heller.core.security.TokenCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Jedno read-only napojení na Fio účet: token + cílový Heller účet + čas posledního syncu. */
data class FioConnection(
    val token: String,
    val accountId: String,
    val lastSyncMillis: Long = 0L,
    /** Číslo bankovního účtu z Fia (info.accountId) — pro detekci převodů mezi vlastními účty. */
    val fioAccountNumber: String? = null,
)

/** Uživatelská nastavení v DataStore (nikoli v Room). */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CURRENCY = stringPreferencesKey("currency")
        // Více Fio připojení jako JSON pole [{token, accountId, lastSync}].
        val FIO_CONNECTIONS = stringPreferencesKey("fio_connections")
        // Staré klíče (jedno připojení) — jen pro migraci na seznam.
        val FIO_TOKEN = stringPreferencesKey("fio_token")
        val FIO_ACCOUNT_ID = stringPreferencesKey("fio_account_id")
        val FIO_LAST_SYNC = longPreferencesKey("fio_last_sync")
    }

    val onboardingDone: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = value }
    }

    /** Zvolená měna aplikace (kód CZK/EUR/PLN/USD), default CZK. */
    val currency: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.CURRENCY] ?: "CZK" }

    suspend fun setCurrency(code: String) {
        context.settingsDataStore.edit { it[Keys.CURRENCY] = code }
    }

    // --- Fio připojení (více účtů) ---

    /** Všechna připojená Fio konta. Než proběhne migrace, čte i staré jedno-připojení. */
    val fioConnections: Flow<List<FioConnection>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.FIO_CONNECTIONS]?.let { parse(it) } ?: run {
            val token = prefs[Keys.FIO_TOKEN]
            val accId = prefs[Keys.FIO_ACCOUNT_ID]
            if (!token.isNullOrBlank() && accId != null) {
                listOf(FioConnection(token, accId, prefs[Keys.FIO_LAST_SYNC] ?: 0L))
            } else {
                emptyList()
            }
        }
    }

    /**
     * Jednorázová migrace starého jednoho připojení do seznamu (a úklid starých klíčů).
     * Zároveň přepíše seznam, takže případné **plaintext tokeny z dřívějška se hned zašifrují**
     * (viz [serialize] / [TokenCrypto]).
     */
    suspend fun migrateLegacyFio() {
        context.settingsDataStore.edit { prefs ->
            val list = currentList(prefs)
            if (list.isNotEmpty()) {
                prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            }
            cleanLegacy(prefs)
        }
    }

    /** Přidá nebo aktualizuje připojení (klíč = accountId); zachová předchozí lastSync. */
    suspend fun upsertFioConnection(token: String, accountId: String) {
        context.settingsDataStore.edit { prefs ->
            val list = currentList(prefs).toMutableList()
            val prev = list.firstOrNull { it.accountId == accountId }
            list.removeAll { it.accountId == accountId }
            list += FioConnection(token.trim(), accountId, prev?.lastSyncMillis ?: 0L)
            prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            cleanLegacy(prefs)
        }
    }

    /** Zapíše čas posledního úspěšného syncu pro dané konto. */
    suspend fun setFioLastSync(accountId: String, epochMillis: Long) {
        context.settingsDataStore.edit { prefs ->
            val list = currentList(prefs).map {
                if (it.accountId == accountId) it.copy(lastSyncMillis = epochMillis) else it
            }
            prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            cleanLegacy(prefs)
        }
    }

    /** Zapíše číslo bankovního účtu z Fia (info.accountId) pro dané konto — kvůli převodům mezi účty. */
    suspend fun setFioAccountNumber(accountId: String, fioAccountNumber: String) {
        if (fioAccountNumber.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val list = currentList(prefs).map {
                if (it.accountId == accountId) it.copy(fioAccountNumber = fioAccountNumber) else it
            }
            prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            cleanLegacy(prefs)
        }
    }

    /** Přepíše celý seznam Fio připojení (z obnovy zálohy) — tokeny se znovu zašifrují novým KeyStore klíčem. */
    suspend fun restoreFioConnections(list: List<FioConnection>) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            cleanLegacy(prefs)
        }
    }

    /** Odpojí jedno Fio konto (naimportované záznamy zůstanou). */
    suspend fun removeFioConnection(accountId: String) {
        context.settingsDataStore.edit { prefs ->
            val list = currentList(prefs).filterNot { it.accountId == accountId }
            prefs[Keys.FIO_CONNECTIONS] = serialize(list)
            cleanLegacy(prefs)
        }
    }

    private fun currentList(prefs: androidx.datastore.preferences.core.Preferences): List<FioConnection> {
        prefs[Keys.FIO_CONNECTIONS]?.let { return parse(it) }
        val token = prefs[Keys.FIO_TOKEN]
        val accId = prefs[Keys.FIO_ACCOUNT_ID]
        return if (!token.isNullOrBlank() && accId != null) {
            listOf(FioConnection(token, accId, prefs[Keys.FIO_LAST_SYNC] ?: 0L))
        } else {
            emptyList()
        }
    }

    private fun cleanLegacy(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs.remove(Keys.FIO_TOKEN)
        prefs.remove(Keys.FIO_ACCOUNT_ID)
        prefs.remove(Keys.FIO_LAST_SYNC)
    }

    // Token se v DataStore ukládá šifrovaně (AndroidKeyStore, viz [TokenCrypto]); zbytek
    // aplikace pracuje s plaintextem. Šifrování/dešifrování je jen na téhle hranici.
    private fun serialize(list: List<FioConnection>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("token", TokenCrypto.encrypt(c.token))
                    .put("accountId", c.accountId)
                    .put("lastSync", c.lastSyncMillis)
                    .put("fioAccountNumber", c.fioAccountNumber ?: JSONObject.NULL),
            )
        }
        return arr.toString()
    }

    private fun parse(json: String): List<FioConnection> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val token = TokenCrypto.decrypt(o.optString("token"))
            val accId = o.optString("accountId")
            if (token.isBlank() || accId.isBlank()) null
            else FioConnection(
                token = token,
                accountId = accId,
                lastSyncMillis = o.optLong("lastSync", 0L),
                fioAccountNumber = o.optString("fioAccountNumber").ifBlank { null },
            )
        }
    }.getOrDefault(emptyList())
}
