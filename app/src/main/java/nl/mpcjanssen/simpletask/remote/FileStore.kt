package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import nl.mpcjanssen.simpletask.TodoApplication
import java.io.FileOutputStream
import java.util.*


object FileStore : IFileStore {

    private val tag = "FileStore"


    @Synchronized
    override fun loadTasksFromFile(uri: Uri): RemoteContents {
        val lines = ArrayList<String>()
        val contentResolver = TodoApplication.app.contentResolver
        contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_READ_URI_PERMISSION)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                lines.addAll(reader.readLines())
            }
        }
        return RemoteContents(uri.metaData(TodoApplication.app).lastModified, lines)
    }

    @Synchronized
    override fun saveTasksToFile(uri: Uri, lines: List<String>, eol: String) {
        val contentResolver = TodoApplication.app.contentResolver
        contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_READ_URI_PERMISSION)
        contentResolver.openOutputStream(uri,"rwt")?.use { stream ->
            (stream as FileOutputStream).channel.truncate(0)
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(lines.joinToString(eol))
            }
        }

    }


    override fun appendTaskToFile(uri: Uri, lines: List<String>, eol: String) {
        val contentResolver = TodoApplication.app.contentResolver
        contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.openOutputStream(uri, "wa")?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use {
                it.write(lines.joinToString(eol)+eol)
            }
        }
    }
}


data class MetaData(val lastModified: String?, val displayName: String?)

fun Uri.metaData(ctxt: Context): MetaData {
    val cursor: Cursor? = ctxt.contentResolver.query(this, null, null, null, null, null)

    cursor?.use {
        // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
        // "if there's anything to look at, look at it" conditionals.
        if (it.moveToFirst()) {

            // Note it's called "Display Name".  This is
            // provider-specific, and might not necessarily be the file name.
            val displayName: String = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            val lastModified = it.getLong(it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
            return MetaData(lastModified.toString(), displayName)
        }
    }
    return MetaData(null, null)
}
