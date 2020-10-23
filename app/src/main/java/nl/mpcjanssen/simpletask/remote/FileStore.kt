package nl.mpcjanssen.simpletask.remote

import java.io.File
import kotlin.reflect.KClass

object FileStore : IFileStore {
    override val isAuthenticated: Boolean
        get() = true

    override fun loadTasksFromFile(file: File): RemoteContents {
        return RemoteContents("", emptyList())
    }

    override fun saveTasksToFile(file: File, lines: List<String>, lastRemote: String?, eol: String): String {
        return ""
    }


    override fun logout() {
        TODO("Not yet implemented")
    }

    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {

    }

    override fun readFile(file: File, fileRead: (contents: String) -> Unit) {

    }

    override fun writeFile(file: File, contents: String) {

    }

    override val isOnline: Boolean
        get() = true

    override fun getRemoteVersion(file: File): String? {
      return ""
    }

    override fun getDefaultFile(): File {
        return File("/")
    }

    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {
        return emptyList()
    }

}
