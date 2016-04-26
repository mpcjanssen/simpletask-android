package nl.mpcjanssen.simpletask;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.io.File;

public class MyAppWidgetProvider extends AppWidgetProvider {

    final static int FROM_LISTVIEW = 0;
    // Create unique numbers for every widget pendingintent
    // Otherwise the will overwrite each other
    final static int FROM_WIDGETS_START = 1;
    private static final String TAG = "MyAppWidgetProvider" ;

    public static void putFilterExtras (Intent target , @NonNull SharedPreferences preferences,  int widgetId) {
        // log.debug(TAG, "putFilter extras  for appwidget " + widgetId);
        ActiveFilter filter = new ActiveFilter();
        filter.initFromPrefs(preferences);
        filter.saveInIntent(target);
    }

	public static RemoteViews updateView(int widgetId, @NonNull Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences("" + widgetId, 0);
        RemoteViews view ;
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        ColorDrawable listColor;
        ColorDrawable headerColor;
        String theme = appPreferences.getString("widget_theme", "");

        if (theme.equals("dark")) {
            view = new RemoteViews(ctx.getPackageName(), R.layout.appwidget_dark);
            listColor = new ColorDrawable(0xFF000000);
            headerColor = new ColorDrawable(0xFF000000);
        } else {
		    view = new RemoteViews(ctx.getPackageName(), R.layout.appwidget);
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

        Intent intent = new Intent(ctx, AppWidgetService.class);
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

        appIntent = new Intent(ctx, MainActivity.class);
        appIntent.setAction(Constants.INTENT_START_FILTER);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, FROM_LISTVIEW, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);

        appIntent = new Intent(ctx, MainActivity.class);
        appIntent.setAction(Constants.INTENT_START_FILTER);
        putFilterExtras(appIntent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(ctx, FROM_WIDGETS_START+widgetId, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.title,pendingIntent);

        appIntent = new Intent(ctx,AddTask.class);
        putFilterExtras(appIntent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(ctx, FROM_WIDGETS_START+widgetId , appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.widgetadd,pendingIntent);
        return view;
	}
	
	@Override
	public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager,
			@NonNull int[] appWidgetIds) {
        Logger log = Logger.INSTANCE;
        for (int widgetId : appWidgetIds) {
            log.debug(TAG, "onUpdate " + widgetId);
			RemoteViews views = updateView(widgetId, context);
			appWidgetManager.updateAppWidget(widgetId, views);

            // Need to update the listview to redraw the listitems when
            // Changing the theme
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId,R.id.widgetlv);
		}
	}

    @Override
    public void onDeleted(@NonNull Context context, @NonNull int[] appWidgetIds) {
        Logger log = Logger.INSTANCE;
        for (int widgetId : appWidgetIds) {
            log.debug(TAG, "cleaning up widget configuration id:" + widgetId);
            // At least clear contents of the other_preferences file
            SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
            preferences.edit().clear().apply();
            File prefs_path = new File(context.getFilesDir(), "../shared_prefs");
            File prefs_xml = new File(prefs_path, widgetId + ".xml");
            // Remove the XML file
            if (!prefs_xml.delete()) {
                log.warn(TAG, "File not deleted: " + prefs_xml.toString());
            }

        }
    }


    public static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, String name) {
        Logger log = Logger.INSTANCE;
        log.debug(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " title=" + name);

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = updateView(appWidgetId,context);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
