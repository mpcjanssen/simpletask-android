package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.SharedPreferences
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.log
import java.util.*

class InternalStorage(val ctx: Context, val filename: String, val eol: String) {

    val prefs : SharedPreferences
    init {
        prefs = ctx.getSharedPreferences("storage", Context.MODE_PRIVATE)
    }
    internal fun save(Tasks: List<Task>, append: Boolean = false) {
        addLines(Tasks.map {it.text}, append)
    }

    fun read(): ArrayList<String> {
        log.info("TodoStorage", "Loading lines from storage..")
        val inputStream = ctx.openFileInput(filename)
        val reader = inputStream.bufferedReader()
        val result = ArrayList<String>()
        reader.forEachLine {
            result.add(it)
        }
        reader.close()
        log.info("TodoStorage", "Loading tasks from storage..done")
        return result
    }

    fun load(): ArrayList<Task> {
        log.info("TodoStorage", "Loading tasks from storage..")
        val inputStream = ctx.openFileInput(filename)
        val reader = inputStream.bufferedReader()
        val result = ArrayList<Task>()
        reader.forEachLine {
            result.add(Task(it))
        }
        reader.close()
        log.info("TodoStorage", "Loading tasks from storage..done")
        return result
    }

    internal fun addLines(lines: List<String>, append : Boolean) {
        log.info("TodoStorage", "Saving to internal file ${filename}, append: $append...")
        val mode = if (append) Context.MODE_APPEND else Context.MODE_PRIVATE
        val outputStream = ctx.openFileOutput(filename,mode)
        val writer = outputStream.bufferedWriter()
        val contentToSave = lines.joinToString(eol)
        if (append) {
            writer.write(eol)
        }
        writer.write(contentToSave)
        outputStream.close()
        log.info("TodoStorage", "Saving to internal file ${filename}, append: $append..done.")

    }
}