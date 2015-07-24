package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.jsonextract.JsonExtractionException;
import com.dropbox.client2.jsonextract.JsonMap;
import com.dropbox.client2.jsonextract.JsonThing;
import com.dropbox.client2.session.AppKeyPair;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final FileChangeListener mFileChangedListerer;
    private final Context mCtx;
    private final Logger log;
    private  SharedPreferences mPrefs;
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
    boolean continuePolling = true;
    Thread onOnline;
    private boolean mIsLoading = false;
    private boolean mOnline;
    private Handler fileOperationsQueue;

    public FileStore(Context ctx, FileChangeListener fileChangedListener) {
        log = LoggerFactory.getLogger(this.getClass());
        mPrefs = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        mFileChangedListerer = fileChangedListener;
        // Set up the message queue
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                fileOperationsQueue = new Handler();
                Looper.loop();
            }
        });
        t.start();
        mCtx = ctx;
        mOnline = isOnline();
        setMDBApi();
    }

    private String loadContentsFromCache() {
        if (mPrefs == null) {
            log.warn("Couldn't load cache from preferences, mPrefs == null");
            return "";
        }
       return  mPrefs.getString(LOCAL_CONTENTS, "");
    }

    public void queueRunnable(final String description, Runnable r) {
        log.info("Handler: Queue " + description);
        while (fileOperationsQueue==null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fileOperationsQueue.post(r);
    }

    public boolean changesPending() {
        if (mPrefs == null) {
            return false;
        }
        return  mPrefs.getBoolean(LOCAL_CHANGES_PENDING, false);
    }

    private void saveToCache(@NonNull String fileName, @NonNull String rev, @NonNull String contents) {
        log.info("Storing file in cache rev: " + rev + " of file: " + fileName);
        if (mPrefs == null) {

            return ;
        }
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(LOCAL_NAME, fileName);
        edit.putString(LOCAL_CONTENTS, contents);
        edit.putString(LOCAL_REVISION, rev);
        edit.commit();
    }

    private void setChangesPending(boolean pending) {
        if (mPrefs == null) {
            return ;
        }
        if (pending) {
            log.info("Changes are pending");
        }
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putBoolean(LOCAL_CHANGES_PENDING, pending).commit();
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
            mDBApi = new DropboxAPI<>(session);
        }
    }

    @NonNull
    static public String getDefaultPath() {
        return "/todo/todo.txt";
    }


    private void startLongPoll ( @NonNull final String polledFile, final int backoffSeconds)  {
        pollingTask = new Thread(new Runnable() {
            @Override
            public void run() {
                int newBackoffSeconds = 0;
                long start_time = 0;
                if (!continuePolling) return;
                try {
                    //log.info("Long polling");

                    ArrayList<String> params = new ArrayList<>();
                    params.add("cursor");
                    params.add(latestCursor);
                    params.add("timeout");
                    params.add("120");
                    if (backoffSeconds!=0) {
                        log.info("Backing off for " + backoffSeconds + " seconds");
                        try {
                            Thread.sleep(backoffSeconds*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!continuePolling) return;
                    start_time = System.currentTimeMillis();
                    Object response = RESTUtility.request(RESTUtility.RequestMethod.GET, "api-notify.dropbox.com", "longpoll_delta", 1, params.toArray(new String[params.size()]), mDBApi.getSession());
                    log.info("Longpoll response: " + response.toString());
                    JsonThing result = new JsonThing(response);
                    JsonMap resultMap = result.expectMap();
                    boolean changes = resultMap.get("changes").expectBoolean();
                    JsonThing backoffThing = resultMap.getOrNull("backoff");

                    if (backoffThing!=null) {
                        newBackoffSeconds = backoffThing.expectInt32();
                    }
                    log.info("Longpoll ended, changes " + changes + " backoff " + newBackoffSeconds);
                    if (changes) {
                        DropboxAPI.DeltaPage<DropboxAPI.Entry> delta = mDBApi.delta(latestCursor);
                        latestCursor = delta.cursor;
                        for (DropboxAPI.DeltaEntry entry : delta.entries) {
                            if (entry.lcPath.equalsIgnoreCase(polledFile)) {
                                DropboxAPI.Entry newMetaData = (DropboxAPI.Entry) entry.metadata;
                                if (newMetaData!=null && getLocalTodoRev().equals(newMetaData.rev)) {
                                    log.info("Remote file " + polledFile + " changed, rev: " + newMetaData.rev  + " same as local rev, not reloading" );
                                } else {
                                    log.info("Remote file " + polledFile + " changed, rev: " + newMetaData.rev  + " reloading" );
                                    mFileChangedListerer.fileChanged(null);
                                }
                            }
                        }
                    }
                } catch (DropboxUnlinkedException e) {
                    log.info("Dropbox unlinked, no more polling");
                    continuePolling = false;
                } catch (DropboxIOException e) {
                    if (SocketTimeoutException.class.isAssignableFrom(e.getCause().getClass())) {
                        //log.info("Longpoll timed out, restarting");
                        if (!isOnline()) {
                            log.info("Device was not online, stopping polling");
                            continuePolling = false;
                        }
                        if (System.currentTimeMillis()-start_time<30*1000) {
                            log.info("Longpoll timed out to quick, backing off for 60 seconds");
                            newBackoffSeconds = 60;
                        }
                    } else {
                        log.info("Longpoll IO exception, restarting backing of {} seconds", 30,  e);
                        newBackoffSeconds = 30;
                    }
                } catch (JsonExtractionException e) {
                    log.info("Longpoll Json exception, restarting backing of {} seconds", 30, e);
                    newBackoffSeconds = 30;
                }  catch (DropboxException e) {
                    log.info("Longpoll Dropbox exception, restarting backing of {} seconds", 30, e);
                    newBackoffSeconds = 30;
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
                log.info("Error authenticating", e);
            }
        }
        return false;
    }

    @Override
    synchronized public List<Task> loadTasksFromFile(String path, final @Nullable BackupInterface backup, String eol) throws IOException {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        log.info( "Loading file fom dropnbox: " + path);
        mIsLoading = true;
        if (!isAuthenticated()) {
            mIsLoading = false;
            throw new IOException("Not authenticated");
        }

        List<Task> tasks = new ArrayList();

        if (!isOnline()) {
            log.info("Device is offline loading from cache");
            mIsLoading = false;
            return tasksFromCache();
        } else if (changesPending()) {
            log.info("Not loading, changes pending");
            Util.showToastLong(mCtx,"Saving pending changes");
            mIsLoading = false;
            tasks = tasksFromCache();
            saveTasksToFile(path, tasks, backup, eol);
            startWatching(path);
            return tasks;
        } else {
            try {
                DropboxAPI.DropboxInputStream openFileStream;
                DropboxAPI.DropboxFileInfo fileInfo;
                try {
                    openFileStream = mDBApi.getFileStream(path, null);
                    fileInfo = openFileStream.getFileInfo();
                    log.info("The file's rev is: " + fileInfo.getMetadata().rev);
                } catch (DropboxServerException e) {
                    log.debug("Dropbox server exception", e);
                    if (e.error == DropboxServerException._404_NOT_FOUND) {
                        log.info("File not found, creating file instead");
                        byte[] toStore = "".getBytes();
                        InputStream in = new ByteArrayInputStream(toStore);
                        mDBApi.putFile(path, in,
                                toStore.length, null, null);
                        openFileStream = mDBApi.getFileStream(path, null);
                        fileInfo = openFileStream.getFileInfo();
                    } else {
                        throw(e);
                    }
                }
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
        queueRunnable("startWatching", new Runnable() {
            @Override
            public void run() {

                if (!isOnline()) {
                    return;
                }
                continuePolling = true;
                if (pollingTask == null) {
                    log.info("Initializing slow polling thread");
                    try {
                        log.info("Finding latest cursor");
                        ArrayList<String> params = new ArrayList<>();
                        params.add("include_media_info");
                        params.add("false");
                        Object response = RESTUtility.request(RESTUtility.RequestMethod.POST, "api.dropbox.com", "delta/latest_cursor", 1, params.toArray(new String[params.size()]), mDBApi.getSession());
                        log.info("Longpoll latestcursor response: " + response.toString());
                        JsonThing result = new JsonThing(response);
                        JsonMap resultMap = result.expectMap();
                        latestCursor = resultMap.get("cursor").expectString();
                    } catch (DropboxException | JsonExtractionException e) {
                        e.printStackTrace();
                        log.error("Error reading polling cursor" + e);
                    }
                    log.info("Starting slow polling");
                    startLongPoll(path, 0);
                }
            }
        });
    }
    

    private void stopWatching() {
        queueRunnable("stopWatching", new Runnable() {
            @Override
            public void run() {
                continuePolling = false;
                pollingTask = null;
            }
        });
    }

    @Override
    public void logout() {
        if(mDBApi!=null) {
            mDBApi.getSession().unlink();
        }
        mPrefs.edit().remove(OAUTH2_TOKEN).commit();
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly) {
        if (!isOnline()) {
            Util.showToastLong(mCtx, "Device is offline");
            log.info("Device is offline, browse closed");
            return;
        }
        FileDialog dialog = new FileDialog(act, path , true);
        dialog.addFileListener(listener);
        dialog.createFileDialog(act, this);
    }

    @Override
    synchronized public void saveTasksToFile(final String path, final List<Task> tasks, @Nullable final BackupInterface backup, String eol) throws IOException {
        if (backup != null) {
            backup.backup(path, Util.joinTasks(tasks, "\n"));
        }
        List<String> lines = Util.tasksToString(tasks);
        final String contents = Util.join(lines, eol) + eol;

        if (!isOnline()) {
            saveToCache(path, getLocalTodoRev(), contents);
            setChangesPending(true);
            throw new IOException("Device is offline");
        }
        Runnable r = new Runnable() {

            @Override
            public void run() {
                String newName = path;
                String rev = getLocalTodoRev();
                try {
                    byte[]  toStore = contents.getBytes("UTF-8");
                    InputStream in = new ByteArrayInputStream(toStore);
                    log.info("Saving to file " + path);
                    DropboxAPI.Entry newEntry = mDBApi.putFile(path, in,
                            toStore.length, rev, null);
                    rev = newEntry.rev;
                    newName = newEntry.path;
                } catch (Exception e) {
                    e.printStackTrace();
                    // Changes are pending
                    setChangesPending(true);
                } finally {
                    // Always save to cache  so you wont lose changes
                    // if actual save fails (e.g. when the device is offline)
                    saveToCache(path, rev, contents);
                }
                // Saved success, nothing pending
                setChangesPending(false);

                if (!newName.equals(path)) {
                    // The file was written under another name
                    // Usually this means the was a conflict.
                    log.info("Filename was changed remotely. New name is: " + newName);
                    Util.showToastLong(mCtx, "Filename was changed remotely. New name is: " + newName);
                    mFileChangedListerer.fileChanged(newName);
                }
            }
        };
        queueRunnable("Save to file " + path, r);
    }

    @Override
    public void appendTaskToFile(final String path, final List<Task> tasks, final String eol) throws IOException {
        if (!isOnline()) {
            throw new IOException("Device is offline");
        }
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {

                    // First read file to append to
                    DropboxAPI.DropboxInputStream openFileStream = mDBApi.getFileStream(path, null);
                    DropboxAPI.DropboxFileInfo fileInfo = openFileStream.getFileInfo();
                    log.info( "The file's rev is: " + fileInfo.getMetadata().rev);
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
                    byte[] toStore = (Util.join(doneContents, eol) + eol).getBytes("UTF-8");
                    InputStream in = new ByteArrayInputStream(toStore);

                    mDBApi.putFile(path, in,
                            toStore.length, fileInfo.getMetadata().rev, null);
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        queueRunnable("Append to file " + path, r);
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
            log.info( "The file's rev is: " + fileInfo.getMetadata().rev);

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
                queueRunnable("onOnline", new Runnable() {
                    @Override
                    public void run() {
                        // Check if we are still online
                        log.info("Device went online, reloading in 5 seconds");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (isOnline()) {
                            continuePolling = true;
                            mFileChangedListerer.fileChanged(null);
                        } else {
                            log.info("Device no longer online skipping reload");
                        }
                    }
                });
            }

        } else if (!mOnline) {
            stopWatching();
        }
        if (prevOnline && !mOnline) {
            log.info("Device went offline");
        }
    }

    public static class FileDialog {
        private static final String PARENT_DIR = "..";
        private final Logger log;
        private String[] fileList;
        private HashMap<String,DropboxAPI.Entry> entryHash = new HashMap<>();
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<>();
        private final Activity activity;
        private boolean txtOnly;
        Dialog dialog;
        private Dialog loadingOverlay;


        /**
         * @param activity
         * @param pathName
         */
        public FileDialog(Activity activity, String pathName, boolean txtOnly) {
            log = LoggerFactory.getLogger(this.getClass());
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
                                    log.warn("Selected file " + chosenFile.getName());
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
            List<String> f = new ArrayList<>();
            List<String> d = new ArrayList<>();

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
                log.warn("Couldn't load list from " + path.getName() + " loading root instead.");
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
