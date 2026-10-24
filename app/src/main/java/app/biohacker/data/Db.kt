package app.biohacker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "reading")
data class ReadingRow(
    @PrimaryKey val day: Long,
    val sleep: Int,
    val energy: Int,
    val mood: Int,
    val focus: Int,
    val calm: Int,
    val at: Long,
)

@Entity(tableName = "mark", primaryKeys = ["day", "mod"])
data class MarkRow(val day: Long, val mod: String)

@Entity(tableName = "trial")
data class TrialRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mod: String,
    val start: Long,
    val days: Int,
    val status: String,
    val end: Long,
    val effect: Float?,
)

@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val k: String, val v: String)

@Dao
interface LabDao {
    @Query("SELECT * FROM reading ORDER BY day") suspend fun readings(): List<ReadingRow>
    @Query("SELECT * FROM mark ORDER BY day") suspend fun marks(): List<MarkRow>
    @Query("SELECT * FROM trial ORDER BY id") suspend fun trials(): List<TrialRow>
    @Query("SELECT * FROM meta") suspend fun meta(): List<MetaRow>

    @Upsert suspend fun reading(r: ReadingRow)
    @Upsert suspend fun mark(m: MarkRow)
    @Query("DELETE FROM mark WHERE day = :day AND mod = :mod") suspend fun unmark(day: Long, mod: String)
    @Insert suspend fun insert(t: TrialRow): Long
    @Upsert suspend fun trial(t: TrialRow)
    @Upsert suspend fun meta(m: List<MetaRow>)
}

@Database(entities = [ReadingRow::class, MarkRow::class, TrialRow::class, MetaRow::class], version = 1, exportSchema = true)
abstract class LabDb : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        fun create(c: Context): LabDb = Room.databaseBuilder(c, LabDb::class.java, "lab.db").build()
    }
}
