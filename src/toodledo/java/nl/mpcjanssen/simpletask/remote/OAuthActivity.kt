package nl.mpcjanssen.simpletask.remote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.NonNull
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import nl.mpcjanssen.simpletask.*
import java.net.HttpURLConnection
import java.util.*


class OAuthActivity : ThemedActivity() {
    private val TAG = javaClass.simpleName
    val log = Logger

    private lateinit var apiKey: String
    private lateinit var apiSecret: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logintoodledo)
        apiKey = getString(R.string.toodledo_api_client_id)
        apiSecret = getString(R.string.toodledo_api_secret)

        val state = "${System.currentTimeMillis()}"
        val getParams = ArrayList<Pair<String, String>>()
        getParams.add(Pair("response_type", "code"))
        getParams.add(Pair("client_id", apiKey))
        getParams.add(Pair("state", state))
        getParams.add(Pair("scope", "basic tasks"))

        val authorizationUrl = RestClient.getUrl("https://api.toodledo.com/3/account/authorize.php", getParams)


        log.info(TAG, "Authorizing toodledo from ${authorizationUrl}")

        val view = findViewById(R.id.web) as WebView?

        view?.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                log.debug(TAG, "Loading url: " + url);
                val uri = Uri.parse(url);
                if (uri.scheme.equals("simpletask") && uri.host.equals("oauth")) {
                    val code = uri.getQueryParameter("code")
                    val newState = uri.getQueryParameter("state")
                    if (state != newState) {
                        log.error(TAG, "OAuth State changed from $state to $newState, aborting authentication")
                    }

                    switchToTodolist()
                    finish()
                    return false
                }
                return true
            }
        });
        view?.loadUrl(authorizationUrl)

        log.info(TAG, "Authenticated with ${intent.data}")
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

}

