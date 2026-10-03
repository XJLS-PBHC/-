package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val inpatientNo: String = "",          // 住院号 / 门诊号
    val bodyParts: String,                // 检查部位，例如 "胸部CT, 腹部增强"
    val partCount: Int = 1,               // 部位数量
    val durationMinutes: Int = 15,        // 预计检查时长（默认每个部位15分钟）
    val appointmentTime: String = "",     // 预约时间，如 "10:30"
    val orderIndex: Int = 0,              // 队列排序序号
    val status: String = STATUS_WAITING,  // 状态：WAITING, IN_PROGRESS, COMPLETED, CANCELLED
    val startTime: Long? = null,          // 开始检查毫秒时间戳
    val endTime: Long? = null,            // 结束检查毫秒时间戳
    val actualDurationMinutes: Int? = null, // 实际检查用时（分）
    val savedMinutes: Int = 0,            // 该病人节省的时长（分）
    val isEmergency: Boolean = false,     // 是否加急/急诊
    val isInserted: Boolean = false,      // 是否为插号病人
    val notes: String = "",               // 备注
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_WAITING = "WAITING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"
    }

    val isWaiting: Boolean get() = status == STATUS_WAITING
    val isInProgress: Boolean get() = status == STATUS_IN_PROGRESS
    val isCompleted: Boolean get() = status == STATUS_COMPLETED
}
