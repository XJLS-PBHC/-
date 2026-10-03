package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_time_records")
data class SavedTimeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val patientName: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val savedMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
