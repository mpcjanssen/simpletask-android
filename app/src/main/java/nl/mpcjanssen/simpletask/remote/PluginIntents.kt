package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.content.Context
import android.content.Intent

object PluginIntents {
    val PLUGIN_AUTHENTICATED = "nl.mpcjanssen.simpletask.plugin.AUTHENTICATED"

}

fun Context.authDone() {
    this.sendBroadcast(Intent(PluginIntents.PLUGIN_AUTHENTICATED))
}

