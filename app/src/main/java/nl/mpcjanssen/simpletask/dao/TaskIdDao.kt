package nl.mpcjanssen.simpletask.dao

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.task.Task
import android.content.Context.MODE_PRIVATE

object TaskIdDao {
    val sharedPrefs = TodoApplication.app.getSharedPreferences("todolist", MODE_PRIVATE)
    val editor = sharedPrefs.edit()

    fun get(taskText: String): String? {
        return sharedPrefs.getString(taskText, null)
    }

    fun add(tasks: List<Task>) {
        tasks.forEach {
            editor.putString(it.text, it.id)
        }
        editor.apply()
    }

    fun add(task: Task) {
        add(listOf(task))
    }

    fun remove(tasks: List<Task>) {
        tasks.forEach {
            editor.remove(it.text)
        }
        editor.apply()
    }

    fun remove(task: Task) {
        remove(listOf(task))
    }
}
