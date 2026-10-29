package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SilenceViewModel
import com.example.ui.components.BetaTrialDialog
import com.example.ui.components.CustomDurationDialog
import com.example.ui.components.DndPermissionCard
import com.example.ui.components.HistorySection
import com.example.ui.components.PresetsSection
import com.example.ui.components.RingerStatusHeader
import com.example.ui.components.SettingsSection
import com.example.ui.components.UnifiedSilenceControllerCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Arabic RTL Layout direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NabilSilenceScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NabilSilenceScreen(
    viewModel: SilenceViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state
    var showCustomDialog by remember { mutableStateOf(false) }
    var showBetaDialog by remember { mutableStateOf(false) }
    var showDndPromptDialog by remember { mutableStateOf(!viewModel.hasDndPermission.value) }

    // Collect reactive state from ViewModel
    val ringerStatus by viewModel.ringerStatus.collectAsStateWithLifecycle()
    val volumePercent by viewModel.volumePercent.collectAsStateWithLifecycle()
    val hasDndPermission by viewModel.hasDndPermission.collectAsStateWithLifecycle()
    val isAutoDetectEnabled by viewModel.isAutoDetectEnabled.collectAsStateWithLifecycle()
    val defaultDurationMinutes by viewModel.defaultDurationMinutes.collectAsStateWithLifecycle()
    val restoreVolumePercent by viewModel.restoreVolumePercent.collectAsStateWithLifecycle()
    val soundAlertOnUnmute by viewModel.soundAlertOnUnmute.collectAsStateWithLifecycle()
    val vibrateOnUnmute by viewModel.vibrateOnUnmute.collectAsStateWithLifecycle()
    val includeVibrateMode by viewModel.includeVibrateMode.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val timerTotalDuration by viewModel.timerTotalDuration.collectAsStateWithLifecycle()
    val timerReason by viewModel.timerReason.collectAsStateWithLifecycle()
    val timerEndTime by viewModel.timerEndTime.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val messageEvent by viewModel.messageEvent.collectAsStateWithLifecycle()
    val isIgnoringBattery by viewModel.isIgnoringBattery.collectAsStateWithLifecycle()

    // Activity result launcher for DND Settings
    val dndSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshState()
    }

    // Activity result launcher for Battery Optimization
    val batterySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshState()
    }

    // Permission launcher for Android 13+ Notification
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Request notification permission on Android 13+ if not already granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    // Lifecycle observer to refresh state when app returns to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show snackbar messages
    LaunchedEffect(messageEvent) {
        messageEvent?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("main_screen_container"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with current state and Beta badge
                RingerStatusHeader(
                    ringerStatus = ringerStatus,
                    volumePercent = volumePercent,
                    onBetaBadgeClick = { showBetaDialog = true }
                )

                // DND permission card
                DndPermissionCard(
                    hasDndPermission = hasDndPermission,
                    onGrantPermissionClick = {
                        dndSettingsLauncher.launch(viewModel.getDndSettingsIntent())
                    }
                )

                // Unified Master Counter & Controller (1 minute to 24 hours)
                UnifiedSilenceControllerCard(
                    defaultDurationMinutes = defaultDurationMinutes,
                    onDurationChanged = { viewModel.updateDefaultDuration(it) },
                    isAutoDetectEnabled = isAutoDetectEnabled,
                    onAutoDetectChanged = { viewModel.updateAutoDetect(it) },
                    remainingSeconds = remainingSeconds,
                    totalDurationMs = timerTotalDuration,
                    reason = timerReason,
                    endTimeMs = timerEndTime,
                    ringerStatus = ringerStatus,
                    onUnmuteClick = { viewModel.unmuteNow() },
                    onAdjustTimeClick = { viewModel.adjustActiveTimer(it) },
                    onCancelClick = { viewModel.cancelTimer() },
                    onStartTimerNow = { minutes ->
                        viewModel.muteCustom(minutes, "كتم مؤقت (${formatDurationMinutesArabic(minutes)})")
                    },
                    onQuickTestClick = { viewModel.testQuickTenSeconds() }
                )

                // Presets Section (Prayer, Meeting, Study, Sleep, etc.)
                PresetsSection(
                    presets = viewModel.presets,
                    onPresetClick = { preset -> viewModel.muteForPreset(preset) },
                    onCustomClick = { showCustomDialog = true }
                )

                // Smart Settings Section
                SettingsSection(
                    isAutoDetectEnabled = isAutoDetectEnabled,
                    onAutoDetectChanged = { viewModel.updateAutoDetect(it) },
                    restoreVolumePercent = restoreVolumePercent,
                    onRestoreVolumeChanged = { viewModel.updateRestoreVolume(it) },
                    includeVibrateMode = includeVibrateMode,
                    onIncludeVibrateChanged = { viewModel.updateIncludeVibrate(it) },
                    soundAlertOnUnmute = soundAlertOnUnmute,
                    onSoundAlertChanged = { viewModel.updateSoundAlert(it) },
                    vibrateOnUnmute = vibrateOnUnmute,
                    onVibrateAlertChanged = { viewModel.updateVibrateAlert(it) },
                    isIgnoringBattery = isIgnoringBattery,
                    onRequestIgnoreBattery = {
                        batterySettingsLauncher.launch(viewModel.getBatteryOptimizationIntent())
                    }
                )

                // History Section
                HistorySection(
                    historyLogs = historyLogs,
                    onClearHistoryClick = { viewModel.clearHistory() }
                )

                // Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "نبيل سيلونس • النسخة التجريبية 1.0 (Beta)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تطبيق ذكي ينحي السيلونس تلقائياً ويضمن عدم تفويت اتصالاتك ورسائلك المهمة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Custom Duration Dialog
    if (showCustomDialog) {
        CustomDurationDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { minutes, reason ->
                showCustomDialog = false
                viewModel.muteCustom(minutes, reason)
            }
        )
    }

    // Beta Trial Dialog
    if (showBetaDialog) {
        BetaTrialDialog(
            onDismiss = { showBetaDialog = false }
        )
    }

    // DND Permission Dialog
    if (showDndPromptDialog && !hasDndPermission) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDndPromptDialog = false },
            title = {
                Text(
                    text = "⚠️ تفعيل إذن عدم الإزعاج (DND)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "لكي يتمكن نبيل سيلونس من رفع كتم الهاتف وإعادته للوضع العام تلقائياً بعد انقضاء الوقت، يحتاج نظام أندرويد إلى منح إذن 'الوصول لسياسة عدم الإزعاج'.\n\nاضغط الزر أدناه لتفعيله بنقرة واحدة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDndPromptDialog = false
                        dndSettingsLauncher.launch(viewModel.getDndSettingsIntent())
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.CyanPrimary,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("منح الإذن الآن ⚡", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDndPromptDialog = false }) {
                    Text("لاحقاً")
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        )
    }
}

private fun formatDurationMinutesArabic(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "$h ساعة و $m دقيقة"
        h > 0 -> "$h ساعة"
        else -> "$m دقيقة"
    }
}
