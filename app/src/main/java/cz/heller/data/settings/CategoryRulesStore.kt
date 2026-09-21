package cz.heller.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rulesDataStore by preferencesDataStore(name = "categorization")

/**
 * **Legacy** naučená pravidla obchodník → kategorie (klíč → categoryId) v DataStore z verzí
 * před 1.4. Nové učení jde přímo z potvrzených záznamů v Room ([cz.heller.data.repo.CategorizationRepository]);
 * tenhle store se už jen čte, aby starší instalace o nic nepřišly.
 */
@Singleton
class CategoryRulesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val RULES = stringPreferencesKey("rules_json")

    val rules: Flow<Map<String, String>> =
        context.rulesDataStore.data.map { parse(it[RULES]) }

    suspend fun snapshot(): Map<String, String> = rules.first()

    suspend fun learn(keyword: String, categoryId: String) {
        if (keyword.isBlank()) return
        context.rulesDataStore.edit { prefs ->
            val map = parse(prefs[RULES]).toMutableMap()
            map[keyword] = categoryId
            prefs[RULES] = serialize(map)
        }
    }

    suspend fun forget(keyword: String) {
        context.rulesDataStore.edit { prefs ->
            val map = parse(prefs[RULES]).toMutableMap()
            map.remove(keyword)
            prefs[RULES] = serialize(map)
        }
    }

    private fun parse(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap { obj.keys().forEach { put(it, obj.getString(it)) } }
        }.getOrDefault(emptyMap())
    }

    private fun serialize(map: Map<String, String>): String =
        JSONObject(map as Map<*, *>).toString()
}
