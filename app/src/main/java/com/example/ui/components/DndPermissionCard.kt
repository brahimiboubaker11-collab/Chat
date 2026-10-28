package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess

@Composable
fun DndPermissionCard(
    hasDndPermission: Boolean,
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!hasDndPermission) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("dnd_permission_warning_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = AmberAccent.copy(alpha = 0.12f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(AmberAccent.copy(alpha = 0.5f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "تحذير الإذن",
                        tint = AmberAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ملاحظة هامة لنظام أندرويد",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "لإلغاء وضع الصامت وإعادة تشغيل رنين الهاتف تلقائياً من وضع الصامت التام، يحتاج التطبيق إلى إذن 'الوصول لسياسة عدم الإزعاج'. اضغط على الزر أدناه لتفعيله مرة واحدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onGrantPermissionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("grant_dnd_permission_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "منح إذن عدم الإزعاج (DND) الآن ⚡",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(EmeraldSuccess.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("dnd_permission_granted_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "مفعل",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إذن التحكم بالنظام مفعل ومستعد للعمل التلقائي",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )
            }
        }
    }
}
