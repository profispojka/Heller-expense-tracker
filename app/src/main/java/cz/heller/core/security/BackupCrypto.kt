package cz.heller.core.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Zadané heslo neodpovídá (nebo je záloha poškozená) — GCM tag nesedí. */
class WrongBackupPasswordException : Exception()

/**
 * Šifrování exportované zálohy **heslem uživatele** (na rozdíl od [TokenCrypto], které váže na
 * konkrétní zařízení přes KeyStore). Heslo si drží uživatel, takže zálohu jde obnovit i po
 * odinstalaci / na jiném telefonu — což KeyStore klíč neumožňuje (odinstalací zaniká).
 *
 * Klíč se odvozuje z hesla přes PBKDF2WithHmacSHA256, obsah se šifruje AES-256/GCM.
 * Formát souboru: MAGIC(8) ‖ iter(4, big-endian) ‖ salt(16) ‖ IV(12) ‖ ciphertext+GCM-tag.
 * Iterace i salt jsou v hlavičce, takže je lze v budoucnu měnit bez rozbití starých záloh.
 */
object BackupCrypto {

    /** ASCII „HELLERB1" — rozlišuje šifrovanou zálohu od staré plaintext SQLite zálohy. */
    val MAGIC: ByteArray = byteArrayOf(0x48, 0x45, 0x4C, 0x4C, 0x45, 0x52, 0x42, 0x31)

    // Počet iterací je uložený v hlavičce, takže ho lze zvýšit bez rozbití starých záloh
    // (decrypt čte hodnotu ze souboru). Chrání heslem odvozený klíč proti offline brute-force.
    private const val ITERATIONS = 300_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val KDF = "PBKDF2WithHmacSHA256"

    private val HEADER_MIN = MAGIC.size + 4 + SALT_LEN + IV_LEN // bez ciphertextu

    /** True, pokud [head] (aspoň prvních [MAGIC].size bajtů) začíná naší signaturou. */
    fun isEncryptedBackup(head: ByteArray): Boolean {
        if (head.size < MAGIC.size) return false
        for (i in MAGIC.indices) if (head[i] != MAGIC[i]) return false
        return true
    }

    /** Zašifruje [plain] heslem [password]; vrací celý blob včetně hlavičky. */
    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key) }
        val iv = cipher.iv // GCM: provider vygeneruje náhodný 12B IV
        val ct = cipher.doFinal(plain)
        return ByteBuffer.allocate(HEADER_MIN + ct.size)
            .put(MAGIC)
            .putInt(ITERATIONS)
            .put(salt)
            .put(iv)
            .put(ct)
            .array()
    }

    /**
     * Dešifruje blob vytvořený [encrypt]. Vyhodí [WrongBackupPasswordException] při špatném hesle
     * / poškozeném souboru a [IllegalArgumentException] pokud to není naše záloha.
     */
    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        require(isEncryptedBackup(blob) && blob.size > HEADER_MIN) { "Not an encrypted Heller backup" }
        val buf = ByteBuffer.wrap(blob)
        buf.position(MAGIC.size)
        val iterations = buf.int
        require(iterations in 1..10_000_000) { "Bad backup header" }
        val salt = ByteArray(SALT_LEN).also { buf.get(it) }
        val iv = ByteArray(IV_LEN).also { buf.get(it) }
        val ct = ByteArray(buf.remaining()).also { buf.get(it) }
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return try {
            cipher.doFinal(ct)
        } catch (e: AEADBadTagException) {
            throw WrongBackupPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
