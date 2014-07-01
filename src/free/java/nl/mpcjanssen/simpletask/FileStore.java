package nl.mpcjanssen.simpletask;

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
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxSyncStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;

/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private DbxFileSystem.PathListener m_observer;
    private DbxAccountManager mDbxAcctMgr;
    private Context mCtx;
    private DbxFileSystem mDbxFs;
    private DbxFileSystem.SyncStatusListener m_syncstatus;
    String activePath;
    private ArrayList<String> mLines;

    public FileStore( Context ctx) {
        mCtx = ctx;
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
        new AsyncTask<String, Void, ArrayList<String>>() {

            @Override
            protected ArrayList<String> doInBackground(String... params) {
                String path = params[0];
                activePath = path;
                ArrayList<String> results;
                DbxFile openFile = openDbFile(path);
                results =  syncGetLines(openFile);
                openFile.close();
                return results;
            }
            @Override
            protected void onPostExecute(ArrayList<String> results) {
                mLines = new ArrayList<String>();
                mLines.addAll(results);
                // Trigger update
                syncInProgress(false);
            }
        }.execute(path);
        return new ArrayList<String>();
    }

    private DbxFile openDbFile(String path) {
        DbxFile dbFile = null;
        DbxFileSystem fs = getDbxFS();
        if (fs == null) {
            return null;
        }
        try {
            fs.awaitFirstSync();
            DbxPath dbPath = new DbxPath(path);
            if (fs.exists(dbPath)) {
                dbFile = fs.open(dbPath);
            } else {
                dbFile = fs.create(dbPath);
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return dbFile;
    }

    private ArrayList<String> syncGetLines(DbxFile dbFile) {
        ArrayList<String> result = new ArrayList<String>();
        DbxFileSystem fs = getDbxFS();
        if (!isAuthenticated() || fs == null || dbFile == null) {
            return result;
        }
        try {
            dbFile.update();
            String contents =  dbFile.readString();
            for (String line : contents.split("\r\n|\r|\n")) {
                result.add(line);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    @Override
    public void append(String path, List<String> lines) {
        append(path, "\r\n" + Util.join(lines, "\r\n").trim());
    }

    private void append(String path, String tasks) {
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
                        openFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(path, tasks);
        }
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
            m_syncstatus = new DbxFileSystem.SyncStatusListener() {

                @Override
                public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
                    DbxSyncStatus status;
                    try {
                        status = dbxFileSystem.getSyncStatus();
                        if (!status.anyInProgress() || status.anyFailure() != null) {
                            mLines = null;
                            get(path);
                        } else {
                            syncInProgress(true);
                        }
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                }
            };
            mDbxFs.addSyncStatusListener(m_syncstatus);
            m_observer = new DbxFileSystem.PathListener() {
                @Override
                public void onPathChange(DbxFileSystem dbxFileSystem, DbxPath dbxPath, Mode mode) {
                    //
                }
            };
            mDbxFs.addPathListener(m_observer, new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
        }
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
            mDbxFs.removePathListener(m_observer,new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
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
                        openFile.writeString(Util.join(contents, "\r\n"));
                        openFile.close();
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
                        dbFile.writeString(Util.join(contents, "\r\n"));
                        dbFile.close();
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
    public int getType() {
        return Constants.STORE_DROPBOX;
    }

    @Override
    public void move(final String sourcePath, final String targetPath, final ArrayList<String> strings) {

        new AsyncTask<String,Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                if (isAuthenticated() && getDbxFS() != null) {
                    try {
                        Log.v(TAG, "Moving lines from " + sourcePath + " to " + targetPath);
                        DbxFile destFile = openDbFile(targetPath);
                        ArrayList<String> contents = new ArrayList<String>();
                        destFile.appendString("\r\n"+Util.join(strings, "\r\n"));
                        destFile.close();
                        DbxFile srcFile = openDbFile(sourcePath);
                        contents.addAll(syncGetLines(srcFile));
                        contents.removeAll(strings);
                        srcFile.writeString(Util.join(contents, "\r\n"));
                        srcFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new TodoException("Dropbox", e);
                    }
                }
                return null;
            }
        }.execute();
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
            createFileDialog().show();
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
