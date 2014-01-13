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

import org.markdown4j.Markdown4jProcessor;

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

        setContentView(R.layout.help);
        WebView wvHelp = (WebView)findViewById(R.id.help_view);

        AssetManager assManager = getAssets();
        String html;
        try {
        InputStream changelog_is = assManager.open("MYN.md");
        html = new Markdown4jProcessor().process(Util.readFully(changelog_is, "UTF-8"));
        changelog_is.close();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't load help: " + e);
            html = e.toString();
        }
        wvHelp.loadDataWithBaseURL("file:///android_asset/",html,"text/html","UTF-8","");
    }
}
