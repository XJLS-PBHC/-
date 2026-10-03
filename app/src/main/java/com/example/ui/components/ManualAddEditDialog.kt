package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PatientEntity
import com.example.ui.theme.MedAlertRed
import com.example.ui.theme.MedSuccessGreen
import com.example.ui.theme.MedTealPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAddEditDialog(
    isOpen: Boolean,
    isEditMode: Boolean,
    initialPatient: PatientEntity? = null,
    insertAfterIndex: Int? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        inpatientNo: String,
        bodyParts: String,
        partCount: Int,
        durationMinutes: Int,
        appointmentTime: String,
        isEmergency: Boolean,
        notes: String
    ) -> Unit
) {
    if (!isOpen) return

    val commonPartsList = listOf(
        "胸部CT平扫", "胸部增强", "颅脑平扫", "头颅MRI",
        "上腹平扫", "全腹增强", "颈椎MRI", "腰椎MRI",
        "盆腔平扫", "膝关节", "甲状腺超声", "心脏彩超"
    )

    var name by remember(initialPatient) {
        mutableStateOf(initialPatient?.name ?: "")
    }
    var inpatientNo by remember(initialPatient) {
        mutableStateOf(initialPatient?.inpatientNo ?: "")
    }
    var bodyParts by remember(initialPatient) {
        mutableStateOf(initialPatient?.bodyParts ?: "胸部CT平扫")
    }
    var partCount by remember(initialPatient) {
        mutableIntStateOf(initialPatient?.partCount ?: 1)
    }
    var durationMinutes by remember(initialPatient, partCount) {
        mutableIntStateOf(initialPatient?.durationMinutes ?: (partCount * 15))
    }
    var appointmentTime by remember(initialPatient) {
        val defaultTime = if (initialPatient != null) {
            initialPatient.appointmentTime
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        }
        mutableStateOf(defaultTime)
    }
    var isEmergency by remember(initialPatient) {
        mutableStateOf(initialPatient?.isEmergency ?: false)
    }
    var notes by remember(initialPatient) {
        mutableStateOf(initialPatient?.notes ?: "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("manual_add_edit_dialog"),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.MedicalInformation else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isEditMode -> "修改病人排号信息"
                                insertAfterIndex != null -> "快捷插号（插入指定位）"
                                else -> "手动添加排号病人"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Patient Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("病人姓名 *") },
                    placeholder = { Text("例如：张建国") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Inpatient No & Appointment Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inpatientNo,
                        onValueChange = { inpatientNo = it },
                        label = { Text("住院号/门诊号") },
                        placeholder = { Text("如 ZY10294") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = appointmentTime,
                        onValueChange = { appointmentTime = it },
                        label = { Text("排号/预约时间") },
                        placeholder = { Text("如 10:30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Quick time adjust chips
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("当前时间", "+15分钟", "+30分钟").forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                appointmentTime = when (label) {
                                    "+15分钟" -> {
                                        cal.add(Calendar.MINUTE, 15)
                                        sdf.format(cal.time)
                                    }
                                    "+30分钟" -> {
                                        cal.add(Calendar.MINUTE, 30)
                                        sdf.format(cal.time)
                                    }
                                    else -> sdf.format(cal.time)
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MedTealPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Examination Body Parts
                OutlinedTextField(
                    value = bodyParts,
                    onValueChange = { bodyParts = it },
                    label = { Text("检查部位与项目 *") },
                    placeholder = { Text("如 胸部CT平扫, 腹部增强") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Body Part Selection Chips
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "快速选择常见部位：",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonPartsList.forEach { part ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (bodyParts.contains(part)) MedTealPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (bodyParts.contains(part)) MedTealPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable {
                                if (bodyParts.isBlank()) {
                                    bodyParts = part
                                    partCount = 1
                                    durationMinutes = 15
                                } else if (!bodyParts.contains(part)) {
                                    bodyParts = "$bodyParts, $part"
                                    partCount++
                                    durationMinutes = partCount * 15
                                }
                            }
                        ) {
                            Text(
                                text = part,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bodyParts.contains(part)) MedTealPrimary else Color(0xFF334155),
                                fontWeight = if (bodyParts.contains(part)) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Part count & 15 minutes per part calculation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "部位数量：$partCount 个",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "默认按每部位15分钟计算",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (partCount > 1) {
                                            partCount--
                                            durationMinutes = partCount * 15
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "减少部位")
                                }

                                Text(
                                    text = "$partCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        partCount++
                                        durationMinutes = partCount * 15
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "增加部位")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom duration adjustment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "预计检查时长：$durationMinutes 分钟",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MedTealPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable {
                                        if (durationMinutes > 5) durationMinutes -= 5
                                    }
                                ) {
                                    Text(
                                        "-5分",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable {
                                        durationMinutes += 5
                                    }
                                ) {
                                    Text(
                                        "+5分",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes / Ward
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（科室/床号/特殊情况）") },
                    placeholder = { Text("如 呼吸科12床、需轮椅") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Emergency Priority Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "急诊 / 优先检查",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEmergency) MedAlertRed else Color(0xFF334155)
                        )
                        Text(
                            text = "急诊高优先级排号提醒",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isEmergency,
                        onCheckedChange = { isEmergency = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MedAlertRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSubmit(
                                name,
                                inpatientNo,
                                bodyParts,
                                partCount,
                                durationMinutes,
                                appointmentTime,
                                isEmergency,
                                notes
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MedTealPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditMode) "保存修改" else if (insertAfterIndex != null) "确认插号" else "确认添加",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
