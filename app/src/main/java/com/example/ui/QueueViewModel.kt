package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PatientEntity
import com.example.data.PatientRepository
import com.example.data.SavedTimeRecord
import com.example.ocr.RequisitionOcrResult
import com.example.ocr.RequisitionOcrService
import com.example.ocr.SampleRequisition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ManualAddState(
    val isOpen: Boolean = false,
    val insertAfterIndex: Int? = null,
    val name: String = "",
    val inpatientNo: String = "",
    val bodyParts: String = "胸部CT平扫",
    val partCount: Int = 1,
    val durationMinutes: Int = 15,
    val appointmentTime: String = "",
    val isEmergency: Boolean = false,
    val notes: String = ""
)

data class OcrDialogState(
    val isOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val recognizedResult: RequisitionOcrResult? = null,
    val insertAfterIndex: Int? = null
)

class QueueViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PatientRepository
    private val ocrService = RequisitionOcrService()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PatientRepository(db.patientDao(), db.savedTimeDao())
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    val queue: StateFlow<List<PatientEntity>> = repository.queuePatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePatient: StateFlow<PatientEntity?> = repository.activePatient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedPatients: StateFlow<List<PatientEntity>> = repository.completedPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavedMinutes: StateFlow<Int> = repository.totalSavedMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savedTimeRecords: StateFlow<List<SavedTimeRecord>> = repository.savedTimeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active exam timer
    private val _activeExamElapsedSeconds = MutableStateFlow(0)
    val activeExamElapsedSeconds: StateFlow<Int> = _activeExamElapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    // Alert for 10-minute saved time accumulation
    private val _showOverTenMinutesAlert = MutableStateFlow<String?>(null)
    val showOverTenMinutesAlert: StateFlow<String?> = _showOverTenMinutesAlert.asStateFlow()

    // Dialog states
    private val _manualAddState = MutableStateFlow(ManualAddState())
    val manualAddState: StateFlow<ManualAddState> = _manualAddState.asStateFlow()

    private val _ocrDialogState = MutableStateFlow(OcrDialogState())
    val ocrDialogState: StateFlow<OcrDialogState> = _ocrDialogState.asStateFlow()

    private val _editingPatient = MutableStateFlow<PatientEntity?>(null)
    val editingPatient: StateFlow<PatientEntity?> = _editingPatient.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        // Observe active patient to run exam timer
        viewModelScope.launch {
            activePatient.collect { patient ->
                timerJob?.cancel()
                if (patient != null && patient.startTime != null) {
                    timerJob = launch {
                        while (isActive) {
                            val elapsedMillis = System.currentTimeMillis() - patient.startTime
                            _activeExamElapsedSeconds.value = (elapsedMillis / 1000).toInt().coerceAtLeast(0)
                            delay(1000)
                        }
                    }
                } else {
                    _activeExamElapsedSeconds.value = 0
                }
            }
        }
    }

    fun dismissTenMinuteAlert() {
        _showOverTenMinutesAlert.value = null
    }

    fun dismissSnack() {
        _snackMessage.value = null
    }

    // --- Active Exam Actions ---

    fun startExamination(patient: PatientEntity) {
        viewModelScope.launch {
            repository.startExamination(patient.id)
            _snackMessage.value = "已开始检查：${patient.name}"
        }
    }

    fun endExamination(patientId: Long) {
        viewModelScope.launch {
            val (saved, totalSaved) = repository.endExamination(patientId)
            timerJob?.cancel()
            _activeExamElapsedSeconds.value = 0

            if (saved > 0) {
                _snackMessage.value = "检查结束！该病人用时缩短，已节省 ${saved} 分钟！"
            } else {
                _snackMessage.value = "检查完成！"
            }

            // Requirement: 累加超过10分钟提醒可插号
            if (totalSaved >= 10) {
                _showOverTenMinutesAlert.value = "🎉 检查用时累加已节省 $totalSaved 分钟（已超10分钟）！当前候检时间充裕，推荐立即快捷插号或安排急诊病人！"
            }
        }
    }

    // --- Reordering & Queue Management ---

    fun movePatientUp(patientId: Long) {
        viewModelScope.launch {
            repository.movePatientUp(patientId)
        }
    }

    fun movePatientDown(patientId: Long) {
        viewModelScope.launch {
            repository.movePatientDown(patientId)
        }
    }

    fun movePatient(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.movePatient(fromIndex, toIndex)
        }
    }

    fun deletePatient(patientId: Long) {
        viewModelScope.launch {
            repository.deletePatient(patientId)
            _snackMessage.value = "已移出排号队列"
        }
    }

    // --- Insert Slot & Time Availability Logic ---

    /**
     * Determines whether there is adequate time buffer under patient at [patientIndex].
     * Returns a pair of (isAdequate: Boolean, availableMinutes: Int).
     * Rule: Green when time is sufficient, Red when insufficient.
     */
    fun checkSlotAvailability(patientIndex: Int): Pair<Boolean, Int> {
        val currentQueue = queue.value
        val totalSaved = totalSavedMinutes.value

        if (patientIndex !in currentQueue.indices) {
            return Pair(totalSaved >= 15, totalSaved)
        }

        val patient = currentQueue[patientIndex]
        val nextPatient = currentQueue.getOrNull(patientIndex + 1)

        // If total accumulated saved minutes >= 15 min, we definitely have enough time for at least 1部位
        if (totalSaved >= 15) {
            return Pair(true, totalSaved)
        }

        // Calculate time gap between current patient end time and next patient appointment time
        if (nextPatient != null && patient.appointmentTime.isNotBlank() && nextPatient.appointmentTime.isNotBlank()) {
            val gapMinutes = calculateMinutesGap(patient.appointmentTime, patient.durationMinutes, nextPatient.appointmentTime)
            val combinedTime = gapMinutes + totalSaved
            if (combinedTime >= 15) {
                return Pair(true, combinedTime)
            } else {
                return Pair(false, combinedTime.coerceAtLeast(0))
            }
        }

        // Default: If saved >= 10 minutes, treat as adequate green, else red
        val isAdequate = totalSaved >= 10
        return Pair(isAdequate, totalSaved)
    }

    private fun calculateMinutesGap(currStartTime: String, currDuration: Int, nextStartTime: String): Int {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currDate = sdf.parse(currStartTime) ?: return 0
            val nextDate = sdf.parse(nextStartTime) ?: return 0
            val currEndMillis = currDate.time + currDuration * 60 * 1000
            val gapMillis = nextDate.time - currEndMillis
            (gapMillis / (60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    // --- Manual Add & Edit Dialogs ---

    fun openManualAdd(insertAfterIndex: Int? = null) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val cal = Calendar.getInstance()
        val defaultTime = sdf.format(cal.time)

        _manualAddState.value = ManualAddState(
            isOpen = true,
            insertAfterIndex = insertAfterIndex,
            appointmentTime = defaultTime,
            bodyParts = "胸部CT平扫",
            partCount = 1,
            durationMinutes = 15
        )
    }

    fun closeManualAdd() {
        _manualAddState.value = ManualAddState(isOpen = false)
    }

    fun submitManualAdd(
        name: String,
        inpatientNo: String,
        bodyParts: String,
        partCount: Int,
        durationMinutes: Int,
        appointmentTime: String,
        isEmergency: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val insertIndex = _manualAddState.value.insertAfterIndex
            repository.insertPatient(
                name = name,
                inpatientNo = inpatientNo,
                bodyParts = bodyParts,
                partCount = partCount,
                durationMinutes = durationMinutes,
                appointmentTime = appointmentTime,
                isEmergency = isEmergency,
                isInserted = insertIndex != null,
                insertAfterIndex = insertIndex,
                notes = notes
            )
            closeManualAdd()
            _snackMessage.value = if (insertIndex != null) "成功快捷插号：$name" else "成功添加病人：$name"
        }
    }

    fun openEditPatient(patient: PatientEntity) {
        _editingPatient.value = patient
    }

    fun closeEditPatient() {
        _editingPatient.value = null
    }

    fun updatePatient(
        id: Long,
        name: String,
        inpatientNo: String,
        bodyParts: String,
        partCount: Int,
        durationMinutes: Int,
        appointmentTime: String,
        isEmergency: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val current = editingPatient.value ?: return@launch
            val updated = current.copy(
                name = name.trim(),
                inpatientNo = inpatientNo.trim(),
                bodyParts = bodyParts.trim(),
                partCount = partCount.coerceAtLeast(1),
                durationMinutes = durationMinutes.coerceAtLeast(5),
                appointmentTime = appointmentTime.trim(),
                isEmergency = isEmergency,
                notes = notes.trim()
            )
            repository.updatePatient(updated)
            closeEditPatient()
            _snackMessage.value = "已更新病人信息"
        }
    }

    // --- OCR & Requisition Flow ---

    fun openOcrScanner(insertAfterIndex: Int? = null) {
        _ocrDialogState.value = OcrDialogState(
            isOpen = true,
            isProcessing = false,
            insertAfterIndex = insertAfterIndex
        )
    }

    fun closeOcrScanner() {
        _ocrDialogState.value = OcrDialogState(isOpen = false)
    }

    fun processCapturedImage(bitmap: Bitmap) {
        _ocrDialogState.value = _ocrDialogState.value.copy(
            isProcessing = true,
            capturedBitmap = bitmap
        )
        viewModelScope.launch {
            val result = ocrService.recognizeRequisition(bitmap)
            _ocrDialogState.value = _ocrDialogState.value.copy(
                isProcessing = false,
                recognizedResult = result
            )
        }
    }

    fun loadSampleRequisition(sample: SampleRequisition) {
        _ocrDialogState.value = _ocrDialogState.value.copy(
            isProcessing = true
        )
        viewModelScope.launch {
            delay(500) // Brief realistic parsing delay
            val result = ocrService.parseFromText(sample.simulatedSlipText).copy(
                appointmentTime = sample.appointmentTime,
                bodyParts = sample.bodyParts,
                partCount = sample.partCount,
                durationMinutes = sample.partCount * 15,
                patientName = sample.patientName,
                inpatientNo = sample.inpatientNo
            )
            _ocrDialogState.value = _ocrDialogState.value.copy(
                isProcessing = false,
                recognizedResult = result
            )
        }
    }

    fun confirmOcrPatient(
        name: String,
        inpatientNo: String,
        bodyParts: String,
        partCount: Int,
        durationMinutes: Int,
        appointmentTime: String,
        isEmergency: Boolean
    ) {
        viewModelScope.launch {
            val insertIndex = _ocrDialogState.value.insertAfterIndex
            repository.insertPatient(
                name = name,
                inpatientNo = inpatientNo,
                bodyParts = bodyParts,
                partCount = partCount,
                durationMinutes = durationMinutes,
                appointmentTime = appointmentTime,
                isEmergency = isEmergency,
                isInserted = insertIndex != null,
                insertAfterIndex = insertIndex,
                notes = "住院单OCR识别录入"
            )
            closeOcrScanner()
            _snackMessage.value = if (insertIndex != null) "住院单识别成功，已快捷插号！" else "住院单识别成功，已加入排号队列！"
        }
    }

    fun resetSavedTimeAccumulator() {
        viewModelScope.launch {
            repository.resetSavedTime()
            _snackMessage.value = "已重置节省时间累加器"
        }
    }
}
