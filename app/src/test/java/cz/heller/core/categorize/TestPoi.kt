package cz.heller.core.categorize

import java.io.File

/** Načte index provozoven z assets (pracovní adresář testů je modul `app/`), pokud existuje. */
object TestPoi {
    val index: PoiIndex? by lazy {
        val f = File("src/main/assets/poi_index.bin")
        if (f.exists()) PoiIndex.load(f.inputStream()).also { PoiIndex.installed = it } else null
    }
}
