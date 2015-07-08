package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
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
import java.util.concurrent.FutureTask;


/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private final FileChangeListener mFileChangedListerer;
    private final Context mCtx;
    private  SharedPreferences mPrefs;
    private String mEol;
    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private static String LOCAL_CONTENTS = "localContents";
    private static String LOCAL_NAME = "localName";
    private static String LOCAL_CHANGES_PENDING = "localChangesPending";
    private static String LOCAL_REVISION = "localRev";
    private static String CACHE_PREFS = "dropboxMeta";
    private static String OAUTH2_TOKEN = "dropboxToken";
    private String latestCursor;
    private Thread pollingTask;
    private boolean ignoreNextEvent = false;
    boolean continuePolling = true;
    Thread onOnline;
    private boolean mIsLoading = false;
    private boolean mOnline;

    private String loadContentsFromCache() {
        if (mPrefs == null) {
            Log.w(TAG, "Couldn't load cache from preferences, mPrefs == null");
            return "";
        }
       return  mPrefs.getString(LOCAL_CONTENTS, "");
    }

    public boolean changesPending() {
        if (mPrefs == null) {
            return false;
        }
        return  mPrefs.getBoolean(LOCAL_CHANGES_PENDING, false);
    }

    private void saveToCache(@NotNull String fileName, @NotNull String rev, @NotNull String contents) {
        Log.v(TAG, "Storing file in cache rev: " + rev + " of file: " + fileName);
        if (mPrefs == null) {

            return ;
        }
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(LOCAL_NAME, fileName);
        edit.putString(LOCAL_CONTENTS, contents);
        edit.putString(LOCAL_REVISION, rev);
        edit.commit();
    }

    private void setChangesPending(@NotNull boolean pending) {
        if (mPrefs == null) {
            return ;
        }
        if (pending) {
            Log.v(TAG, "Changes are pending");
        }
        SharedPreferences.Editor edit = mPrefs.edit();
        mPrefs.edit().putBoolean(LOCAL_CHANGES_PENDING, pending).commit();
    }

    public FileStore(Context ctx, FileChangeListener fileChangedListener,  String eol) {
        mPrefs = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        mFileChangedListerer = fileChangedListener;
        mCtx = ctx;
        mEol = eol;
        mOnline = isOnline();
        setMDBApi();
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private String getLocalTodoRev () {
        if (mPrefs==null) {
            return null;
        }
        return mPrefs.getString(LOCAL_REVISION, null);
    }

    private void setMDBApi() {
        if (mDBApi == null) {
            String app_secret = mCtx.getString(R.string.dropbox_consumer_secret);
            String app_key = mCtx.getString(R.string.dropbox_consumer_key);
            app_key = app_key.replaceFirst("^db-", "");
            // And later in some initialization function:
            AppKeyPair appKeys = new AppKeyPair(app_key, app_secret);
            String savedAuth = mPrefs.getString(OAUTH2_TOKEN, null);
            AndroidAuthSession session = new AndroidAuthSession(appKeys, savedAuth);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        }
    }

    @NotNull
    static public String getDefaultPath() {
        return "/todo/todo.txt";
    }


    private void startLongPoll ( @NotNull final String polledFile, @NotNull final int backoffSeconds)  {
        pollingTask = new Thread(new Runnable() {
            @Override
            public void run() {
                int newBackoffSeconds = 0;
                if (!continuePolling) return;
                try {
                    Log.v(TAG, "Long polling");

                    ArrayList<String> params = new ArrayList<>();
                    params.add("cursor");
                    params.add(latestCursor);
                    if (backoffSeconds!=0) {
                        Log.v(TAG, "Backing off for " + backoffSeconds + " seconds");
                        try {
                            Thread.sleep(backoffSeconds*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!continuePolling) return;

                    Object response = RESTUtility.request(RESTUtility.RequestMethod.GET, "api-notify.dropbox.com", "longpoll_delta", 1, params.toArray(new String[0]), mDBApi.getSession());
                    Log.v(TAG, "Longpoll response: " + response.toString());
                    JsonThing result = new JsonThing(response);
                    JsonMap resultMap = result.expectMap();
                    boolean changes = resultMap.get("changes").expectBoolean();
                    JsonThing backoffThing = resultMap.getOrNull("backoff");

                    if (backoffThing!=null) {
                        newBackoffSeconds = backoffThing.expectInt32();
                    }
                    Log.v(TAG, "Longpoll ended, changes " + changes + " backoff " + newBackoffSeconds);
                    if (changes) {
                        DropboxAPI.DeltaPage<DropboxAPI.Entry> delta = mDBApi.delta(latestCursor);
                        latestCursor = delta.cursor;
                        for (DropboxAPI.DeltaEntry entry : delta.entries) {
                            if (entry.lcPath.equalsIgnoreCase(polledFile)) {
                                Log.v(TAG, "File " + polledFile + " changed, reloading");
                                if (!ignoreNextEvent) {
                                    mFileChangedListerer.fileChanged(null);
                                    break;
                                } else {
                                    ignoreNextEvent = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (DropboxIOException e) {
                    Log.v(TAG, "Longpoll timed out, restarting");
                } catch (JsonExtractionException e) {
                    e.printStackTrace();
                } catch (DropboxUnlinkedException e) {
                    Log.v(TAG, "Dropbox unlinked, no more polling");
                    continuePolling = false;
                } catch (DropboxException e) {
                    e.printStackTrace();
                }
                startLongPoll(polledFile,newBackoffSeconds);
            }
        });
        pollingTask.start();
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
                mPrefs.edit().putString(OAUTH2_TOKEN, accessToken).commit();
                return true;
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
        return false;
    }

    @Override
    synchronized public List<Task> loadTasksFromFile(String path, final @Nullable BackupInterface backup) throws IOException {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        Log.i(TAG, "Loading file fom dropnbox: " + path);
        mIsLoading = true;
        if (!isAuthenticated()) {
            mIsLoading = false;
            throw new IOException("Not authenticated");
        }

        List<Task> tasks = new ArrayList();

        if (!isOnline()) {
            Log.v(TAG, "Device is offline loading from cache");
            mIsLoading = false;
            return tasksFromCache();
        } else if (changesPending()) {
            Log.v(TAG, "Not loading, changes pending");
            mIsLoading = false;
            startWatching(path);
            return tasksFromCache();
        } else {
            try {
                DropboxAPI.DropboxInputStream openFileStream = mDBApi.getFileStream(path, null);
                DropboxAPI.DropboxFileInfo fileInfo = openFileStream.getFileInfo();
                Log.i(TAG, "The file's rev is: " + fileInfo.getMetadata().rev);

                BufferedReader reader = new BufferedReader(new InputStreamReader(openFileStream, "UTF-8"));
                String line;
                ArrayList<String> readFile = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    tasks.add(new Task(line));
                    readFile.add(line);
                }
                openFileStream.close();
                String contents = Util.join(readFile, "\n");
                backup.backup(path, contents);
                saveToCache(fileInfo.getMetadata().fileName(), fileInfo.getMetadata().rev, contents);
                startWatching(path);
            } catch (DropboxException e) {
                // Couldn't download file use cached version
                e.printStackTrace();
                tasks.clear();
                tasks.addAll(tasksFromCache());
                Util.showToastLong(mCtx, "Drobox error, loading from cache");
            } catch (IOException e) {
                mIsLoading = false;
                throw new IOException(e);
            }
        }
        mIsLoading = false;
        return tasks;
    }

    private List<Task> tasksFromCache() {
        List <Task> result = new ArrayList<>();
        String contents = loadContentsFromCache();
        for (String line : contents.split("(\r\n|\r|\n)")) {
            result.add(new Task(line));
        }
        return result;
    }

    @Override
    public void startLogin(Activity caller, int i) {
        // MyActivity below should be your activity class name
       mDBApi.getSession().startOAuth2Authentication(caller);
    }



    private void startWatching(final String path) {
        if(!isOnline()) {
            return;
        }
        continuePolling = true;
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
            } catch (JsonExtractionException e) {
                e.printStackTrace();
            }
            startLongPoll(path,0);
        }
    }
    

    private void stopWatching() {
        continuePolling=false;
        pollingTask=null;
    }

    @Override
    public void logout() {
        if(mDBApi!=null) {
            mDBApi.getSession().unlink();
        }
        mPrefs.edit().remove(OAUTH2_TOKEN);
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly) {
        if (!isOnline()) {
            Util.showToastLong(mCtx, "Device is offline");
            Log.v(TAG, "Device is offline, browse closed");
            return;
        }
        FileDialog dialog = new FileDialog(act, path , true);
        dialog.addFileListener(listener);
        dialog.createFileDialog(act, this);
    }

    @Override
    synchronized public void saveTasksToFile(String path, List<Task> tasks, @Nullable final BackupInterface backup) throws IOException {
        if (backup != null) {
            backup.backup(path, Util.joinTasks(tasks, "\n"));
        }
        if (!isOnline()) {
            setChangesPending(true);
            throw new IOException("Device is offline");
        }
        String rev = getLocalTodoRev();
        String newName = path;
        List<String> lines = Util.tasksToString(tasks);
        String contents = Util.join(lines, mEol)+mEol;

        try {
            ignoreNextEvent= true;
            byte[] toStore = new byte[0];
            toStore = contents.getBytes("UTF-8");
            InputStream in = new ByteArrayInputStream(toStore);
            Log.v(TAG,"Saving to file " + path);
            DropboxAPI.Entry newEntry = mDBApi.putFile(path, in,
                    toStore.length, rev, null);
            rev  = newEntry.rev;
            newName = newEntry.path;
        } catch (Exception e) {
            e.printStackTrace();
            // Changes are pending
            setChangesPending(true);
            throw new IOException(e);
        } finally {
            // Always save to cache  so you wont lose changes
            // if actual save fails (e.g. when the device is offline)
            saveToCache(path,rev,contents);
        }
        // Saved success, nothing pending
        setChangesPending(false);

        if(!newName.equals(path)) {
            // The file was written under another name
            // Usually this means the was a conflict.
            Log.v(TAG, "Filename was changed remotely. New name is: " + newName);
            Util.showToastLong(mCtx, "Filename was changed remotely. New name is: " + newName);
            mFileChangedListerer.fileChanged(newName);
        }
    }

    @Override
    public void appendTaskToFile(String path, List<Task> tasks) throws IOException {
        if (!isOnline()) {
            throw new IOException("Device is offline");
        }
        try {

            // First read file to append to
            DropboxAPI.DropboxInputStream openFileStream =  mDBApi.getFileStream(path, null);
            DropboxAPI.DropboxFileInfo fileInfo = openFileStream.getFileInfo();
            Log.i(TAG, "The file's rev is: " + fileInfo.getMetadata().rev);
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileStream, "UTF-8"));
            String line;
            ArrayList<String> doneContents = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                doneContents.add(line);
            }
            openFileStream.close();

            // Then append
            for (Task t : tasks) {
                doneContents.add(t.inFileFormat());
            }
            byte[] toStore = (Util.join(doneContents, mEol)+mEol).getBytes("UTF-8");
            InputStream in = new ByteArrayInputStream(toStore);

            DropboxAPI.Entry newEntry = mDBApi.putFile(path, in,
                   toStore.length, fileInfo.getMetadata().rev, null);
            in.close();
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }


    @Override
    public void setEol(String eol) {
        mEol = eol;
    }

    @Override
    public void sync() {
        if (!isOnline()) {
            Util.showToastLong(mCtx, "Device is offline");
            return;
        }
        mFileChangedListerer.fileChanged(null);
    }

    @Override
    public String readFile(String path, @Nullable FileReadListener fileRead) throws IOException {
        if (!isAuthenticated() || !isOnline()) {
            return "";
        }
        mIsLoading = true;
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
            String contents = Util.join(readFile, "\n");
            if (fileRead!=null) {
                fileRead.fileRead(contents);
            }
            return contents;
        } catch (DropboxException e) {
            throw (new IOException(e));
        } finally {
            mIsLoading = false;
        }
    }

    @Override
    public boolean supportsSync() {
        return true;
    }

    @Override
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    public int getType() {
        return Constants.STORE_DROPBOX;
    }


    public void changedConnectionState(Intent intent) {
        boolean prevOnline = mOnline;
        mOnline = isOnline();
        if (!prevOnline && mOnline) {
            // Schedule a task to reload the file
            // Give some time to settle so we ignore rapid connectivity changes
            // Only schedule if another thread is not running
            if (onOnline==null || !onOnline.isAlive()) {
                onOnline = new  Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Check if we are still online
                        Log.v(TAG, "Device went online, reloading in 5 seconds");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (isOnline()) {
                            continuePolling = true;
                            mFileChangedListerer.fileChanged(null);
                        } else {
                            Log.v(TAG, "Device no longer online skipping reload");
                        }
                    }
                });
                onOnline.start();
            }

        } else if (!mOnline) {
            stopWatching();
        }
        if (prevOnline && !mOnline) {
            Log.v(TAG, "Device went offline");
        }
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

            // Use an asynctask because we need to manage the UI
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadFileList(act, api, currentPath);
                    loadingOverlay = Util.showLoadingOverlay(act, loadingOverlay, false);
                    Util.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
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
                            dialog = builder.create();
                            if (dialog != null) {
                                dialog.cancel();
                                dialog.dismiss();
                            }
                            dialog.show();
                        }
                    });
                }
            }).start();
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
