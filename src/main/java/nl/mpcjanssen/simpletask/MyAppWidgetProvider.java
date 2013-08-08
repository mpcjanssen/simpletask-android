package nl.mpcjanssen.simpletask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.util.Util;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class MyAppWidgetProvider extends AppWidgetProvider {

	final static String TAG = MyAppWidgetProvider.class.getSimpleName();
    final static int FROM_LISTVIEW = 0;
    // Create unique numbers for every widget pendingintent
    // Otherwise the will overwrite eachother
    final static int FROM_WIDGETS_START = 1;

    public static void putFilterExtras (Intent target , SharedPreferences preferences,  int widgetId) {
        Log.d(TAG, "putFilter extras  for appwidget " + widgetId);
        ActiveFilter filter = new ActiveFilter(null);
        filter.initFromPrefs(preferences);
        filter.saveInIntent(target);
    }

	public static RemoteViews updateView(int widgetId, Context context) {
        SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
        RemoteViews view ;
        SharedPreferences appPreferences = TodoApplication.getPrefs();
        String theme = appPreferences.getString("widget_theme", "");
        if (theme.equals("android.R.style.Theme_Holo")) {
            view = new RemoteViews(context.getPackageName(), R.layout.appwidget_dark);
        } else {
		    view = new RemoteViews(context.getPackageName(), R.layout.appwidget);
        }
        Intent intent = new Intent(context, AppWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the App Widget layout.
        view.setRemoteAdapter(R.id.widgetlv, intent);
        ActiveFilter filter = new ActiveFilter(null);
        filter.initFromPrefs(preferences);
        view.setTextViewText(R.id.title,filter.getName());

        // Make sure we use different intents for the different pendingIntents or
        // they will replace each other

        intent = new Intent(Constants.INTENT_START_FILTER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, FROM_LISTVIEW, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);

        intent = new Intent(Constants.INTENT_START_FILTER);
        putFilterExtras(intent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(context, FROM_WIDGETS_START+widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.title,pendingIntent);

        intent = new Intent(context, AddTask.class);
        putFilterExtras(intent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(
                context, FROM_WIDGETS_START+widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.widgetadd,pendingIntent);
        return view;
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int widgetId = appWidgetIds[i];
			Log.v(TAG, "Updating widget:" + widgetId);
			RemoteViews views = updateView(widgetId, context);
			appWidgetManager.updateAppWidget(widgetId, views);
		}
        super.onUpdate(context, appWidgetManager, appWidgetIds);
	}


    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String name) {
        Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " title=" + name);

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = updateView(appWidgetId,context);

        appWidgetManager.updateAppWidget(appWidgetId, views);

    }
}
