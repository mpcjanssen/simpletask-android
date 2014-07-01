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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;

public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private final Context mCtx;
    private final LocalBroadcastManager bm;
    private FileObserver m_observer;

    public FileStore( Context ctx) {
        mCtx = ctx;
        m_observer = null;
        this.bm = LocalBroadcastManager.getInstance(ctx);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public ArrayList<String> get(String path) {
        try {
            return TaskIo.loadFromFile(new File(path), new ArrayList<String>());
        } catch (IOException e) {
            ArrayList<String> failed = new ArrayList<String>();
            return failed;
        }
    }

    @Override
    public void append(String path, List<String> lines) {
        append(path, "\r\n"+ Util.join(lines, "\r\n"));
    }


    private void append(String path, String data) {
        TaskIo.writeToFile(data,new File(path),true);
    }

    @Override
    public void startLogin(Activity caller, int i) {

    }

    @Override
    public void startWatching(final String path) {
        if (m_observer==null) {
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
                        }
                    }
                }
            };
        }
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
    public void update(String mTodoName, List<String> original, List<String> updated) {
        ArrayList<String> detectedEOL = new ArrayList<String>();
        try {
            File file = new File(mTodoName);
            ArrayList<String> lines = TaskIo.loadFromFile(file, detectedEOL);
            for (int i=0 ; i<original.size();i++) {
                int index = lines.indexOf(original.get(i));
                if (index!=-1) {
                    lines.remove(index);
                    lines.add(index,updated.get(i));
                }
            }
            String strEOL = detectedEOL.get(0);
            TaskIo.writeToFile(Util.join(lines, strEOL), file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String mTodoName, List<String> strings) {
        ArrayList<String> detectedEOL = new ArrayList<String>();
        try {
            File file = new File(mTodoName);
            ArrayList<String> lines = TaskIo.loadFromFile(file, detectedEOL);
            lines.removeAll(strings);
            String strEOL = detectedEOL.get(0);
            TaskIo.writeToFile(Util.join(lines, strEOL), file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
