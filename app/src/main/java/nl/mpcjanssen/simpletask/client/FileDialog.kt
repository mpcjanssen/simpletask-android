package nl.mpcjanssen.simpletask.client

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.util.Log
import nl.mpcjanssen.simpletask.remote.IFileStorePlugin

import nl.mpcjanssen.simpletask.util.ListenerList
import nl.mpcjanssen.simpletask.util.runOnMainThread
import nl.mpcjanssen.simpletask.util.showLoadingOverlay
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File
import java.util.ArrayList

class FileDialog {
    private val PARENT_DIR = ".."
    private val fileListenerList = ListenerList<FileSelectedListener>()
    private var loadingOverlay: Dialog? = null
    private var showingDialog: AlertDialog? = null


    fun createFileDialog(act: Activity, fileStore: IFileStorePlugin, startPath: String, txtOnly: Boolean) {
        // Use an async task because we need to manage the UI
        Thread(Runnable {
            var root = false
            val filelist = ArrayList<String>()
            val folderlist = ArrayList<String>()
            try {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, null, true)
                })
                root = fileStore.loadFileList(startPath, txtOnly, folderlist, filelist)
            } catch (e: Throwable) {
                Log.w(TAG, "Can't load fileList from $startPath")
                if (!root) {
                    Log.w(TAG, "Trying root")
                    fileStore.loadFileList(null, txtOnly, folderlist, filelist)
                } else {
                    Log.e(TAG, "Can't load fileList from roor), browser closed")
                    showToastLong(act, "Can't retrieve file list")
                }
            } finally {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, loadingOverlay, false)
                })
            }
            Log.i(TAG, "File list from $startPath loaded")
            val entries = ArrayList<FileEntry>()
            if (!root) {
                entries.add(FileEntry(PARENT_DIR,true))
            }
            entries.addAll(folderlist.sorted().map { FileEntry(it,true) })
            entries.addAll(filelist.sorted().map { FileEntry(it,false) })

            runOnMainThread(Runnable {
                val builder = AlertDialog.Builder(act)
                builder.setTitle(startPath)
                val namesList = entries.map { it.path }.toTypedArray()
                builder.setItems(namesList, DialogInterface.OnClickListener { dialog, which ->
                    val fileNameChosen = namesList[which]
                    if (fileNameChosen == PARENT_DIR) {
                        createFileDialog(act, fileStore, File(startPath).parentFile.canonicalPath, txtOnly)
                        return@OnClickListener
                    }

                    val fileEntry = entries.find { fileNameChosen == it.path }
                    if (fileEntry == null) {
                        Log.w(TAG, "Selected file was $fileEntry")
                        dialog.dismiss()
                        return@OnClickListener
                    }
                    Log.i(TAG, "Selected entry ${fileEntry.path}, folder: ${fileEntry.folder}")

                    val newPath = File(startPath, fileEntry.path).path
                    if (fileEntry.folder) {
                        createFileDialog(act, fileStore, newPath, txtOnly)
                    } else {
                        fireFileSelectedEvent(newPath)
                    }
                })
                val dialog = showingDialog
                if (dialog != null && dialog.isShowing) {
                    dialog.cancel()
                    dialog.dismiss()
                }
                builder.create().show()
            })
        }).start()
    }

    fun addFileListener(listener: FileSelectedListener) {
        fileListenerList.add(listener)
    }

    private fun fireFileSelectedEvent(file: String) {
        fileListenerList.fireEvent(object : ListenerList.FireHandler<FileSelectedListener> {
            override fun fireEvent(listener: FileSelectedListener) {
                listener.fileSelected(file)
            }
        })
    }


    companion object {
        const val TAG = "FileDialog"
        fun browseForNewFile(act: Activity, fileStore: IFileStorePlugin, path: String, listener: FileSelectedListener, txtOnly: Boolean) {
            val dialog = FileDialog()
            dialog.addFileListener(listener)
            try {
                dialog.createFileDialog(act, fileStore, path, txtOnly)
            } catch (e: Exception) {
                Log.e(TAG, "Browsing for new file failed", e)
            }
        }
    }

    interface FileSelectedListener {
        fun fileSelected(file: String)
    }

    data class FileEntry(val path: String, val folder: Boolean)
}



