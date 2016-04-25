package nl.mpcjanssen.simpletask.util

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import org.json.JSONArray
import org.json.JSONObject

/**
 * Defines extension functions.
 *
 * Some method are separated from the main class to reduce dependencies on external libs (such as JSON)
 *
 */

fun Task.toJSON () : JSONObject {
    val json = JSONArray()
    tokens.forEach {
        json.put(it.toJSON())
    }
    return JSONObject().put("tokens", json)
}


fun TToken.toJSON(): JSONObject {
    val json = JSONObject()
    json.put("type", this.type)
    json.put("value", this.value)
    json.put("text", this.text)
    return json
}