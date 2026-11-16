package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val likesCount: Long = 0L,
    val videosCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Video(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val creatorDisplayName: String = "",
    val creatorAvatarUrl: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "عام",
    val hashtags: List<String> = emptyList(),
    val videoUri: String = "",
    val thumbnailUri: String = "",
    val durationMs: Long = 0L,
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val viewsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VideoComment(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LikedVideoRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val videoId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val type: String = "like",
    val videoId: String = "",
    val message: String = "",
    @field:JvmField val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
