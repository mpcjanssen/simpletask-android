package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.Logger;
import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getSimpleName();
    private final LocalBroadcastManager bm;
    private final FileChangeListener m_fileChangedListener;
    private Logger log;
    private TodoObserver m_observer;
    private boolean mIsLoading;
    private Handler fileOperationsQueue;

    public FileStore(Context ctx, FileChangeListener fileChangedListener) {
        log = Logger.INSTANCE;
        log.info(TAG, "onCreate");
        m_fileChangedListener = fileChangedListener;
        m_observer = null;
        this.bm = LocalBroadcastManager.getInstance(ctx);

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
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    public void queueRunnable(final String description, Runnable r) {
        log.info(TAG, "Handler: Queue " + description);
        while (fileOperationsQueue==null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fileOperationsQueue.post(r);
    }

    @Override
    synchronized public List<String> loadTasksFromFile(final String path,  @Nullable BackupInterface backup, String eol) {
        log.info(TAG, "Loading tasks from file: {}" + path);
        final CopyOnWriteArrayList<String> result= new CopyOnWriteArrayList<>();
        mIsLoading = true;
        try {
            ArrayList<String> completeFile = new ArrayList<>();
            for (String line : TaskIo.loadFromFile(new File(path))) {
                completeFile.add(line);
                result.add(line);
            }
            if (backup != null) {
                backup.backup(path, Util.join(completeFile, "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mIsLoading = false;
        }
        setWatching(path);
        return result;
    }

    @Override
    public void sync() {

    }

    @Override
    public String readFile(String file, FileReadListener fileRead) {
        log.info(TAG, "Reading file: {}"+ file);
        mIsLoading = true;
        String contents = "";
        try {
            contents = Util.join(TaskIo.loadFromFile(new File(file)), "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            mIsLoading = false;
        }
        if (fileRead != null) {
            fileRead.fileRead(contents);
        }
        return contents;
    }

    @Override
    public boolean supportsSync() {
        return false;
    }

    @Override
    public boolean changesPending() {
        return false;
    }

    @Override
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    public void startLogin(Activity caller, int i) {

    }

    synchronized TodoObserver getObserver() {
        return m_observer;
    }

    synchronized private void setWatching(final String path) {

       log.info(TAG, "Observer: adding {} " + new File(path).getParentFile().getAbsolutePath());
        TodoObserver obs = getObserver();
        if (obs != null && path.equals(obs.path)) {
            log.warn(TAG, "Observer: already watching: {}");
            return;
        } else if (obs != null) {
            log.warn(TAG, "Observer: already watching different path: {}"+  obs.path);
            obs.ignoreEvents(true);
            obs.stopWatching();
        }
        m_observer = new TodoObserver(path, m_fileChangedListener);
        log.info(TAG, "Observer: modifying done");
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean showTxt) {
        FileDialog dialog = new FileDialog(act, path, showTxt);
        dialog.addFileListener(listener);
        dialog.createFileDialog(act, this);
    }

    @Override
    synchronized public void saveTasksToFile(final String path, final List<String> lines, @Nullable final BackupInterface backup, final String eol) {
        log.info(TAG, "Saving tasks to file: {}" + path);
        if (backup != null) {
            backup.backup(path, Util.join(lines, "\n"));
        }
        final TodoObserver obs = getObserver();
        obs.ignoreEvents(true);

        queueRunnable("Save to file " + path, new Runnable() {
            @Override
            public void run() {
                try {
                    TaskIo.writeToFile(Util.join(lines, eol) + eol, new File(path), false);
                } catch (IOException e) {
                    e.printStackTrace();
                    bm.sendBroadcast(new Intent(Constants.BROADCAST_FILE_WRITE_FAILED));
                } finally {
                    obs.delayedStartListen(1000);
                }


            }
        });

    }

    @Override
    public void appendTaskToFile(final String path, final List<String> lines, final String eol) {
        log.info(TAG, "Appending tasks to file: " + path);
        final int size = lines.size();
        queueRunnable("Appending " + size + " tasks to " + path, new Runnable() {
            @Override
            public void run() {
                log.info(TAG, "Appending " + size + " tasks to " + path);
                try {
                    TaskIo.writeToFile(Util.join(lines, eol) + eol, new File(path), true);
                } catch (IOException e) {
                    e.printStackTrace();
                    bm.sendBroadcast(new Intent(Constants.BROADCAST_FILE_WRITE_FAILED));
                }
                bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
            }
        });
    }

    @Override
    public int getType() {
        return Constants.STORE_SDCARD;
    }

    public static String getDefaultPath(TodoApplication app) {
        return Environment.getExternalStorageDirectory() + "/data/nl.mpcjanssen.simpletask/todo.txt";
    }

    public static class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<>();
        private final Activity activity;
        private boolean txtOnly;

        /**
         * @param activity
         * @param pathName
         */
        public FileDialog(Activity activity, String pathName, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly = txtOnly;
            File path = new File(pathName);
            if (!path.exists() || !path.isDirectory()) path = Environment.getExternalStorageDirectory();
            loadFileList(path);
        }

        /**
         * @return file dialog
         */
        public Dialog createFileDialog(Context ctx, FileStoreInterface fs) {
            Dialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(currentPath.getPath());

            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String fileChosen = fileList[which];
                    File chosenFile = getChosenFile(fileChosen);
                    if (chosenFile.isDirectory()) {
                        loadFileList(chosenFile);
                        dialog.cancel();
                        dialog.dismiss();
                        showDialog();
                    } else fireFileSelectedEvent(chosenFile);
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
            createFileDialog(null, null).show();
        }

        private void fireFileSelectedEvent(final File file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private void loadFileList(File path) {
            this.currentPath = path;
            List<String> r = new ArrayList<String>();
            if (path.exists()) {
                if (path.getParentFile() != null) r.add(PARENT_DIR);
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        File sel = new File(dir, filename);
                        if (!sel.canRead()) return false;
                        else {
                            boolean txtFile = filename.toLowerCase(Locale.getDefault()).endsWith(".txt");
                            return !txtOnly || sel.isDirectory() || txtFile;
                        }
                    }
                };
                String[] fileList1 = path.list(filter);
                if (fileList1 != null) {
                    Collections.addAll(r, fileList1);
                } else {
                    // Fallback to root
                    r.add("/");
                }
            } else {
                // Fallback to root
                r.add("/");
            }
            Collections.sort(r);
            fileList = r.toArray(new String[r.size()]);
        }

        private File getChosenFile(String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
            else return new File(currentPath, fileChosen);
        }
    }

    @Override
    public void logout() {
    }

    private class TodoObserver extends FileObserver {

        private final String path;
        private final String fileName;
        private final FileChangeListener fileChangedListener;
        private final Logger log;
        private boolean ignoreEvents;
        private Handler handler;
        private Runnable delayedEnable = new Runnable() {
            @Override
            public void run() {
                log.info(TAG, "Observer: Delayed enabling events for: " + path);
                ignoreEvents(false);
            }
        };

        public TodoObserver(String path, FileChangeListener fileChanged) {
            super(new File(path).getParentFile().getAbsolutePath());
            this.log = Logger.INSTANCE;
            this.startWatching();
            this.fileName = new File(path).getName();
            log.info(TAG, "Observer: creating observer on: {}");
            this.path = path;
            this.ignoreEvents = false;
            this.fileChangedListener = fileChanged;
            this.handler = new Handler(Looper.getMainLooper());

        }

        public void ignoreEvents(boolean ignore) {
            log.info(TAG, "Observer: observing events on " + this.path + "? ignoreEvents: " + ignore);
            this.ignoreEvents = ignore;
        }

        @Override
        public void onEvent(int event, String eventPath) {
            if (eventPath != null && eventPath.equals(fileName)) {
                log.debug(TAG, "Observer event: " + path + ":" + event);
                if (event == FileObserver.CLOSE_WRITE ||
                        event == FileObserver.MODIFY ||
                        event == FileObserver.MOVED_TO) {
                    if (ignoreEvents) {
                        log.info(TAG, "Observer: ignored event on: " + path);
                    } else {
                        log.info(TAG, "File changed {}" + path);
                        fileChangedListener.fileChanged(path);
                    }
                }
            }

        }

        public void delayedStartListen(final int ms) {
            // Cancel any running timers
            handler.removeCallbacks(delayedEnable);
            // Reschedule
            log.info(TAG, "Observer: Adding delayed enabling to queue");
            handler.postDelayed(delayedEnable, ms);
        }
    }
}
