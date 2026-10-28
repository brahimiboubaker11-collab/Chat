package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess

@Composable
fun BetaTrialDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "نسخة تجريبية",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "نبيل سيلونس - النسخة التجريبية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الإصدار التجريبي 1.0 (Beta Preview)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مرحباً بك في النسخة التجريبية من تطبيق 'نبيل سيلونس' المصمم خصيصاً لراحتك حتى لا تفوتك أي مكالمة مهمة بعد الآن!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                FeatureHighlightRow(
                    icon = "🔄",
                    title = "مراقبة تلقائية حية",
                    desc = "يكتشف فوراً عندما تضع الهاتف على الصامت بأزرار الهاتف ويبدأ المؤقت تلقائياً."
                )

                FeatureHighlightRow(
                    icon = "⏱️",
                    title = "مؤقت انسيابي دقيق",
                    desc = "واجهة دائرية عصرية مع عد تنازلي يظهر في التطبيق وفي شريط الإشعارات."
                )

                FeatureHighlightRow(
                    icon = "🔔",
                    title = "إلغاء الكتم بنقرة واحدة",
                    desc = "أزرار فورية في الإشعار: إلغاء الكتم فوراً، إضافة 15 دقيقة، أو إيقاف المؤقت."
                )

                FeatureHighlightRow(
                    icon = "🔊",
                    title = "استعادة الصوت ومستواه",
                    desc = "يعيد الصوت تلقائياً إلى النسبة المحددة مع نغمة واهتزاز لطيف عند الانتهاء."
                )

                FeatureHighlightRow(
                    icon = "⚡",
                    title = "أوضاع سريعة جاهزة",
                    desc = "أوقات مخصصة للصلاة، الاجتماعات، الدراسة، السينما والنوم."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("close_beta_dialog_button")
            ) {
                Text("فهمت، تابع الاستخدام", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun FeatureHighlightRow(
    icon: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
