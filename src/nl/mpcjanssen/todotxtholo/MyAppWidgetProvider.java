package nl.mpcjanssen.todotxtholo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import nl.mpcjanssen.todotxtholo.util.Util;

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
        
		// Create an Intent to launch ExampleActivity
        intent = new Intent(Constants.INTENT_START_FILTER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        view.setPendingIntentTemplate(R.id.widgetlv, pendingIntent);
        return view;
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		SharedPreferences preferences;
		for (int widgetId : appWidgetIds) {
			Log.v(TAG, "Updating widget:" + widgetId);
			
			RemoteViews views = updateView(widgetId, context);
			
            
			appWidgetManager.updateAppWidget(widgetId, views);			
		}
		// TODO Auto-generated method stub
		super.onUpdate(context, appWidgetManager, appWidgetIds);

	}

}
