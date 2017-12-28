package nl.mpcjanssen.simpletask.util

import android.content.Context
import android.content.Intent

import me.smichel.android.KPreferences.Preferences
import nl.mpcjanssen.simpletask.Query
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.FileStore
import org.json.JSONObject
import java.io.File

object QueryStore : Preferences(TodoApplication.app, "filters") {

    val ctx = TodoApplication.app // TODO: TEMP

    var queryIds by StringSetPreference("ids", HashSet<String>())

    var maxId by IntPreference("max_id", 1)

    fun getQuery(id: String?): Query {
        val query = Query(luaModule = "mainui", showSelected = true)
        id?.let {
            val query_pref = ctx.getSharedPreferences(id, Context.MODE_PRIVATE)
            query.initFromPrefs(query_pref)
            query.id = id
        }
        return query
    }

    fun save(query: Query, name: String? = null, id: String? = null) {
        name?.let { query.name = it }
        if (query.name.isNullOrBlank()) return
        val ids = HashSet<String>()
        ids.addAll(queryIds)
        val queryId = id ?: query.id ?: {
            val newId = maxId + 1
            maxId = newId
            "filter_" + newId
        }()
        ids.add(queryId)
        queryIds = ids
        val query_pref = ctx.getSharedPreferences(queryId, Context.MODE_PRIVATE)
        query.saveInPrefs(query_pref)
    }

    fun delete(id: String) {
        val ids = HashSet<String>()
        ids.addAll(queryIds)
        ids.remove(id)
        queryIds = ids
        val deleted_query = getQuery(id)
        val query_pref = ctx.getSharedPreferences(id, Context.MODE_PRIVATE)
        query_pref.edit().clear().apply()
        val prefs_path = File(ctx.filesDir, "../shared_prefs")
        val prefs_xml = File(prefs_path, id + ".xml")
        val deleted = prefs_xml.delete()
        if (!deleted) {
            log.warn(TAG, "Failed to delete saved query: " + deleted_query.name!!)
        }
    }
}
