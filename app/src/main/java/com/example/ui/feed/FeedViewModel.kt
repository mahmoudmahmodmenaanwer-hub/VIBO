package com.example.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.VideoComment
import com.example.data.repository.UserRepository
import com.example.data.repository.VideoRepository
import com.example.data.util.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeedTab {
    FOR_YOU,
    FOLLOWING
}

class FeedViewModel(
    private val videoRepository: VideoRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FeedTab.FOR_YOU)
    val selectedTab: StateFlow<FeedTab> = _selectedTab.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _activeCommentVideoId = MutableStateFlow<String?>(null)
    val activeCommentVideoId: StateFlow<String?> = _activeCommentVideoId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val recordedViews = mutableSetOf<String>()

    val videosState: StateFlow<UiState<List<Video>>> = videoRepository.observePublicVideos()
        .map<List<Video>, UiState<List<Video>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing public videos", root)
            emit(UiState.Error(root.message ?: "تعذر تحميل الفيديوهات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val likedVideoIds: StateFlow<Set<String>> = videoRepository.observeLikedVideoIds(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.unwrapFirestoreException() == null) throw error
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val followingIds: StateFlow<Set<String>> = userRepository.observeFollowingIds(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.unwrapFirestoreException() == null) throw error
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val currentUserProfile: StateFlow<UserProfile?> = userRepository.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.unwrapFirestoreException() == null) throw error
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeComments: StateFlow<List<VideoComment>> = _activeCommentVideoId
        .flatMapLatest { videoId ->
            if (videoId == null) flowOf(emptyList())
            else videoRepository.observeComments(videoId).catch { emit(emptyList()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    fun selectTab(tab: FeedTab) {
        _selectedTab.value = tab
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun openComments(videoId: String) {
        _activeCommentVideoId.value = videoId
    }

    fun closeComments() {
        _activeCommentVideoId.value = null
    }

    fun clearError() {
        _actionError.value = null
    }

    fun toggleLike(video: Video, forceLikeOnly: Boolean = false) {
        if (forceLikeOnly && video.id in likedVideoIds.value) return
        viewModelScope.launch {
            try {
                videoRepository.toggleLike(video, currentUserProfile.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "تعذر تحديث الإعجاب"
            }
        }
    }

    fun addComment(video: Video, text: String) {
        viewModelScope.launch {
            try {
                videoRepository.addComment(video, text, currentUserProfile.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "تعذر إرسال التعليق"
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        if (targetUserId == currentUserId || targetUserId.isBlank()) return
        viewModelScope.launch {
            try {
                userRepository.toggleFollow(targetUserId, currentUserProfile.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "تعذر تحديث المتابعة"
            }
        }
    }

    fun recordShare(videoId: String) {
        viewModelScope.launch {
            try {
                videoRepository.incrementShareCount(videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun recordViewIfNeeded(videoId: String) {
        if (!recordedViews.add(videoId)) return
        viewModelScope.launch {
            try {
                videoRepository.incrementViewCount(videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        const val TAG = "FeedViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
