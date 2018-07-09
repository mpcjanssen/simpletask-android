package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import nl.mpcjanssen.simpletask.Logger
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

    fun createFileDialog(act: Activity, fileStore: IFileStore, startPath: String, txtOnly: Boolean) {
        // Use an async task because we need to manage the UI
        Thread(Runnable {
            val unsortedFileList: List<FileEntry> = try {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, null, true)
                })
                fileStore.loadFileList(startPath, txtOnly)
            } catch (e: Throwable) {
                Logger.warn(TAG, "Can't load fileList from $startPath")
                if (startPath != ROOT_DIR) {
                    Logger.warn(TAG, "Trying root")
                    fileStore.loadFileList(ROOT_DIR, txtOnly)
                } else {
                    Logger.error(TAG, "Can't load fileList from $ROOT_DIR), browser closed")
                    showToastLong(act, "Can't retrieve file list")
                    null
                }
            } finally {
                runOnMainThread(Runnable {
                    loadingOverlay = showLoadingOverlay(act, loadingOverlay, false)
                })
            } ?: return@Runnable
            Logger.info(TAG, "File list from $startPath loaded")
            val fileList = unsortedFileList.sortedWith(compareBy({ !it.isFolder }, { it.name })).toMutableList()

            if (startPath != ROOT_DIR) {
                fileList.add(0, FileEntry(PARENT_DIR, isFolder = true))
            }
            runOnMainThread(Runnable {
                val builder = AlertDialog.Builder(act)
                builder.setTitle(startPath)
                val namesList = fileList.map { it.name }.toTypedArray()
                builder.setItems(namesList, DialogInterface.OnClickListener { dialog, which ->
                    val fileNameChosen = namesList[which]
                    if (fileNameChosen == PARENT_DIR) {
                        createFileDialog(act, fileStore, File(startPath).parentFile.canonicalPath, txtOnly)
                        return@OnClickListener
                    }

                    val fileEntry = fileList.find { fileNameChosen == it.name }
                    if (fileEntry == null) {
                        Logger.warn(TAG, "Selected file was $fileEntry")
                        dialog.dismiss()
                        return@OnClickListener
                    }
                    Logger.info(TAG, "Selected entry ${fileEntry.name}, folder: ${fileEntry.isFolder}")

                    val newPath = File(startPath, fileEntry.name).path
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

    private fun fireFileSelectedEvent(file: String) {
        fileListenerList.fireEvent(object : ListenerList.FireHandler<FileSelectedListener> {
            override fun fireEvent(listener: FileSelectedListener) {
                listener.fileSelected(file)
            }
        })
    }


    companion object {
        const val TAG = "FileDialog"
        fun browseForNewFile(act: Activity, fileStore: FileStore, path: String, listener: FileSelectedListener, txtOnly: Boolean) {
            if (!FileStore.isOnline) {
                showToastLong(act, "Device is offline")
                Logger.info(TAG, "Device is offline, browser closed")
                return
            }
            val dialog = FileDialog()
            dialog.addFileListener(listener)
            try {
                dialog.createFileDialog(act, fileStore, path, txtOnly)
            } catch (e: Exception) {
                Logger.error(TAG, "Browsing for new file failed", e)
            }
        }
    }

    interface FileSelectedListener {
        fun fileSelected(file: String)
    }
}



