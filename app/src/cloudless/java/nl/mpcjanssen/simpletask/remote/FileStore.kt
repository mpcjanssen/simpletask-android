package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import java.io.File
import java.io.FilenameFilter
import java.util.*
import kotlin.reflect.KClass

object FileStore : IFileStore {
    override fun getRemoteVersion(filename: String): String {
        return File(filename).lastModified().toString()
    }

    override val isOnline = true
    private val TAG = "FileStore"


    init {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Default path: ${getDefaultPath()}")
    }

    override val isAuthenticated: Boolean
        get() {
            val permissionCheck = ContextCompat.checkSelfPermission(TodoApplication.app,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionCheck == PackageManager.PERMISSION_GRANTED
        }

    override fun loadTasksFromFile(path: String): RemoteContents {
        Log.i(TAG, "Loading tasks")
        val file = File(path)
        val lines = file.readLines()
        Log.i(TAG, "Read ${lines.size} lines from $path")
        return RemoteContents(file.lastModified().toString(), lines)
    }

    override fun writeFile(file: File, contents: String) {
        Log.i(TAG, "Writing file to  ${file.canonicalPath}")
        file.writeText(contents)
    }

    override fun readFile(file: String, fileRead: (String) -> Unit) {
        Log.i(TAG, "Reading file: {}" + file)
        val contents: String
        val lines = File(file).readLines()
        contents = join(lines, "\n")
        fileRead(contents)
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }


    override fun saveTasksToFile(path: String, lines: List<String>, eol: String) {
        Log.i(TAG, "Saving tasks to file: $path")
        val file = File(path)
        writeToFile(lines, eol, file, false)
    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        Log.i(TAG, "Appending ${lines.size} tasks to $path")
        writeToFile(lines, eol, File(path), true)
    }

    override fun logout() {

    }

    override fun getDefaultPath(): String {
        return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        val result = ArrayList<FileEntry>()
        val file = File(path)
        if (path == "/") {
            result.add(FileEntry(Environment.getExternalStorageDirectory().path, true))
        }

        val filter = FilenameFilter { dir, filename ->
            val sel = File(dir, filename)
            if (!sel.canRead())
                false
            else {
                if (sel.isDirectory) {
                    result.add(FileEntry(sel.name, true))
                } else {
                    !txtOnly || filename.toLowerCase(Locale.getDefault()).endsWith(".txt")
                    result.add(FileEntry(sel.name, false))
                }
            }
        }
        // Run the file applyFilter for side effects
        file.list(filter)
        return result
    }
}
