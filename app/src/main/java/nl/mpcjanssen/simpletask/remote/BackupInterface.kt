package nl.mpcjanssen.simpletask.remote

interface BackupInterface {
    fun backup(name: String, lines: List<String>)
}
