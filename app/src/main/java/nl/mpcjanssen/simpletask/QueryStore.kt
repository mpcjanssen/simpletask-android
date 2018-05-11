package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences

import java.io.File

object QueryStore {
    private const val ID_PREFIX: String = "filter_"
    val TAG = "QueryStore"


    fun ids() : List<String> {
        val prefsPath = "../shared_prefs"
        val prefsXml = File(TodoApplication.app.filesDir, "$prefsPath/")
        if (prefsXml.exists() && prefsXml.isDirectory) {
            val ids = prefsXml.listFiles { dir, name -> name.startsWith(ID_PREFIX) }
                    .map { it.relativeTo(prefsXml).name }
                    .map { it -> it.substringBeforeLast(".xml") }
            Logger.debug(TAG, "Saved applyFilter ids: $ids")
            return ids
        } else {
            Logger.warn(TAG, "No pref_xml folder ${prefsXml.path}")
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
        val prefsPath = "../shared_prefs"
        val prefsXml = File(TodoApplication.app.filesDir, "$prefsPath/$id.xml")
        val deleted = prefsXml.delete()
        if (!deleted) {
            Logger.warn(TAG, "Failed to delete saved query: $id")
        }
    }

    fun rename(squery: NamedQuery, newName: String) {
        val oldId = prefName(squery.id())
        save(squery.query, newName)
        delete(oldId)
    }
}

