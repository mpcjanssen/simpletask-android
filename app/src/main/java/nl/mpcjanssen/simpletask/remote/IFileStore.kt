package nl.mpcjanssen.simpletask.remote

import android.net.Uri
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass

interface IFileStore {

    fun loadFile(uri: Uri): String?
    fun saveFile(uri: Uri, contents: String)
}


