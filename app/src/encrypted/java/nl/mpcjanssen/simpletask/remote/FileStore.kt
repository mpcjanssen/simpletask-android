package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.Simpletask
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.broadcastAuthFailed
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption
import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption.EncryptionFailedException
import other.de.stanetz.jpencconverter.PasswordStore
import other.net.gsantner.opoc.util.MFileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import kotlin.reflect.KClass

object FileStore : IFileStore {
    private var lastSeenRemoteId by TodoApplication.config.StringOrNullPreference(R.string.file_current_version_id)

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getDefaultPassword(): CharArray? {
        return PasswordStore(TodoApplication.app).loadKey(R.string.pref_key__default_encryption_password)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isDefaultPasswordSet(): Boolean {
        val key = getDefaultPassword()
        return key != null && key.isNotEmpty()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun setDefaultPassword(password: String?) {
        PasswordStore(TodoApplication.app).storeKey(
            password,
            R.string.pref_key__default_encryption_password
        )
    }

    override val isOnline = true
    private const val TAG = "FileStore"
    private var observer: TodoObserver? = null

    init {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Default path: ${getDefaultFile().path}")
        observer = null
    }

    override val isEncrypted: Boolean
        get() = true

    val isAuthenticated: Boolean
        get() {
            val permissionCheck = ContextCompat.checkSelfPermission(TodoApplication.app,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionCheck == PackageManager.PERMISSION_GRANTED
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun loadTasksFromFile(file: File): List<String> {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return emptyList()
        }
        Log.i(TAG, "Loading tasks")
        val lines = readEncrypted(file).split("\n")
        Log.i(TAG, "Read ${lines.size} lines from $file")
        setWatching(file)
        lastSeenRemoteId = file.lastModified().toString()
        return lines
    }

    override fun needSync(file: File): Boolean {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return true
        }
        return lastSeenRemoteId != file.lastModified().toString()
    }

    override fun todoNameChanged() {
        lastSeenRemoteId = ""
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        writeEncryptedToFile(file, contents)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun readFile(file: File, fileRead: (contents: String) -> Unit) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        Log.i(TAG, "Reading file: ${file.path}")
        val contents: String = readEncrypted(file)
        fileRead(contents)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun readEncrypted(file: File): String {

        val content: String
        val pw = getPasswordWithWarning()
        if (isEncrypted(file) && pw != null) {
            content = try {
                val encryptedContext: ByteArray = MFileUtils.readCloseStreamWithSize(
                    FileInputStream(file),
                    file.length().toInt()
                )
                if (encryptedContext.size > JavaPasswordbasedCryption.Version.NAME_LENGTH) {
                    JavaPasswordbasedCryption.getDecryptedText(encryptedContext, pw)
                } else {
                    String(encryptedContext, StandardCharsets.UTF_8)
                }
            } catch (e: FileNotFoundException) {
                // TODO error log
                ""
            } catch (e: EncryptionFailedException) {
                // TODO error log
                ""
            } catch (e: IllegalArgumentException) {
                // TODO error log
                ""
            }
        } else {
            //TODO log its not encrypted
            content = join(file.readLines(), "\n") // same as cloudless for plain txt files
        }
        return content
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    private fun setWatching(file: File) {
        Log.i(TAG, "Observer: adding folder watcher on ${file.parent}")
        val obs = observer
        if (obs != null && file.canonicalPath == obs.fileName) {
            Log.w(TAG, "Observer: already watching: ${file.canonicalPath}")
            return
        } else if (obs != null) {
            Log.w(TAG, "Observer: already watching different path: ${obs.fileName}")
            obs.ignoreEvents(true)
            obs.stopWatching()
            observer = null
        }
        observer = TodoObserver(file)
        Log.i(TAG, "Observer: modifying done")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun saveTasksToFile(file: File, lines: List<String>, eol: String) : File {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return file
        }
        Log.i(TAG, "Saving tasks to file: ${file.path}")
        val obs = observer
        obs?.ignoreEvents(true)
        writeEncryptedToFile(file, lines, eol)
        obs?.delayedStartListen(1000)
        lastSeenRemoteId = file.lastModified().toString()
        return file
    }

    private fun isEncrypted(file: File): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.name.endsWith(
            JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getPasswordWithWarning(): CharArray? {
        val pw: CharArray? = getDefaultPassword()
        if (pw == null || pw.isEmpty()) {
            val warningText = "No password!"
//            Toast.makeText(context, warningText, Toast.LENGTH_LONG).show() // TODO
            Log.w(TAG, warningText)
            return null
        }
        return pw
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun writeEncryptedToFile(file: File, lines: List<String>, eol: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        val content = lines.joinToString(eol)
        writeEncryptedToFile(file, content)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun writeEncryptedToFile(file: File, content: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        Log.i(TAG, "Writing file to  ${file.canonicalPath}")
        try {
            val pw = getPasswordWithWarning()
            val contentAsBytes: ByteArray = if (isEncrypted(file) && pw != null) {
                JavaPasswordbasedCryption(
                    Build.VERSION.SDK_INT,
                    SecureRandom()
                ).encrypt(content, pw)
            } else {
                // TODO log not encrypting
                content.toByteArray()
            }
            writeToFile(contentAsBytes, file)
        } catch (e: EncryptionFailedException) {
            Log.w(TAG, "Failed to encrypt! $e")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        Log.i(TAG, "Appending ${lines.size} tasks to ${file.path}")
        val oldLines = readEncrypted(file).split(eol)
        val appended = oldLines + lines
        writeEncryptedToFile(file, appended, eol)
    }

    override fun logout() {

    }

    override fun getDefaultFile(): File {
        return File(TodoApplication.app.getExternalFilesDir(null), "todo.txt" + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)
    }

    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return emptyList()
        }
        val result = ArrayList<FileEntry>()
        if (file.canonicalPath == "/") {
            TodoApplication.app.getExternalFilesDir(null)?.let {
                result.add(FileEntry(it, true))
            }
        }

        val filter = FilenameFilter { dir, filename ->
            val sel = File(dir,filename)
            if (!sel.canRead())
                false
            else {
                if (sel.isDirectory) {
                    result.add(FileEntry(File(filename), true))
                } else {
                    !txtOnly || filename.toLowerCase(Locale.getDefault()).endsWith(".txt")
                    result.add(FileEntry(File(filename), false))
                }
            }
        }
        // Run the file applyFilter for side effects
        file.list(filter)
        return result
    }

    class TodoObserver(val file: File) : FileObserver(file.canonicalPath) {
        private val tag = "FileWatchService"
        val fileName : String = file.canonicalPath
        private var ignoreEvents: Boolean = false
        private val handler: Handler

        private val delayedEnable = Runnable {
            Log.i(tag, "Observer: Delayed enabling events for: $fileName ")
            ignoreEvents(false)
        }

        init {
            this.startWatching()
            Log.i(tag, "Observer: creating observer on: $fileName")
            this.ignoreEvents = false
            this.handler = Handler(Looper.getMainLooper())

        }

        fun ignoreEvents(ignore: Boolean) {
            Log.i(tag, "Observer: observing events on $fileName? ignoreEvents: $ignore")
            this.ignoreEvents = ignore
        }

        override fun onEvent(event: Int, eventPath: String?) {
            if (eventPath != null && eventPath == fileName) {
                Log.d(tag, "Observer event: $fileName:$event")
                if (event == CLOSE_WRITE ||
                        event == MODIFY ||
                        event == MOVED_TO) {
                    if (ignoreEvents) {
                        Log.i(tag, "Observer: ignored event on: $fileName")
                    } else {
                        Log.i(tag, "File changed {}$fileName")
                        FileStore.remoteTodoFileChanged()
                    }
                }
            }

        }

        fun delayedStartListen(ms: Int) {
            // Cancel any running timers
            handler.removeCallbacks(delayedEnable)
            // Reschedule
            Log.i(tag, "Observer: Adding delayed enabling to todoQueue")
            handler.postDelayed(delayedEnable, ms.toLong())
        }

    }
}
