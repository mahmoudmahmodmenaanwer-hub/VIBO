package com.example.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.UiState
import com.example.data.repository.NotificationRepository
import com.example.data.util.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val currentUserId: String
) : ViewModel() {

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        notificationRepository.observeNotifications(currentUserId)
            .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Error observing notifications", root)
                emit(UiState.Error(root.message ?: "تعذر تحميل الإشعارات"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val unreadCount: StateFlow<Int> = notificationsState
        .map { state ->
            (state as? UiState.Success)?.data?.count { !it.isRead } ?: 0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mark notification read", e)
            }
        }
    }

    fun markAllAsRead() {
        val current = (notificationsState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mark all notifications read", e)
            }
        }
    }

    private companion object {
        const val TAG = "NotificationsVM"
    }
}
