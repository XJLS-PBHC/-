package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Query("SELECT * FROM patients WHERE status IN ('WAITING', 'IN_PROGRESS') ORDER BY orderIndex ASC, id ASC")
    fun getQueuePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY orderIndex ASC, id ASC")
    fun getAllPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActivePatientFlow(): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE status = 'COMPLETED' ORDER BY endTime DESC, id DESC")
    fun getCompletedPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Query("SELECT MAX(orderIndex) FROM patients WHERE status IN ('WAITING', 'IN_PROGRESS')")
    suspend fun getMaxOrderIndex(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<PatientEntity>): List<Long>

    @Update
    suspend fun update(patient: PatientEntity)

    @Update
    suspend fun updateAll(patients: List<PatientEntity>)

    @Delete
    suspend fun delete(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM patients")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getCount(): Int
}
