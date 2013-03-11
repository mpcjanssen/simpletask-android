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

import com.dropbox.sync.android.DbxAccountManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import nl.mpcjanssen.todotxtholo.util.Util;


public class LoginScreen extends Activity {

    final static String TAG = LoginScreen.class.getSimpleName();

    private TodoApplication m_app;

	private DbxAccountManager dbxAcctMgr;
    
    static final int REQUEST_LINK_TO_DBX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = (TodoApplication)getApplication();
        setContentView(R.layout.login);
        Button m_LoginButton = (Button) findViewById(R.id.login);
        m_LoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	linkToDropBox();
            }
        });
    }
    
    public void linkToDropBox () {
    		m_app.getDbxAcctMgr().startLink(this, REQUEST_LINK_TO_DBX);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                m_app.initTaskBag();
                switchToTodolist();
            } else {
                // ... Link failed or was cancelled by the user.
            }            
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    
    }
    private void switchToTodolist() {
        Intent intent = new Intent(LoginScreen.this, TodoTxtTouch.class);
        startActivity(intent);
        finish();
    }

}
