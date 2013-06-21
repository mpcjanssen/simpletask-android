/**
 * This file is part of simpletask.
 *
 * LICENSE:
 *
 * Simpletask is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Simpletask.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>, Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013 Mark Janssen
 */
package nl.mpcjanssen.simpletask.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import nl.mpcjanssen.simpletaskdonate.R;
import nl.mpcjanssen.simpletask.TodoException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.util.*;


public class Util {

    private static String TAG = Util.class.getSimpleName();

    private static final int CONNECTION_TIMEOUT = 120000;

    private static final int SOCKET_TIMEOUT = 120000;

    private Util() {
    }

    public static HttpParams getTimeoutHttpParams() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        return params;
    }

    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.w(TAG, "Close stream exception", e);
            }
        }
    }

    public static InputStream getInputStreamFromUrl(String url)
            throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet(url);
        DefaultHttpClient client = new DefaultHttpClient(getTimeoutHttpParams());
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            Log.e(TAG, "Failed to get stream for: " + url);
            throw new IOException("Failed to get stream for: " + url);
        }
        return response.getEntity().getContent();
    }

    public static String fetchContent(String url)
            throws ClientProtocolException, IOException {
        InputStream input = getInputStreamFromUrl(url);
        try {
            int c;
            byte[] buffer = new byte[8192];
            StringBuilder sb = new StringBuilder();
            while ((c = input.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, c));
            }
            return sb.toString();
        } finally {
            closeStream(input);
        }
    }

    public static String readStream(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            int c;
            byte[] buffer = new byte[8192];
            StringBuilder sb = new StringBuilder();
            while ((c = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, c));
            }
            return sb.toString();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(is);
        }
        return null;
    }

    public static void writeFile(InputStream is, File file)
            throws ClientProtocolException, IOException {
        FileOutputStream os = new FileOutputStream(file);
        try {
            int c;
            byte[] buffer = new byte[8192];
            while ((c = is.read(buffer)) != -1) {
                os.write(buffer, 0, c);
            }
        } finally {
            closeStream(is);
            closeStream(os);
        }
    }

    public static void showToastLong(Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
    }

    public static void showToastShort(Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
    }

    public static void showToastShort(Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
    }

    public interface OnSingleChoiceDialogListener {
        void onClick(String selected);
    }

    public static Dialog createSingleChoiceDialog(Context cxt,
                                                  CharSequence[] keys, String[] values, int selected, Integer titleId,
                                                  Integer iconId, final OnSingleChoiceDialogListener listener) {

        assert(values.length == keys.length);
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        if (iconId != null) {
            builder.setIcon(iconId);
        }
        if (titleId != null) {
            builder.setTitle(titleId);
        }

        final String[] res = values;
        final int checkedItem = selected;

        builder.setSingleChoiceItems(keys, checkedItem, null);

        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int index = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        listener.onClick(res[index]);
                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    public interface OnMultiChoiceDialogListener {
        void onClick(boolean[] selected);
    }

    public static Dialog createMultiChoiceDialog(Context cxt,
                                                 CharSequence[] keys, boolean[] values, Integer titleId,
                                                 Integer iconId, final OnMultiChoiceDialogListener listener) {
        final boolean[] res;
        if (values == null) {
            res = new boolean[keys.length];
        } else {
            res = values;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        if (iconId != null) {
            builder.setIcon(iconId);
        }
        if (titleId != null) {
            builder.setTitle(titleId);
        }
        builder.setMultiChoiceItems(keys, values,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int whichButton, boolean isChecked) {
                        res[whichButton] = isChecked;
                    }
                });
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        listener.onClick(res);
                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    public static void showDialog(Context cxt, int titleid, int msgid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showDialog(Context cxt, int titleid, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showConfirmationDialog(Context cxt, int msgid,
                                              OnClickListener oklistener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        // builder.setTitle(cxt.getPackageName());
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showConfirmationDialog(Context cxt, int msgid,
                                              OnClickListener oklistener, int titleid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        // builder.setTitle(cxt.getPackageName());
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showDeleteConfirmationDialog(Context cxt,
                                                    OnClickListener oklistener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(R.string.delete_task_title);
        builder.setMessage(R.string.delete_task_message);
        builder.setPositiveButton(R.string.delete_task_confirm, oklistener);
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public interface InputDialogListener {
        void onClick(String input);
    }

    public interface LoginDialogListener {
        void onClick(String username, String password);
    }

    public static void createParentDirectory(File dest) throws TodoException {
        if (dest == null) {
            throw new TodoException("createParentDirectory: dest is null");
        }
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            createParentDirectory(dir);
        }
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Could not create dirs: " + dir.getAbsolutePath());
                throw new TodoException("Could not create dirs: "
                        + dir.getAbsolutePath());
            }
        }
    }

    public static void renameFile(File origFile, File newFile, boolean overwrite) {
        if (!origFile.exists()) {
            Log.e(TAG, "Error renaming file: " + origFile + " does not exist");
            throw new TodoException("Error renaming file: " + origFile
                    + " does not exist");
        }

        createParentDirectory(newFile);

        if (overwrite && newFile.exists()) {
            if (!newFile.delete()) {
                Log.e(TAG, "Error renaming file: failed to delete " + newFile);
                throw new TodoException(
                        "Error renaming file: failed to delete " + newFile);
            }
        }

        if (!origFile.renameTo(newFile)) {
            Log.e(TAG, "Error renaming " + origFile + " to " + newFile);
            throw new TodoException("Error renaming " + origFile + " to "
                    + newFile);
        }
    }

    public static ArrayAdapter<String> newSpinnerAdapter(Context cxt,
                                                         List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(cxt,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
        	return "";
        }
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static void setColor(SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<String>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(SpannableString ss, int color ,  List<String> items) {
        String data = ss.toString();
        for (String item : items) {
            int i = data.indexOf(item);
            if (i != -1) {
                ss.setSpan(new ForegroundColorSpan(color), i,
                        i + item.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static Date addWeeksToDate(Date date, int weeks) {
        Date newDate = new Date(date.getTime());

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(newDate);
        calendar.add(Calendar.DATE, weeks);
        newDate.setTime(calendar.getTime().getTime());
        return newDate;
    }

    public static Date addMonthsToDate(Date date, int months) {
        Date newDate = new Date(date.getTime());

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(newDate);
        calendar.add(Calendar.MONTH, months);
        newDate.setTime(calendar.getTime().getTime());
        return newDate;
    }
}
