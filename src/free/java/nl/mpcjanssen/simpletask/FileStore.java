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
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

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
    private String mTodoFileName;
    private DbxPath mTodoFile;

    private final String TAG = getClass().getName();
    private DbxFileSystem.PathListener m_observer;
    private String app_key;
    private String app_secret;
    private DbxAccountManager mDbxAcctMgr;
    private DbxFileSystem dbxFs;
    private Context mCtx;
    private ArrayList<String> lines;
    private LocalBroadcastManager bm;
    private Intent bmIntent;

    public FileStore( Context ctx, String todoFile) {
        this.init(ctx, todoFile);
    }

    @Override
    public void init (Context ctx, String todoFile) {
        mCtx = ctx;
        app_key = ctx.getString(R.string.dropbox_consumer_key);
        app_key = app_key.replaceFirst("^db-","");
        app_secret = ctx.getString(R.string.dropbox_consumer_secret);
        mDbxAcctMgr = DbxAccountManager.getInstance(ctx, app_key, app_secret);
        if (Strings.isEmptyOrNull(todoFile)) {
            mTodoFileName = "/todo/todo.txt";
            mTodoFile = new DbxPath(mTodoFileName);
        }
        lines = new ArrayList<String>();
        if (isAuthenticated()) {
            try {
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                dbxFs.awaitFirstSync();
                DbxFile file = dbxFs.open(mTodoFile);
                String contents = file.readString();
                for (String line :  contents.split("(\r\n|\r|\n)")) {
                    lines.add(line);
                }

                file.close();

            } catch (IOException e) {
                e.printStackTrace();
                throw new TodoException("Dropbox", e);
            }
        }
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
    public ArrayList<String> get(TaskBag.Preferences preferences) {
        ArrayList<String> result = new ArrayList<String>();
        for (String line: lines) {
            if (!line.trim().equals("")) {
                result.add(line);
            }
        }
        return result;
    }

    @Override
    public void store(String data) {
        if (isAuthenticated()) {
            try {
                stopWatching();
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                dbxFs.awaitFirstSync();
                DbxFile file = dbxFs.open(mTodoFile);
                file.writeString(data);
                file.close();
                init(mCtx,mTodoFileName);
                startWatching(bm, bmIntent);

            } catch (IOException e) {
                e.printStackTrace();
                throw new TodoException("Dropbox", e);
            }
        }
    }

    @Override
    public void append(String path, String data) {
        if (isAuthenticated()) {
            try {
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                dbxFs.awaitFirstSync();
                DbxFile file = dbxFs.open(new DbxPath(path));
                file.appendString(data);
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new TodoException("Dropbox", e);
            }
        }

    }

    @Override
    public void startLogin(Activity caller, int i) {
        mDbxAcctMgr.startLink(caller, 0);
    }

    @Override
    public void startWatching(final LocalBroadcastManager broadCastManager, final Intent intent) {
        this.bm = broadCastManager;
        this.bmIntent = intent;
        if (mDbxAcctMgr==null || mDbxAcctMgr.getLinkedAccount()==null) {
            return;
        }
        if (dbxFs==null) {
            try {
                Log.v("XXX", "" + mDbxAcctMgr.getLinkedAccount());
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                dbxFs.awaitFirstSync();
            } catch (DbxException e) {
                e.printStackTrace();
                return;
            }
        }
        m_observer = new DbxFileSystem.PathListener() {
            @Override
            public void onPathChange(DbxFileSystem dbxFileSystem, final DbxPath dbxPath, Mode mode) {
                Log.v (TAG, "Sync change detected on dropbox, reloading " + dbxPath.getName());
                dbxFileSystem.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
                    @Override
                    public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
                        try {
                            if(!dbxFileSystem.getSyncStatus().anyInProgress()) {
                                Log.v(TAG, "Sync done " + dbxPath.getName());
                                init(mCtx, mTodoFileName);
                                broadCastManager.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                                broadCastManager.sendBroadcast(intent);
                            } else {
                                broadCastManager.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
                            }
                        } catch (DbxException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        Log.v (TAG, "Listening for changes on " +  mTodoFile.getName());
        dbxFs.addPathListener(m_observer,mTodoFile, DbxFileSystem.PathListener.Mode.PATH_ONLY);
    }

    @Override
    public void stopWatching() {
        if (m_observer!=null) {
            dbxFs.removePathListener(m_observer,mTodoFile, DbxFileSystem.PathListener.Mode.PATH_ONLY);
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
    public void browseForNewFile(Activity act, FileSelectedListener listener) {
        FileDialog dialog = new FileDialog(act, mTodoFile.getParent(), true);
        dialog.addFileListener(listener);
        dialog.createFileDialog();
    }

    private class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private DbxPath currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private ListenerList<DirectorySelectedListener> dirListenerList = new ListenerList<DirectorySelectedListener>();
        private final Activity activity;
        private boolean txtOnly;

        /**
         * @param activity
         * @param path
         */
        public FileDialog(Activity activity, DbxPath path, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
            loadFileList(path.getParent());
        }

        /**
         * @return file dialog
         */
        public Dialog createFileDialog() {
            Dialog dialog = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(currentPath.getParent().getName());

            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String fileChosen = fileList[which];
                    DbxPath chosenFile = getChosenFile(fileChosen);
                    try {
                        if (dbxFs.getFileInfo(chosenFile).isFolder) {
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

        private void fireDirectorySelectedEvent(final File directory) {
            dirListenerList.fireEvent(new ListenerList.FireHandler<DirectorySelectedListener>() {
                public void fireEvent(DirectorySelectedListener listener) {
                    listener.directorySelected(directory);
                }
            });
        }

        private void loadFileList(DbxPath path) {
            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();
            if (path.getParent() != null) d.add(PARENT_DIR);
            try {
                for (DbxFileInfo fInfo : dbxFs.listFolder(path)) {
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
