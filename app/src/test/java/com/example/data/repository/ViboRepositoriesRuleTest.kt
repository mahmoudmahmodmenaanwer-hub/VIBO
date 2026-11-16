package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ViboRepositoriesRuleTest : FirestoreEmulatorTestBase() {

    private fun aliceEmail() = "alice_${UUID.randomUUID().toString().take(6)}@vibo.test"
    private fun bobEmail() = "bob_${UUID.randomUUID().toString().take(6)}@vibo.test"

    @Test
    fun ensureProfile_publishVideo_like_comment_follow_and_notify_succeeds() = runBlocking {
        val aliceMail = aliceEmail()
        val bobMail = bobEmail()
        val aliceUid = signInTestUser(aliceMail)
        val userRepo = UserRepository(firestore, auth)
        val videoRepo = VideoRepository(firestore, auth)
        val notifRepo = NotificationRepository(firestore, auth)

        val aliceProfile = withTimeout(TIMEOUT_MS) {
            userRepo.ensureCurrentUserProfile(displayNameFallback = "أليس")
        }
        assertEquals(aliceUid, aliceProfile.userId)

        val videoTitle = "فيديو اختبار ${UUID.randomUUID().toString().take(4)}"
        val videoId = withTimeout(TIMEOUT_MS) {
            videoRepo.publishVideo(
                creator = aliceProfile,
                title = videoTitle,
                description = "وصف قصير",
                category = "تقنية",
                hashtags = listOf("VIBO", "برمجة"),
                videoUri = "https://example.com/demo.mp4",
                durationMs = 15000L,
                visibility = "public"
            )
        }
        assertTrue(videoId.isNotEmpty())

        val publicVideos = withTimeout(TIMEOUT_MS) {
            videoRepo.observePublicVideos().first { list -> list.any { it.id == videoId } }
        }
        val video = publicVideos.first { it.id == videoId }
        assertEquals(videoTitle, video.title)

        // Sign in as Bob to like, comment, and follow Alice
        val bobUid = signInTestUser(bobMail)
        val bobProfile = withTimeout(TIMEOUT_MS) {
            userRepo.ensureCurrentUserProfile(displayNameFallback = "بوب")
        }

        val liked = withTimeout(TIMEOUT_MS) {
            videoRepo.toggleLike(video, bobProfile)
        }
        assertTrue(liked)

        withTimeout(TIMEOUT_MS) {
            videoRepo.addComment(video, "مبدعة جداً!", bobProfile)
        }
        val comments = withTimeout(TIMEOUT_MS) {
            videoRepo.observeComments(videoId).first { it.isNotEmpty() }
        }
        assertEquals(1, comments.size)
        assertEquals("مبدعة جداً!", comments.first().text)

        val followed = withTimeout(TIMEOUT_MS) {
            userRepo.toggleFollow(aliceUid, bobProfile)
        }
        assertTrue(followed)
        val followingIds = withTimeout(TIMEOUT_MS) {
            userRepo.observeFollowingIds(bobUid).first { aliceUid in it }
        }
        assertTrue(aliceUid in followingIds)

        // Sign back in as Alice and verify updated counters and notifications
        signInTestUser(aliceMail)
        val updatedVideo = withTimeout(TIMEOUT_MS) {
            videoRepo.getVideoById(videoId)
        }
        assertNotNull(updatedVideo)
        assertEquals(1L, updatedVideo?.likesCount)
        assertEquals(1L, updatedVideo?.commentsCount)

        val aliceNotifications = withTimeout(TIMEOUT_MS) {
            notifRepo.observeNotifications(aliceUid).first { it.size >= 3 }
        }
        assertTrue(aliceNotifications.any { it.type == "like" })
        assertTrue(aliceNotifications.any { it.type == "comment" })
        assertTrue(aliceNotifications.any { it.type == "follow" })

        // Verify Bob has no notifications for himself
        signInTestUser(bobMail)
        val bobNotifications = withTimeout(TIMEOUT_MS) {
            notifRepo.observeNotifications(bobUid).first()
        }
        assertTrue(bobNotifications.isEmpty())
    }

    @Test
    fun notifications_crossUserAndMarkRead_enforced() = runBlocking {
        val aliceMail = aliceEmail()
        val bobMail = bobEmail()
        val aliceUid = signInTestUser(aliceMail)
        val userRepo = UserRepository(firestore, auth)
        val notifRepo = NotificationRepository(firestore, auth)

        val aliceProfile = withTimeout(TIMEOUT_MS) {
            userRepo.ensureCurrentUserProfile("أليس")
        }

        val bobUid = signInTestUser(bobMail)
        val bobProfile = withTimeout(TIMEOUT_MS) {
            userRepo.ensureCurrentUserProfile("بوب")
        }

        val followed = withTimeout(TIMEOUT_MS) {
            userRepo.toggleFollow(aliceUid, bobProfile)
        }
        assertTrue(followed)

        // Bob cannot read Alice's notifications
        var denied = false
        try {
            withTimeout(TIMEOUT_MS) {
                notifRepo.observeNotifications(aliceUid).first()
            }
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Cross-user notification read must fail with PERMISSION_DENIED", denied)

        // Alice reads her notification and marks it as read
        signInTestUser(aliceMail)
        val aliceNotifs = withTimeout(TIMEOUT_MS) {
            notifRepo.observeNotifications(aliceUid).first { it.isNotEmpty() }
        }
        assertFalse(aliceNotifs.first().isRead)
        withTimeout(TIMEOUT_MS) {
            notifRepo.markAsRead(aliceNotifs.first().id)
        }
        val afterRead = withTimeout(TIMEOUT_MS) {
            notifRepo.observeNotifications(aliceUid).first { it.first().isRead }
        }
        assertTrue(afterRead.first().isRead)
    }

    @Test
    fun unauthenticatedQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val videoRepo = VideoRepository(firestore, auth)
        var threw = false
        try {
            withTimeout(TIMEOUT_MS) {
                videoRepo.observePublicVideos().first()
            }
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
    }
}
