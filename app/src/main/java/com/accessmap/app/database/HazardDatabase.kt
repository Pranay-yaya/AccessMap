package com.accessmap.app.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class HazardType {
    CONSTRUCTION,
    LOCKED_GATE,
    BROKEN_ELEVATOR,
    BLOCKED_PATH,
    UNEVEN_PAVEMENT,
    AI_CONFIRMED_STAIRS,
    AI_CONFIRMED_OBSTACLE
}

@Entity(tableName = "hazards")
data class HazardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val type: HazardType,
    val description: String,
    val timestamp: Long,
    val isResolved: Boolean = false,
    val nodeId: String = "",
    val photoPath: String = "",
    val confirmations: Int = 1,
    val lastConfirmedAt: Long = timestamp
)

@Dao
interface HazardDao {
    @Query("SELECT * FROM hazards WHERE isResolved = 0 AND lastConfirmedAt >= :minTimestamp ORDER BY timestamp DESC")
    fun getActiveHazards(minTimestamp: Long = 0L): Flow<List<HazardEntity>>

    @Query("SELECT * FROM hazards WHERE isResolved = 0 AND lastConfirmedAt >= :minTimestamp ORDER BY timestamp DESC")
    suspend fun getActiveHazardsOnce(minTimestamp: Long = 0L): List<HazardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hazard: HazardEntity): Long

    @Update
    suspend fun update(hazard: HazardEntity)

    @Query("UPDATE hazards SET isResolved = 1 WHERE id = :hazardId")
    suspend fun markResolved(hazardId: Long)

    @Query("UPDATE hazards SET confirmations = confirmations + 1, lastConfirmedAt = :now WHERE id = :hazardId")
    suspend fun confirmStillThere(hazardId: Long, now: Long)

    @Query("DELETE FROM hazards WHERE isResolved = 0 AND lastConfirmedAt < :cutoff")
    suspend fun deleteExpired(cutoff: Long)

    @Delete
    suspend fun delete(hazard: HazardEntity)
}

class Converters {
    @TypeConverter
    fun fromHazardType(type: HazardType): String = type.name

    @TypeConverter
    fun toHazardType(value: String): HazardType =
        runCatching { HazardType.valueOf(value) }.getOrDefault(HazardType.BLOCKED_PATH)
}

@Database(entities = [HazardEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hazardDao(): HazardDao

    companion object {
        const val EXPIRY_MILLIS = 7L * 24 * 60 * 60 * 1000L // 7 days

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accessmap.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
