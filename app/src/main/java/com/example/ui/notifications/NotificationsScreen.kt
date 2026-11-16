package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ViboAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpenVideo: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.notifications_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = ViboCoral,
                        shape = CircleShape
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (unreadCount > 0) {
                TextButton(
                    onClick = viewModel::markAllAsRead,
                    modifier = Modifier.testTag("mark_all_read_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = ViboCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.notifications_mark_all),
                        color = ViboCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        when (val state = notificationsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViboCoral)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        title = "تعذر تحميل الإشعارات",
                        subtitle = state.message
                    )
                }
            }

            is UiState.Success -> {
                val items = state.data
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            title = stringResource(R.string.notifications_empty_title),
                            subtitle = stringResource(R.string.notifications_empty_subtitle)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.id }) { notification ->
                            NotificationItemCard(
                                notification = notification,
                                onClick = {
                                    if (!notification.isRead) {
                                        viewModel.markAsRead(notification.id)
                                    }
                                    if (notification.videoId.isNotBlank()) {
                                        onOpenVideo(notification.videoId)
                                    } else if (notification.senderId.isNotBlank()) {
                                        onOpenUserProfile(notification.senderId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val badgeIcon = when (notification.type) {
        "like" -> Icons.Rounded.Favorite
        "comment" -> Icons.Rounded.ChatBubble
        else -> Icons.Rounded.PersonAdd
    }
    val badgeColor = when (notification.type) {
        "like" -> ViboCoral
        "comment" -> ViboCyan
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = if (!notification.isRead) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(18.dp),
        tonalElevation = if (!notification.isRead) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                ViboAvatar(
                    avatarUrl = notification.senderAvatarUrl,
                    displayName = notification.senderDisplayName,
                    size = 46.dp,
                    borderWidth = 1.5.dp
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.senderDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRelativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ViboCoral)
                )
            }
        }
    }
}
