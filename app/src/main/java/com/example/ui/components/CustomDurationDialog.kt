package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary

@Composable
fun CustomDurationDialog(
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int, reason: String) -> Unit
) {
    var selectedMinutes by remember { mutableFloatStateOf(45f) }
    var reasonText by remember { mutableStateOf("كتم مخصص") }

    val quickChoices = listOf(15, 30, 45, 60, 120, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⏱️ تحديد مدة كتم مخصصة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "حدد عدد الدقائق التي تريد كتم الهاتف خلالها (من 1 د إلى 3 ساعات):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val totalM = selectedMinutes.toInt()
                val h = totalM / 60
                val m = totalM % 60
                val displayStr = when {
                    h > 0 && m > 0 -> "$h ساعة و $m دقيقة ($totalM د)"
                    h > 0 -> "$h ساعة ($totalM د)"
                    else -> "$m دقيقة"
                }

                Text(
                    text = displayStr,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )

                Slider(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    valueRange = 1f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("custom_dialog_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick choices chips (up to 3 hours)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        15 to "15 د",
                        30 to "30 د",
                        45 to "45 د",
                        60 to "1 س",
                        120 to "2 س",
                        180 to "3 س"
                    ).forEach { (minVal, label) ->
                        FilterChip(
                            selected = selectedMinutes.toInt() == minVal,
                            onClick = { selectedMinutes = minVal.toFloat() },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("سبب الكتم (اختياري)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_reason_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedMinutes.toInt(),
                        if (reasonText.isBlank()) "كتم مخصص" else reasonText.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("confirm_custom_duration_button")
            ) {
                Text("بدء الكتم الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
