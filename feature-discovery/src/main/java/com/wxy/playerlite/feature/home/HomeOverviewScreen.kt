package com.wxy.playerlite.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeOverviewScreen(
    overviewState: HomeOverviewUiState,
    bottomContentPadding: Dp,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = onRetry,
    avatarUrl: String? = null,
    currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(visualTokens.canvas)
    ) {
        PullToRefreshBox(
            isRefreshing = overviewState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_discovery_list"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = HomeDiscoveryLayoutSpec.pageTopPadding,
                    end = 20.dp,
                    bottom = bottomContentPadding + navigationBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(HomeDiscoveryLayoutSpec.sectionSpacing)
            ) {
                item(key = "home_header") {
                    HomeHeader(
                        greeting = homeGreetingForHour(currentHour),
                        keyword = overviewState.currentSearchKeyword,
                        avatarUrl = avatarUrl,
                        onSearchClick = onSearchClick
                    )
                }

                if (overviewState.errorMessage != null && overviewState.sections.isNotEmpty()) {
                    item {
                        HomeOverviewInlineError(
                            message = overviewState.errorMessage,
                            onRetry = onRetry
                        )
                    }
                }

                when {
                    overviewState.isLoading && overviewState.sections.isEmpty() -> {
                        item {
                            HomeOverviewStatusCard(
                                title = "发现内容加载中",
                                subtitle = "正在同步首页推荐内容，请稍候。"
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    !overviewState.isLoading && overviewState.sections.isEmpty() && overviewState.errorMessage != null -> {
                        item {
                            HomeOverviewStatusCard(
                                title = "首页加载失败",
                                subtitle = overviewState.errorMessage
                            ) {
                                OutlinedButton(onClick = onRetry) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("重新加载")
                                }
                            }
                        }
                    }

                    !overviewState.isLoading && overviewState.sections.isEmpty() -> {
                        item {
                            HomeOverviewStatusCard(
                                title = "首页暂无发现内容",
                                subtitle = "稍后再来看看新的推荐内容。"
                            )
                        }
                    }

                    else -> {
                        items(
                            items = overviewState.sections,
                            key = { section -> section.code }
                        ) { section ->
                            HomeDiscoverySection(
                                section = section,
                                onAction = onAction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    keyword: String,
    avatarUrl: String?,
    onSearchClick: () -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("home_header_greeting")
                )
                Text(
                    text = "听点什么？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = visualTokens.textSecondary
                )
            }
            Surface(
                modifier = Modifier
                    .size(HomeDiscoveryLayoutSpec.headerAvatarSize)
                    .testTag("home_header_avatar"),
                shape = CircleShape,
                color = visualTokens.surfaceHighlight,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = visualTokens.accentSupport,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("home_header_avatar_placeholder")
                    )
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "用户头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .testTag("home_header_avatar_image"),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        HomeSearchBox(
            keyword = keyword,
            onClick = onSearchClick
        )
    }
}

internal fun homeGreetingForHour(hour: Int): String {
    return when (hour) {
        in 5..11 -> "早上好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }
}

@Composable
private fun HomeSearchBox(
    keyword: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HomeDiscoveryLayoutSpec.searchBoxHeight)
            .testTag("home_search_box_container"),
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.searchBoxCornerRadius),
        color = visualTokens.surfacePrimary,
        tonalElevation = 0.dp,
        shadowElevation = HomeDiscoveryLayoutSpec.searchBoxShadowElevation,
        border = BorderStroke(
            width = 1.dp,
            color = visualTokens.dividerSubtle
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.testTag("home_search_box"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = visualTokens.textSecondary
                )
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeOverviewInlineError(
    message: String,
    onRetry: () -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Surface(
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.standardCardCornerRadius),
        color = visualTokens.surfaceMuted,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = visualTokens.dividerSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun HomeOverviewStatusCard(
    title: String,
    subtitle: String,
    actionContent: @Composable (() -> Unit)? = null
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Surface(
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.standardCardCornerRadius),
        color = visualTokens.surfacePrimary,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = visualTokens.dividerSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            actionContent?.invoke()
        }
    }
}

@Composable
private fun HomeDiscoverySection(
    section: HomeSectionUiModel,
    onAction: (HomeAction) -> Unit
) {
    val usesSongLayout = section.usesSongCardLayout()
    val usesEditorialLayout = section.usesEditorialCarousel()
    val displayTitle = when {
        usesEditorialLayout -> "今日推荐"
        else -> section.title
    }
    val playAllAction = if (usesSongLayout) {
        (section.items.firstOrNull()?.action as? HomeAction.ReplaceQueueAndOpenPlayer)
            ?.copy(activeIndex = 0)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_${section.code}"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (displayTitle.isNotBlank()) {
            HomeSectionTitle(
                title = displayTitle,
                trailingLabel = if (playAllAction != null) "播放全部" else null,
                onTrailingClick = playAllAction?.let { action ->
                    { onAction(action) }
                }
            )
        }

        if (usesEditorialLayout) {
            HomeBannerCarousel(
                items = section.items,
                onItemClick = onAction
            )
        } else if (usesSongLayout) {
            val songColumns = if (section.usesSongCardLayout()) {
                section.items.chunked(HomeDiscoveryLayoutSpec.songColumnItemCount)
            } else {
                emptyList()
            }
            val pagerState = rememberPagerState(pageCount = { songColumns.size })
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_song_pager"),
                    contentPadding = HomeDiscoveryLayoutSpec.songSectionContentPadding,
                    pageSpacing = HomeDiscoveryLayoutSpec.itemSpacing
                ) { columnIndex ->
                    val items = songColumns[columnIndex]
                    HomeSongColumn(
                        columnIndex = columnIndex,
                        startIndex = columnIndex * HomeDiscoveryLayoutSpec.songColumnItemCount,
                        items = items,
                        onAction = onAction
                    )
                }
                if (songColumns.size > 1) {
                    HomePagerIndicator(
                        pageCount = songColumns.size,
                        selectedPage = pagerState.currentPage
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = HomeDiscoveryLayoutSpec.rowContentPadding,
                horizontalArrangement = Arrangement.spacedBy(HomeDiscoveryLayoutSpec.itemSpacing)
            ) {
                items(
                    items = section.items,
                    key = { item -> item.id }
                ) { item ->
                    when (section.layout) {
                        HomeSectionLayout.ICON_GRID -> CompactSectionCard(
                            item = item,
                            onClick = { onAction(item.action) }
                        )

                        else -> DiscoverySectionCard(
                            item = item,
                            onClick = { onAction(item.action) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePagerIndicator(
    pageCount: Int,
    selectedPage: Int
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(
                        width = if (selectedPage == index) 14.dp else 4.dp,
                        height = 4.dp
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selectedPage == index) {
                            visualTokens.accentStrong
                        } else {
                            visualTokens.dividerSubtle
                        }
                    )
            )
        }
    }
}

@Composable
private fun HomeBannerCarousel(
    items: List<HomeSectionItemUiModel>,
    onItemClick: (HomeAction) -> Unit
) {
    val actualCount = items.size
    if (actualCount == 0) {
        return
    }
    val pageCount = if (actualCount > 1 && HomeDiscoveryLayoutSpec.bannerUsesInfiniteLoop) {
        HomeDiscoveryLayoutSpec.virtualBannerPageCount
    } else {
        actualCount
    }
    val initialPage = HomeDiscoveryLayoutSpec.initialBannerPage(actualCount)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )
    LaunchedEffect(actualCount, pagerState) {
        if (actualCount <= 1 || !HomeDiscoveryLayoutSpec.bannerUsesInfiniteLoop) {
            return@LaunchedEffect
        }
        snapshotFlow { pagerState.settledPage }
            .collectLatest { page ->
                val recenteredPage = HomeDiscoveryLayoutSpec.recenterBannerPage(
                    currentPage = page,
                    itemCount = actualCount
                )
                if (recenteredPage != page) {
                    pagerState.scrollToPage(recenteredPage)
                }
            }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeDiscoveryLayoutSpec.bannerHeight),
        contentPadding = HomeDiscoveryLayoutSpec.bannerContentPadding,
        pageSpacing = HomeDiscoveryLayoutSpec.itemSpacing
    ) { page ->
        val itemIndex = page % actualCount
        BannerSectionCard(
            item = items[itemIndex],
            onClick = { onItemClick(items[itemIndex].action) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BannerSectionCard(
    item: HomeSectionItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("home_banner_card_${item.id}"),
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.bannerCardCornerRadius),
        color = visualTokens.surfaceMuted,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        Box {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(visualTokens.surfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = visualTokens.accentStrong.copy(alpha = 0.72f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.14f),
                                Color.Black.copy(alpha = 0.62f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .padding(start = 18.dp, top = 18.dp, end = 74.dp, bottom = 18.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!item.badge.isNullOrBlank() && item.badge != item.title) {
                    Text(
                        text = item.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("home_banner_badge_${item.id}")
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = HomeDiscoveryLayoutSpec.titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = HomeDiscoveryLayoutSpec.subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(46.dp),
                shape = CircleShape,
                color = visualTokens.surfacePrimary.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = visualTokens.accentStrong,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverySectionCard(
    item: HomeSectionItemUiModel,
    onClick: () -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val hasArtwork = !item.imageUrl.isNullOrBlank()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .testTag("home_discovery_card_${item.id}")
            .width(HomeDiscoveryLayoutSpec.discoveryCardWidth)
            .height(HomeDiscoveryLayoutSpec.discoveryCardHeight),
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.standardCardCornerRadius),
        color = if (hasArtwork) visualTokens.surfaceMuted else visualTokens.surfaceHighlight,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasArtwork) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.62f)
                                )
                            )
                        )
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = visualTokens.accentStrong.copy(alpha = 0.68f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasArtwork) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactSectionCard(
    item: HomeSectionItemUiModel,
    onClick: () -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val icon = resolveCompactSectionCardIcon(item)
    val isPrimary = item.title.contains("每日推荐")
    Surface(
        onClick = onClick,
        modifier = Modifier
            .testTag("home_compact_card_${item.id}")
            .width(HomeDiscoveryLayoutSpec.compactCardWidth)
            .height(HomeDiscoveryLayoutSpec.compactCardHeight),
        shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.compactCardCornerRadius),
        color = visualTokens.surfaceMuted,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = visualTokens.dividerSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Surface(
                modifier = Modifier
                    .size(HomeDiscoveryLayoutSpec.compactImageSize)
                    .testTag("home_compact_card_icon_${item.id}"),
                shape = RoundedCornerShape(18.dp),
                color = if (isPrimary) visualTokens.accentStrong else visualTokens.surfacePrimary,
                tonalElevation = 0.dp,
                shadowElevation = if (isPrimary) 6.dp else 2.dp,
                border = if (isPrimary) {
                    null
                } else {
                    BorderStroke(
                        width = 1.dp,
                        color = visualTokens.dividerSubtle
                    )
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPrimary) Color.White else visualTokens.accentStrong,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = HomeDiscoveryLayoutSpec.titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeSongColumn(
    columnIndex: Int,
    startIndex: Int,
    items: List<HomeSectionItemUiModel>,
    onAction: (HomeAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_song_column_$columnIndex")
    ) {
        val visualTokens = PlayerLiteVisualTheme.colors
        items.forEachIndexed { itemIndex, item ->
            HomeSongRow(
                position = startIndex + itemIndex + 1,
                item = item,
                onAction = onAction
            )
            if (itemIndex != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = HomeDiscoveryLayoutSpec.songDividerStartPadding,
                            end = 2.dp,
                            top = HomeDiscoveryLayoutSpec.songDividerVerticalPadding,
                            bottom = HomeDiscoveryLayoutSpec.songDividerVerticalPadding
                        )
                        .height(1.dp)
                        .background(
                            visualTokens.dividerSubtle.copy(
                                alpha = HomeDiscoveryLayoutSpec.songDividerAlpha
                            )
                        )
                        .testTag("home_song_divider_${columnIndex}_$itemIndex")
                )
            }
        }
    }
}

@Composable
private fun HomeSongRow(
    position: Int,
    item: HomeSectionItemUiModel,
    onAction: (HomeAction) -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val songCard = item.songCard ?: return
    var menuExpanded by remember(item.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeDiscoveryLayoutSpec.songCardHeight)
            .testTag("home_song_row_${item.id}")
            .clickable { onAction(item.action) }
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = position.toString().padStart(length = 2, padChar = '0'),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = visualTokens.accentStrong,
            textAlign = TextAlign.Start,
            modifier = Modifier.width(28.dp)
        )
        Surface(
            modifier = Modifier
                .size(HomeDiscoveryLayoutSpec.songCardCoverSize)
                .testTag("home_song_row_cover_${item.id}"),
            shape = RoundedCornerShape(HomeDiscoveryLayoutSpec.songCardCoverCornerRadius),
            color = visualTokens.surfaceMuted
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = visualTokens.accentStrong
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = songCard.metadataLine,
                style = MaterialTheme.typography.bodySmall,
                color = visualTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatSongDuration(songCard.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = visualTokens.textSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp)
        )
        Box(
            modifier = Modifier.width(HomeDiscoveryLayoutSpec.songCardMenuButtonSize),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(HomeDiscoveryLayoutSpec.songCardMenuButtonSize)
                    .testTag("home_song_row_more_${item.id}"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = visualTokens.textSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多操作",
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                songCard.menuActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            menuExpanded = false
                            onAction(action.action)
                        }
                    )
                }
            }
        }
    }
}

private fun HomeSectionUiModel.usesSongCardLayout(): Boolean {
    if (layout != HomeSectionLayout.HORIZONTAL_LIST || items.isEmpty()) {
        return false
    }
    return items.all { it.songCard != null }
}

private fun HomeSectionUiModel.usesEditorialCarousel(): Boolean {
    return HomeDiscoveryLayoutSpec.usesCarousel(layout) ||
        code.contains("PLAYLIST_RCMD", ignoreCase = true) ||
        title == "推荐歌单"
}

@Composable
private fun HomeSectionTitle(
    title: String,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontSize = HomeDiscoveryLayoutSpec.sectionTitleFontSize,
            lineHeight = HomeDiscoveryLayoutSpec.sectionTitleLineHeight,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = HomeDiscoveryLayoutSpec.sectionTitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("home_section_title")
        )
        if (trailingLabel != null && onTrailingClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onTrailingClick)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .testTag("home_song_play_all"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = visualTokens.accentStrong,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = visualTokens.accentStrong,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatSongDuration(durationMs: Long): String {
    if (durationMs <= 0L) {
        return "--:--"
    }
    val totalSeconds = durationMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}

private fun resolveCompactSectionCardIcon(
    item: HomeSectionItemUiModel
): androidx.compose.ui.graphics.vector.ImageVector {
    val normalizedTitle = item.title.lowercase()
    return when {
        normalizedTitle.contains("搜索") -> Icons.Rounded.Search
        normalizedTitle.contains("歌单") -> Icons.Rounded.LibraryMusic
        normalizedTitle.contains("推荐") || normalizedTitle.contains("私人") -> Icons.Rounded.Home
        item.id.hashCode().mod(2) == 0 -> Icons.Rounded.LibraryMusic
        else -> Icons.Rounded.Album
    }
}
