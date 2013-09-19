package nl.mpcjanssen.simpletask.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.remote.DropboxFile;

public class DropboxFileDialog {
    private static final String PARENT_DIR = "..";
    private final DropboxAPI api;
    private String currentPathString;
    private String[] fileList;
    private AlertDialog dialogShown;
    private final String TAG = getClass().getName();

    public interface FileSelectedListener {
        void fileSelected(File file);
    }


    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<DropboxFileDialog.FileSelectedListener>();
    private final Activity activity;

    /**
     * @param activity
     * @param path
     */
    public DropboxFileDialog(Activity activity, DropboxAPI api, File path) {
        this.activity = activity;
        this.api = api;
        this.currentPathString = path.getAbsolutePath();
    }

    /**
     * @return file dialog
     */
    public void createFileDialog() {
        loadMetadata(currentPathString);
    }


    public void addFileListener(FileSelectedListener listener) {
        fileListenerList.add(listener);
    }

    public void removeFileListener(FileSelectedListener listener) {
        fileListenerList.remove(listener);
    }



    private void fireFileSelectedEvent(final File file) {
        fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
            public void fireEvent(FileSelectedListener listener) {
                listener.fileSelected(file);
            }
        });
    }

    private void loadMetadata(String path) {
        DropboxMetadataLoader t = new DropboxMetadataLoader();
        t.execute(api, this, path);
    }

    public void loadMetadataDone(DropboxAPI.Entry pathEntry) {
        List<String> r = new ArrayList<String>();
        if (pathEntry==null) {
            Log.e(TAG, "Error downloading meta data" );
            Util.showToastLong(this.activity, "Error downloading file list from Dropbox, are you connected?");
            if (dialogShown!=null) {
                dialogShown.cancel();
                dialogShown.dismiss();
            }
            return;
        }
        currentPathString = pathEntry.path;
        if (pathEntry.isDir) {
            if (!pathEntry.path.equals("/")) r.add(PARENT_DIR);
            for (DropboxAPI.Entry child : pathEntry.contents) {
                if (child.isDeleted) continue;
                r.add(child.fileName());
            }
        } else {
            fireFileSelectedEvent(new File(pathEntry.path));
            return;
        }

        Collections.sort(r);
        fileList = (String[]) r.toArray(new String[]{});
        if (dialogShown!=null) {
            dialogShown.cancel();
            dialogShown.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(currentPathString);
        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String fileChosen = fileList[which];
                loadMetadata(currentPathString + "/" + fileChosen);
            }
        });
        dialogShown = builder.show();
    }
}


class DropboxMetadataLoader extends AsyncTask<Object, Integer, DropboxAPI.Entry> {

    private DropboxAPI api;
    private DropboxFileDialog dialog;
    private final String TAG = getClass().getName();

    @Override
    protected DropboxAPI.Entry doInBackground(Object... params) {
        DropboxAPI.Entry pathEntry = null;
        this.api = (DropboxAPI) params[0];
        this.dialog = (DropboxFileDialog) params[1];
        String path = (String) params[2];
        try {
            pathEntry = api.metadata(path, 0, null, true, null);
        } catch (DropboxException e) {
            try {
                pathEntry = api.metadata("/", 0, null, true, null);
            } catch (DropboxException e1) {
                Log.e(TAG, "Can't get metadata for: " + path);
                return null;
            }
        }
        return pathEntry;
    }

    protected void onPostExecute(DropboxAPI.Entry pathEntry) {
        // Update UI when doInBackground exection completes
        dialog.loadMetadataDone(pathEntry);
    }
}
