package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Video
import com.example.data.util.HomeShortcutHelper
import com.example.data.util.ShortcutTargetTab
import com.example.data.util.ThemeMode
import com.example.data.util.VideoStorageHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCompactCount
import com.example.ui.feed.VideoFeedPage
import com.example.ui.theme.ViboTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readAppName_isVibo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VIBO", appName)
    }

    @Test
    fun viboTheme_setsRtlLayoutDirection() {
        var resolvedDirection: LayoutDirection? = null
        composeRule.setContent {
            ViboTheme(themeMode = ThemeMode.DARK) {
                resolvedDirection = LocalLayoutDirection.current
            }
        }
        composeRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, resolvedDirection)
    }

    @Test
    fun authScreen_displaysArabicSignInButton() {
        composeRule.setContent {
            ViboTheme(themeMode = ThemeMode.LIGHT) {
                AuthScreen(enableAutoSignIn = false)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("تسجيل الدخول بحساب Google").assertIsDisplayed()
    }

    @Test
    fun emptyStateCard_rendersTitleAndActionButton() {
        composeRule.setContent {
            ViboTheme(themeMode = ThemeMode.DARK) {
                EmptyStateCard(
                    title = "لا توجد فيديوهات بعد",
                    subtitle = "كن أول من يشارك لحظة إبداعية على VIBO!",
                    actionLabel = "ارفع فيديو الآن",
                    onActionClick = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("لا توجد فيديوهات بعد").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_state_action_button").assertIsDisplayed()
    }

    @Test
    fun formatCompactCount_formatsThousandsAndMillions() {
        assertEquals("42", formatCompactCount(42L))
        assertEquals("1.5K", formatCompactCount(1500L))
        assertEquals("2.3M", formatCompactCount(2_300_000L))
    }

    @Test
    fun homeShortcutHelper_buildsAndRegistersShortcuts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = HomeShortcutHelper.buildShortcutIntent(context, ShortcutTargetTab.UPLOAD)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("upload", intent.getStringExtra(HomeShortcutHelper.EXTRA_START_TAB))

        HomeShortcutHelper.registerDynamicShortcuts(context)
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
        assertEquals(3, shortcuts.size)
    }

    @Test
    fun videoStorageHelper_rejectsHlsAndSavesLocalVideo() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hlsResult = VideoStorageHelper.saveVideoToPhoneGallery(
            context = context,
            videoUriString = "https://example.com/stream/playlist.m3u8",
            title = "بث مباشر"
        )
        assertTrue(hlsResult.isFailure)

        val tempFile = File(context.cacheDir, "sample_vibo_test.mp4").apply {
            writeBytes(byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112))
        }
        val localResult = VideoStorageHelper.saveVideoToPhoneGallery(
            context = context,
            videoUriString = Uri.fromFile(tempFile).toString(),
            title = "تجربة_حفظ"
        )
        assertTrue(localResult.isSuccess)
    }

    @Test
    fun videoFeedPage_displaysSaveToPhoneButton() {
        val sampleVideo = Video(
            id = "vid_1",
            creatorId = "user_1",
            creatorUsername = "ahmed",
            creatorDisplayName = "أحمد",
            title = "لحظة رائعة",
            videoUri = "https://example.com/video.mp4"
        )
        composeRule.setContent {
            ViboTheme(themeMode = ThemeMode.DARK) {
                VideoFeedPage(
                    video = sampleVideo,
                    isActive = false,
                    isMuted = true,
                    isLiked = false,
                    isFollowingCreator = false,
                    isOwnVideo = false,
                    onToggleMute = {},
                    onLikeClick = {},
                    onDoubleTapLike = {},
                    onCommentClick = {},
                    onShareClick = {},
                    onSaveToPhoneClick = {},
                    onCreatorClick = {},
                    onFollowClick = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("feed_save_phone_button").assertIsDisplayed()
    }
}
