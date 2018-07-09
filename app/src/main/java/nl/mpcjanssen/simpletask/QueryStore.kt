package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences
import nl.mpcjanssen.simpletask.util.Config

import java.io.File

object QueryStore {
    val TAG = "QueryStore"


    fun ids() : List<String> {
        return Config.savedQueries.map { it.name  }
    }


    fun get(id: String): NamedQuery {
        return  Config.savedQueries.first { it.name == id }
    }

    fun save(query: Query, name: String) {
        val queries = Config.savedQueries.toMutableList()
        queries.add(NamedQuery(name,query))
        Config.savedQueries = queries
    }



    fun delete(id: String) {
        val newQueries = Config.savedQueries.filterNot { it.name == id }
        Config.savedQueries = newQueries
    }

    fun rename(squery: NamedQuery, newName: String) {
        val queries = Config.savedQueries.toMutableList()
        val idx = queries.indexOf(squery)
        if (idx != -1 ) {
            queries[idx] = NamedQuery(newName, squery.query)
        }
        Config.savedQueries = queries
    }
}

object LegacyQueryStore {
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

}

