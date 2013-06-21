package nl.mpcjanssen.simpletask;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletaskdonate.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MyAppWidgetProvider extends AppWidgetProvider {

	final static String TAG = MyAppWidgetProvider.class.getSimpleName();

    public static RemoteViews updateView(int widgetId, Context context) {

		RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.appwidget);
		// Set up the intent that starts the StackViewService, which will
        // provide the views for this collection.
        Intent intent = new Intent(context, AppWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the App Widget layout.
        view.setRemoteAdapter(R.id.widgetlv, intent);

        SharedPreferences preferences = context.getSharedPreferences("" + widgetId, 0);
        ArrayList<String> m_contexts = new ArrayList<String>();
        m_contexts.addAll(preferences.getStringSet(Constants.INTENT_CONTEXTS_FILTER, new HashSet<String>()));
        Log.v(TAG, "contexts: " + m_contexts);
        ArrayList<String> prio_strings = new ArrayList<String>();
        prio_strings.addAll(preferences.getStringSet(Constants.INTENT_PRIORITIES_FILTER, new HashSet<String>()));
        ArrayList<Priority> m_prios = Priority.toPriority(prio_strings);
        Log.v(TAG, "prios: " + m_prios);
        ArrayList<String> m_projects = new ArrayList<String>();
        m_projects.addAll(preferences.getStringSet(Constants.INTENT_PROJECTS_FILTER, new HashSet<String>()));
        Log.v(TAG, "tags: " + m_projects);
        boolean m_contextsNot = preferences.getBoolean(Constants.INTENT_CONTEXTS_FILTER_NOT, false);
        boolean m_projectsNot = preferences.getBoolean(Constants.INTENT_PROJECTS_FILTER_NOT, false);
        boolean m_priosNot = preferences.getBoolean(Constants.INTENT_PRIORITIES_FILTER_NOT, false);
        ArrayList<String> m_sorts = new ArrayList<String>();
        m_sorts.addAll(Arrays.asList(preferences.getString(Constants.INTENT_SORT_ORDER, "").split("\n")));
        String m_title = preferences.getString(Constants.INTENT_TITLE, "Simpletask");
        view.setTextViewText(R.id.title, m_title);

        Intent target = new Intent(Constants.INTENT_START_FILTER);
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER, Util.join(m_contexts, "\n"));
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
        target.putExtra(Constants.INTENT_PROJECTS_FILTER, Util.join(m_projects, "\n"));
        target.putExtra(Constants.INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER, Util.join(m_prios, "\n"));
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
        target.putExtra(Constants.INTENT_SORT_ORDER, Util.join(m_sorts,"\n"));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, target, 0);
        view.setOnClickPendingIntent(R.id.title, pendingIntent);

		// Create an pending to launch simpletask on click.
        // Details will be filled in getView
        intent = new Intent(context, Simpletask.class);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);

        intent = new Intent(context, AddTask.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        view.setOnClickPendingIntent(R.id.widgetadd,pi);

        return view;
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int i = 0; i < appWidgetIds.length; i++) {
            int widgetId = appWidgetIds[i];
			Log.v(TAG, "Updating widget:" + widgetId);
			RemoteViews views = updateView(widgetId, context);
			appWidgetManager.updateAppWidget(widgetId, views);

		}
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
