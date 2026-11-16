package com.example.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ViboAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onVideoClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.discover_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = {
                Text(
                    text = stringResource(R.string.discover_search_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ViboCoral,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("discover_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DISCOVER_CATEGORIES) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(category) },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ViboCoral,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (val state = discoverState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViboCoral)
                }
            }

            is UiState.Error -> {
                EmptyStateCard(
                    title = "تعذر تحميل البحث",
                    subtitle = state.message
                )
            }

            is UiState.Success -> {
                val data = state.data
                if (data.videos.isEmpty() && data.creators.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            title = stringResource(R.string.discover_no_results),
                            subtitle = "جرّب البحث بكلمة أخرى أو استعراض تصنيف مختلف."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (data.creators.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.discover_creators_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(data.creators, key = { it.userId }) { creator ->
                                        CreatorDiscoverCard(
                                            creator = creator,
                                            isFollowing = creator.userId in followingIds,
                                            onClick = { onCreatorClick(creator.userId) },
                                            onFollowToggle = { viewModel.toggleFollow(creator.userId) }
                                        )
                                    }
                                }
                            }
                        }

                        if (data.videos.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.discover_videos_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }

                            val rows = data.videos.chunked(2)
                            items(rows) { rowVideos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        VideoGridCard(
                                            video = video,
                                            onClick = { onVideoClick(video.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowVideos.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorDiscoverCard(
    creator: UserProfile,
    isFollowing: Boolean,
    onClick: () -> Unit,
    onFollowToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViboAvatar(
                avatarUrl = creator.avatarUrl,
                displayName = creator.displayName,
                size = 56.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = creator.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${creator.username}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowToggle,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = stringResource(R.string.following_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Button(
                    onClick = onFollowToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = ViboCoral, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = stringResource(R.string.follow_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1F1635), Color(0xFF121829))
                )
            )
            .clickable(onClick = onClick)
            .testTag("video_grid_card_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Surface(
            color = ViboCoral.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = video.category,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD07070E)))
                )
                .padding(12.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${video.creatorUsername}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ViboCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = ViboCoral,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatCompactCount(video.likesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
