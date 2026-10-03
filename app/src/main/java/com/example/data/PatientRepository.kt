package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PatientRepository(
    private val patientDao: PatientDao,
    private val savedTimeDao: SavedTimeDao
) {
    val queuePatients: Flow<List<PatientEntity>> = patientDao.getQueuePatients()
    val completedPatients: Flow<List<PatientEntity>> = patientDao.getCompletedPatientsFlow()
    val activePatient: Flow<PatientEntity?> = patientDao.getActivePatientFlow()
    val totalSavedMinutes: Flow<Int> = savedTimeDao.getTotalSavedMinutesFlow()
    val savedTimeRecords: Flow<List<SavedTimeRecord>> = savedTimeDao.getAllRecords()

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        patientDao.getCount()
    }

    suspend fun insertPatient(
        name: String,
        inpatientNo: String,
        bodyParts: String,
        partCount: Int,
        durationMinutes: Int,
        appointmentTime: String,
        isEmergency: Boolean = false,
        isInserted: Boolean = false,
        insertAfterIndex: Int? = null,
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val currentQueue = patientDao.getQueuePatients().firstOrNull() ?: emptyList()
        val targetOrderIndex: Int

        if (insertAfterIndex != null && insertAfterIndex in -1 until currentQueue.size) {
            targetOrderIndex = insertAfterIndex + 1
            // Shift subsequent patients
            val updatedQueue = currentQueue.map { patient ->
                if (patient.orderIndex >= targetOrderIndex) {
                    patient.copy(orderIndex = patient.orderIndex + 1)
                } else {
                    patient
                }
            }
            patientDao.updateAll(updatedQueue)
        } else {
            val maxOrder = patientDao.getMaxOrderIndex() ?: -1
            targetOrderIndex = maxOrder + 1
        }

        val entity = PatientEntity(
            name = name.trim(),
            inpatientNo = inpatientNo.trim(),
            bodyParts = bodyParts.trim(),
            partCount = partCount.coerceAtLeast(1),
            durationMinutes = durationMinutes.coerceAtLeast(5),
            appointmentTime = appointmentTime.trim(),
            orderIndex = targetOrderIndex,
            status = PatientEntity.STATUS_WAITING,
            isEmergency = isEmergency,
            isInserted = isInserted,
            notes = notes.trim()
        )
        patientDao.insert(entity)
    }

    suspend fun updatePatient(patient: PatientEntity) = withContext(Dispatchers.IO) {
        patientDao.update(patient)
    }

    suspend fun deletePatient(patientId: Long) = withContext(Dispatchers.IO) {
        patientDao.deleteById(patientId)
        normalizeQueueOrder()
    }

    suspend fun startExamination(patientId: Long) = withContext(Dispatchers.IO) {
        val patient = patientDao.getPatientById(patientId) ?: return@withContext
        val updated = patient.copy(
            status = PatientEntity.STATUS_IN_PROGRESS,
            startTime = System.currentTimeMillis()
        )
        patientDao.update(updated)
    }

    suspend fun endExamination(patientId: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val patient = patientDao.getPatientById(patientId) ?: return@withContext Pair(0, 0)
        val endTime = System.currentTimeMillis()
        val startTime = patient.startTime ?: endTime

        val elapsedMillis = (endTime - startTime).coerceAtLeast(0)
        val actualMinutes = Math.max(1, Math.ceil(elapsedMillis / 60000.0).toInt())
        val saved = Math.max(0, patient.durationMinutes - actualMinutes)

        val updated = patient.copy(
            status = PatientEntity.STATUS_COMPLETED,
            endTime = endTime,
            actualDurationMinutes = actualMinutes,
            savedMinutes = saved
        )
        patientDao.update(updated)

        if (saved > 0) {
            savedTimeDao.insert(
                SavedTimeRecord(
                    patientId = patient.id,
                    patientName = patient.name,
                    plannedMinutes = patient.durationMinutes,
                    actualMinutes = actualMinutes,
                    savedMinutes = saved,
                    timestamp = endTime
                )
            )
        }

        val totalSaved = savedTimeDao.getTotalSavedMinutes()
        normalizeQueueOrder()
        Pair(saved, totalSaved)
    }

    suspend fun movePatient(fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val currentQueue = patientDao.getQueuePatients().firstOrNull()?.toMutableList() ?: return@withContext
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) {
            return@withContext
        }

        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val updated = currentQueue.mapIndexed { idx, p ->
            p.copy(orderIndex = idx)
        }
        patientDao.updateAll(updated)
    }

    suspend fun movePatientUp(patientId: Long) = withContext(Dispatchers.IO) {
        val currentQueue = patientDao.getQueuePatients().firstOrNull() ?: return@withContext
        val index = currentQueue.indexOfFirst { it.id == patientId }
        if (index > 0) {
            movePatient(index, index - 1)
        }
    }

    suspend fun movePatientDown(patientId: Long) = withContext(Dispatchers.IO) {
        val currentQueue = patientDao.getQueuePatients().firstOrNull() ?: return@withContext
        val index = currentQueue.indexOfFirst { it.id == patientId }
        if (index in 0 until currentQueue.size - 1) {
            movePatient(index, index + 1)
        }
    }

    suspend fun resetSavedTime() = withContext(Dispatchers.IO) {
        savedTimeDao.clearAll()
    }

    private suspend fun normalizeQueueOrder() {
        val queue = patientDao.getQueuePatients().firstOrNull() ?: return
        val normalized = queue.mapIndexed { idx, p ->
            p.copy(orderIndex = idx)
        }
        patientDao.updateAll(normalized)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        if (patientDao.getCount() == 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val calendar = Calendar.getInstance()

            val samples = listOf(
                PatientEntity(
                    name = "张建国",
                    inpatientNo = "ZY-10294",
                    bodyParts = "胸部CT平扫",
                    partCount = 1,
                    durationMinutes = 15,
                    appointmentTime = sdf.format(calendar.time),
                    orderIndex = 0,
                    status = PatientEntity.STATUS_WAITING,
                    notes = "住院心内科"
                ),
                PatientEntity(
                    name = "李秀兰",
                    inpatientNo = "ZY-10311",
                    bodyParts = "颈椎MRI, 腰椎MRI",
                    partCount = 2,
                    durationMinutes = 30,
                    appointmentTime = run {
                        calendar.add(Calendar.MINUTE, 20)
                        sdf.format(calendar.time)
                    },
                    orderIndex = 1,
                    status = PatientEntity.STATUS_WAITING,
                    notes = "骨科病区"
                ),
                PatientEntity(
                    name = "王明华",
                    inpatientNo = "ZY-10382",
                    bodyParts = "全腹平扫+增强",
                    partCount = 2,
                    durationMinutes = 30,
                    appointmentTime = run {
                        calendar.add(Calendar.MINUTE, 35)
                        sdf.format(calendar.time)
                    },
                    orderIndex = 2,
                    status = PatientEntity.STATUS_WAITING,
                    notes = "普外科"
                ),
                PatientEntity(
                    name = "赵立民",
                    inpatientNo = "MZ-88204",
                    bodyParts = "颅脑CT平扫",
                    partCount = 1,
                    durationMinutes = 15,
                    appointmentTime = run {
                        calendar.add(Calendar.MINUTE, 25)
                        sdf.format(calendar.time)
                    },
                    orderIndex = 3,
                    status = PatientEntity.STATUS_WAITING,
                    notes = "门诊神经内科"
                )
            )
            patientDao.insertAll(samples)
        }
    }
}
