package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.jsonextract.JsonExtractionException;
import com.dropbox.client2.jsonextract.JsonMap;
import com.dropbox.client2.jsonextract.JsonThing;
import com.dropbox.client2.session.AppKeyPair;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private final FileChangeListener mFileChangedListerer;
    private final Context mCtx;
    private final SharedPreferences mPrefs;
    private String mEol;
    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private String mWatchedFile;
    private AsyncTask<Void, Void, Void> pollingTask;
    private String latestCursor;

    private static String LOCAL_CONTENTS = "localContents";
    private static String LOCAL_NAME = "localName";
    private static String LOCAL_REVISION = "localRev";
    private static String CACHE_PREFS = "dropboxMeta";


    private String loadContentsFromCache() {
        if (mPrefs == null) {
            return "";
        }
       return  mPrefs.getString(LOCAL_CONTENTS, "");
    }

    private void saveToCache(@NotNull DropboxAPI.Entry metaData, @NotNull String contents) {
        Log.v(TAG, "Storing rev: " + metaData.rev + " of file: " + metaData.fileName());
        if (mPrefs == null) {

            return ;
        }
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(LOCAL_NAME, metaData.fileName());
        edit.putString(LOCAL_CONTENTS, contents);
        edit.putString(LOCAL_REVISION, metaData.rev);
        edit.commit();
    }

    public FileStore(Context ctx, FileChangeListener fileChangedListener,  String eol) {
        mFileChangedListerer = fileChangedListener;
        mCtx = ctx;
        mEol = eol;
        setMDBApi();
        mPrefs = ctx.getSharedPreferences(CACHE_PREFS,Context.MODE_PRIVATE);
    }

    private void setMDBApi() {
        if (mDBApi == null) {
            String app_secret = mCtx.getString(R.string.dropbox_consumer_secret);
            String app_key = mCtx.getString(R.string.dropbox_consumer_key);
            app_key = app_key.replaceFirst("^db-", "");
            // And later in some initialization function:
            AppKeyPair appKeys = new AppKeyPair(app_key, app_secret);
            AndroidAuthSession session = new AndroidAuthSession(appKeys);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        }
    }

    @NotNull
    static public String getDefaultPath() {
        return "/todo/todo.txt";
    }

    @Override
    public boolean isAuthenticated() {
        if (mDBApi == null) {
            return false;
        }
        if (mDBApi.getSession().isLinked()) {
            return true;
        }
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                return true;
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
        return false;
    }

    @Override
    public TodoList loadTasksFromFile(String path, @Nullable TodoList.TodoListChanged todoListChanged, final @Nullable BackupInterface backup) throws IOException {
        Log.i(TAG, "Loading file fom dropnbox: " + path);
        if (!isAuthenticated()) {
            TodoList result = new TodoList(null);
            return result;
        }

        final TodoList todoList = new TodoList(todoListChanged);

        try {
            DropboxAPI.DropboxInputStream openFileStream = mDBApi.getFileStream(path, null);
            DropboxAPI.DropboxFileInfo fileInfo = openFileStream.getFileInfo();
            Log.i(TAG, "The file's rev is: " + fileInfo.getMetadata().rev);

            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileStream, "UTF-8"));
            String line;
            ArrayList<String> readFile = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                todoList.add(new Task(line));
                readFile.add(line);
            }
            openFileStream.close();
            String contents =  Util.join(readFile,"\n");
            backup.backup(path, contents);
            saveToCache(fileInfo.getMetadata(),contents);
            startWatching(path);
        } catch (DropboxException e) {
            // Couldn't download file use cached version
            e.printStackTrace();
            String contents = loadContentsFromCache();
            for (String line: contents.split("(\r\n|\r|\n)")) {
                todoList.add(new Task(line));
            }
        } ;

        return todoList;
    }



    @Override
    public void startLogin(Activity caller, int i) {
        // MyActivity below should be your activity class name
       mDBApi.getSession().startOAuth2Authentication(caller);
    }



    private void startWatching(final String path) {
        mWatchedFile = path;
        if (pollingTask == null) {
            Log.v(TAG, "Initializing slow polling thread");
            try {
                Log.v(TAG, "Finding latest cursor");
                ArrayList<String> params = new ArrayList<>();
                params.add("include_media_info");
                params.add("false");
                Object response = RESTUtility.request(RESTUtility.RequestMethod.POST, "api.dropbox.com", "delta/latest_cursor", 1, params.toArray(new String[0]), mDBApi.getSession());
                Log.v(TAG, "Longpoll latestcursor response: " + response.toString());
                JsonThing result = new JsonThing(response);
                JsonMap resultMap = result.expectMap();
                latestCursor = resultMap.get("cursor").expectString();
            } catch (DropboxException e) {
                e.printStackTrace();
                latestCursor = null;
            } catch (JsonExtractionException e) {
                latestCursor = null;
                e.printStackTrace();
            }
            startLongPoll();
        }
    }


    private void startLongPoll ()  {
        pollingTask = new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                try {
                    Log.v(TAG, "Long polling");
                    ArrayList<String> params = new ArrayList<>();
                    params.add("cursor");
                    params.add(latestCursor);
                    params.add("timeout");
                    params.add("120");
                    Object response = RESTUtility.request(RESTUtility.RequestMethod.GET, "api-notify.dropbox.com", "longpoll_delta", 1, params.toArray(new String[0]), mDBApi.getSession());
                    Log.v(TAG, "Longpoll response: " + response.toString());
                    JsonThing result = new JsonThing(response);
                    JsonMap resultMap = result.expectMap();
                    boolean changes = resultMap.get("changes").expectBoolean();
                    JsonThing backoff =  resultMap.getOrNull("backoff");
                    Log.v(TAG, "Longpoll ended, changes " + changes + " backoff " + backoff);
                    if (changes) {
                        DropboxAPI.DeltaPage<DropboxAPI.Entry> delta = mDBApi.delta(latestCursor);
                        latestCursor = delta.cursor;
                        for (DropboxAPI.DeltaEntry entry : delta.entries) {
                            if (entry.lcPath.equalsIgnoreCase(mWatchedFile)) {
                                Log.v (TAG, "File " + mWatchedFile + " changed, reloading");
                                mFileChangedListerer.fileChanged();
                                return null;
                            }
                        }
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                } catch (JsonExtractionException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void v) {
                startLongPoll();
            }
        }.execute();
    }
    

    private void stopWatching(String path) {
        if (pollingTask ==null || mDBApi == null) {
            return;
        }
        pollingTask.cancel(true);
        pollingTask = null;
    }

    @Override
    public void logout() {
        if(mDBApi!=null) {
            mDBApi.getSession().unlink();
        }
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly) {
        FileDialog dialog = new FileDialog(act, path , true);
        dialog.addFileListener(listener);
        dialog.createFileDialog(act, this);
    }

    @Override
    public void saveTasksToFile(String path, TodoList todoList , @Nullable  final BackupInterface backup) {
        //FIXME read only
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        if (backup!=null) {
            backup.backup(path, Util.joinTasks(todoList.getTasks(), "\n"));
        }
        stopWatching(path);
        startWatching(path);
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
    }

    @Override
    public void appendTaskToFile(String path, List<Task> tasks) throws IOException {
          //FIXME read only
    }


    @Override
    public void setEol(String eol) {
        mEol = eol;
    }

    @Override
    public void sync() {
        mFileChangedListerer.fileChanged();
    }

    @Override
    public String readFile(String path) throws IOException {
        if (!isAuthenticated()) {
            return "";
        }
        try {
            DropboxAPI.DropboxInputStream openFileStream = mDBApi.getFileStream(path, null);
            DropboxAPI.DropboxFileInfo fileInfo = openFileStream.getFileInfo();
            Log.i(TAG, "The file's rev is: " + fileInfo.getMetadata().rev);

            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileStream, "UTF-8"));
            String line;
            ArrayList<String> readFile = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                readFile.add(line);
            }
            openFileStream.close();
            return Util.join(readFile,"\n");
        } catch (DropboxException e) {
            throw (new IOException(e));
        }
    }

    @Override
    public boolean supportsSync() {
        return true;
    }

    @Override
    public int getType() {
        return Constants.STORE_DROPBOX;
    }

    public static class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private HashMap<String,DropboxAPI.Entry> entryHash = new HashMap<>();
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;
        private boolean txtOnly;
        Dialog dialog;
        private Dialog loadingOverlay;


        /**
         * @param activity
         * @param pathName
         */
        public FileDialog(Activity activity, String pathName, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
            currentPath = new File(pathName);

        }

        /**
         *
         */
        public void createFileDialog(final Activity act, final FileStoreInterface fs) {
            loadingOverlay = Util.showLoadingOverlay(act, null, true);

            final DropboxAPI<AndroidAuthSession> api = ((FileStore)fs).mDBApi;
            if (api==null) {
                return;
            }
            new AsyncTask<Void,Void, AlertDialog.Builder>() {
                @Override
                protected AlertDialog.Builder doInBackground(Void... params) {

                    loadFileList(act, api, currentPath);
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(currentPath.getPath());

                    builder.setItems(fileList, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String fileChosen = fileList[which];
                            if (fileChosen.equals(PARENT_DIR)) {
                                currentPath = new File(currentPath.getParent());
                                createFileDialog(act, fs);
                                return;
                            }
                            File chosenFile = getChosenFile(fileChosen);
                            Log.w("FileStore", "Selected file " + chosenFile.getName());
                            DropboxAPI.Entry entry = entryHash.get(fileChosen);
                            if (entry.isDir) {
                                currentPath = chosenFile;
                                createFileDialog(act, fs);
                            } else {
                                dialog.cancel();
                                dialog.dismiss();
                                fireFileSelectedEvent(chosenFile);
                            }
                        }
                    });
                    return builder;
                }

                @Override
                protected void onPostExecute(AlertDialog.Builder builder) {
                    loadingOverlay = Util.showLoadingOverlay(act, loadingOverlay, false);
                    if (dialog!=null) {
                        dialog.cancel();
                        dialog.dismiss();
                    }
                    dialog = builder.create();
                    dialog.show();
                }

            }.execute();
        }


        public void addFileListener(FileSelectedListener listener) {
            fileListenerList.add(listener);
        }


        private void fireFileSelectedEvent(final File file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private DropboxAPI.Entry getPathMetaData(DropboxAPI api, File path) throws DropboxException {
            if (api!=null) {
                return api.metadata(path.toString(), 0, null, true, null);
            } else {
                return null;
            }
        }

        private void loadFileList(Activity act, DropboxAPI<AndroidAuthSession> api, File path) {
            if (path==null) {
                path = new File("/");
            }

            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();

            try {
                DropboxAPI.Entry entries = getPathMetaData(api,path) ;
                entryHash.clear();
                if (!entries.isDir) return;
                if (!path.toString().equals("/")) {
                    d.add(PARENT_DIR);
                }
                for (DropboxAPI.Entry entry : entries.contents) {
                    if (entry.isDeleted) continue;
                    if (entry.isDir) {
                        d.add(entry.fileName());
                    } else {
                        f.add(entry.fileName());
                    }
                    entryHash.put(entry.fileName(), entry);
                }
            } catch (DropboxException e) {
                Log.w("FileStore", "Couldn't load list from " + path.getName() + " loading root instead.");
                loadFileList(act, api, null);
                return;
            }
            Collections.sort(d);
            Collections.sort(f);
            d.addAll(f);
            fileList = d.toArray(new String[d.size()]);
        }

        private File getChosenFile(String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
            else return new File(currentPath, fileChosen);
        }
    }
}
