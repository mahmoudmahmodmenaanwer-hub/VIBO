package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VideoComment
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

@Composable
fun ViboAvatar(
    avatarUrl: String,
    displayName: String,
    size: Dp = 44.dp,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "V"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(listOf(ViboCoral, ViboCyan)),
                shape = CircleShape
            )
            .padding(borderWidth)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vibo_empty),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ViboCoral,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_state_action_button")
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<VideoComment>,
    onDismiss: () -> Unit,
    onSubmitComment: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.comments_title)} (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(comments, key = { it.id.ifBlank { it.text + it.authorId } }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAuthorClick(comment.authorId) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ViboAvatar(
                                avatarUrl = comment.authorAvatarUrl,
                                displayName = comment.authorDisplayName,
                                size = 38.dp,
                                borderWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = comment.authorDisplayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatRelativeTime(comment.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.comments_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ViboCoral,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = commentText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSubmitComment(trimmed)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (commentText.isNotBlank()) ViboCoral else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_send_comment),
                        tint = if (commentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000L -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "الآن"
    val diffMs = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        mins < 1 -> "الآن"
        mins < 60 -> "منذ $mins د"
        hours < 24 -> "منذ $hours س"
        days < 30 -> "منذ $days ي"
        else -> "منذ ${days / 30} ش"
    }
}
