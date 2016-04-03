package nl.mpcjanssen.simpletask.remote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.NonNull
import android.util.Base64
import android.util.JsonReader
import android.webkit.WebView
import android.webkit.WebViewClient

import nl.mpcjanssen.simpletask.*
import org.json.JSONObject

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
        getParams.add(Pair("scope", "basic tasks write"))

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
                        switchToTodolist()
                        finish()
                        return false
                    }
                    val params = ArrayList<Pair<String, String>>()
                    val headers = getHeaderWithAuthorization()
                    params.add(Pair("grant_type","authorization_code"))
                    params.add(Pair("code",code))
                    val t = object : Thread() {
                        override fun run() {
                            val result = RestClient.performPostCall("https://api.toodledo.com/3/account/token.php",
                                    params,
                                    headers)
                            val json = JSONObject(result)
                            val access_token = json.getString("access_token")
                            log.debug(TAG, "get token result ${result}, access_token = ${access_token}" )
                            log.debug(TAG, "account info " + getAccountInfo(access_token))
                            switchToTodolist()
                            finish()
                        }
                    }
                    t.start()
                    return true

                }
                return false
            }
        });
        view?.loadUrl(authorizationUrl)

    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

   fun getHeaderWithAuthorization(): ArrayList<Pair<String, String>> {
       val headers = ArrayList<Pair<String, String>>()
       headers.add(Pair("Authorization", RestClient.basicAuthorizationString(apiKey, apiSecret)))
       return headers
   }

   fun getAccountInfo(accesToken : String ): String {
       val params = ArrayList<Pair<String,String>>()
       params.add(Pair("access_token", accesToken))
       return RestClient.performGetCall(
               "https://api.toodledo.com/3/account/get.php",
               params)
   }
}

