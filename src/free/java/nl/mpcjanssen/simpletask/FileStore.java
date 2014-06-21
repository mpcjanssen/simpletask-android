package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.TaskIo;

/**
 * Created by a156712 on 10-6-2014.
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private DbxFileSystem.PathListener m_observer;
    private String app_key;
    private String app_secret;
    private DbxAccountManager mDbxAcctMgr;
    private Context mCtx;
    private LocalBroadcastManager bm;
    private Intent bmIntent;
    private DbxFileSystem mDbxFs;

    public FileStore( Context ctx, LocalBroadcastManager broadCastManager, Intent intent) {
        mCtx = ctx;
        this.bm = broadCastManager;
        this.bmIntent = intent;
        app_key = ctx.getString(R.string.dropbox_consumer_key);
        app_key = app_key.replaceFirst("^db-","");
        app_secret = ctx.getString(R.string.dropbox_consumer_secret);
        mDbxAcctMgr = DbxAccountManager.getInstance(ctx, app_key, app_secret);
    }

    private DbxFileSystem getDbxFS () {
        if (mDbxFs!=null) {
            return mDbxFs;
        }
        if (isAuthenticated()) {
            try {
                this.mDbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                mDbxFs.awaitFirstSync();
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
        if(mDbxAcctMgr!=null) {
            return mDbxAcctMgr.hasLinkedAccount();
        } else {
            return false;
        }
    }

    @Override
    public ArrayList<String> get(String path, TaskBag.Preferences preferences) {
        ArrayList<String> result = new ArrayList<String>();
        DbxFileSystem fs = getDbxFS();
        if (isAuthenticated()==false || getDbxFS()==null) {
            return result;
        }
        try {
            DbxFile dbFile = getDbxFS().open(new DbxPath(path));
            String [] lines = dbFile.readString().split("\r|\n|\r\n");
            dbFile.close();
            for (String line: lines) {
                if (!line.trim().equals("")) {
                    result.add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void store(String path, String data) {
        if (isAuthenticated() && getDbxFS()!=null) {
            try {
                stopWatching(path);
                DbxFile file = mDbxFs.open(new DbxPath(path));
                file.writeString(data);
                file.close();
                startWatching(path);

            } catch (IOException e) {
                e.printStackTrace();
                throw new TodoException("Dropbox", e);
            }
        }
    }

    @Override
    public boolean append(String path, String data) {
        if (isAuthenticated() && getDbxFS()!=null ) {
            try {
                DbxPath dbxPath = new DbxPath(path);
                DbxFile file;
                if (mDbxFs.exists(dbxPath)) {
                    file = mDbxFs.open(dbxPath);
                } else {
                    file = mDbxFs.create(dbxPath);
                }
                file.appendString(data);
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;

    }

    @Override
    public void startLogin(Activity caller, int i) {
        mDbxAcctMgr.startLink(caller, 0);
    }

    @Override
    public void startWatching(String path) {
        if (isAuthenticated() && getDbxFS() != null) {

            m_observer = new DbxFileSystem.PathListener() {
                @Override
                public void onPathChange(DbxFileSystem dbxFileSystem, final DbxPath dbxPath, Mode mode) {
                    Log.v(TAG, "Sync change detected on dropbox, reloading " + dbxPath.getName());
                    boolean latest = false;
                    try {
                        DbxFile fInfo = dbxFileSystem.open(dbxPath);
                        latest = fInfo.getSyncStatus().isLatest;
                        fInfo.close();
                        Log.v(TAG, "File " + dbxPath.getName() + " changed. Latest? :" + latest);
                    } catch (DbxException e) {


                    }
                    // If we got triggered because of local changes we are already at latest versionm
                    if(latest) {
                        return;
                    }
                    dbxFileSystem.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
                        @Override
                        public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
                            try {
                                DbxSyncStatus status = dbxFileSystem.getSyncStatus();
                                if (status.anyInProgress()) {
                                    bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
                                } else {
                                    bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                                    bm.sendBroadcast(bmIntent);
                                    dbxFileSystem.removeSyncStatusListener(this);
                                }
                            } catch (DbxException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };
            mDbxFs.addPathListener(m_observer, new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
        }
    }

    @Override
    public void stopWatching(String path) {
        if (m_observer!=null && getDbxFS() != null) {
            mDbxFs.removePathListener(m_observer,new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
            m_observer = null;
        }
    }

    @Override
    public boolean supportsAuthentication() {
        return true;
    }

    @Override
    public void deauthenticate() {
        mDbxAcctMgr.unlink();
    }

    @Override
    public boolean isLocal() {
        return false;
    }


    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener) {
        FileDialog dialog = new FileDialog(act, new DbxPath(path), true);
        dialog.addFileListener(listener);
        dialog.createFileDialog();
    }

    private class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private DbxPath currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;
        private boolean txtOnly;

        /**
         * @param activity
         * @param path
         */
        public FileDialog(Activity activity, DbxPath path, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
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
            Dialog dialog = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            String title = currentPath.getName();
            if (Strings.isEmptyOrNull(title)) {
                title = "/";
            }
            builder.setTitle(currentPath.getName());

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
