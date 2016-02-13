package nl.mpcjanssen.simpletask.remote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.NonNull
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.OAuthConfig
import com.github.scribejava.core.model.Verifier
import nl.mpcjanssen.simpletask.HelpScreen
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.ThemedActivity

class OAuthActivity : ThemedActivity() {
    private val TAG = javaClass.simpleName
    val log = Logger
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logintoodledo)
        val service = ServiceBuilder()
                .apiKey(getString(R.string.toodledo_api_client_id))
                .state("${System.currentTimeMillis()}")
                .scope("basic tasks")
                .callback("simpletask://oauth")
                .apiSecret(getString(R.string.toodledo_api_secret))
                .build(object : DefaultApi20() {
                    override fun getAuthorizationUrl(p0: OAuthConfig?): String? {
                        return "https://api.toodledo.com/3/account/authorize.php?response_type=code&client_id=${p0?.apiKey}&state=${p0?.state}&scope=${p0?.scope}"
                    }

                    override fun getAccessTokenEndpoint(): String? {
                        return "https://api.toodledo.com/3/account/token.php"
                    }
                } );
        val authorizationUrl = service.authorizationUrl;
        log.info(TAG, "Authorizing toodledo from ${authorizationUrl}")

        val view = findViewById(R.id.web) as WebView?

        view?.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view : WebView, url : String) : Boolean {
                log.debug(TAG, "Loading url: " + url);
                val uri = Uri.parse(url);
                if (uri.scheme.equals("simpletask") && uri.host.equals("oauth")) {
                    val code = uri.getQueryParameter("code")
                    val state = uri.getQueryParameter("state")
                    if (state != service.config.state) {
                        log.error(TAG, "OAuth State changed from ${service.config.state} to $state, aborting authentication")
                    }

                    log.debug(TAG, "OAuth response received code = $code")
                    val verifier = Verifier(code)
                    val token  = service.getAccessToken(verifier)
                    log.info(TAG, "Got token ${token}")
                    finish()
                    return false
                }
                return true
            }
        });
        view?.loadUrl(authorizationUrl)

        log.info(TAG, "Authenticated with ${intent.data}")
    }
}