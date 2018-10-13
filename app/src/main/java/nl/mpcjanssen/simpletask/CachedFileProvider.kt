package nl.mpcjanssen.simpletask

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log

import java.io.File
import java.io.FileNotFoundException

class CachedFileProvider : ContentProvider() {

    // UriMatcher used to match against incoming requests
    private var uriMatcher: UriMatcher? = null

    override fun onCreate(): Boolean {

        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        // Add a URI to the matcher which will match against the form
        // 'content://com.stephendnicholas.gmailattach.provider/*'
        // and return 1 in the case that the incoming Uri matches this pattern
        uriMatcher!!.addURI(AUTHORITY, "*", 1)

        return true
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {

        Log.d(TAG, "Called with uri: '" + uri + "'." + uri.lastPathSegment)

        // Check incoming Uri against the matcher
        when (uriMatcher!!.match(uri)) {

        // If it returns 1 - then it matches the Uri defined in onCreate
            1 -> {

                // The desired file name is specified by the last segment of the
                // path
                // E.g.
                // 'content://com.stephendnicholas.gmailattach.provider/Test.txt'
                // Take this and build the path to the file
                val fileLocation = File(context.cacheDir , uri.lastPathSegment)

                // Create & return a ParcelFileDescriptor pointing to the file
                // Note: I don't care what mode they ask for - they're only getting
                // read only
                return ParcelFileDescriptor.open(fileLocation, ParcelFileDescriptor.MODE_READ_ONLY)
            }

        // Otherwise unrecognised Uri
            else -> {
                Log.d(TAG, "Unsupported uri: '$uri'.")
                throw FileNotFoundException("Unsupported uri: " + uri.toString())
            }
        }
    }

    // //////////////////////////////////////////////////////////////
    // Not supported / used / required for this example
    // //////////////////////////////////////////////////////////////

    override fun update(uri: Uri, contentvalues: ContentValues, s: String,
                        `as`: Array<String>): Int {
        return 0
    }

    override fun delete(uri: Uri, s: String, `as`: Array<String>): Int {
        return 0
    }

    override fun insert(uri: Uri, contentvalues: ContentValues): Uri? {
        return null
    }

    override fun getType(uri: Uri): String {
        if (uri.toString().endsWith(".db")) {
            return "application/x-sqlite3"
        }
        return "text/plain"
    }

    override fun query(uri: Uri, projection: Array<String>?, s: String?, as1: Array<String>?,
                       s1: String? ): Cursor? {
        return null
    }

    companion object {

        private val TAG = CachedFileProvider::class.java.simpleName

        // The authority is the symbolic name for the provider class
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider." + BuildConfig.FLAVOR
    }
}
