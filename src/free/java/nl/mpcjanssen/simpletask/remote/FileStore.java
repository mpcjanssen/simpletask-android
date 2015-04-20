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

import com.google.common.io.LineProcessor;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.util.TaskIo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
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
    DbxFile mWatchedFile;


    private DbxFileSystem mDbxFs;
    @Nullable
    private DbxFileSystem.SyncStatusListener m_syncstatus;


    private boolean m_isSyncing = true;
    private DbxFileSystem.PathListener mPathListener;

    public FileStore( Context ctx, String eol) {
        mCtx = ctx;
        mEol = eol;
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

    @Override
    public void loadTasksFromFile(final String path, final TaskCache taskCache) throws IOException {
        new AsyncTask<String, Void, Void> () {
            @Override
            protected Void doInBackground(String... params) {
                Log.v(TAG, "Loading file in background");
                try {
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
                    final DbxFileSystem fs = getDbxFS();
                    fs.syncNowAndWait();
                    DbxFile openFile = openDbFile(path);
                    if (openFile == null) {
                        return null;
                    }

                    Log.v(TAG, "Opening file " + path + " sync states latest?: " + openFile.getSyncStatus().isLatest);
                    int times = 0;
                    try {
                        while (!openFile.getSyncStatus().isLatest && times < 30) {
                            Log.v(TAG, "Sleeping for 1000ms, sync states latest?: " + openFile.getSyncStatus().isLatest);
                            Thread.sleep(1000);
                            times++;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    openFile.update();
                    FileInputStream stream = openFile.getReadStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    taskCache.startLoading();
                    int i = 0;
                    String line;
                    while ((line =  reader.readLine())!=null) {
                        taskCache.load(new Task(i, line));
                        i++;
                    }
                    openFile.close();
                    taskCache.endLoading();
                    startWatching(path);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
                } catch ( DbxException e ) {
                    Log.e(TAG, "Load from file failed: " + e.getCause());
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute(path);

    }

    @Nullable
    private DbxFile openDbFile(String path) throws DbxException {
        DbxFileSystem fs = getDbxFS();
        if (fs == null) {
            return null;
        }
        DbxPath dbPath = new DbxPath(path);
        if (fs.exists(dbPath)) {
            return fs.open(dbPath);
        } else {
            return fs.create(dbPath);
        }
    }

    @Override
    public void startLogin(Activity caller, int i) {
        mDbxAcctMgr.startLink(caller, 0);
    }

    private void startWatching(final String path) {
        if (isAuthenticated() && getDbxFS() != null) {
            mPathListener = new DbxFileSystem.PathListener() {
                @Override
                public void onPathChange(DbxFileSystem dbxFileSystem, DbxPath dbxPath, Mode mode) {
                    try {
                        DbxFile changedFile = dbxFileSystem.open(dbxPath);
                        DbxFileStatus changedFileStatus = changedFile.getNewerStatus();
                        changedFile.close();
                        if (changedFileStatus==null) {
                            Log.v(TAG, "File changed " + dbxPath.getName() + " but newerStatus == null, not refreshing");
                        } else {
                            Log.v(TAG, "File changed " + dbxPath.getName() + " synced: " + changedFileStatus.isCached);
                            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
                        }
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                }
            };
            mDbxFs.addPathListener(mPathListener, new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
        }
    }

    private void stopWatching(String path) {

        if (getDbxFS()==null || mPathListener == null) {
            return;
        }
        mDbxFs.removePathListener(mPathListener,new DbxPath(path), DbxFileSystem.PathListener.Mode.PATH_ONLY);
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
    public void saveTasksToFile(String path, TaskCache taskCache) {
        try {
            DbxFile outFile = openDbFile(path);
            outFile.writeString(Util.joinTasks(taskCache.getTasks(), mEol));
            outFile.close();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendTaskToFile(String path, List<Task> tasks) throws IOException {
        DbxFile outFile = openDbFile(path);
        outFile.appendString(Util.joinTasks(tasks, mEol));
        outFile.close();
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
    public void sync() {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
    }

    @Override
    public String readFile(String file) {
        if (file==null) {
            return "";
        }
        try {
            DbxFile openFile = mDbxFs.open(new DbxPath(file));
            int times = 0;
            while ( openFile.getSyncStatus()!=null &&
                    !openFile.getSyncStatus().isLatest &&
                    times < 30) {
                Log.v(TAG, "Sleeping for 1000ms, sync states latest?: " + openFile.getSyncStatus().isLatest);
                Thread.sleep(1000);
                times++;
            }
            openFile.update();
            String result =  openFile.readString();
            openFile.close();
            return result;
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
