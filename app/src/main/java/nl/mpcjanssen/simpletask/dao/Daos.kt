package nl.mpcjanssen.simpletask.dao

import android.content.Context
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


    @Insert
    fun insertAll(vararg users: TodoFile)

    @Query ("DELETE from TodoFile where date < :timestamp")
    fun removeBefore(timestamp: Long)

    @Query ("DELETE from TodoFile")
    fun deleteAll()


}

@Database(entities = arrayOf(TodoFile::class), version = SCHEMA_VERSION)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoFileDao(): TodoFileDao
    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(it.applicationContext,
                AppDatabase::class.java, DB_FILE).fallbackToDestructiveMigration()
                .build()
    })
}


open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}


