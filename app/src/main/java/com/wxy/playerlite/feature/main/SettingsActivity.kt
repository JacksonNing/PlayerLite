package com.wxy.playerlite.feature.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.wxy.playerlite.feature.user.AccountVisualStyle
import com.wxy.playerlite.feature.user.AccountVisualTheme
import com.wxy.playerlite.feature.user.LoginActivity
import com.wxy.playerlite.designsystem.theme.ThemeMode
import com.wxy.playerlite.playback.model.PlaybackAudioQuality
import com.wxy.playerlite.ui.theme.PlayerLiteAppTheme
import com.wxy.playerlite.ui.theme.applyInitialPlayerLiteSystemBars
import kotlin.math.ln
import kotlin.math.pow
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SettingsGreenText = Color(0xFF237653)
private val SettingsGreenSurface = Color(0x1A1F8758)

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Unit
    }

    private val audioSourceManifestLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val rawJson = runCatching {
                readAudioSourceManifest(uri)
            }.getOrElse { error ->
                viewModel.showAudioSourceValidationMessage(
                    error.message ?: "本地音源导入失败"
                )
                return@launch
            }
            viewModel.importAudioSourceFromLocalJson(
                rawJson = rawJson,
                displayLabel = uri.toString()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyInitialPlayerLiteSystemBars()
        setContent {
            PlayerLiteAppTheme {
                val state = viewModel.uiStateFlow.collectAsStateWithLifecycle().value
                BackHandler(onBack = ::finish)
                SettingsScreen(
                    state = state,
                    onBack = ::finish,
                    onLoginClick = {
                        loginLauncher.launch(LoginActivity.createIntent(this@SettingsActivity))
                    },
                    onShowLogoutConfirm = viewModel::showLogoutConfirmation,
                    onDismissLogoutConfirm = viewModel::dismissLogoutConfirmation,
                    onConfirmLogout = viewModel::logout,
                    onThemeModeChange = viewModel::updateThemeMode,
                    onRefreshCache = viewModel::refreshCache,
                    onClearCache = viewModel::clearManagedCache,
                    onPlaybackCacheLimitChange = viewModel::updatePendingPlaybackCacheLimitMb,
                    onSavePlaybackCacheLimit = viewModel::savePlaybackCacheLimit,
                    onShowPreferredAudioQualityDialog = viewModel::showPreferredAudioQualityDialog,
                    onDismissPreferredAudioQualityDialog =
                        viewModel::dismissPreferredAudioQualityDialog,
                    onPreferredAudioQualityChange = viewModel::updatePreferredAudioQuality,
                    onRestoreLastPlaybackOnStartupChange =
                        viewModel::updateRestoreLastPlaybackOnStartup,
                    onResumeFromLastPositionChange = viewModel::updateResumeFromLastPosition,
                    onWeakNetworkAutoRetryChange = viewModel::updateWeakNetworkAutoRetry,
                    onShowCacheFailureNotificationsChange =
                        viewModel::updateShowCacheFailureNotifications,
                    onPlaybackPrewarmEnabledChange = viewModel::updatePlaybackPrewarmEnabled,
                    onPlaybackPrewarmBudgetChange = viewModel::updatePlaybackPrewarmBudget,
                    onPendingImportUrlChange = viewModel::updatePendingImportUrl,
                    onImportAudioSourceFromUrl = viewModel::importAudioSourceFromUrl,
                    onImportAudioSourceFromLocal = {
                        audioSourceManifestLauncher.launch("*/*")
                    },
                    onSetActiveAudioSource = viewModel::setActiveAudioSource,
                    onRemoveAudioSource = viewModel::removeAudioSource
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    private suspend fun readAudioSourceManifest(uri: Uri): String = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            reader.readText().trim()
        }?.takeIf { it.isNotBlank() }
            ?: error("无法读取所选音源文件")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onShowLogoutConfirm: () -> Unit,
    onDismissLogoutConfirm: () -> Unit,
    onConfirmLogout: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onRefreshCache: () -> Unit,
    onClearCache: () -> Unit,
    onPlaybackCacheLimitChange: (String) -> Unit,
    onSavePlaybackCacheLimit: () -> Unit,
    onShowPreferredAudioQualityDialog: () -> Unit = {},
    onDismissPreferredAudioQualityDialog: () -> Unit = {},
    onPreferredAudioQualityChange: (PlaybackAudioQuality) -> Unit,
    onRestoreLastPlaybackOnStartupChange: (Boolean) -> Unit = {},
    onResumeFromLastPositionChange: (Boolean) -> Unit = {},
    onWeakNetworkAutoRetryChange: (Boolean) -> Unit = {},
    onShowCacheFailureNotificationsChange: (Boolean) -> Unit = {},
    onPlaybackPrewarmEnabledChange: (Boolean) -> Unit = {},
    onPlaybackPrewarmBudgetChange: (PlaybackPrewarmBudgetPreset) -> Unit = {},
    onPendingImportUrlChange: (String) -> Unit,
    onImportAudioSourceFromUrl: () -> Unit,
    onImportAudioSourceFromLocal: () -> Unit,
    onSetActiveAudioSource: (String) -> Unit,
    onRemoveAudioSource: (String) -> Unit
) {
    var isThemeModeDialogVisible by remember { mutableStateOf(false) }
    var isCacheLimitDialogVisible by remember { mutableStateOf(false) }
    var isPrewarmBudgetDialogVisible by remember { mutableStateOf(false) }
    var isOnlineSourceImportDialogVisible by remember { mutableStateOf(false) }

    if (state.accountState.isLogoutConfirmVisible) {
        AlertDialog(
            onDismissRequest = onDismissLogoutConfirm,
            title = { Text("确认退出登录") },
            text = { Text("退出后只会清理当前在线账户状态，本地播放和设置项不会被清空。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissLogoutConfirm()
                        onConfirmLogout()
                    },
                    modifier = Modifier.testTag("settings_logout_confirm")
                ) {
                    Text("退出登录")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissLogoutConfirm) {
                    Text("取消")
                }
            }
        )
    }
    if (state.playbackPreferencesState.isPreferredAudioQualityDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissPreferredAudioQualityDialog,
            title = { Text("选择默认音质") },
            text = {
                Surface(
                    modifier = Modifier.testTag("settings_playback_quality_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        supportedSettingsAudioQualities().forEachIndexed { index, quality ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            val isCurrentQuality =
                                state.playbackPreferencesState.preferredAudioQuality == quality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(
                                        "settings_playback_quality_dialog_option_${quality.wireValue}"
                                    )
                                    .clickable(enabled = !isCurrentQuality) {
                                        onPreferredAudioQualityChange(quality)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isCurrentQuality) "当前默认" else "点击切换",
                                    color = if (isCurrentQuality) {
                                        AccountVisualTheme.accentText
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = onDismissPreferredAudioQualityDialog) {
                    Text("取消")
                }
            }
        )
    }
    if (isThemeModeDialogVisible) {
        SettingsThemeModeDialog(
            selectedMode = state.appearanceState.themeMode,
            onDismiss = { isThemeModeDialogVisible = false },
            onModeSelected = { mode ->
                onThemeModeChange(mode)
                isThemeModeDialogVisible = false
            }
        )
    }
    if (isCacheLimitDialogVisible) {
        SettingsCacheLimitDialog(
            playbackState = state.playbackPreferencesState,
            cacheState = state.cacheState,
            onPlaybackCacheLimitChange = onPlaybackCacheLimitChange,
            onDismiss = { isCacheLimitDialogVisible = false },
            onSave = {
                onSavePlaybackCacheLimit()
                isCacheLimitDialogVisible = false
            }
        )
    }
    if (isPrewarmBudgetDialogVisible) {
        SettingsPrewarmBudgetDialog(
            selectedPreset = PlaybackPrewarmBudgetPreset.fromPreferences(
                state.playbackPreferencesState.prewarmPreferences.sanitized()
            ),
            onDismiss = { isPrewarmBudgetDialogVisible = false },
            onPresetChange = { preset ->
                onPlaybackPrewarmBudgetChange(preset)
                isPrewarmBudgetDialogVisible = false
            }
        )
    }
    if (isOnlineSourceImportDialogVisible) {
        SettingsOnlineSourceImportDialog(
            state = state.sourcesState,
            onImportUrlChange = onPendingImportUrlChange,
            onDismiss = { isOnlineSourceImportDialogVisible = false },
            onImport = {
                onImportAudioSourceFromUrl()
                isOnlineSourceImportDialogVisible = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .testTag("settings_scroll_content"),
                contentPadding = PaddingValues(
                    start = AccountVisualStyle.contentHorizontalPadding,
                    top = 4.dp,
                    end = AccountVisualStyle.contentHorizontalPadding,
                    bottom = 36.dp
                ),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                item {
                    SettingsAccountSection(
                        state = state.accountState,
                        onLoginClick = onLoginClick,
                        onLogoutClick = onShowLogoutConfirm
                    )
                }
                item {
                    SettingsAppearanceSection(
                        state = state.appearanceState,
                        onThemeModeClick = { isThemeModeDialogVisible = true }
                    )
                }
                item {
                    SettingsPlaybackPreferencesSection(
                        playbackState = state.playbackPreferencesState,
                        onShowPreferredAudioQualityDialog = onShowPreferredAudioQualityDialog,
                        onRestoreLastPlaybackOnStartupChange =
                            onRestoreLastPlaybackOnStartupChange,
                        onResumeFromLastPositionChange = onResumeFromLastPositionChange,
                        onWeakNetworkAutoRetryChange = onWeakNetworkAutoRetryChange
                    )
                }
                item {
                    SettingsCachePolicySection(
                        playbackState = state.playbackPreferencesState,
                        cacheState = state.cacheState,
                        onShowCacheLimitDialog = { isCacheLimitDialogVisible = true },
                        onShowCacheFailureNotificationsChange =
                            onShowCacheFailureNotificationsChange,
                        onPlaybackPrewarmEnabledChange = onPlaybackPrewarmEnabledChange,
                        onShowPrewarmBudgetDialog = {
                            isPrewarmBudgetDialogVisible = true
                        }
                    )
                }
                item {
                    SettingsCacheSection(
                        state = state.cacheState,
                        onRefresh = onRefreshCache,
                        onClear = onClearCache
                    )
                }
                item {
                    SettingsAudioSourcesSection(
                        state = state.sourcesState,
                        onShowOnlineImport = { isOnlineSourceImportDialogVisible = true },
                        onImportFromLocal = onImportAudioSourceFromLocal,
                        onSetActiveSource = onSetActiveAudioSource,
                        onRemoveSource = onRemoveAudioSource
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsThemeModeDialog(
    selectedMode: ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag("settings_theme_mode_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("选择主题模式") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_theme_mode_option_${mode.testTagSuffix}")
                            .selectable(
                                selected = selectedMode == mode,
                                role = Role.RadioButton,
                                onClick = { onModeSelected(mode) }
                            )
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = null
                        )
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SettingsAppearanceSection(
    state: SettingsAppearanceUiState,
    onThemeModeClick: () -> Unit
) {
    SettingsGroup(
        modifier = Modifier.testTag("settings_appearance_section"),
        title = "外观"
    ) {
        SettingsRow(
            title = "日间 / 夜间模式",
            subtitle = "选择跟随系统、日间模式或夜间模式",
            value = state.themeMode.displayName,
            modifier = Modifier.testTag("settings_theme_mode_trigger"),
            valueTestTag = "settings_theme_mode_current_value",
            onClick = onThemeModeClick
        )
    }
}

private val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "日间模式"
        ThemeMode.DARK -> "夜间模式"
    }

private val ThemeMode.testTagSuffix: String
    get() = wireValue

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionBoundary()
            content()
            SettingsSectionBoundary()
        }
    }
}

@Composable
private fun SettingsAccountSection(
    state: SettingsAccountUiState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_account_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!state.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = AccountVisualTheme.accentSoft,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = AccountVisualTheme.accent,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = state.title,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("settings_account_title")
                )
                Text(
                    text = state.summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.isLoggedIn) {
                TextButton(
                    onClick = onLogoutClick,
                    enabled = !state.isBusy,
                    modifier = Modifier.testTag("settings_logout_button"),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AccountVisualTheme.accentText
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "退出",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = onLoginClick,
                    enabled = !state.isBusy,
                    modifier = Modifier.testTag("settings_login_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccountVisualTheme.accent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("登录")
                }
            }
        }
        SettingsSectionBoundary()
    }
}

@Composable
private fun SettingsPlaybackPreferencesSection(
    playbackState: SettingsPlaybackPreferencesUiState,
    onShowPreferredAudioQualityDialog: () -> Unit,
    onRestoreLastPlaybackOnStartupChange: (Boolean) -> Unit,
    onResumeFromLastPositionChange: (Boolean) -> Unit,
    onWeakNetworkAutoRetryChange: (Boolean) -> Unit
) {
    val behaviorPreferences = playbackState.behaviorPreferences
    SettingsGroup(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_playback_preferences_section"),
        title = "播放"
    ) {
        SettingsRow(
            title = "默认音质",
            subtitle = "用于在线播放解析和起播",
            value = playbackState.preferredAudioQuality.displayName,
            valueTestTag = "settings_playback_quality_current_value",
            modifier = Modifier.testTag("settings_playback_quality_trigger"),
            enabled = !playbackState.isSavingPreferredAudioQuality,
            onClick = onShowPreferredAudioQualityDialog
        )
        SettingsDividerLine()
        SettingsSwitchRow(
            title = "恢复上次播放",
            subtitle = "冷启动后恢复最近队列和当前歌曲",
            checked = behaviorPreferences.restoreLastPlaybackOnStartup,
            onCheckedChange = onRestoreLastPlaybackOnStartupChange,
            modifier = Modifier.testTag("settings_restore_last_playback_switch"),
            switchTestTag = "settings_restore_last_playback_switch_control"
        )
        SettingsDividerLine()
        SettingsSwitchRow(
            title = "断点续播",
            subtitle = "恢复歌曲时跳到上次记录的进度",
            checked = behaviorPreferences.resumeFromLastPosition,
            onCheckedChange = onResumeFromLastPositionChange,
            modifier = Modifier.testTag("settings_resume_from_last_position_switch"),
            switchTestTag = "settings_resume_from_last_position_switch_control"
        )
        SettingsDividerLine()
        SettingsSwitchRow(
            title = "弱网自动重试",
            subtitle = "在线播放失败时重新解析并尝试起播",
            checked = behaviorPreferences.weakNetworkAutoRetry,
            onCheckedChange = onWeakNetworkAutoRetryChange,
            modifier = Modifier.testTag("settings_weak_network_retry_switch"),
            switchTestTag = "settings_weak_network_retry_switch_control"
        )
    }
}

@Composable
private fun SettingsCachePolicySection(
    playbackState: SettingsPlaybackPreferencesUiState,
    cacheState: SettingsCacheUiState,
    onShowCacheLimitDialog: () -> Unit,
    onShowCacheFailureNotificationsChange: (Boolean) -> Unit,
    onPlaybackPrewarmEnabledChange: (Boolean) -> Unit,
    onShowPrewarmBudgetDialog: () -> Unit
) {
    val cachePolicyPreferences = playbackState.cachePolicyPreferences
    val prewarmPreferences = playbackState.prewarmPreferences.sanitized()
    val prewarmPreset = PlaybackPrewarmBudgetPreset.fromPreferences(prewarmPreferences)
    SettingsGroup(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_cache_policy_section"),
        title = "缓存与预热"
    ) {
        SettingsRow(
            title = "歌曲缓存上限",
            subtitle = "只影响在线播放缓存",
            value = "${cacheState.playbackCacheLimitBytes / BYTES_PER_MB} MB",
            modifier = Modifier.testTag("settings_cache_limit_trigger"),
            onClick = onShowCacheLimitDialog
        )
        SettingsDividerLine()
        SettingsSwitchRow(
            title = "缓存失败时提示",
            subtitle = "缓存写入失败时提示，不和歌曲播放失败混淆",
            checked = cachePolicyPreferences.showCacheFailureNotifications,
            onCheckedChange = onShowCacheFailureNotificationsChange,
            modifier = Modifier.testTag("settings_cache_failure_notice_switch"),
            switchTestTag = "settings_cache_failure_notice_switch_control"
        )
        SettingsDividerLine()
        SettingsSwitchRow(
            title = "在线播放预热",
            subtitle = "有限缓存当前后续片段和下一首首段，不等同整首下载",
            checked = prewarmPreferences.enabled,
            onCheckedChange = onPlaybackPrewarmEnabledChange,
            modifier = Modifier.testTag("settings_playback_prewarm_switch"),
            switchTestTag = "settings_playback_prewarm_switch_control"
        )
        SettingsDividerLine()
        SettingsRow(
            title = "预热预算",
            subtitle = formatPrewarmBudgetSummary(prewarmPreferences),
            value = prewarmPreset.displayName,
            subtitleTestTag = "settings_playback_prewarm_budget_summary",
            modifier = Modifier.testTag("settings_playback_prewarm_budget_trigger"),
            onClick = onShowPrewarmBudgetDialog
        )
        SettingsDividerLine()
        SettingsRow(
            title = "缓存清理策略",
            subtitle = "超过容量上限时自动清理",
            value = "最近使用优先保留",
            valueTestTag = "settings_cache_cleanup_policy_value",
            modifier = Modifier.testTag("settings_cache_cleanup_policy")
        )
    }
}

@Composable
private fun SettingsCacheLimitDialog(
    playbackState: SettingsPlaybackPreferencesUiState,
    cacheState: SettingsCacheUiState,
    onPlaybackCacheLimitChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("歌曲缓存上限") },
        text = {
            Column(
                modifier = Modifier.testTag("settings_cache_limit_editor"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "只限制在线播放产生的缓存，不影响本地歌曲。当前 ${cacheState.playbackCacheLimitBytes / BYTES_PER_MB} MB。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cacheState.pendingPlaybackCacheLimitMb,
                    onValueChange = onPlaybackCacheLimitChange,
                    suffix = { Text("MB") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_playback_cache_limit_input")
                )
                cacheState.playbackCacheLimitMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccountVisualTheme.accentText,
                        modifier = Modifier.testTag("settings_playback_cache_limit_feedback")
                    )
                }
                playbackState.feedbackMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccountVisualTheme.accentText,
                        modifier = Modifier.testTag("settings_playback_quality_feedback")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !cacheState.isSavingPlaybackCacheLimit,
                modifier = Modifier.testTag("settings_playback_cache_limit_save")
            ) {
                if (cacheState.isSavingPlaybackCacheLimit) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsPrewarmBudgetDialog(
    selectedPreset: PlaybackPrewarmBudgetPreset,
    onDismiss: () -> Unit,
    onPresetChange: (PlaybackPrewarmBudgetPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预热预算") },
        text = {
            Column {
                PlaybackPrewarmBudgetPreset.entries.forEachIndexed { index, preset ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                        )
                    }
                    val isSelected = preset == selectedPreset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSelected) { onPresetChange(preset) }
                            .padding(vertical = 14.dp)
                            .testTag(
                                "settings_playback_prewarm_budget_${preset.name.lowercase()}"
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = preset.displayName,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatPrewarmBudgetSummary(
                                    preset.toPreferences(enabled = true)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccountVisualTheme.accentText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatPrewarmBudgetSummary(
    preferences: com.wxy.playerlite.playback.model.PlaybackPrewarmPreferences
): String {
    return "${preferences.budgetDurationMs / 1000} 秒 / ${preferences.budgetBytes / BYTES_PER_MB} MB"
}

@Composable
private fun SettingsCacheSection(
    state: SettingsCacheUiState,
    onRefresh: () -> Unit,
    onClear: () -> Unit
) {
    SettingsGroup(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_cache_section"),
        title = "缓存明细"
    ) {
        SettingsRow(
            title = "总占用",
            subtitle = "歌曲缓存和歌词缓存",
            value = formatStorageSize(state.snapshot?.totalBytes ?: 0L),
            modifier = Modifier.testTag("settings_cache_total")
        )
        state.snapshot?.entries?.forEach { entry ->
            SettingsDividerLine()
            SettingsRow(
                title = entry.label,
                subtitle = if (entry.kind == ManagedCacheKind.PLAYBACK) {
                    "Range 边播边缓存"
                } else {
                    "已保存的歌词资源"
                },
                value = formatStorageSize(entry.bytes)
            )
        }
        SettingsDividerLine()
        SettingsActionRow(
            title = if (state.isRefreshing) "正在刷新" else "刷新缓存统计",
            enabled = !state.isRefreshing && !state.isClearing,
            onClick = onRefresh,
            modifier = Modifier.testTag("settings_refresh_cache_button")
        )
        SettingsDividerLine()
        SettingsActionRow(
            title = if (state.isClearing) "正在清理" else "清理全部缓存",
            enabled = !state.isRefreshing && !state.isClearing,
            destructive = true,
            onClick = onClear,
            modifier = Modifier.testTag("settings_clear_cache_button")
        )
        state.feedbackMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isClearing) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    AccountVisualTheme.accentText
                },
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("settings_cache_feedback")
            )
        }
    }
}

@Composable
private fun SettingsAudioSourcesSection(
    state: SettingsSourcesUiState,
    onShowOnlineImport: () -> Unit,
    onImportFromLocal: () -> Unit,
    onSetActiveSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit
) {
    SettingsGroup(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_audio_sources_section"),
        title = "音源"
    ) {
        val activeSource = state.items.firstOrNull { it.isActive }
        SettingsRow(
            title = "当前音源",
            subtitle = activeSource?.sourceStatusSummary() ?: "导入或启用音源后用于在线播放解析",
            value = activeSource?.displayName ?: "未设置",
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .testTag("settings_audio_source_current_summary")
        )
        SettingsDividerLine()
        SettingsRow(
            title = "在线导入音源",
            subtitle = "通过音源清单地址添加",
            modifier = Modifier.testTag("settings_audio_source_import_url_trigger"),
            enabled = !state.isImporting,
            onClick = onShowOnlineImport
        )
        SettingsDividerLine()
        SettingsRow(
            title = "从本地文件导入",
            subtitle = "选择 JSON 音源清单",
            modifier = Modifier.testTag("settings_audio_source_import_local"),
            enabled = !state.isImporting,
            onClick = onImportFromLocal
        )
        state.importFeedbackMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AccountVisualTheme.accentText,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("settings_audio_source_feedback")
            )
        }
        state.validationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AccountVisualTheme.accentText,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("settings_audio_source_validation")
            )
        }
        if (state.items.isEmpty()) {
            SettingsDividerLine()
            Text(
                text = "还没有添加音源",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("settings_audio_source_empty_state")
            )
        } else {
            state.items.forEach { item ->
                SettingsDividerLine()
                AudioSourceRow(
                    item = item,
                    onSetActiveSource = onSetActiveSource,
                    onRemoveSource = onRemoveSource,
                    modifier = Modifier.testTag("settings_audio_source_item_${item.id}")
                )
            }
        }
    }
}

@Composable
private fun SettingsOnlineSourceImportDialog(
    state: SettingsSourcesUiState,
    onImportUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("在线导入音源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "粘贴音源 JSON 清单地址。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.pendingImportUrl,
                    onValueChange = onImportUrlChange,
                    label = { Text("在线导入地址") },
                    placeholder = { Text("https://cdn.example.com/source.json") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_audio_source_import_url_input")
                )
                state.validationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccountVisualTheme.accentText,
                        modifier = Modifier.testTag("settings_audio_source_validation")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = !state.isImporting,
                modifier = Modifier.testTag("settings_audio_source_import_url_submit")
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    switchTestTag: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = (switchTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                .scale(0.82f),
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccountVisualTheme.accent.copy(alpha = 0.86f),
                checkedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.28f
                )
            )
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    modifier: Modifier = Modifier,
    subtitleTestTag: String? = null,
    valueTestTag: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 2.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = subtitleTestTag?.let { Modifier.testTag(it) } ?: Modifier
                )
            }
        }
        if (value != null) {
            val valueModifier = Modifier.widthIn(max = 132.dp)
            Text(
                text = value,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = valueTestTag?.let { valueModifier.testTag(it) } ?: valueModifier
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun SettingsDividerLine() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    )
}

@Composable
private fun SettingsSectionBoundary() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = if (destructive) {
                AccountVisualTheme.accentText
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
        )
    }
}

@Composable
private fun AudioSourceRow(
    item: ManagedAudioSource,
    onSetActiveSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.displayName,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.sourceStatusSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.isActive) {
                Surface(
                    color = SettingsGreenSurface,
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        text = "当前",
                        style = MaterialTheme.typography.labelSmall,
                        color = SettingsGreenText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("settings_audio_source_current_${item.id}")
                    )
                }
            }
        }
        item.sourceMetadataSummary()?.let { metadata ->
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = item.baseUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        item.importUrl?.let { importUrl ->
            Text(
                text = "导入地址：$importUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        (item.initError ?: item.detailMessage)?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.initError != null) {
                    AccountVisualTheme.accentText
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.isActive) {
                TextButton(
                    onClick = { onSetActiveSource(item.id) },
                    enabled = item.enabled,
                    modifier = Modifier.testTag("settings_audio_source_activate_${item.id}")
                ) {
                    Text("设为当前")
                }
            }
            if (!item.isBuiltIn) {
                TextButton(
                    onClick = { onRemoveSource(item.id) },
                    modifier = Modifier.testTag("settings_audio_source_remove_${item.id}"),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AccountVisualTheme.accentText
                    )
                ) {
                    Text("删除")
                }
            }
        }
    }
}

private fun supportedSettingsAudioQualities(): List<PlaybackAudioQuality> {
    return PlaybackAudioQuality.descendingPreference.filterNot { it == PlaybackAudioQuality.VIVID }
}

private fun ManagedAudioSource.sourceMetadataSummary(): String? {
    return listOfNotNull(author, version?.let { "v$it" }).joinToString(" · ").takeIf { it.isNotBlank() }
}

private fun ManagedAudioSource.sourceStatusSummary(): String {
    return buildList {
        add(if (isBuiltIn) "内置源" else "已导入")
        add(resolverType.displayName)
        add(if (enabled) "已启用" else "已禁用")
        if (isActive) {
            add("当前音源")
        }
    }.joinToString(" · ")
}

private fun formatStorageSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    if (safeBytes < 1024L) {
        return "${safeBytes} B"
    }
    val units = listOf("KB", "MB", "GB", "TB")
    val digitGroup = (ln(safeBytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.size)
    val unitBase = 1024.0.pow(digitGroup.toDouble())
    val value = safeBytes / unitBase
    val unit = units[digitGroup - 1]
    return String.format("%.1f %s", value, unit)
}
