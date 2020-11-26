package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import java.util.*
import kotlin.reflect.KClass
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.room.util.CursorUtil.getColumnIndexOrThrow
import androidx.documentfile.provider.DocumentFile
import nl.mpcjanssen.simpletask.Simpletask
import java.io.*


object FileStore : IFileStore {
    private val TAG = "FileStore"

    override fun loadFile(uri: Uri): String {
        TodoApplication.app.applicationContext.contentResolver.let {
            it.persistedUriPermissions
            it.refresh(uri, null, null)
            it.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use {return it.readText() }
            }
        }
        throw IOException("File error $uri")
    }

    override fun saveFile (uri: Uri, contents: String) {

        TodoApplication.app.applicationContext.contentResolver.let {
            it.persistedUriPermissions
            it.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(contents)
                }
            }
        }
    }
}

