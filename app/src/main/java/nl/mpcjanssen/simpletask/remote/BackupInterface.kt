package nl.mpcjanssen.simpletask.remote

import java.io.File

interface BackupInterface {
    fun backup(file: File, lines: List<String>)
}
