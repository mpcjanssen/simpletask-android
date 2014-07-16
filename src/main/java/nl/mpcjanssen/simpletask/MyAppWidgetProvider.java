package nl.mpcjanssen.simpletask;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class MyAppWidgetProvider extends AppWidgetProvider {

	final static String TAG = MyAppWidgetProvider.class.getSimpleName();
    final static int FROM_LISTVIEW = 0;
    // Create unique numbers for every widget pendingintent
    // Otherwise the will overwrite each other
    final static int FROM_WIDGETS_START = 1;

    public static void putFilterExtras (Intent target , @NotNull SharedPreferences preferences,  int widgetId) {
        Log.d(TAG, "putFilter extras  for appwidget " + widgetId);
        ActiveFilter filter = new ActiveFilter();
        filter.initFromPrefs(preferences);
        filter.saveInIntent(target);
    }

	public static RemoteViews updateView(int widgetId, @NotNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
        RemoteViews view ;
        SharedPreferences appPreferences = TodoApplication.getPrefs();
        ColorDrawable listColor;
        ColorDrawable headerColor;
        String theme = appPreferences.getString("widget_theme", "");

        if (theme.equals("android.R.style.Theme_Holo")) {
            view = new RemoteViews(context.getPackageName(), R.layout.appwidget_dark);
            listColor = new ColorDrawable(0xFF000000);
            headerColor = new ColorDrawable(0xFF000000);
        } else {
		    view = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            listColor = new ColorDrawable(0xFFFFFFFF);
            headerColor = new ColorDrawable(0xFF0099CC);
        }

        int header_transparency = appPreferences.getInt("widget_header_transparency", 0);
        int background_transparency = appPreferences.getInt("widget_background_transparency", 0);

        int header_alpha = ((100-header_transparency)*255)/100;
        int background_alpha = ((100-background_transparency)*255)/100;
        headerColor.setAlpha(header_alpha);
        listColor.setAlpha(background_alpha);

        view.setInt(R.id.widgetlv,"setBackgroundColor",listColor.getColor());
        view.setInt(R.id.header,"setBackgroundColor",headerColor.getColor());

        Intent intent = new Intent(context, AppWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the App Widget layout.
        view.setRemoteAdapter(R.id.widgetlv, intent);

        ActiveFilter filter = new ActiveFilter();
        filter.initFromPrefs(preferences);
        view.setTextViewText(R.id.title,filter.getName());

        // Make sure we use different intents for the different pendingIntents or
        // they will replace each other
        
        Intent appIntent;

        appIntent = new Intent(context,Simpletask.class);
        appIntent.setAction(Constants.INTENT_START_FILTER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, FROM_LISTVIEW, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);

        appIntent = new Intent(context,Simpletask.class);
        appIntent.putExtra(Constants.INTENT_SELECTED_TASK,"");
        appIntent.setAction(Constants.INTENT_START_FILTER);
        putFilterExtras(appIntent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(context, FROM_WIDGETS_START+widgetId, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.title,pendingIntent);

        appIntent = new Intent(context,AddTask.class);
        putFilterExtras(appIntent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(context, FROM_WIDGETS_START+widgetId , appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.widgetadd,pendingIntent);
        return view;
	}
	
	@Override
	public void onUpdate(@NotNull Context context, @NotNull AppWidgetManager appWidgetManager,
			@NotNull int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
			RemoteViews views = updateView(widgetId, context);
			appWidgetManager.updateAppWidget(widgetId, views);

            // Need to update the listview to redraw the listitems when
            // Changing the theme
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId,R.id.widgetlv);
		}
	}

    @Override
    public void onDeleted(@NotNull Context context, @NotNull int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            Log.v(TAG, "cleaning up widget configuration id:" + widgetId);
            // At least clear contents of the preferences file
            SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
            preferences.edit().clear().apply();
            File prefs_path = new File(context.getFilesDir(), "../shared_prefs");
            File prefs_xml = new File(prefs_path, widgetId + ".xml");
            // Remove the XML file
            if (!prefs_xml.delete()) {
                Log.w(TAG, "File not deleted: " + prefs_xml.toString());
            }

        }
    }


    public static void updateAppWidget(@NotNull Context context, @NotNull AppWidgetManager appWidgetManager, int appWidgetId, String name) {
        Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " title=" + name);

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = updateView(appWidgetId,context);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
