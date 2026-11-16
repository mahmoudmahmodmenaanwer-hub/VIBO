package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Follow
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreException
import com.google.firebase.Firebase
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

class UserRepository(
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

    suspend fun ensureCurrentUserProfile(
        displayNameFallback: String? = null,
        avatarUrlFallback: String? = null
    ): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("تعذر قراءة ملف المستخدم")
            } else {
                val rawDisplay = displayNameFallback?.takeIf { it.isNotBlank() }
                    ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: "مستخدم VIBO"
                val cleanHandleBase = rawDisplay
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "vibo" }
                    .take(18)
                val username = "${cleanHandleBase}_${uid.take(5).lowercase()}"
                val avatar = avatarUrlFallback ?: auth.currentUser?.photoUrl?.toString().orEmpty()

                val payload = mapOf(
                    "userId" to uid,
                    "username" to username,
                    "displayName" to rawDisplay.take(50),
                    "bio" to "مبدع محتوى على VIBO",
                    "avatarUrl" to avatar.take(500),
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "likesCount" to 0L,
                    "videosCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await()
                created.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(
                        id = uid,
                        userId = uid,
                        username = username,
                        displayName = rawDisplay.take(50),
                        bio = "مبدع محتوى على VIBO",
                        avatarUrl = avatar.take(500)
                    )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        requireUserId()
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAllCreators(limitCount: Long = 40L): Flow<List<UserProfile>> = flow {
        requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .limit(limitCount)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun getUserProfile(userId: String): UserProfile? {
        requireUserId()
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateCurrentUserProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String? = null
    ) {
        val uid = requireUserId()
        val cleanUsername = username.trim().removePrefix("@").take(30)
        val cleanDisplay = displayName.trim().take(50)
        val cleanBio = bio.trim().take(200)
        require(cleanUsername.length >= 2) { "اسم المستخدم يجب أن يكون حرفين على الأقل" }
        require(cleanDisplay.isNotEmpty()) { "الاسم المعروض مطلوب" }

        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "username" to cleanUsername,
            "displayName" to cleanDisplay,
            "bio" to cleanBio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (avatarUrl != null) {
            updates["avatarUrl"] = avatarUrl.take(500)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeFollowingIds(followerId: String): Flow<Set<String>> = flow {
        requireUserId()
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("followerId", followerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Follow::class.java).map { it.followingId }.toSet()
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

    suspend fun toggleFollow(
        targetUserId: String,
        currentUserProfile: UserProfile?
    ): Boolean {
        val uid = requireUserId()
        require(uid != targetUserId) { "لا يمكنك متابعة نفسك" }
        val followId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followId)
        val targetRef = db.collection("users").document(targetUserId)
        val selfRef = db.collection("users").document(uid)

        return try {
            val existingFollow = followRef.get().await()
            val batch = db.batch()
            if (existingFollow.exists()) {
                batch.delete(followRef)
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    selfRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                false
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "followingId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    selfRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val notifId = UUID.randomUUID().toString()
                val notifRef = db.collection("notifications").document(notifId)
                val senderUsername = currentUserProfile?.username?.ifBlank { "user" } ?: "user"
                val senderDisplay = currentUserProfile?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"
                val senderAvatar = currentUserProfile?.avatarUrl.orEmpty()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to targetUserId,
                        "senderId" to uid,
                        "senderUsername" to senderUsername,
                        "senderDisplayName" to senderDisplay,
                        "senderAvatarUrl" to senderAvatar,
                        "type" to "follow",
                        "videoId" to "",
                        "message" to "بدأ بمتابعتك الآن",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            throw e
        }
    }
}
