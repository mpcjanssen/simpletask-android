package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences

import me.smichel.android.KPreferences.Preferences
import nl.mpcjanssen.simpletask.util.TAG
import java.io.File

class SavedQuery(
        val id: String = nextId(),
        var query: Query = savedQuery(id)
) {
    val name: String get() = query.name ?: ""

    init {
        assert(validId(id))
    }

    fun save() = query.saveInPrefs(prefs(id))

    fun saveAs(name: String) {
        query.name = name
        save()
    }

    fun delete() {
        ids = ids - id
        prefs(id).edit().clear().apply()
        val prefs_path = "../shared_prefs"
        val prefs_xml = File(context.filesDir, "$prefs_path/$id.xml")
        val deleted = prefs_xml.delete()
        if (!deleted) {
            Logger.warn(TAG, "Failed to delete saved query: " + name)
        }
    }

    companion object QueryStore : Preferences(TodoApplication.app, "filters") {
        var ids by StringSetPreference("ids", HashSet<String>())
        private var maxId by IntPreference("max_id", 1)

        private fun validId(id: String): Boolean {
            return id.startsWith(ID_PREFIX) && id.removePrefix(ID_PREFIX).toInt() <= maxId
        }

        private fun nextId() : String {
            val newMaxId = maxId + 1
            val nextId = ID_PREFIX + newMaxId

            maxId = newMaxId
            ids = ids + nextId

            return nextId
        }

        private fun savedQuery(id: String): Query {
            return Query(luaModule = "mainui", showSelected = true).apply {
                initFromPrefs(prefs(id))
            }
        }

        private fun prefs(id: String): SharedPreferences {
            return context.getSharedPreferences(id, Context.MODE_PRIVATE)
        }

        private const val ID_PREFIX: String = "filter_"
    }
}
