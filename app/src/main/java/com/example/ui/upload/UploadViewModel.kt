package com.example.ui.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.data.repository.VideoRepository
import com.example.data.util.VideoStorageHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UploadSourceTab {
    PHONE_GALLERY,
    DIRECT_URL
}

val UPLOAD_CATEGORIES = listOf(
    "ترفيه",
    "تقنية",
    "رياضة",
    "سفر",
    "موسيقى",
    "تعليم",
    "طبخ",
    "عام"
)

class UploadViewModel(
    private val videoRepository: VideoRepository,
    userRepository: UserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _sourceTab = MutableStateFlow(UploadSourceTab.PHONE_GALLERY)
    val sourceTab: StateFlow<UploadSourceTab> = _sourceTab.asStateFlow()

    private val _preparedVideoUri = MutableStateFlow("")
    val preparedVideoUri: StateFlow<String> = _preparedVideoUri.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _directUrlInput = MutableStateFlow("")
    val directUrlInput: StateFlow<String> = _directUrlInput.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _category = MutableStateFlow("ترفيه")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _hashtagsInput = MutableStateFlow("")
    val hashtagsInput: StateFlow<String> = _hashtagsInput.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic.asStateFlow()

    private val _isPreparingFile = MutableStateFlow(false)
    val isPreparingFile: StateFlow<Boolean> = _isPreparingFile.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _publishedVideoId = MutableStateFlow<String?>(null)
    val publishedVideoId: StateFlow<String?> = _publishedVideoId.asStateFlow()

    val currentUserProfile: StateFlow<UserProfile?> = userRepository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun selectSourceTab(tab: UploadSourceTab) {
        _sourceTab.value = tab
        _errorMessage.value = null
    }

    fun updateDirectUrl(url: String) {
        _directUrlInput.value = url
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun selectCategory(value: String) {
        _category.value = value
    }

    fun updateHashtags(value: String) {
        _hashtagsInput.value = value
    }

    fun setPublicVisibility(public: Boolean) {
        _isPublic.value = public
    }

    fun onVideoPickedFromDevice(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isPreparingFile.value = true
            _errorMessage.value = null
            try {
                val prepared = VideoStorageHelper.copyPickedVideoToInternalStorage(context, uri)
                _preparedVideoUri.value = prepared.uriString
                _durationMs.value = prepared.durationMs
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "تعذر تجهيز الفيديو المختار"
            } finally {
                _isPreparingFile.value = false
            }
        }
    }

    fun clearPublishedEvent() {
        _publishedVideoId.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun publishVideo() {
        val activeUri = when (_sourceTab.value) {
            UploadSourceTab.PHONE_GALLERY -> _preparedVideoUri.value
            UploadSourceTab.DIRECT_URL -> _directUrlInput.value.trim()
        }
        val cleanTitle = _title.value.trim()
        if (activeUri.isBlank()) {
            _errorMessage.value = "يرجى اختيار فيديو من الهاتف أو إدخال رابط مباشر صالح"
            return
        }
        if (_sourceTab.value == UploadSourceTab.DIRECT_URL &&
            !activeUri.startsWith("http://") &&
            !activeUri.startsWith("https://")
        ) {
            _errorMessage.value = "يرجى إدخال رابط يبدأ بـ http:// أو https://"
            return
        }
        if (cleanTitle.isEmpty()) {
            _errorMessage.value = "يرجى كتابة عنوان للفيديو"
            return
        }

        val creator = currentUserProfile.value ?: UserProfile(
            id = currentUserId,
            userId = currentUserId,
            username = "vibo_${currentUserId.take(5).lowercase()}",
            displayName = "مستخدم VIBO"
        )

        val tags = _hashtagsInput.value
            .split(" ", ",", "،", "\n")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            _isPublishing.value = true
            _errorMessage.value = null
            try {
                val videoId = videoRepository.publishVideo(
                    creator = creator,
                    title = cleanTitle,
                    description = _description.value.trim(),
                    category = _category.value,
                    hashtags = tags,
                    videoUri = activeUri,
                    durationMs = _durationMs.value,
                    visibility = if (_isPublic.value) "public" else "private"
                )
                // Reset form on success
                _preparedVideoUri.value = ""
                _directUrlInput.value = ""
                _title.value = ""
                _description.value = ""
                _hashtagsInput.value = ""
                _durationMs.value = 0L
                _publishedVideoId.value = videoId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "تعذر نشر الفيديو، حاول مرة أخرى"
            } finally {
                _isPublishing.value = false
            }
        }
    }
}
