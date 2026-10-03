package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MedSuccessGreen
import com.example.ui.theme.MedTealDark
import com.example.ui.theme.MedTealPrimary

@Composable
fun SavedTimeDashboard(
    totalSavedMinutes: Int,
    waitingCount: Int,
    onResetSavedTime: () -> Unit,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverTenMinutes = totalSavedMinutes >= 10

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_time_dashboard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverTenMinutes) Color(0xFFF0FDF4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = if (isOverTenMinutes) 1.5.dp else 1.dp,
                    color = if (isOverTenMinutes) MedSuccessGreen else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accumulated saved time display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverTenMinutes) MedSuccessGreen.copy(alpha = 0.15f)
                                else MedTealPrimary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverTenMinutes) Icons.Default.Bolt else Icons.Default.Timer,
                            contentDescription = "节省时间",
                            tint = if (isOverTenMinutes) MedSuccessGreen else MedTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "已累计省下",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalSavedMinutes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverTenMinutes) MedSuccessGreen else MedTealDark
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "分钟",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Text(
                            text = if (isOverTenMinutes) "🔥 累计已超过10分钟，当前时间充裕，可快捷插号！"
                            else "提前结束检查将自动累加省下用时",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverTenMinutes) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverTenMinutes) MedSuccessGreen else Color(0xFF94A3B8)
                        )
                    }
                }

                // Actions: Reset or Quick Insert button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalSavedMinutes > 0) {
                        IconButton(
                            onClick = onResetSavedTime,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "重置累计",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isOverTenMinutes) {
                        Button(
                            onClick = onQuickAddClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedSuccessGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("立即插号", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverTenMinutesAlertDialog(
    message: String?,
    onDismiss: () -> Unit,
    onInsertClick: () -> Unit
) {
    if (message == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MedSuccessGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MedSuccessGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "可插号提醒（省时超10分钟）",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF334155),
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onInsertClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MedSuccessGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("立即安排插号")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后处理")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
