package com.accessmap.app.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class HazardType { CONSTRUCTION, LOCKED_GATE, BROKEN_ELEVATOR, BLOCKED_PATH, UNEVEN_PAVEMENT }

@Entity(tableName = "hazards")
data class HazardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val type: HazardType,
    val description: String,
    val timestamp: Long,
    val isResolved: Boolean = false
)

@Dao
interface HazardDao {
    @Query("SELECT * FROM hazards WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveHazards(): Flow<List<HazardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hazard: HazardEntity): Long

    @Update
    suspend fun update(hazard: HazardEntity)

    @Query("UPDATE hazards SET isResolved = 1 WHERE id = :hazardId")
    suspend fun markResolved(hazardId: Long)

    @Delete
    suspend fun delete(hazard: HazardEntity)
}

class Converters {
    @TypeConverter
    fun fromHazardType(type: HazardType): String = type.name

    @TypeConverter
    fun toHazardType(value: String): HazardType = HazardType.valueOf(value)
}

@Database(entities = [HazardEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hazardDao(): HazardDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accessmap.db"
                ).build().also { INSTANCE = it }
            }
    }
}
