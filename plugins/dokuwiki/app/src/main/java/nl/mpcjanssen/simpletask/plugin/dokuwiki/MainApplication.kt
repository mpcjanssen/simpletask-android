package nl.mpcjanssen.simpletask.plugin.dokuwiki

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy



class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this

        // Allow network on the main thread.
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }
    companion object {
        lateinit var app: Application
    }
}