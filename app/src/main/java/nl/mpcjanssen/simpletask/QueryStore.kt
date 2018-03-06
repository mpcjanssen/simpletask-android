package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences

import java.io.File

object QueryStore {
    private const val ID_PREFIX: String = "filter_"
    val TAG = "QueryStore"


    fun ids() : List<String> {
        val prefs_path = "../shared_prefs"
        val prefs_xml = File(TodoApplication.app.filesDir, "$prefs_path/")
        if (prefs_xml.exists() && prefs_xml.isDirectory) {
            val ids = prefs_xml.listFiles { dir, name -> name.startsWith(ID_PREFIX) }
                    .map { it.relativeTo(prefs_xml).name }
                    .map { it -> it.substringBeforeLast(".xml") }
            Logger.debug(TAG, "Saved applyFilter ids: $ids")
            return ids
        } else {
            Logger.warn(TAG, "No pref_xml folder ${prefs_xml.path}")
            return emptyList()
        }
    }


    fun get(id: String): NamedQuery {
        val prefs = prefs(id)
        return NamedQuery.initFromPrefs(prefs, "mainui", id)
    }

    fun save(query: Query, name: String) {
        val squery = NamedQuery(name , query)
        squery.saveInPrefs(prefs(prefName(squery.id())))
    }

    fun prefName(name: String ) : String {
        return "$ID_PREFIX$name"
    }

    private fun prefs(id: String): SharedPreferences {
        return TodoApplication.app.getSharedPreferences(id, Context.MODE_PRIVATE)
    }

    fun delete(id: String) {
        val prefs_path = "../shared_prefs"
        val prefs_xml = File(TodoApplication.app.filesDir, "$prefs_path/$id.xml")
        val deleted = prefs_xml.delete()
        if (!deleted) {
            Logger.warn(TAG, "Failed to delete saved query: " + id)
        }
    }

    fun rename(squery: NamedQuery, newName: String) {
        val oldId = prefName(squery.id())
        save(squery.query, newName)
        delete(oldId)
    }
}

