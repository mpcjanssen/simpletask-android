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
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

public class HelpScreen extends Activity {

    final static String TAG = LoginScreen.class.getSimpleName();

    private TodoApplication m_app;
    private BroadcastReceiver m_broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = (TodoApplication) getApplication();
        setTheme(m_app.getActiveTheme());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.help);
        WebView wvHelp = (WebView)findViewById(R.id.help_view);
        wvHelp.setWebViewClient(new WebViewClient()  {  
            @Override  
            public boolean shouldOverrideUrlLoading(WebView view, String url)  {  
                Log.v("HELP", "Loading url: " + url);
                return false;  
            }  
        });  
        wvHelp.loadUrl("file:///android_asset/Changelog.html");
    }
}
