/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import nl.mpcjanssen.simpletask.util.markdownAssetAsHtml
import nl.mpcjanssen.simpletask.util.Config
import java.util.*

class HelpScreen : ThemedActionBarActivity() {

    private val history = Stack<String>()

    private var wvHelp: WebView? = null

    private fun loadDesktop(wv: WebView, url: String) {
        wv.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.45 Safari/535.19"
        wv.loadUrl(url)
    }

    override fun onBackPressed() {
        Log.d(TAG, "History " + history + "empty: " + history.empty())
        history.pop()
        if (!history.empty()) {
            showMarkdownAsset(wvHelp!!, this, history.pop())
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var page = "index." + getText(R.string.help_locale).toString() + ".md"
        val i = intent
        if (i.hasExtra(Constants.EXTRA_HELP_PAGE)) {
            page = i.getStringExtra(Constants.EXTRA_HELP_PAGE) + "." + getText(R.string.help_locale).toString() + ".md"
        }

        setContentView(R.layout.help)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        wvHelp = findViewById<WebView>(R.id.help_view)

        // Prevent brief flash of white when loading WebView.
        if (TodoApplication.config.isDarkTheme || TodoApplication.config.isBlackTheme) {
            val tv = TypedValue()
            theme.resolveAttribute(android.R.attr.windowBackground, tv, true)
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                val windowBackgroundColor = tv.data
                wvHelp!!.setBackgroundColor(windowBackgroundColor)
            }
        }

        wvHelp!!.webViewClient = object : WebViewClient() {
            // Replacement is API >= 21 only
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d(TAG, "Loading url: " + url)
                if (url.startsWith("https://www.paypal.com")) {
                    // Paypal links don't work in the mobile browser so this hack is needed
                    loadDesktop(view, url)
                    // Don't store paypal redirects in history
                    if (history.peek() != "paypal") {
                        history.push("paypal")
                    }
                    return true
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.settings.userAgentString = null
                    openUrl(url)
                    return true
                } else if (url.endsWith(".md")) {
                    showMarkdownAsset(view, this@HelpScreen, url.replace(BASE_URL, ""))
                    return true
                } else {
                    return false
                }
            }
        }
        showMarkdownAsset(wvHelp!!, this, page)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.help, menu)
        return true
    }

    fun openUrl(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun showMarkdownAsset(wv: WebView, ctxt: Context, name: String) {
        Log.d(TAG, "Loading asset $name into $wv($ctxt)")
        val html = markdownAssetAsHtml(ctxt, name)
        history.push(name)
        wv.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", "file:///android_asset/index." + getText(R.string.help_locale) + ".md")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // Respond to the action bar's Up/Home button
            R.id.menu_simpletask -> {
                showMarkdownAsset(wvHelp!!, this, "index." + getText(R.string.help_locale) + ".md")
                return true
            }
        // Changelog is English only
            R.id.menu_changelog -> {
                showMarkdownAsset(wvHelp!!, this, "changelog.en.md")
                return true
            }
            R.id.menu_myn -> {
                showMarkdownAsset(wvHelp!!, this, "MYN." + getText(R.string.help_locale) + ".md")
                return true
            }
            R.id.menu_script -> {
                showMarkdownAsset(wvHelp!!, this, "script." + getText(R.string.help_locale) + ".md")
                return true
            }
            R.id.menu_intents -> {
                showMarkdownAsset(wvHelp!!, this, "intents." + getText(R.string.help_locale) + ".md")
                return true
            }
            R.id.menu_ui -> {
                showMarkdownAsset(wvHelp!!, this, "ui." + getText(R.string.help_locale) + ".md")
                return true
            }
            R.id.menu_donate -> {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=mpc%2ejanssen%40gmail%2ecom&item_name=mpcjanssen%2enl&item_number=Simpletask&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted")
                startActivity(i)
                return true
            }
            R.id.menu_tracker -> {
                openUrl("https://github.com/mpcjanssen/simpletask-android")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        internal val TAG = HelpScreen::class.java.simpleName
        internal val BASE_URL = "file:///android_asset/"
    }

}
