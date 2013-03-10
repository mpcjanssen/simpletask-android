/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.todotxtholo;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Preferences extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    
    
    
	@Override
	public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen, Preference preference) {
		if(preference.getKey().equals("archive_now")) {
			Log.v("PREFERENCES", "Archiving completed items from preferences");
			Intent i = new Intent();
			i.setAction(Constants.INTENT_ACTION_ARCHIVE);
			getApplicationContext().sendBroadcast(i);
			finish();
		} else if(preference.getKey().equals("logout_dropbox")) {
			Log.v("PREFERENCES", "Logging out from Dropbox");
			Intent i = new Intent();
			i.setAction(Constants.INTENT_ACTION_LOGOUT);
			getApplicationContext().sendBroadcast(i);
			finish();		
		}
		return true;
	}
}
