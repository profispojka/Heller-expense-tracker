package cz.heller.core.categorize

import java.io.DataInputStream
import java.io.InputStream

/**
 * Index provozoven (POI) z Overture Maps Places pro CZ / SK / PL: 64bitové FNV-1a hashe
 * normalizovaných názvů → kategorie. Generuje `tools/lexicon/generate-poi.mjs` do
 * `assets/poi_index.bin`. Názvy v indexu nejsou, jen hashe, takže je malý (9 B na provozovnu)
 * a hledá se binárním půlením v seřazeném poli.
 *
 * Shoda: v tokenech názvu plátce (bez stop slov a čísel) se zkusí všechny souvislé podfráze
 * od nejdelší (max. 6 tokenů) po jednotlivé tokeny; první nalezená vyhrává.
 */
class PoiIndex private constructor(
    private val hashes: LongArray,
    private val categories: ByteArray,
    private val names: Array<String>,
) {
    data class Match(val categoryId: String, val tokens: Int)

    val size: Int get() = hashes.size

    fun lookup(phrase: String): String? {
        val i = hashes.binarySearch(fnv1a64(phrase))
        return if (i >= 0) names[categories[i].toInt() and 0xFF] else null
    }

    /** Nejdelší podfráze [tokens] známá v indexu. */
    fun match(tokens: List<String>): Match? {
        val t = tokens.filter { it !in MerchantText.STOP && it.none { c -> c.isDigit() } && it.length > 1 }
        if (t.isEmpty()) return null
        for (len in minOf(MAX_TOKENS, t.size) downTo 1) {
            for (start in 0..t.size - len) {
                if (len == 1 && t[start].length < MIN_SINGLE_TOKEN) continue
                val phrase = if (len == 1) t[start] else t.subList(start, start + len).joinToString(" ")
                lookup(phrase)?.let { return Match(it, len) }
            }
        }
        return null
    }

    companion object {
        const val MAX_TOKENS = 6
        /** Jednoslovná shoda jen u dlouhých slov — krátká kolidují s ulicemi a běžnými slovy. */
        const val MIN_SINGLE_TOKEN = 8

        /** Načtený index pro celou aplikaci (nastaví appka ze `assets`, testy ze souboru). */
        @Volatile var installed: PoiIndex? = null

        fun load(input: InputStream): PoiIndex = DataInputStream(input.buffered()).use { din ->
            val magic = ByteArray(4).also { din.readFully(it) }
            require(String(magic, Charsets.US_ASCII) == "HPOI") { "Not a POI index" }
            require(din.readUnsignedByte() == 1) { "Unsupported POI index version" }
            val count = din.readInt()
            val catCount = din.readUnsignedByte()
            val names = Array(catCount) {
                val len = din.readUnsignedByte()
                String(ByteArray(len).also { b -> din.readFully(b) }, Charsets.UTF_8)
            }
            val hashes = LongArray(count) { din.readLong() }
            val cats = ByteArray(count).also { din.readFully(it) }
            PoiIndex(hashes, cats, names)
        }

        /** FNV-1a 64 nad UTF-8 bajty — stejně jako generátor. */
        fun fnv1a64(s: String): Long {
            var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
            for (b in s.toByteArray(Charsets.UTF_8)) {
                h = h xor (b.toLong() and 0xFF)
                h *= 0x100000001b3L
            }
            return h
        }
    }
}
