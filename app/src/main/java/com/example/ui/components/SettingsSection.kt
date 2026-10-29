package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary

@Composable
fun SettingsSection(
    isAutoDetectEnabled: Boolean,
    onAutoDetectChanged: (Boolean) -> Unit,
    restoreVolumePercent: Int,
    onRestoreVolumeChanged: (Int) -> Unit,
    includeVibrateMode: Boolean,
    onIncludeVibrateChanged: (Boolean) -> Unit,
    soundAlertOnUnmute: Boolean,
    onSoundAlertChanged: (Boolean) -> Unit,
    vibrateOnUnmute: Boolean,
    onVibrateAlertChanged: (Boolean) -> Unit,
    isIgnoringBattery: Boolean = true,
    onRequestIgnoreBattery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "⚙️ إعدادات التحكم الذكي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "خصص طريقة عمل وتفاعل نبيل سيلونس مع هاتفك",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Auto-detection toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المراقبة التلقائية للوضع الصامت",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يبدأ المؤقت تلقائياً عند كتم الهاتف بأزرار الصوت أو لوحة الإشعارات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = isAutoDetectEnabled,
                    onCheckedChange = onAutoDetectChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("auto_detect_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 2. Restore volume level slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مستوى الصوت بعد إلغاء الكتم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$restoreVolumePercent%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                }
                Text(
                    text = "درجة علو رنين الهاتف عند استعادة الصوت الطبيعي",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = restoreVolumePercent.toFloat(),
                    onValueChange = { onRestoreVolumeChanged(it.toInt()) },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("restore_volume_slider")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 4. Include Vibrate mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "شمل وضع الاهتزاز (Vibrate)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تطبيق المؤقت أيضاً عندما تضع الهاتف على وضع الاهتزاز",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeVibrateMode,
                    onCheckedChange = onIncludeVibrateChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("include_vibrate_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 5. Sound alert on unmute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نغمة تنبيه لطيفة عند إلغاء الكتم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تشغيل صوت خفيف للتأكيد على عودة الصوت للعمل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundAlertOnUnmute,
                    onCheckedChange = onSoundAlertChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("sound_alert_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 6. Vibrate on unmute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اهتزاز لطيف عند إلغاء الكتم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اهتزاز نبضي خفيف لإشعارك باكتمال المدة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = vibrateOnUnmute,
                    onCheckedChange = onVibrateAlertChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("vibrate_alert_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 7. Background Battery Optimization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الحماية الدائمة في الخلفية (عدم إيقاف التطبيق)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isIgnoringBattery)
                            "نبيل سيلونس محمي من الإيقاف وسيعمل دائماً حتى عند إغلاق التطبيق"
                        else
                            "اضغط لتجاوز قيود توفير الطاقة وضمان عمل التطبيق بعد الخروج",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIgnoringBattery) androidx.compose.ui.graphics.Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isIgnoringBattery) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "محمي ✅",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF81C784),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    androidx.compose.material3.Button(
                        onClick = onRequestIgnoreBattery,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = androidx.compose.ui.graphics.Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("request_battery_opt_button")
                    ) {
                        Text("تفعيل ⚡", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
