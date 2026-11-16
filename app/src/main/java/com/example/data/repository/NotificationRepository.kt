package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreException
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotificationRepository(
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
            ?: throw IllegalStateException("يجب تسجيل الدخول أولاً قبل عرض الإشعارات.")
    }

    fun observeNotifications(recipientId: String): Flow<List<AppNotification>> = flow {
        requireUserId()
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("recipientId", recipientId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun markAsRead(notificationId: String) {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        try {
            docRef.update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markAllAsRead(notifications: List<AppNotification>) {
        requireUserId()
        val unread = notifications.filter { !it.isRead && it.id.isNotBlank() }
        if (unread.isEmpty()) return
        try {
            val batch = db.batch()
            unread.take(400).forEach { notif ->
                batch.update(db.collection("notifications").document(notif.id), "isRead", true)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            throw e
        }
    }
}
