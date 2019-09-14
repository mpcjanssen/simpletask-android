package nl.mpcjanssen.simpletask.dao

import androidx.room.*

const val SCHEMA_VERSION=1013
const val DB_FILE="TodoFiles_v1.db"

@Entity
data class TodoFile(
        @PrimaryKey var contents: String,
        @ColumnInfo var name: String,
        @ColumnInfo var date: Long
)

@Dao
interface TodoFileDao {
    @Query("SELECT * FROM TodoFile")
    fun getAll(): List<TodoFile>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(contents: TodoFile) : Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(contents: TodoFile)

    @Query ("DELETE from TodoFile where date < :timestamp")
    fun removeBefore(timestamp: Long)

    @Query ("DELETE from TodoFile")
    fun deleteAll()


}

@Database(entities = arrayOf(TodoFile::class), version = SCHEMA_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoFileDao(): TodoFileDao
}





