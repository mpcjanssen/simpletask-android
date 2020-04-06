package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.util.Config
import org.json.JSONObject

import java.io.File

object QueryStore {
    val TAG = "QueryStore"

    fun importFilters(importFile: File) {
        FileStore.readFile(importFile) { contents ->
            val jsonFilters = JSONObject(contents)
            jsonFilters.keys().forEach { name ->
                val json = jsonFilters.getJSONObject(name)
                val newQuery = Query(json, luaModule = "mainui")
                QueryStore.save(newQuery, name)
            }
        }
    }

    fun exportFilters(exportFile: File) {
        val json = TodoApplication.config.savedQueriesJSONString
        FileStore.writeFile(exportFile, json)
    }

    fun ids() : List<String> {
        return TodoApplication.config.savedQueries.map { it.name  }
    }


    fun get(id: String): NamedQuery {
        return  TodoApplication.config.savedQueries.first { it.name == id }
    }

    fun save(query: Query, name: String) {
        val queries = TodoApplication.config.savedQueries.toMutableList()
        queries.add(NamedQuery(name,query))
        TodoApplication.config.savedQueries = queries
    }



    fun delete(id: String) {
        val newQueries = TodoApplication.config.savedQueries.filterNot { it.name == id }
        TodoApplication.config.savedQueries = newQueries
    }

    fun rename(squery: NamedQuery, newName: String) {
        val queries = TodoApplication.config.savedQueries.toMutableList()
        val idx = queries.indexOf(squery)
        if (idx != -1 ) {
            queries[idx] = NamedQuery(newName, squery.query)
        }
        TodoApplication.config.savedQueries = queries
    }
}

object LegacyQueryStore {
    private const val ID_PREFIX: String = "filter_"
    val TAG = "QueryStore"


    fun ids() : List<String> {
        val prefsPath = "../shared_prefs"
        val prefsXml = File(TodoApplication.app.filesDir, "$prefsPath/")
        if (prefsXml.exists() && prefsXml.isDirectory) {
            val ids = prefsXml.listFiles { _, name -> name.startsWith(ID_PREFIX) }
                    .map { it.relativeTo(prefsXml).name }
                    .map { it -> it.substringBeforeLast(".xml") }
            Log.d(TAG, "Saved applyFilter ids: $ids")
            return ids
        } else {
            Log.w(TAG, "No pref_xml folder ${prefsXml.path}")
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
            Log.w(TAG, "Failed to delete saved query: $id")
        }
    }

}

