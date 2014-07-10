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

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxSyncStatus;
import com.google.common.io.CharStreams;

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
    private DbxFile.Listener m_observer;
    private DbxAccountManager mDbxAcctMgr;
    private Context mCtx;
    private DbxFileSystem mDbxFs;
    private DbxFileSystem.SyncStatusListener m_syncstatus;
    String activePath;
    private ArrayList<String> mLines;
    private boolean mReloadFile;
    DbxFile mDbxFile;

    public FileStore( Context ctx, String eol) {
        mCtx = ctx;
        mEol = eol;
        this.activePath = null;
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
    static public String getDefaultPath() {
        return "/todo/todo.txt";
    }

    @Override
    public boolean isAuthenticated() {
        return mDbxAcctMgr != null && mDbxAcctMgr.hasLinkedAccount();
    }

    @Override
    public ArrayList<String> get(String path) {
        if (activePath != null && activePath.equals(path) && mLines!=null) {
            return mLines;
        }

        // Clear and reload cache
        mLines = null;

        // Did we switch todo file?
        if (activePath!=null && !activePath.equals(path) && mDbxFile!=null) {
            mDbxFile.close();
            mDbxFile = null;
        }
        new AsyncTask<String, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(String... params) {
                String path = params[0];
                activePath = path;
                ArrayList<String> results;
                DbxFile openFile = openDbFile(path);
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
                notifyFileChanged();
                mLines = results;
            }
        }.execute(path);
        return new ArrayList<String>();
    }

    private synchronized DbxFile openDbFile(String path) {
        if (mDbxFile != null) {
            return mDbxFile;
        }
        DbxFileSystem fs = getDbxFS();
        if (fs == null) {
            return null;
        }
        try {
            // Try intial sync to prevent creating new
            // todo files because first sync wasn't done yet
            if(!fs.hasSynced()) {
                fs.awaitFirstSync();
            }
        } catch (DbxException e) {
            Log.v(TAG, "Initial sync failed, are you connected?");
            e.printStackTrace();
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

    private synchronized ArrayList<String> syncGetLines(DbxFile dbFile) {
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

    @Override
    public void startWatching(final String path) {
        if (isAuthenticated() && getDbxFS() != null) {
            if (m_syncstatus==null) {
                m_syncstatus = new DbxFileSystem.SyncStatusListener() {

                    @Override
                    public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
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
                    public void onFileChange(DbxFile dbxFile) {
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
                openDbFile(path).addListener(m_observer);
            }
        }
    }

    private void notifyFileChanged() {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
    }

    @Override
    public void stopWatching(String path) {
        if (getDbxFS()==null) {
            return;
        }
        if (m_syncstatus!=null) {
            mDbxFs.removeSyncStatusListener(m_syncstatus);
            m_syncstatus = null;
        }
        if (m_observer!=null) {
            mDbxFile.removeListener(m_observer);
            m_observer = null;
        }
    }

    @Override
    public void deauthenticate() {
        mDbxAcctMgr.unlink();
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener) {
        FileDialog dialog = new FileDialog(act, new DbxPath(path), true);
        dialog.addFileListener(listener);
        dialog.createFileDialog();
    }

    @Override
    public void append(String path, final List<String> lines) {
        if (isAuthenticated() && getDbxFS() != null) {
            new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... params) {
                    String path = params[0];
                    String data = params[1];
                    Log.v(TAG, "Saving " + path + "in background thread");
                    try {
                        DbxFile openFile = openDbFile(path);
                        openFile.appendString(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public void onPostExecute (Void v) {
                    mLines.addAll(lines);
                }
            }.execute(path, mEol + Util.join(lines, mEol).trim());
        }
    }

    @Override
    public void update(final String filename, final List<String> alOriginal, final List<String> alUpdated) {
        new AsyncTask<String,Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                if (isAuthenticated() && getDbxFS() != null) {
                    try {
                        DbxFile openFile = openDbFile(filename);
                        ArrayList<String> contents = new ArrayList<String>();
                        contents.addAll(syncGetLines(openFile));
                        for (int i=0 ; i<alOriginal.size();i++) {
                            int index = contents.indexOf(alOriginal.get(i));
                            if (index!=-1) {
                                contents.remove(index);
                                contents.add(index,alUpdated.get(i));
                            }
                        }
                        openFile.writeString(Util.join(contents, mEol));
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
    public void delete(final String mTodoName, final List<String> stringsToDelete) {

        new AsyncTask<String,Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                if (isAuthenticated() && getDbxFS() != null) {
                    try {
                        DbxFile dbFile = openDbFile(mTodoName);
                        ArrayList<String> contents = new ArrayList<String>();
                        contents.addAll(syncGetLines(dbFile));
                        contents.removeAll(stringsToDelete);
                        dbFile.writeString(Util.join(contents, mEol));
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new TodoException("Dropbox", e);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                mLines.removeAll(stringsToDelete);
            }
        }.execute();
    }

    @Override
    public void move(final String sourcePath, final String targetPath, final ArrayList<String> strings) {

        new AsyncTask<String,Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                DbxFileSystem fs  = getDbxFS();
                if (isAuthenticated() && getDbxFS() != null) {
                    try {
                        Log.v(TAG, "Moving lines from " + sourcePath + " to " + targetPath);
                        DbxPath dbPath = new DbxPath(targetPath);
                        DbxFile destFile;
                        if (fs.exists(dbPath)) {
                            destFile = fs.open(dbPath);
                        } else {
                            destFile = fs.create(dbPath);
                        }
                        ArrayList<String> contents = new ArrayList<String>();
                        destFile.appendString(mEol+Util.join(strings, mEol));
                        destFile.close();
                        DbxFile srcFile = openDbFile(sourcePath);
                        contents.addAll(syncGetLines(srcFile));
                        contents.removeAll(strings);
                        srcFile.writeString(Util.join(contents, mEol));
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new TodoException("Dropbox", e);
                    }
                }
                return null;
            }

            @Override
            public void onPostExecute (Void v) {
               mLines.removeAll(strings);
            }
        }.execute();
    }

    @Override
    public void setEol(String eol) {
        mEol = eol;
    }

    @Override
    public void invalidateCache() {
        mLines = null;
    }

    @Override
    public int getType() {
        return Constants.STORE_DROPBOX;
    }

    private class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private DbxPath currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;

        /**
         * @param activity  Activity to display the file dialog
         * @param path      File path to start the dialog at
         * @param txtOnly   Show only txt files. Not used for Dropbox
         */
        @SuppressWarnings({"UnusedDeclaration"})
        public FileDialog(Activity activity, DbxPath path, boolean txtOnly ) {
            this.activity = activity;
            this.currentPath = path;
            loadFileList(path.getParent());
        }

        /**
         * @return file dialog
         */
        public Dialog createFileDialog() {
            if (getDbxFS()==null) {
                return null;
            }
            Dialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            String title = currentPath.getName();
            if (Strings.isEmptyOrNull(title)) {
                title = "/";
            }
            builder.setTitle(title);

            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String fileChosen = fileList[which];
                    DbxPath chosenFile = getChosenFile(fileChosen);
                    try {
                        if (mDbxFs.getFileInfo(chosenFile).isFolder) {
                            loadFileList(chosenFile);
                            dialog.cancel();
                            dialog.dismiss();
                            showDialog();
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
        public void showDialog() {
            Dialog d = createFileDialog();
            if(this.activity.isFinishing()) {
                d.show();
            }
        }

        private void fireFileSelectedEvent(final DbxPath file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private void loadFileList(DbxPath path) {
            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();
            if (path != DbxPath.ROOT) d.add(PARENT_DIR);
            try {
                for (DbxFileInfo fInfo : mDbxFs.listFolder(path)) {
                    if (fInfo.isFolder) {
                        d.add(fInfo.path.getName());
                    } else {
                        f.add (fInfo.path.getName());
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

        private DbxPath getChosenFile(String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParent();
            else return new DbxPath(currentPath, fileChosen);
        }
    }
}
