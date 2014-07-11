package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.Override;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;

public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private final Context mCtx;
    private final LocalBroadcastManager bm;
    private String mEol;
    private FileObserver m_observer;
    private String activePath;
    private ArrayList<String> mLines;

    public FileStore(Context ctx, String eol) {
        mCtx = ctx;
        mEol = eol;
        m_observer = null;
        this.bm = LocalBroadcastManager.getInstance(ctx);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    private void notifyFileChanged() {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
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
    public List<String> get(final String path) {
        if (activePath != null && activePath.equals(path) && mLines!=null) {
            return mLines;
        }

        // Did we switch todo file?
        if (!path.equals(activePath)) {
            stopWatching(activePath);
            startWatching(path);
        }

        // Clear and reload cache
        mLines = null;
        bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        new AsyncTask<String, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(String... params) {
                return TaskIo.loadFromFile(new File(path));
            }
            @Override
            protected void onPostExecute(ArrayList<String> results) {
                // Trigger update
                activePath = path;
                mLines = results;
                bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                notifyFileChanged();
            }
        }.execute(path);
        return new ArrayList<String>();
    }

    @Override
    public void append(final String path, final List<String> lines) {
        updateStart(path);
        mLines.addAll(lines);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                append(path, mEol + Util.join(lines, mEol));
                return null;
            }

            @Override
            public void onPostExecute(Void v) {
                updateDone(path);
            }
        }.execute();
    }


    private void append(String path, String data) {
        TaskIo.writeToFile(data,new File(path),true);
    }

    @Override
    public void startLogin(Activity caller, int i) {

    }

    @Override
    public void startWatching(final String path) {
        Log.v(TAG,"Observer adding on: " + new File(path).getParentFile().getAbsolutePath());
        final String folder = new File(path).getParentFile().getAbsolutePath();
        final String filename = new File(path).getName();
        m_observer = new FileObserver(folder) {
            @Override
            public void onEvent(int event, String eventPath) {
                if (eventPath!=null && eventPath.equals(filename)) {
                    // Log.v(TAG, "Observer event: " + eventPath + ":" + event);
                    if (event == FileObserver.CLOSE_WRITE ||
                            event == FileObserver.MODIFY ||
                            event == FileObserver.MOVED_TO) {
                        bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
                        Log.v(TAG, "Observer " + path + " modified....sync done");
                        bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                        bm.sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
                        mLines=null;
                            }
                }
            }
        };
        m_observer.startWatching();
    }

    @Override
    public void stopWatching(String path) {
        if (m_observer!=null) {
            Log.v(TAG,"Observer removing on: " + path);
            m_observer.stopWatching();
            m_observer = null;
        }
    }


    @Override
    public void deauthenticate() {

    }

    @Override
    public void browseForNewFile(Activity act, String path,  FileSelectedListener listener) {
        FileDialog dialog = new FileDialog(act, new File(path).getParentFile(), true);
        dialog.addFileListener(listener);
        dialog.createFileDialog();
    }

    @Override
    public void update(final String mTodoName, final List<String> original, final List<String> updated) {
            final File file = new File(mTodoName);
            updateStart(mTodoName);
            new AsyncTask<Void,Void,Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ArrayList<String> lines = TaskIo.loadFromFile(file);
                    for (int i=0 ; i<original.size();i++) {
                        int index = lines.indexOf(original.get(i));
                        if (index!=-1) {
                            mLines.remove(index);
                            mLines.add(updated.get(i));
                            lines.remove(index);
                            lines.add(index,updated.get(i));
                        }
                    }
                    TaskIo.writeToFile(Util.join(lines, mEol), file, false);
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    updateDone(mTodoName);
                }
            }.execute();
    }


    private void updateStart(String path) {
        bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        stopWatching(path);
    }

    private void updateDone(String path) {
        bm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
        startWatching(path);
    }

    @Override
    public void delete(final String mTodoName, final List<String> strings) {
        updateStart(mTodoName);
        mLines.removeAll(strings);
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                File file = new File(mTodoName);
                final List<String> lines = TaskIo.loadFromFile(file);
                lines.removeAll(strings);
                TaskIo.writeToFile(Util.join(lines, mEol), file, false);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                updateDone(mTodoName);
            }
        }.execute();
    }

    @Override
    public int getType() {
        return Constants.STORE_SDCARD;
    }

    @Override
    public void move(String sourcePath, String targetPath, ArrayList<String> strings) {
        append(targetPath,strings);
        delete(sourcePath,strings);
    }

    public static String getDefaultPath() {
        return Environment.getExternalStorageDirectory() +"/data/nl.mpcjanssen.simpletask/todo.txt";
    }

    private class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;
        private boolean txtOnly;

        /**
         * @param activity
         * @param path
         */
        public FileDialog(Activity activity, File path, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
            if (!path.exists() || !path.isDirectory()) path = Environment.getExternalStorageDirectory();
            loadFileList(path);
        }

        /**
         * @return file dialog
         */
        public Dialog createFileDialog() {
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
            createFileDialog().show();
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
                            return !txtOnly ||  sel.isDirectory() || txtFile;
                        }
                    }
                };
                String[] fileList1 = path.list(filter);
                Collections.addAll(r, fileList1);
            }
            Collections.sort(r);
            fileList = r.toArray(new String[r.size()]);
        }

        private File getChosenFile(String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
            else return new File(currentPath, fileChosen);
        }
    }
}
