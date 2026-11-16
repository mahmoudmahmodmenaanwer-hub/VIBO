package com.example.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.UserRepository
import com.example.data.repository.VideoRepository
import com.example.data.util.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProfileTab {
    VIDEOS,
    LIKED
}

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val videoRepository: VideoRepository,
    private val currentUserId: String,
    val targetUserId: String
) : ViewModel() {

    val isOwnProfile: Boolean = currentUserId == targetUserId

    private val _selectedTab = MutableStateFlow(ProfileTab.VIDEOS)
    val selectedTab: StateFlow<ProfileTab> = _selectedTab.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = userRepository.observeUserProfile(targetUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "تعذر تحميل الملف الشخصي"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val currentUserProfile: StateFlow<UserProfile?> = userRepository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isFollowing: StateFlow<Boolean> = userRepository.observeFollowingIds(currentUserId)
        .map { targetUserId in it }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val creatorVideos: StateFlow<List<Video>> = videoRepository.observeVideosByCreator(targetUserId)
        .catch { error ->
            if (error is CancellationException && error.unwrapFirestoreException() == null) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val likedVideos: StateFlow<List<Video>> = if (isOwnProfile) {
        combine(
            videoRepository.observePublicVideos(),
            videoRepository.observeLikedVideoIds(currentUserId)
        ) { videos, likedIds ->
            videos.filter { it.id in likedIds }
        }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun selectTab(tab: ProfileTab) {
        _selectedTab.value = tab
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun toggleFollow() {
        if (isOwnProfile) return
        viewModelScope.launch {
            try {
                userRepository.toggleFollow(targetUserId, currentUserProfile.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionMessage.value = e.localizedMessage ?: "تعذر تحديث المتابعة"
            }
        }
    }

    fun updateProfile(username: String, displayName: String, bio: String) {
        if (!isOwnProfile) return
        viewModelScope.launch {
            try {
                userRepository.updateCurrentUserProfile(username, displayName, bio)
                _actionMessage.value = "تم تحديث الملف الشخصي"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionMessage.value = e.localizedMessage ?: "تعذر تحديث الملف الشخصي"
            }
        }
    }

    fun deleteVideo(videoId: String) {
        if (!isOwnProfile) return
        viewModelScope.launch {
            try {
                videoRepository.deleteVideo(videoId)
                _actionMessage.value = "تم حذف الفيديو"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionMessage.value = e.localizedMessage ?: "تعذر حذف الفيديو"
            }
        }
    }

    private companion object {
        const val TAG = "ProfileVM"
    }
}
