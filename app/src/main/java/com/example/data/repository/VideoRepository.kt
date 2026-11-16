package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LikedVideoRef
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.VideoComment
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreException
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VideoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول أولاً قبل تنفيذ هذه العملية.")
    }

    fun observePublicVideos(): Flow<List<Video>> = flow {
        requireUserId()
        val path = "videos"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeVideosByCreator(creatorId: String): Flow<List<Video>> = flow {
        val currentUid = requireUserId()
        val path = "videos"
        val query = if (currentUid == creatorId) {
            db.collection(path).whereEqualTo("creatorId", creatorId)
        } else {
            db.collection(path)
                .whereEqualTo("creatorId", creatorId)
                .whereEqualTo("visibility", "public")
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLikedVideoIds(userId: String): Flow<Set<String>> = flow {
        requireUserId()
        val path = "users/$userId/liked_videos"
        emitAll(
            db.collection("users").document(userId).collection("liked_videos")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LikedVideoRef::class.java).map { it.videoId }.toSet()
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getVideoById(videoId: String): Video? {
        requireUserId()
        val docRef = db.collection("videos").document(videoId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun publishVideo(
        creator: UserProfile,
        title: String,
        description: String,
        category: String,
        hashtags: List<String>,
        videoUri: String,
        durationMs: Long,
        visibility: String = "public"
    ): String {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(120)
        require(cleanTitle.isNotEmpty()) { "عنوان الفيديو مطلوب" }
        require(videoUri.isNotBlank()) { "يرجى اختيار فيديو أولاً" }

        val videoId = UUID.randomUUID().toString()
        val videoRef = db.collection("videos").document(videoId)
        val userRef = db.collection("users").document(uid)

        val cleanTags = hashtags
            .map { it.trim().removePrefix("#").take(40) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        val payload = mapOf(
            "creatorId" to uid,
            "creatorUsername" to creator.username.ifBlank { "vibo_user" }.take(40),
            "creatorDisplayName" to creator.displayName.ifBlank { "مبدع VIBO" }.take(60),
            "creatorAvatarUrl" to creator.avatarUrl.take(500),
            "title" to cleanTitle,
            "description" to description.trim().take(500),
            "category" to category.trim().ifBlank { "عام" }.take(40),
            "hashtags" to cleanTags,
            "videoUri" to videoUri.take(2000),
            "thumbnailUri" to "",
            "durationMs" to durationMs.coerceAtLeast(0L),
            "visibility" to if (visibility == "private") "private" else "public",
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "sharesCount" to 0L,
            "viewsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.set(videoRef, payload)
            batch.update(
                userRef,
                mapOf(
                    "videosCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            videoId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, videoRef.path)
            throw e
        }
    }

    suspend fun deleteVideo(videoId: String) {
        val uid = requireUserId()
        val videoRef = db.collection("videos").document(videoId)
        try {
            videoRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, videoRef.path)
            throw e
        }
    }

    suspend fun toggleLike(
        video: Video,
        currentUserProfile: UserProfile?
    ): Boolean {
        val uid = requireUserId()
        val videoRef = db.collection("videos").document(video.id)
        val likeRef = videoRef.collection("likes").document(uid)
        val userLikedRef = db.collection("users").document(uid).collection("liked_videos").document(video.id)
        val creatorRef = db.collection("users").document(video.creatorId)

        return try {
            val existing = likeRef.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.delete(likeRef)
                batch.delete(userLikedRef)
                batch.update(
                    videoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (video.creatorId.isNotBlank()) {
                    batch.update(
                        creatorRef,
                        mapOf(
                            "likesCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
                false
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "videoId" to video.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    userLikedRef,
                    mapOf(
                        "userId" to uid,
                        "videoId" to video.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    videoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (video.creatorId.isNotBlank()) {
                    batch.update(
                        creatorRef,
                        mapOf(
                            "likesCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (video.creatorId.isNotBlank() && video.creatorId != uid) {
                    val notifRef = db.collection("notifications").document(UUID.randomUUID().toString())
                    batch.set(
                        notifRef,
                        mapOf(
                            "recipientId" to video.creatorId,
                            "senderId" to uid,
                            "senderUsername" to (currentUserProfile?.username?.ifBlank { "user" } ?: "user"),
                            "senderDisplayName" to (currentUserProfile?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"),
                            "senderAvatarUrl" to currentUserProfile?.avatarUrl.orEmpty(),
                            "type" to "like",
                            "videoId" to video.id,
                            "message" to "أعجب بالفيديو: ${video.title.take(40)}",
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            throw e
        }
    }

    fun observeComments(videoId: String): Flow<List<VideoComment>> = flow {
        requireUserId()
        val path = "videos/$videoId/comments"
        emitAll(
            db.collection("videos").document(videoId).collection("comments")
                .whereEqualTo("videoId", videoId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VideoComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(
        video: Video,
        text: String,
        currentUserProfile: UserProfile?
    ) {
        val uid = requireUserId()
        val cleanText = text.trim().take(500)
        require(cleanText.isNotEmpty()) { "التعليق لا يمكن أن يكون فارغاً" }

        val commentId = UUID.randomUUID().toString()
        val videoRef = db.collection("videos").document(video.id)
        val commentRef = videoRef.collection("comments").document(commentId)

        val payload = mapOf(
            "videoId" to video.id,
            "authorId" to uid,
            "authorUsername" to (currentUserProfile?.username?.ifBlank { "user" } ?: "user"),
            "authorDisplayName" to (currentUserProfile?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"),
            "authorAvatarUrl" to currentUserProfile?.avatarUrl.orEmpty(),
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                videoRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (video.creatorId.isNotBlank() && video.creatorId != uid) {
                val notifRef = db.collection("notifications").document(UUID.randomUUID().toString())
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to video.creatorId,
                        "senderId" to uid,
                        "senderUsername" to (currentUserProfile?.username?.ifBlank { "user" } ?: "user"),
                        "senderDisplayName" to (currentUserProfile?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"),
                        "senderAvatarUrl" to currentUserProfile?.avatarUrl.orEmpty(),
                        "type" to "comment",
                        "videoId" to video.id,
                        "message" to "علّق: ${cleanText.take(60)}",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    suspend fun incrementShareCount(videoId: String) {
        requireUserId()
        val videoRef = db.collection("videos").document(videoId)
        try {
            videoRef.update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
        }
    }

    suspend fun incrementViewCount(videoId: String) {
        requireUserId()
        val videoRef = db.collection("videos").document(videoId)
        try {
            videoRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
        }
    }
}
