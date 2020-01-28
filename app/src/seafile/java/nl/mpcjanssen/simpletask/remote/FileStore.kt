package nl.mpcjanssen.simpletask.remote


import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.TodoException
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.File
import java.lang.RuntimeException
import kotlin.reflect.KClass


/**
 * FileStore implementation backed by Seafile
 */
object FileStore : IFileStore {

    var accessToken by TodoApplication.config.StringOrNullPreference("token")
    val client = OkHttpClient()

    override val isAuthenticated: Boolean =
        !accessToken.isNullOrEmpty()

    override fun loadTasksFromFile(path: String): RemoteContents {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveTasksToFile(path: String, lines: List<String>, eol: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    override fun logout() {
        accessToken = null
    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readFile(file: String, fileRead: (String) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeFile(file: File, contents: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val isOnline: Boolean = true
    override fun getRemoteVersion(filename: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefaultPath(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getAccessToken(username: String, password: String, serverUrl: String) {
        val url = serverUrl.toHttpUrlOrNull()?.newBuilder()
                ?.addPathSegments("api2/auth-token/")?.build() ?: throw(RuntimeException("invalid server URL"))
        val body = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        val req = Request.Builder()
                .url(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            accessToken == null
            throw(TodoException(resp.message))
        } else {
            accessToken = JSONObject(resp.body?.string()).getString("token")
        }
    }
}