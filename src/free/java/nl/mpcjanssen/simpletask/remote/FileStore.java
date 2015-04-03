package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxSyncStatus;
import com.google.common.io.CharStreams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoException;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;

/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private String mEol;
    @Nullable
    private DbxFile.Listener m_observer;
    private DbxAccountManager mDbxAcctMgr;
    private Context mCtx;

    public DbxFileSystem getmDbxFs() {
        return mDbxFs;
    }

    private DbxFileSystem mDbxFs;
    @Nullable
    private DbxFileSystem.SyncStatusListener m_syncstatus;
    @Nullable
    String activePath;
    @Nullable
    private ArrayList<String> mLines;
    private boolean mReloadFile;
    @Nullable
    DbxFile mDbxFile;
    private boolean m_isSyncing = true;

    public FileStore( Context ctx, String eol) {
        mCtx = ctx;
        mEol = eol;
        this.activePath = null;
        syncInProgress(true);
        setDbxAcctMgr();
    }

    private void setDbxAcctMgr () {
        if (mDbxAcctMgr==null) {
            String app_secret = mCtx.getString(R.string.dropbox_consumer_secret);
            String app_key = mCtx.getString(R.string.dropbox_consumer_key);
            app_key = app_key.replaceFirst("^db-","");
            mDbxAcctMgr = DbxAccountManager.getInstance(mCtx, app_key, app_secret);
        }
    }

    @Nullable
    private DbxFileSystem getDbxFS () {
        if (mDbxFs!=null) {
            return mDbxFs;
        }
        if (isAuthenticated()) {
            try {
                this.mDbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                return mDbxFs;
            } catch (IOException e) {
                e.printStackTrace();
                throw new TodoException("Dropbox", e);
            }
        }
        return null;
    }
    @NotNull
    static public String getDefaultPath() {
        return "/todo/todo.txt";
    }

    @Override
    public boolean isAuthenticated() {
        return mDbxAcctMgr != null && mDbxAcctMgr.hasLinkedAccount();
    }

    private void initialSync(final DbxFileSystem fs) {
        syncInProgress(true);
        new AsyncTask<Void,Void,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                Log.v(TAG, "Initial sync in background");
                try {
                    fs.awaitFirstSync();
                } catch (DbxException e) {
                    Log.e(TAG,"First sync failed: " + e.getCause());
                    e.printStackTrace();
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            protected void onPostExecute(Boolean success) {
                Log.v(TAG, "Intial sync status" + success);
                if (success) {
                    notifyFileChanged();
                }
            }
        }.execute();


    }

    @Nullable
    @Override
    public ArrayList<String> get(final String path) {
        Log.v(TAG, "Getting contents of: " + path);
        if (!isAuthenticated()) {
            Log.v(TAG, "Not authenticated");
            return new ArrayList<String>();
        }
        DbxFileSystem fs = getDbxFS();
        if (fs==null) {
            return new ArrayList<String>();
        }
        try {
            if (!fs.hasSynced()) {
                initialSync(fs);
                return new ArrayList<String>();
            }
        } catch (DbxException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }
        startWatching(path);
        if (activePath != null && activePath.equals(path) && mLines!=null) {
            return mLines;
        }
        syncInProgress(true);

        // Clear and reload cache
        mLines = null;

        // Did we switch todo file?
        if (activePath!=null && !activePath.equals(path) && mDbxFile!=null) {
            mDbxFile.close();
            mDbxFile = null;
            stopWatching(activePath);
        }
        new AsyncTask<String, Void, ArrayList<String>>() {
            @Nullable
            @Override
            protected ArrayList<String> doInBackground(String... params) {
                syncInProgress(true);
                String path = params[0];
                activePath = path;
                ArrayList<String> results;
                DbxFile openFile = openDbFile(path);
                if (openFile==null) {
                    return null;
                }
                try {
                    openFile.update();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
                results =  syncGetLines(openFile);
                return results;
            }
            @Override
            protected void onPostExecute(ArrayList<String> results) {
                // Trigger update
                if (results!=null) {
                    syncInProgress(false);
                    notifyFileChanged();
                }
                mLines = results;
            }
        }.execute(path);
        return new ArrayList<String>();
    }


    @Nullable
    private synchronized DbxFile openDbFile(String path) {
        if (mDbxFile != null) {
            return mDbxFile;
        }
        DbxFileSystem fs = getDbxFS();
        if (fs == null) {
            return null;
        }
        try {
            DbxPath dbPath = new DbxPath(path);
            if (fs.exists(dbPath)) {
                mDbxFile = fs.open(dbPath);
            } else {
                mDbxFile = fs.create(dbPath);
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return mDbxFile;
    }

    @NotNull
    private synchronized ArrayList<String> syncGetLines(@Nullable DbxFile dbFile) {
        ArrayList<String> result = new ArrayList<String>();
        DbxFileSystem fs = getDbxFS();
        if (!isAuthenticated() || fs == null || dbFile == null) {
            return result;
        }
        try {
            try {
                dbFile.update();
            } catch (DbxException e) {
                Log.v(TAG, "Couldn't download latest" + e.toString());
            }
            FileInputStream stream = dbFile.getReadStream();
            result.addAll(CharStreams.readLines(new InputStreamReader(stream)));
            stream.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    private void syncInProgress(boolean inProgress) {
        m_isSyncing = inProgress;
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(mCtx);
        if (inProgress) {
            bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        } else {
            bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
        }
    }

    @Override
    public void startLogin(Activity caller, int i) {
        mDbxAcctMgr.startLink(caller, 0);
    }

    private void startWatching(final String path) {
        if (isAuthenticated() && getDbxFS() != null) {
            if (m_syncstatus==null) {
                m_syncstatus = new DbxFileSystem.SyncStatusListener() {

                    @Override
                    public void onSyncStatusChange(@NotNull DbxFileSystem dbxFileSystem) {
                        DbxSyncStatus status;
                        try {
                            status = dbxFileSystem.getSyncStatus();
                            Log.v(TAG, "Synchronizing: v " + status.download + " ^ " + status.upload);
                            if (!status.anyInProgress() || status.anyFailure() != null) {
                                Log.v(TAG, "Synchronizing done");
                                if (mReloadFile) {
                                    mLines = null;
                                    get(path);
                                }
                                syncInProgress(false);
                            } else {
                                syncInProgress(true);
                            }
                        } catch (DbxException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mDbxFs.addSyncStatusListener(m_syncstatus);
            }
            if (m_observer==null) {
                m_observer = new DbxFile.Listener() {
                    @Override
                    public void onFileChange(@NotNull DbxFile dbxFile) {
                        DbxFileStatus status;
                        try {
                            status = dbxFile.getSyncStatus();
                            Log.v(TAG, "Synchronizing path change: " + dbxFile.getPath().getName() + " latest: " + status.isLatest +
                                       status.bytesTransferred + "/" + status.bytesTotal);
                            mReloadFile = !status.isLatest;
                        } catch (DbxException e) {
                            e.printStackTrace();
                        }
                    }
                };
                DbxFile openFile = openDbFile(path);
                if (openFile!=null) {
                    Log.v(TAG, "Start watching: " + openFile.getPath().toString());
                    openFile.addListener(m_observer);
                }
            }
        }
    }

    private void notifyFileChanged() {
        Log.v(TAG, "File changed: " + activePath);
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
    }

    private void stopWatching(String path) {
        if (getDbxFS()==null) {
            return;
        }
        if (m_syncstatus!=null) {
            mDbxFs.removeSyncStatusListener(m_syncstatus);
            m_syncstatus = null;
        }
        if (m_observer!=null && mDbxFile!=null) {
            mDbxFile.removeListener(m_observer);
        }
        m_observer = null;
    }

    @Override
    public void deauthenticate() {
        mDbxAcctMgr.unlink();
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly) {
        FileDialog dialog = new FileDialog(act, path , true);
        dialog.addFileListener(listener);
        dialog.createFileDialog(mCtx, this);
    }

    @Override
    public void archive(String path, final List<String> lines) {
        final DbxFileSystem fs = getDbxFS();
        if (isAuthenticated() && fs != null) {
            new AsyncTask<String, Void, Void>() {
                @Nullable
                @Override
                protected Void doInBackground(String... params) {
                    String path = params[0];
                    String data = params[1];
                    Log.v(TAG, "Saving " + path + "in background thread");
                    try {
                        DbxPath dbPath = new DbxPath(path);
                        DbxFile openFile;
                        if (fs.exists(dbPath)) {
                            openFile = fs.open(dbPath);
                        } else {
                            openFile = fs.create(dbPath);
                        }
                        openFile.appendString(data);
                        openFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(path, Util.join(lines, mEol) + mEol);
        }
    }

    @Override
    public void modify(final String mTodoName, final List<String> original,
                       final List<String> updated,
                       final List<String> added,
                       final List<String> removed) {
        new AsyncTask<String, Void, Void>() {
            @Nullable
            @Override
            protected Void doInBackground(String... params) {
                if (isAuthenticated() && getDbxFS() != null) {
                    try {
                        final int numUpdated = original!=null ? updated.size() : 0;
                        int numAdded = added!=null ? added.size() : 0;
                        int numRemoved = removed!=null ? removed.size() : 0;
                        Log.v(TAG, "Modifying " + mTodoName
                                + " Updated: " + numUpdated
                                + ", added: " + numAdded
                                + ", removed: " + numRemoved);
                        DbxFile openFile = openDbFile(mTodoName);
                        if (openFile==null) {
                            Log.w(TAG, "Failed to open: " + mTodoName + " tasks not updated");
                            return null;
                        }
                        ArrayList<String> contents = new ArrayList<String>();
                        contents.addAll(syncGetLines(openFile));
                        if (original!=null) {
                            for (int i = 0; i < original.size(); i++) {
                                int index = contents.indexOf(original.get(i));
                                if (index != -1) {
                                    contents.remove(index);
                                    contents.add(index, updated.get(i));
                                }
                            }
                        }
                        if (added!=null) {
                            for (String item : added) {
                                contents.add(item);
                            }
                        }
                        if (removed!=null) {
                            for (String item : removed) {
                                contents.remove(item);
                            }
                        }
                        openFile.writeString(Util.join(contents, mEol)+mEol);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new TodoException("Dropbox", e);
                    }
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void setEol(String eol) {
        mEol = eol;
    }

    @Override
    public boolean isSyncing() {
        return m_isSyncing;
    }

    @Override
    public void invalidateCache() {
        mLines = null;
    }

    @Override
    public void sync() {
        DbxFileSystem fs = getDbxFS();
        if (fs!=null) {
            try {
                fs.syncNowAndWait();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String readFile(String file) {
        if (file==null) {
            return null;
        }
        try {
            DbxFile openFile = mDbxFs.open(new DbxPath(file));
            openFile.update();
            return openFile.readString();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
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
        private DbxPath currentPath;

        @NotNull
        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;

        /**
         * @param activity  Activity to display the file dialog
         * @param path      File path to start the dialog at
         * @param txtOnly   Show only txt files. Not used for Dropbox
         */
        public FileDialog(Activity activity, @NotNull String path, boolean txtOnly ) {
            this.activity = activity;
            this.currentPath = new DbxPath(path);
        }

        /**
         * @return file dialog
         */
        @Nullable
        public Dialog createFileDialog(final Context ctx, final FileStoreInterface fileStore) {
            final DbxFileSystem fs = ((FileStore)fileStore).getDbxFS();
            if (fs==null) {
                return null;
            }
            Dialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            String title = currentPath.getName();
            if (Strings.isEmptyOrNull(title)) {
                title = "/";
            }
            loadFileList(fs,currentPath);
            if (fileList==null) {
                Toast.makeText(ctx,"Awaiting first Dropbox Sync", Toast.LENGTH_LONG).show();
                return null;
            }
            builder.setTitle(title);

            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(@NotNull DialogInterface dialog, int which) {
                    String fileChosen = fileList[which];
                    DbxPath chosenFile = getChosenFile(fileChosen);
                    try {
                        if (fs.getFileInfo(chosenFile).isFolder) {
                            loadFileList(fs, chosenFile);
                            dialog.cancel();
                            dialog.dismiss();
                            showDialog(ctx,fileStore);
                        } else fireFileSelectedEvent(chosenFile);
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                }
            });
            dialog = builder.show();
            return dialog;
        }


        public void addFileListener(FileSelectedListener listener) {
            fileListenerList.add(listener);
        }

        /**
         * Show file dialog
         */
        public void showDialog(Context ctx, FileStoreInterface fs) {
            Dialog d = createFileDialog(ctx, fs);
            if(d!=null && !this.activity.isFinishing()) {
                d.show();
            }
        }

        private void fireFileSelectedEvent(@NotNull final DbxPath file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(@NotNull FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private void loadFileList(DbxFileSystem fs, DbxPath path) {
            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();
            if (path != DbxPath.ROOT) d.add(PARENT_DIR);

            try {
                if (!fs.hasSynced()) {
                    fileList = null ;
                    return;
                } else {
                    for (DbxFileInfo fInfo : fs.listFolder(path)) {
                        if (fInfo.isFolder) {
                            d.add(fInfo.path.getName());
                        } else {
                            f.add(fInfo.path.getName());
                        }
                    }
                }
            } catch (DbxException e) {
                e.printStackTrace();
            }

            Collections.sort(d);
            Collections.sort(f);
            d.addAll(f);
            fileList = d.toArray(new String[d.size()]);
        }

        private DbxPath getChosenFile(@NotNull String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParent();
            else return new DbxPath(currentPath, fileChosen);
        }
    }

    @Override
    public boolean initialSyncDone() {
        if (mDbxFs!=null) {
            try {
                return mDbxFs.hasSynced();
            } catch (DbxException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
