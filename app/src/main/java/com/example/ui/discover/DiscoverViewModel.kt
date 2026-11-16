package com.example.ui.discover

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

val DISCOVER_CATEGORIES = listOf(
    "الكل",
    "ترفيه",
    "تقنية",
    "رياضة",
    "سفر",
    "موسيقى",
    "تعليم",
    "طبخ",
    "عام"
)

data class DiscoverData(
    val videos: List<Video>,
    val creators: List<UserProfile>
)

class DiscoverViewModel(
    private val videoRepository: VideoRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val followingIds: StateFlow<Set<String>> = userRepository.observeFollowingIds(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.unwrapFirestoreException() == null) throw error
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    private val currentUserProfile: StateFlow<UserProfile?> = userRepository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val discoverState: StateFlow<UiState<DiscoverData>> = combine(
        videoRepository.observePublicVideos(),
        userRepository.observeAllCreators(),
        _searchQuery,
        _selectedCategory
    ) { videos, creators, query, category ->
        val q = query.trim().lowercase()
        val filteredVideos = videos.filter { video ->
            val matchesCategory = category == "الكل" || video.category == category
            val matchesQuery = q.isEmpty() ||
                video.title.lowercase().contains(q) ||
                video.description.lowercase().contains(q) ||
                video.creatorUsername.lowercase().contains(q) ||
                video.creatorDisplayName.lowercase().contains(q) ||
                video.hashtags.any { it.lowercase().contains(q) }
            matchesCategory && matchesQuery
        }.sortedByDescending { it.likesCount + it.viewsCount }

        val filteredCreators = creators
            .filter { it.userId != currentUserId }
            .filter { creator ->
                q.isEmpty() ||
                    creator.username.lowercase().contains(q) ||
                    creator.displayName.lowercase().contains(q) ||
                    creator.bio.lowercase().contains(q)
            }
            .sortedByDescending { it.followersCount }

        UiState.Success(DiscoverData(videos = filteredVideos, creators = filteredCreators))
    }
        .map<UiState.Success<DiscoverData>, UiState<DiscoverData>> { it }
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Discover error", root)
            emit(UiState.Error(root.message ?: "تعذر تحميل صفحة الاكتشاف"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                userRepository.toggleFollow(targetUserId, currentUserProfile.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Follow failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "DiscoverVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
