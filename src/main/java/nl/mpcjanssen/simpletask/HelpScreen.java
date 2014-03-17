/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import nl.mpcjanssen.simpletask.util.Util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

public class HelpScreen extends Activity {

    final static String TAG = LoginScreen.class.getSimpleName();

    private TodoApplication m_app;
    private BroadcastReceiver m_broadcastReceiver;
    private WebView wvHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = (TodoApplication) getApplication();
        setTheme(m_app.getActiveTheme());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.help);
        wvHelp = (WebView)findViewById(R.id.help_view);
        wvHelp.setWebViewClient(new WebViewClient()  {  
            @Override  
            public boolean shouldOverrideUrlLoading(WebView view, String url)  {  
                Log.v(TAG, "Loading url: " + url);
		if (url.startsWith("http://") || url.startsWith("https://")) {
            openUrl(url);
		    return true;

		} else {
		    return false;
		}  
            }  
        });  
        wvHelp.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.help, menu);
        return true;
    }

    public void openUrl (String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case R.id.menu_simpletask:
                wvHelp.loadUrl("file:///android_asset/index.html");
                return true;
            case R.id.menu_changelog:
                wvHelp.loadUrl("file:///android_asset/changelog.html");
                return true;
            case R.id.menu_myn:
                wvHelp.loadUrl("file:///android_asset/MYN.html");
                return true;
            case R.id.menu_intents:
                wvHelp.loadUrl("file:///android_asset/intents.html");
                return true;
            case R.id.menu_donate:
                openUrl("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=mpc%2ejanssen%40gmail%2ecom&lc=NL&item_name=mpcjanssen%2enl&item_number=Simpletask&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
                return true;
            case R.id.menu_tracker:
                openUrl("http://mpcjanssen.nl/tracker/projects/simpletask-android");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
