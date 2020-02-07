package nl.mpcjanssen.simpletask.remote


import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import nl.mpcjanssen.simpletask.TodoApplication

import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import java.io.File
import java.io.FilenameFilter
import java.util.*

class CloudlessFileStorePluginService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        // Return the interface
        return binder
    }

    private val binder = object : IFileStorePlugin.Stub() {
        val TAG = "FileStorePlugin"
        override fun login() {
            val loginActivity = LoginScreen::class.java
            loginActivity.let {
                val intent = Intent(this@CloudlessFileStorePluginService, it)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                this@CloudlessFileStorePluginService.startActivity(intent)
            }
        }

        override fun loadTasksFromFile(path: String?, lines: MutableList<String>?): String {
            Log.i(TAG, "Loading tasks")
            val file = File(path)
            lines?.let {
                it.clear()
                it.addAll(file.readLines())
                Log.i(TAG, "Read ${lines.size} lines from $path")
            }
            return file.lastModified().toString()
        }


        override fun getDefaultPath(): String {
            return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
        }

        override fun isAuthenticated(): Boolean {
            val permissionCheck = ContextCompat.checkSelfPermission(TodoApplication.app,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionCheck == PackageManager.PERMISSION_GRANTED
        }

        override fun getRemoteVersion(filename: String): String {
            return File(filename).lastModified().toString()
        }

        override fun saveTasksToFile(path: String?, lines: MutableList<String>?, eol: String?, append: Boolean): String {
            val file = File(path)
            lines?.let {
                Log.i(TAG, "Saving tasks to file: $path")
                writeToFile(lines, eol ?: "\n", file, append)
            }
            return file.lastModified().toString()
        }

        override fun readFile(path: String?): String {
            return path?.let {
                Log.i(TAG, "Reading file: $path")
                val contents: String
                val lines = File(path).readLines()
                contents = join(lines, "\n")
                contents
            } ?: ""

        }

        override fun loadFileList(path: String?, txtOnly: Boolean, folders: MutableList<String>?, files: MutableList<String>?): Boolean {
            files?.clear()
            folders?.clear()
            val file = File(path)
            if (path == "/") {
                folders?.add(Environment.getExternalStorageDirectory().path)
            }

            val filter = FilenameFilter { dir, filename ->
                val sel = File(dir, filename)
                if (!sel.canRead())
                    false
                else {
                    if (sel.isDirectory) {
                        folders?.add(sel.name)
                    } else {
                        !txtOnly || filename.toLowerCase(Locale.getDefault()).endsWith(".txt")
                        files?.add(sel.name)
                    }
                    true
                }
            }
            // Run the file applyFilter for side effects
            file.list(filter)
            return path == "/"
        }

        override fun writeFile(path: String?, contents: String?) {
            path?.let {
                val file = File(it)
                Log.i(TAG, "Writing file to  ${file.canonicalPath}")
                contents?.let { file.writeText(it) }
            }
        }

        override fun logout() {
            // NOOP
        }
    }
}

