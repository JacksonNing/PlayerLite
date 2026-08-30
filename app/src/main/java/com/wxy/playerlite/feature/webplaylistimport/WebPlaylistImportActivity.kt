package com.wxy.playerlite.feature.webplaylistimport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme
import com.wxy.playerlite.feature.detail.createOpenPlayerAfterQueueReplacementIntent
import com.wxy.playerlite.feature.user.LoginActivity
import com.wxy.playerlite.ui.theme.PlayerLiteTheme

class WebPlaylistImportActivity : ComponentActivity() {
    private val viewModel: WebPlaylistImportViewModel by viewModels {
        WebPlaylistImportViewModel.factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayerLiteTheme {
                val state = viewModel.uiStateFlow.collectAsStateWithLifecycle().value
                BackHandler(onBack = ::finish)
                LaunchedEffect(viewModel) {
                    viewModel.uiEvents.collect { event ->
                        when (event) {
                            WebPlaylistImportUiEvent.OpenPlayer -> {
                                startActivity(
                                    createOpenPlayerAfterQueueReplacementIntent(
                                        this@WebPlaylistImportActivity
                                    )
                                )
                                finish()
                            }

                            is WebPlaylistImportUiEvent.ShowMessage -> {
                                Toast.makeText(
                                    this@WebPlaylistImportActivity,
                                    event.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                WebPlaylistImportScreen(
                    state = state,
                    onBack = ::finish,
                    onUrlChanged = viewModel::onUrlChanged,
                    onSubmit = viewModel::submitUrl,
                    onConfirmImport = viewModel::confirmImport,
                    onOpenLogin = {
                        startActivity(LoginActivity.createIntent(this@WebPlaylistImportActivity))
                    }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, WebPlaylistImportActivity::class.java)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPlaylistImportScreen(
    state: WebPlaylistImportUiState,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirmImport: () -> Unit,
    onOpenLogin: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("导入歌单") },
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
        when (val stage = state.stage) {
            WebPlaylistImportStage.Input -> {
                WebPlaylistImportInputContent(
                    state = state,
                    innerPadding = innerPadding,
                    onUrlChanged = onUrlChanged,
                    onSubmit = onSubmit
                )
            }

            WebPlaylistImportStage.LoginRequired -> {
                WebPlaylistImportLoginRequiredContent(
                    innerPadding = innerPadding,
                    onOpenLogin = onOpenLogin
                )
            }

            is WebPlaylistImportStage.Loading -> {
                WebPlaylistImportLoadingContent(
                    innerPadding = innerPadding,
                    message = stage.message
                )
            }

            is WebPlaylistImportStage.Preview -> {
                WebPlaylistImportPreviewContent(
                    innerPadding = innerPadding,
                    snapshot = stage.snapshot,
                    isImporting = stage.isImporting,
                    onConfirmImport = onConfirmImport
                )
            }

            is WebPlaylistImportStage.Error -> {
                WebPlaylistImportErrorContent(
                    innerPadding = innerPadding,
                    title = stage.title,
                    message = stage.message,
                    onRetry = onSubmit
                )
            }
        }
    }
}

@Composable
private fun WebPlaylistImportInputContent(
    state: WebPlaylistImportUiState,
    innerPadding: PaddingValues,
    onUrlChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "粘贴网页歌单链接",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "当前支持网易云歌单和 QQ 音乐歌单。第一版会先读取歌单信息，再进入导入预览。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = state.inputUrl,
            onValueChange = onUrlChanged,
            label = { Text("歌单网页地址") },
            placeholder = { Text("https://music.163.com/#/playlist?id=...") },
            supportingText = {
                state.inputErrorMessage?.let { Text(text = it) }
            },
            isError = state.inputErrorMessage != null,
            minLines = 3,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("web_playlist_import_url_field")
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("web_playlist_import_submit")
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始解析")
        }
    }
}

@Composable
private fun WebPlaylistImportLoginRequiredContent(
    innerPadding: PaddingValues,
    onOpenLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("web_playlist_import_login_required"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "导入前需要登录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "当前导入流程需要在线搜索和播放能力，先完成登录后再继续。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onOpenLogin,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("web_playlist_import_login_button")
            ) {
                Text("去登录")
            }
        }
    }
}

@Composable
private fun WebPlaylistImportLoadingContent(
    innerPadding: PaddingValues,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("web_playlist_import_loading"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun WebPlaylistImportPreviewContent(
    innerPadding: PaddingValues,
    snapshot: ImportedPlaylistSnapshot,
    isImporting: Boolean,
    onConfirmImport: () -> Unit
) {
    val summary = snapshot.summary
    val importableCount = summary.importableCount
    val matchingProgress = snapshot.matchingProgress
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("web_playlist_import_preview"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImportPreviewCover(
                    coverUrl = snapshot.coverUrl,
                    size = 96.dp,
                    cornerRadius = 20.dp,
                    tag = "web_playlist_import_preview_cover"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = snapshot.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("web_playlist_import_preview_title")
                    )
                    Text(
                        text = "${snapshot.source.wireValue} · ${snapshot.creatorName.ifBlank { "未知创建者" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (snapshot.description.isNotBlank()) {
                        Text(
                            text = snapshot.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ImportSummaryRow(
                    tag = "web_playlist_import_summary_total",
                    label = "总曲目数",
                    value = summary.totalCount.toString()
                )
                ImportSummaryRow(
                    tag = "web_playlist_import_progress",
                    label = if (matchingProgress.isPaused) "匹配进度（已暂停）" else "匹配进度",
                    value = matchingProgress.progressText
                )
                if (matchingProgress.isPaused) {
                    Text(
                        text = matchingProgress.pauseMessage ?: "匹配已暂停，请稍后重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("web_playlist_import_paused_notice")
                    )
                }
                ImportSummaryRow(
                    tag = "web_playlist_import_summary_direct",
                    label = "直接可导入",
                    value = summary.directCount.toString()
                )
                ImportSummaryRow(
                    tag = "web_playlist_import_summary_matched",
                    label = "匹配成功",
                    value = summary.matchedCount.toString()
                )
                ImportSummaryRow(
                    tag = "web_playlist_import_summary_ambiguous",
                    label = "存在歧义",
                    value = summary.ambiguousCount.toString()
                )
                ImportSummaryRow(
                    tag = "web_playlist_import_summary_unmatched",
                    label = "未匹配",
                    value = summary.unmatchedCount.toString()
                )
                Button(
                    onClick = onConfirmImport,
                    enabled = importableCount > 0 && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = 16.dp)
                        .testTag("web_playlist_import_confirm")
                ) {
                    Text(
                        if (isImporting) {
                            "正在导入..."
                        } else if (importableCount > 0) {
                            "导入 $importableCount 首到当前播放列表"
                        } else {
                            "当前没有可导入歌曲"
                        }
                    )
                }
                if (importableCount == 0) {
                    Text(
                        text = "当前仅会导入 direct 与 matched 条目",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("web_playlist_import_confirm_disabled_reason")
                    )
                }
            }
        }
        itemsIndexed(
            items = snapshot.tracks,
            key = { index, item -> item.sourceTrackId ?: "${item.title}-$index" }
        ) { index, track ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        thickness = 0.5.dp,
                        color = PlayerLiteVisualTheme.colors.dividerSubtle
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImportPreviewCover(
                        coverUrl = track.coverUrl,
                        size = 56.dp,
                        cornerRadius = 12.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${track.title}",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = track.artistText.ifBlank { "未知歌手" },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (track.albumTitle.isNotBlank()) {
                            Text(
                                text = track.albumTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = track.resolution.asLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewCover(
    coverUrl: String?,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    tag: String? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportSummaryRow(
    tag: String,
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        if (tag != "web_playlist_import_summary_total") {
            HorizontalDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WebPlaylistImportErrorContent(
    innerPadding: PaddingValues,
    title: String,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("web_playlist_import_error"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("web_playlist_import_retry")
            ) {
                Text("重试")
            }
        }
    }
}

private fun ImportedTrackResolution.asLabel(): String {
    return when (this) {
        ImportedTrackResolution.Pending -> "待匹配"
        is ImportedTrackResolution.Direct -> "可直接导入"
        is ImportedTrackResolution.Matched -> "已匹配"
        is ImportedTrackResolution.Ambiguous -> "存在歧义"
        ImportedTrackResolution.Unmatched -> "未匹配"
    }
}
