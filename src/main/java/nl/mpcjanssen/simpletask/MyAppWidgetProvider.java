package nl.mpcjanssen.simpletask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;

import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.todotxtholo.R;

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
        ArrayList<String> m_contexts = new ArrayList<String>();
        m_contexts.addAll(preferences.getStringSet(Constants.INTENT_CONTEXTS_FILTER, new HashSet<String>()));
        ArrayList<String> prio_strings = new ArrayList<String>();
        prio_strings.addAll(preferences.getStringSet(Constants.INTENT_PRIORITIES_FILTER, new HashSet<String>()));
        ArrayList<Priority> m_prios = Priority.toPriority(prio_strings);
        ArrayList<String> m_projects = new ArrayList<String>();
        m_projects.addAll(preferences.getStringSet(Constants.INTENT_PROJECTS_FILTER, new HashSet<String>()));
        Log.d(TAG, "putFilter contexts " + m_contexts + " for " + widgetId);
        boolean m_contextsNot = preferences.getBoolean(Constants.INTENT_CONTEXTS_FILTER_NOT, false);
        boolean m_projectsNot = preferences.getBoolean(Constants.INTENT_PROJECTS_FILTER_NOT, false);
        boolean m_priosNot = preferences.getBoolean(Constants.INTENT_PRIORITIES_FILTER_NOT, false);
        ArrayList<String> m_sorts = new ArrayList<String>();
        m_sorts.addAll(Arrays.asList(preferences.getString(Constants.INTENT_SORT_ORDER, "").split("\n")));
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER, Util.join(m_contexts, "\n"));
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
        target.putExtra(Constants.INTENT_PROJECTS_FILTER, Util.join(m_projects, "\n"));
        target.putExtra(Constants.INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER, Util.join(m_prios, "\n"));
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
        target.putExtra(Constants.INTENT_SORT_ORDER, Util.join(m_sorts,"\n"));
    }

	public static RemoteViews updateView(int widgetId, Context context) {

		RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        Intent intent = new Intent(context, AppWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the App Widget layout.
        view.setRemoteAdapter(R.id.widgetlv, intent);
        SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
        view.setTextViewText(R.id.title,preferences.getString(Constants.INTENT_TITLE, "Simpletask"));

        // Make sure we use different intents for the different pendingIntents or
        // they will replace each other

        intent = new Intent(Constants.INTENT_START_FILTER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, FROM_LISTVIEW, intent, 0);
        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);

        intent = new Intent(Constants.INTENT_START_FILTER);
        putFilterExtras(intent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(context, FROM_WIDGETS_START+widgetId, intent, 0);
        view.setOnClickPendingIntent(R.id.title,pendingIntent);

        intent = new Intent(context, AddTask.class);
        putFilterExtras(intent, preferences, widgetId);
        pendingIntent = PendingIntent.getActivity(
                context, FROM_WIDGETS_START+widgetId, intent, 0);
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
