package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTimeDao {

    @Query("SELECT * FROM saved_time_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SavedTimeRecord>>

    @Query("SELECT COALESCE(SUM(savedMinutes), 0) FROM saved_time_records")
    fun getTotalSavedMinutesFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(savedMinutes), 0) FROM saved_time_records")
    suspend fun getTotalSavedMinutes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SavedTimeRecord): Long

    @Query("DELETE FROM saved_time_records")
    suspend fun clearAll()
}
