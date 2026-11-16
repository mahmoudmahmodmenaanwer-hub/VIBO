package com.example.ui.feed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.Video
import com.example.data.util.VideoStorageHelper
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ViboAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    currentUserId: String,
    onNavigateToUpload: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val activeCommentVideoId by viewModel.activeCommentVideoId.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = videosState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViboCoral)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "تعذر تحميل المقاطع",
                        subtitle = state.message
                    )
                }
            }

            is UiState.Success -> {
                val displayedVideos = remember(state.data, selectedTab, followingIds) {
                    when (selectedTab) {
                        FeedTab.FOR_YOU -> state.data
                        FeedTab.FOLLOWING -> state.data.filter { it.creatorId in followingIds }
                    }
                }

                if (displayedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == FeedTab.FOLLOWING) {
                            EmptyStateCard(
                                title = stringResource(R.string.feed_following_empty_title),
                                subtitle = stringResource(R.string.feed_following_empty_subtitle),
                                actionLabel = stringResource(R.string.feed_action_discover),
                                onActionClick = onNavigateToDiscover
                            )
                        } else {
                            EmptyStateCard(
                                title = stringResource(R.string.feed_empty_title),
                                subtitle = stringResource(R.string.feed_empty_subtitle),
                                actionLabel = stringResource(R.string.feed_action_upload),
                                onActionClick = onNavigateToUpload
                            )
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { displayedVideos.size }
                    )

                    LaunchedEffect(pagerState.settledPage, displayedVideos) {
                        val current = displayedVideos.getOrNull(pagerState.settledPage)
                        if (current != null) {
                            viewModel.recordViewIfNeeded(current.id)
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        key = { index -> displayedVideos[index].id },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("feed_vertical_pager")
                    ) { pageIndex ->
                        val video = displayedVideos[pageIndex]
                        val isActive = pagerState.settledPage == pageIndex && activeCommentVideoId == null
                        VideoFeedPage(
                            video = video,
                            isActive = isActive,
                            isMuted = isMuted,
                            isLiked = video.id in likedIds,
                            isFollowingCreator = video.creatorId in followingIds,
                            isOwnVideo = video.creatorId == currentUserId,
                            isSavingToPhone = savingVideoId == video.id,
                            onToggleMute = viewModel::toggleMute,
                            onLikeClick = { viewModel.toggleLike(video) },
                            onDoubleTapLike = { viewModel.toggleLike(video, forceLikeOnly = true) },
                            onCommentClick = { viewModel.openComments(video.id) },
                            onShareClick = {
                                viewModel.recordShare(video.id)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_video_subject))
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${video.title} - @${video.creatorUsername} على VIBO\n${video.videoUri}"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, context.getString(R.string.cd_share_video))
                                )
                            },
                            onSaveToPhoneClick = {
                                if (savingVideoId == null) {
                                    scope.launch {
                                        savingVideoId = video.id
                                        val result = VideoStorageHelper.saveVideoToPhoneGallery(
                                            context = context,
                                            videoUriString = video.videoUri,
                                            title = video.title
                                        )
                                        savingVideoId = null
                                        val msg = result.fold(
                                            onSuccess = { folder ->
                                                context.getString(R.string.save_video_success, folder)
                                            },
                                            onFailure = { err ->
                                                err.localizedMessage ?: context.getString(R.string.save_video_error)
                                            }
                                        )
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                            onCreatorClick = { onNavigateToUserProfile(video.creatorId) },
                            onFollowClick = { viewModel.toggleFollow(video.creatorId) }
                        )
                    }
                }

                val commentTargetVideo = remember(activeCommentVideoId, state.data) {
                    state.data.firstOrNull { it.id == activeCommentVideoId }
                }
                if (commentTargetVideo != null) {
                    CommentsBottomSheet(
                        comments = activeComments,
                        onDismiss = viewModel::closeComments,
                        onSubmitComment = { text -> viewModel.addComment(commentTargetVideo, text) },
                        onAuthorClick = { authorId ->
                            viewModel.closeComments()
                            onNavigateToUserProfile(authorId)
                        }
                    )
                }
            }
        }

        // Top Overlay Tabs: تتابعهم | لك
        FeedTopTabBar(
            selectedTab = selectedTab,
            onTabSelected = viewModel::selectTab,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        )
    }
}

@Composable
fun SingleVideoViewerScreen(
    videoId: String,
    viewModel: FeedViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val activeCommentVideoId by viewModel.activeCommentVideoId.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var savingVideoId by remember { mutableStateOf<String?>(null) }

    val allVideos = (videosState as? UiState.Success)?.data.orEmpty()
    val startIndex = remember(allVideos, videoId) {
        allVideos.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (allVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ViboCoral)
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = startIndex,
                pageCount = { allVideos.size }
            )
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { allVideos[it].id },
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val video = allVideos[pageIndex]
                VideoFeedPage(
                    video = video,
                    isActive = pagerState.settledPage == pageIndex && activeCommentVideoId == null,
                    isMuted = isMuted,
                    isLiked = video.id in likedIds,
                    isFollowingCreator = video.creatorId in followingIds,
                    isOwnVideo = video.creatorId == currentUserId,
                    isSavingToPhone = savingVideoId == video.id,
                    onToggleMute = viewModel::toggleMute,
                    onLikeClick = { viewModel.toggleLike(video) },
                    onDoubleTapLike = { viewModel.toggleLike(video, forceLikeOnly = true) },
                    onCommentClick = { viewModel.openComments(video.id) },
                    onShareClick = {
                        viewModel.recordShare(video.id)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${video.title} - @${video.creatorUsername} على VIBO")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.cd_share_video)))
                    },
                    onSaveToPhoneClick = {
                        if (savingVideoId == null) {
                            scope.launch {
                                savingVideoId = video.id
                                val result = VideoStorageHelper.saveVideoToPhoneGallery(
                                    context = context,
                                    videoUriString = video.videoUri,
                                    title = video.title
                                )
                                savingVideoId = null
                                val msg = result.fold(
                                    onSuccess = { folder ->
                                        context.getString(R.string.save_video_success, folder)
                                    },
                                    onFailure = { err ->
                                        err.localizedMessage ?: context.getString(R.string.save_video_error)
                                    }
                                )
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    onCreatorClick = { onNavigateToUserProfile(video.creatorId) },
                    onFollowClick = { viewModel.toggleFollow(video.creatorId) }
                )
            }

            val commentTargetVideo = allVideos.firstOrNull { it.id == activeCommentVideoId }
            if (commentTargetVideo != null) {
                CommentsBottomSheet(
                    comments = activeComments,
                    onDismiss = viewModel::closeComments,
                    onSubmitComment = { text -> viewModel.addComment(commentTargetVideo, text) },
                    onAuthorClick = { authorId ->
                        viewModel.closeComments()
                        onNavigateToUserProfile(authorId)
                    }
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag("video_detail_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = Color.White
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        )
    }
}

@Composable
private fun FeedTopTabBar(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeedTabChip(
                label = stringResource(R.string.feed_tab_following),
                selected = selectedTab == FeedTab.FOLLOWING,
                onClick = { onTabSelected(FeedTab.FOLLOWING) },
                testTag = "tab_following"
            )
            FeedTabChip(
                label = stringResource(R.string.feed_tab_for_you),
                selected = selectedTab == FeedTab.FOR_YOU,
                onClick = { onTabSelected(FeedTab.FOR_YOU) },
                testTag = "tab_for_you"
            )
        }
    }
}

@Composable
private fun FeedTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.68f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (selected) ViboCoral else Color.Transparent)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoFeedPage(
    video: Video,
    isActive: Boolean,
    isMuted: Boolean,
    isLiked: Boolean,
    isFollowingCreator: Boolean,
    isOwnVideo: Boolean,
    isSavingToPhone: Boolean = false,
    onToggleMute: () -> Unit,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveToPhoneClick: () -> Unit = {},
    onCreatorClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "likeScale"
    )
    val discTransition = rememberInfiniteTransition(label = "disc")
    val discRotation by discTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayerBox(
            videoUri = video.videoUri,
            isActive = isActive,
            isMuted = isMuted,
            onDoubleTapLike = onDoubleTapLike,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient scrim for crisp Arabic text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC07070E))
                    )
                )
        )

        // Creator info & video details (bottom start -> right in RTL)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 22.dp)
        ) {
            Surface(
                color = ViboCoral.copy(alpha = 0.24f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = video.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "@${video.creatorUsername}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clickable(onClick = onCreatorClick)
                    .padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (video.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (video.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    video.hashtags.take(4).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = ViboCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Right-hand (End -> Left in RTL) floating glassmorphic action rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                ViboAvatar(
                    avatarUrl = video.creatorAvatarUrl,
                    displayName = video.creatorDisplayName,
                    size = 50.dp,
                    modifier = Modifier
                        .clickable(onClick = onCreatorClick)
                        .testTag("feed_creator_avatar")
                )
                if (!isOwnVideo && !isFollowingCreator) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ViboCoral)
                            .clickable(onClick = onFollowClick)
                            .testTag("feed_follow_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_follow_creator),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            FeedActionButton(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                count = formatCompactCount(video.likesCount),
                tint = if (isLiked) ViboCoral else Color.White,
                contentDescription = stringResource(R.string.cd_like_video),
                iconModifier = Modifier.scale(likeScale),
                onClick = onLikeClick,
                testTag = "feed_like_button"
            )

            FeedActionButton(
                icon = Icons.Rounded.ChatBubble,
                count = formatCompactCount(video.commentsCount),
                tint = Color.White,
                contentDescription = stringResource(R.string.cd_comment_video),
                onClick = onCommentClick,
                testTag = "feed_comment_button"
            )

            FeedActionButton(
                icon = Icons.Rounded.Share,
                count = formatCompactCount(video.sharesCount),
                tint = Color.White,
                contentDescription = stringResource(R.string.cd_share_video),
                onClick = onShareClick,
                testTag = "feed_share_button"
            )

            FeedActionButton(
                icon = Icons.Rounded.Download,
                count = stringResource(
                    if (isSavingToPhone) R.string.saving_video_label else R.string.save_video_label
                ),
                tint = if (isSavingToPhone) ViboCyan else Color.White,
                contentDescription = stringResource(R.string.cd_save_to_phone),
                onClick = onSaveToPhoneClick,
                testTag = "feed_save_phone_button"
            )

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = stringResource(R.string.cd_mute_toggle),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .rotate(if (isActive) discRotation else 0f)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E32))
                    .border(2.dp, Brush.linearGradient(listOf(ViboCoral, ViboCyan)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedActionButton(
    icon: ImageVector,
    count: String,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    iconModifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}
