package nl.mpcjanssen.simpletask.dao

import de.greenrobot.dao.converter.PropertyConverter
import nl.mpcjanssen.simpletask.task.Task

class TaskDaoConverter : PropertyConverter<Task, String> {
    override fun convertToEntityProperty(databaseValue: String?): Task? {
        databaseValue?.let {
            return Task(databaseValue)
        }
        return null
    }

    override fun convertToDatabaseValue(entityProperty: Task?): String? {
        return entityProperty?.text
    }
}