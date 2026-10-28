package com.example.model

enum class RingerStatus(val labelArabic: String) {
    NORMAL("صوت عادي (مفعل)"),
    VIBRATE("وضع الاهتزاز فقط"),
    SILENT("وضع الصامت (مكتوم)"),
    UNKNOWN("غير محدد")
}

data class PresetOption(
    val id: String,
    val title: String,
    val minutes: Int,
    val subtitle: String,
    val emoji: String
)

data class SilenceLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val reason: String,
    val completedSuccessfully: Boolean,
    val triggerType: String // "تلقائي" أو "يدوي"
)
