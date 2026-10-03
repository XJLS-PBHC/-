package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PatientEntity
import com.example.ui.components.ActiveExamCard
import com.example.ui.components.CompletedHistoryDialog
import com.example.ui.components.InsertSlotButton
import com.example.ui.components.ManualAddEditDialog
import com.example.ui.components.OcrScannerDialog
import com.example.ui.components.OverTenMinutesAlertDialog
import com.example.ui.components.PatientQueueItem
import com.example.ui.components.SavedTimeDashboard
import com.example.ui.components.SlotChoiceDialog
import com.example.ui.theme.MedSlateBg
import com.example.ui.theme.MedSuccessGreen
import com.example.ui.theme.MedTealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val activePatient by viewModel.activePatient.collectAsStateWithLifecycle()
    val completedPatients by viewModel.completedPatients.collectAsStateWithLifecycle()
    val totalSavedMinutes by viewModel.totalSavedMinutes.collectAsStateWithLifecycle()
    val activeExamElapsedSeconds by viewModel.activeExamElapsedSeconds.collectAsStateWithLifecycle()
    val showOverTenMinutesAlert by viewModel.showOverTenMinutesAlert.collectAsStateWithLifecycle()

    val manualAddState by viewModel.manualAddState.collectAsStateWithLifecycle()
    val ocrDialogState by viewModel.ocrDialogState.collectAsStateWithLifecycle()
    val editingPatient by viewModel.editingPatient.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCompletedHistory by remember { mutableStateOf(false) }
    var chosenSlotIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnack()
        }
    }

    // Separate waiting patients from in-progress
    val waitingPatients = remember(queue, activePatient) {
        queue.filter { it.status == PatientEntity.STATUS_WAITING }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MedSlateBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MedTealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "医排助手 · 检查排号",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCompletedHistory = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "历史记录",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openOcrScanner() },
                containerColor = MedTealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_ocr_scan")
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "拍照OCR")
                Spacer(modifier = Modifier.width(8.dp))
                Text("拍照识别加号", fontWeight = FontWeight.Bold)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Active Exam Banner with Magnified End Button
            item(key = "active_exam_section") {
                AnimatedVisibility(
                    visible = activePatient != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    activePatient?.let { patient ->
                        ActiveExamCard(
                            patient = patient,
                            elapsedSeconds = activeExamElapsedSeconds,
                            onEndExam = { patientId ->
                                viewModel.endExamination(patientId)
                            }
                        )
                    }
                }
            }

            // Section 2: Saved Time Accumulator & Efficiency Dashboard
            item(key = "saved_time_dashboard") {
                SavedTimeDashboard(
                    totalSavedMinutes = totalSavedMinutes,
                    waitingCount = waitingPatients.size,
                    onResetSavedTime = { viewModel.resetSavedTimeAccumulator() },
                    onQuickAddClick = { viewModel.openOcrScanner() }
                )
            }

            // Section 3: Queue Header & Add Actions
            item(key = "queue_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "候检队列 (${waitingPatients.size}人等待)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "默认按每部位15分钟排号，可上下调整顺序",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.openManualAdd() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF0F172A)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("手动加号", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.openOcrScanner() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedTealPrimary.copy(alpha = 0.12f),
                                contentColor = MedTealPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("拍照录入", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Empty state if queue is empty
            if (waitingPatients.isEmpty() && activePatient == null) {
                item(key = "empty_queue") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "当前排号队列为空",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "请点击下方按钮拍照识别住院单或手动添加病人",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.openOcrScanner() },
                                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("拍照识别住院单")
                            }
                        }
                    }
                }
            }

            // Section 4: Queue List Items
            // Each patient card has a dedicated Insert Slot button underneath!
            itemsIndexed(
                items = waitingPatients,
                key = { _, item -> item.id }
            ) { index, patient ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Patient Queue Card
                    PatientQueueItem(
                        patient = patient,
                        indexInQueue = index,
                        isFirst = index == 0,
                        isLast = index == waitingPatients.size - 1,
                        hasActivePatient = activePatient != null,
                        onStartExam = { viewModel.startExamination(it) },
                        onMoveUp = { viewModel.movePatientUp(it) },
                        onMoveDown = { viewModel.movePatientDown(it) },
                        onEdit = { viewModel.openEditPatient(it) },
                        onDelete = { viewModel.deletePatient(it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ========================================================
                    // USER REQUIREMENT:
                    // "每个病人下面都有插号按钮，当有充足时间的时候插号按钮为绿色，不足时为红色。"
                    // ========================================================
                    val (isAdequate, availableMinutes) = viewModel.checkSlotAvailability(index)
                    InsertSlotButton(
                        slotIndex = index,
                        isAdequate = isAdequate,
                        availableMinutes = availableMinutes,
                        onInsertClick = { slotIdx ->
                            chosenSlotIndex = slotIdx
                        }
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    // 10-Minute Alert Popup
    OverTenMinutesAlertDialog(
        message = showOverTenMinutesAlert,
        onDismiss = { viewModel.dismissTenMinuteAlert() },
        onInsertClick = {
            viewModel.dismissTenMinuteAlert()
            viewModel.openOcrScanner()
        }
    )

    // Slot Choice Dialog (when clicking green/red insert button under patient)
    SlotChoiceDialog(
        isOpen = chosenSlotIndex != null,
        slotIndex = chosenSlotIndex,
        onDismiss = { chosenSlotIndex = null },
        onChooseOcr = { slotIdx ->
            viewModel.openOcrScanner(insertAfterIndex = slotIdx)
        },
        onChooseManual = { slotIdx ->
            viewModel.openManualAdd(insertAfterIndex = slotIdx)
        }
    )

    // OCR Scanner Dialog
    OcrScannerDialog(
        state = ocrDialogState,
        onDismiss = { viewModel.closeOcrScanner() },
        onProcessImage = { bitmap ->
            viewModel.processCapturedImage(bitmap)
        },
        onLoadSample = { sample ->
            viewModel.loadSampleRequisition(sample)
        },
        onConfirm = { name, inpatientNo, bodyParts, partCount, durationMinutes, appointmentTime, isEmergency ->
            viewModel.confirmOcrPatient(
                name = name,
                inpatientNo = inpatientNo,
                bodyParts = bodyParts,
                partCount = partCount,
                durationMinutes = durationMinutes,
                appointmentTime = appointmentTime,
                isEmergency = isEmergency
            )
        }
    )

    // Manual Add Dialog
    ManualAddEditDialog(
        isOpen = manualAddState.isOpen,
        isEditMode = false,
        insertAfterIndex = manualAddState.insertAfterIndex,
        onDismiss = { viewModel.closeManualAdd() },
        onSubmit = { name, inpatientNo, bodyParts, partCount, durationMinutes, appointmentTime, isEmergency, notes ->
            viewModel.submitManualAdd(
                name = name,
                inpatientNo = inpatientNo,
                bodyParts = bodyParts,
                partCount = partCount,
                durationMinutes = durationMinutes,
                appointmentTime = appointmentTime,
                isEmergency = isEmergency,
                notes = notes
            )
        }
    )

    // Edit Existing Patient Dialog
    ManualAddEditDialog(
        isOpen = editingPatient != null,
        isEditMode = true,
        initialPatient = editingPatient,
        onDismiss = { viewModel.closeEditPatient() },
        onSubmit = { name, inpatientNo, bodyParts, partCount, durationMinutes, appointmentTime, isEmergency, notes ->
            editingPatient?.let { current ->
                viewModel.updatePatient(
                    id = current.id,
                    name = name,
                    inpatientNo = inpatientNo,
                    bodyParts = bodyParts,
                    partCount = partCount,
                    durationMinutes = durationMinutes,
                    appointmentTime = appointmentTime,
                    isEmergency = isEmergency,
                    notes = notes
                )
            }
        }
    )

    // Completed History Dialog
    CompletedHistoryDialog(
        isOpen = showCompletedHistory,
        completedPatients = completedPatients,
        onDismiss = { showCompletedHistory = false }
    )
}
