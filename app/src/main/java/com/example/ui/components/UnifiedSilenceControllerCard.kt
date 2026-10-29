package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RingerStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseSilent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedSilenceControllerCard(
    defaultDurationMinutes: Int,
    onDurationChanged: (Int) -> Unit,
    isAutoDetectEnabled: Boolean,
    onAutoDetectChanged: (Boolean) -> Unit,
    remainingSeconds: Long,
    totalDurationMs: Long,
    reason: String,
    endTimeMs: Long,
    ringerStatus: RingerStatus,
    onUnmuteClick: () -> Unit,
    onAdjustTimeClick: (Int) -> Unit,
    onCancelClick: () -> Unit,
    onStartTimerNow: (Int) -> Unit,
    onQuickTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTimerActive = remainingSeconds > 0
    var showManualInputDialog by remember { mutableStateOf(false) }

    val hours = defaultDurationMinutes / 60
    val minutes = defaultDurationMinutes % 60

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unified_silence_controller_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isTimerActive) {
                Brush.horizontalGradient(listOf(CyanPrimary, AmberAccent))
            } else {
                Brush.horizontalGradient(listOf(CyanPrimary.copy(alpha = 0.5f), AmberAccent.copy(alpha = 0.3f)))
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Status + Auto Detection Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isTimerActive) listOf(RoseSilent, AmberAccent)
                                    else listOf(CyanPrimary, CyanGlow)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTimerActive) Icons.Default.HourglassTop else Icons.Default.Alarm,
                            contentDescription = "العداد الموحد",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isTimerActive) "مؤقت الصامت نشط حالياً" else "عداد مدة الصامت والاهتزاز",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isTimerActive) "الرجوع التلقائي للوضع العام" else "تحكم في المدة من 1 دقيقة إلى 3 ساعات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = isAutoDetectEnabled,
                        onCheckedChange = onAutoDetectChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("auto_detect_master_switch")
                    )
                    Text(
                        text = if (isAutoDetectEnabled) "المراقبة مفعلة" else "المراقبة متوقفة",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAutoDetectEnabled) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isTimerActive) {
                // ==========================================
                // 1. ACTIVE LIVE COUNTDOWN TIMER VIEW
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Target State Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseSilent.copy(alpha = 0.15f))
                                .border(1.dp, RoseSilent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🔴 $reason",
                                color = RoseSilent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldSuccess.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "الهدف: 🟢 الوضع العام (صوت كامل)",
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Circular Progress Dial
                    val progress = if (totalDurationMs > 0) {
                        val remMs = remainingSeconds * 1000L
                        (remMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 1f

                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(CyanGlow.copy(alpha = 0.22f), Color.Transparent)
                                    )
                                )
                        )

                        Canvas(modifier = Modifier.size(190.dp)) {
                            val strokeWidth = 14.dp.toPx()
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(CyanPrimary, AmberAccent, CyanPrimary)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatDigitalClock(remainingSeconds),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ساعة : دقيقة : ثانية",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "متبقي للوضع العام",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detailed Live Remaining Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .border(1.dp, CyanPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏳ " + formatDetailedRemainingArabic(remainingSeconds),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            if (endTimeMs > 0L) {
                                val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                val endFormatted = timeFormat.format(Date(endTimeMs))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏰ موعد عودة الصوت ورنين الهاتف: $endFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanPrimary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live adjustments: +15m, +1h, -15m
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onAdjustTimeClick(15) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+15 د", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onAdjustTimeClick(60) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 ساعة", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onAdjustTimeClick(-15) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("-15 د", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Unmute Now Button
                    Button(
                        onClick = onUnmuteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("unmute_now_master_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "إلغاء الكتم",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إلغاء الكتم والرجوع للوضع العام فوراً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إيقاف", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "إيقاف المؤقت (البقاء صامتاً بدون مؤقت)", style = MaterialTheme.typography.labelMedium)
                    }
                }

            } else {
                // ==========================================
                // 2. IDLE MASTER CONTROLLER (1 MINUTE TO 24 HOURS)
                // ==========================================

                // Hero display showing configured duration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(vertical = 16.dp, horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "مدة الصامت والاهتزاز المضبوطة:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatDurationFull(defaultDurationMinutes),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "($defaultDurationMinutes دقيقة إجمالية)",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // DUAL SLIDERS: Hours & Minutes (Max 3 hours)
                Text(
                    text = "تحكم دقيق بالساعات والدقائق (من 1 د إلى 3 ساعات كحد أقصى):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Hours Control (0 to 3)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الساعات (0 إلى 3):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$hours ساعة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                val newTotal = ((hours - 1).coerceAtLeast(0) * 60) + minutes
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1 س")
                        }

                        Slider(
                            value = hours.toFloat().coerceIn(0f, 3f),
                            onValueChange = { newH ->
                                val targetH = newH.toInt()
                                val targetM = if (targetH == 3) 0 else minutes
                                val newTotal = (targetH * 60) + targetM
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            valueRange = 0f..3f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("hours_slider")
                        )

                        FilledIconButton(
                            onClick = {
                                val targetH = (hours + 1).coerceAtMost(3)
                                val targetM = if (targetH == 3) 0 else minutes
                                val newTotal = (targetH * 60) + targetM
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1 س")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Minutes Control (0 to 59)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الدقائق:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$minutes دقيقة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                val newTotal = (hours * 60) + (minutes - 1).coerceAtLeast(0)
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1 د")
                        }

                        Slider(
                            value = minutes.toFloat().coerceIn(0f, 59f),
                            onValueChange = { newM ->
                                val targetH = if (hours >= 3) 2 else hours
                                val newTotal = (targetH * 60) + newM.toInt()
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            valueRange = 0f..59f,
                            steps = 58,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("minutes_slider")
                        )

                        FilledIconButton(
                            onClick = {
                                val targetH = if (hours >= 3) 2 else hours
                                val newTotal = (targetH * 60) + (minutes + 1).coerceAtMost(59)
                                onDurationChanged(newTotal.coerceIn(1, 180))
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1 د")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick preset chips (up to 3 hours)
                Text(
                    text = "أوقات شائعة بنقرة واحدة (أقصى حد 3 ساعات):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                val presetList = listOf(
                    1 to "1 دقيقة",
                    5 to "5 دقائق",
                    15 to "15 دقيقة",
                    30 to "30 دقيقة",
                    45 to "45 دقيقة",
                    60 to "ساعة",
                    90 to "ساعة ونصف",
                    120 to "ساعتان",
                    150 to "ساعتان ونصف",
                    180 to "3 ساعات (أقصى حد)"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetList.forEach { (m, label) ->
                        val isSelected = defaultDurationMinutes == m
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDurationChanged(m) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Direct write chip
                    FilterChip(
                        selected = false,
                        onClick = { showManualInputDialog = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = "كتابة", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("كتابة رقم دقيق...", fontSize = 11.sp)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Information banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isAutoDetectEnabled) EmeraldSuccess.copy(alpha = 0.12f)
                            else AmberAccent.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isAutoDetectEnabled) EmeraldSuccess.copy(alpha = 0.35f)
                            else AmberAccent.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAutoDetectEnabled) "💡" else "⚠️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAutoDetectEnabled) {
                                "كل مرة تدير فيها هاتفك على الصامت أو الاهتزاز بأزرار الهاتف، نبيل سيلونس راح يرجع الصوت للوضع العام تلقائياً بعد مرور ${formatDurationFull(defaultDurationMinutes)}."
                            } else {
                                "المراقبة التلقائية متوقفة حالياً. قم بتشغيل المفتاح في الأعلى ليعود الصوت تلقائياً كل مرة."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Start timer now or quick test
                Button(
                    onClick = { onStartTimerNow(defaultDurationMinutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_timer_with_duration_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "بدء المؤقت",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "▶️ كتم الهاتف وتفعيل المؤقت الحي (${formatDurationFull(defaultDurationMinutes)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = onQuickTestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("quick_test_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⚡ فحص فوري (كتم لـ 10 ثوانٍ للتأكد من عودة الصوت تلقائياً)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Direct manual duration input dialog
    if (showManualInputDialog) {
        var inputMinutesText by remember { mutableStateOf(defaultDurationMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Text("✍️ كتابة مدة الصامت بالدقائق", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "أدخل عدد الدقائق بالضبط (من 1 دقيقة إلى 180 دقيقة = 3 ساعات):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputMinutesText,
                        onValueChange = { inputMinutesText = it.filter { char -> char.isDigit() } },
                        label = { Text("عدد الدقائق (أقصى حد 180)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputMinutesText.toIntOrNull() ?: defaultDurationMinutes
                        onDurationChanged(parsed.coerceIn(1, 180))
                        showManualInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Text("حفظ المدة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun formatDigitalClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatDetailedRemainingArabic(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 && minutes > 0 -> "متبقي: $hours ساعة و $minutes دقيقة و $seconds ثانية"
        hours > 0 -> "متبقي: $hours ساعة و $seconds ثانية"
        minutes > 0 -> "متبقي: $minutes دقيقة و $seconds ثانية"
        else -> "متبقي: $seconds ثانية فقط"
    }
}

private fun formatDurationFull(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60

    return when {
        h > 0 && m > 0 -> "$h ساعة و $m دقيقة"
        h > 0 -> "$h ساعة"
        else -> "$m دقيقة"
    }
}
