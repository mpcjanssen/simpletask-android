package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.util.Log
import nl.mpcjanssen.simpletask.remote.IFileStore.Companion.PARENT_DIR
import nl.mpcjanssen.simpletask.remote.IFileStore.Companion.ROOT_DIR
import nl.mpcjanssen.simpletask.util.ListenerList
import nl.mpcjanssen.simpletask.util.runOnMainThread
import nl.mpcjanssen.simpletask.util.showLoadingOverlay
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File

class FileDialog {

    private val fileListenerList = ListenerList<FileSelectedListener>()
    private var loadingOverlay: Dialog? = null
    private var showingDialog: AlertDialog? = null

    fun createFileDialog(act: Activity, fileStore: IFileStore, startFolder: File, txtOnly: Boolean) {
        // Use an async task because we need to manage the UI
        Thread(Runnable {
            val unsortedFileList: List<FileEntry> = try {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, null, true)
                })
                fileStore.loadFileList(startFolder, txtOnly)
            } catch (e: Throwable) {
                Log.w(TAG, "Can't load fileList from ${startFolder.path}")
                if (startFolder.canonicalPath != ROOT_DIR) {
                    Log.w(TAG, "Trying root")
                    fileStore.loadFileList(File(ROOT_DIR), txtOnly)
                } else {
                    Log.e(TAG, "Can't load fileList from $ROOT_DIR), browser closed")
                    showToastLong(act, "Can't retrieve file list")
                    null
                }
            } finally {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, loadingOverlay, false)
                })
            } ?: return@Runnable
            Log.i(TAG, "File list from ${startFolder.path} loaded")
            val fileList = unsortedFileList.sortedWith(compareBy({ !it.isFolder }, { it.file.name })).toMutableList()

            if (startFolder.canonicalPath != ROOT_DIR) {
                fileList.add(0, FileEntry(File(PARENT_DIR), isFolder = true))
            }
            runOnMainThread(Runnable {
                val builder = AlertDialog.Builder(act)
                builder.setTitle(startFolder.canonicalPath)
                val namesList = fileList.map { it.file.path }.toTypedArray()
                builder.setItems(namesList, DialogInterface.OnClickListener { dialog, which ->
                    val fileNameChosen = namesList[which]
                    if (fileNameChosen == PARENT_DIR) {
                        createFileDialog(act, fileStore, startFolder.parentFile, txtOnly)
                        return@OnClickListener
                    }

                    val fileEntry = fileList.find { fileNameChosen == it.file.path }
                    if (fileEntry == null) {
                        Log.w(TAG, "Selected file was $fileEntry")
                        dialog.dismiss()
                        return@OnClickListener
                    }
                    Log.i(TAG, "Selected entry ${fileEntry.file.path}, folder: ${fileEntry.isFolder}")

                    val newPath = File(startFolder, fileEntry.file.path)
                    if (fileEntry.isFolder) {
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

    private fun fireFileSelectedEvent(file: File) {
        fileListenerList.fireEvent(object : ListenerList.FireHandler<FileSelectedListener> {
            override fun fireEvent(listener: FileSelectedListener) {
                listener.fileSelected(file)
            }
        })
    }


    companion object {
        const val TAG = "FileDialog"
        fun browseForNewFile(act: Activity, fileStore: FileStore, folder: File, listener: FileSelectedListener, txtOnly: Boolean) {
            if (!FileStore.isOnline) {
                showToastLong(act, "Device is offline")
                Log.i(TAG, "Device is offline, browser closed")
                return
            }
            val dialog = FileDialog()
            dialog.addFileListener(listener)
            try {
                dialog.createFileDialog(act, fileStore, folder, txtOnly)
            } catch (e: Exception) {
                Log.e(TAG, "Browsing for new file failed", e)
            }
        }
    }

    interface FileSelectedListener {
        fun fileSelected(file: File)
    }
}



