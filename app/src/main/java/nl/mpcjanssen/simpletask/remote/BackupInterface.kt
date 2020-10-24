package nl.mpcjanssen.simpletask.remote

import android.net.Uri

interface BackupInterface {
    fun backup(uri: Uri?, lines: List<String>)
}
