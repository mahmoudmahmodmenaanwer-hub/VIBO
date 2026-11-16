package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerBox(
    videoUri: String,
    isActive: Boolean,
    isMuted: Boolean,
    onDoubleTapLike: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isUserPaused by remember(videoUri) { mutableStateOf(false) }
    var isBuffering by remember(videoUri) { mutableStateOf(true) }
    var hasError by remember(videoUri) { mutableStateOf(false) }
    var progress by remember(videoUri) { mutableFloatStateOf(0f) }
    var showHeartBurst by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasError = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isActive, isUserPaused) {
        exoPlayer.playWhenReady = isActive && !isUserPaused
        if (!isActive) {
            isUserPaused = false
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(lifecycleOwner, exoPlayer, isActive, isUserPaused) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive && !isUserPaused) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(exoPlayer, isActive, isUserPaused) {
        while (isActive && !isUserPaused) {
            val dur = exoPlayer.duration
            val pos = exoPlayer.currentPosition
            if (dur > 0) {
                progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            delay(180L)
        }
    }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            heartScale.snapTo(0.4f)
            heartScale.animateTo(1.25f, animationSpec = spring(dampingRatio = 0.5f))
            delay(420L)
            showHeartBurst = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(videoUri) {
                detectTapGestures(
                    onTap = {
                        isUserPaused = !isUserPaused
                    },
                    onDoubleTap = {
                        showHeartBurst = true
                        onDoubleTapLike()
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && isActive && !hasError) {
            CircularProgressIndicator(
                color = ViboCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = ViboCoral,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تعذر تشغيل الفيديو حالياً",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        hasError = false
                        isBuffering = true
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ViboCoral)
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }
        }

        AnimatedVisibility(
            visible = isUserPaused && !hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        if (showHeartBurst) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = ViboCoral,
                modifier = Modifier
                    .size(96.dp)
                    .scale(heartScale.value)
                    .align(Alignment.Center)
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            color = ViboCoral,
            trackColor = Color.White.copy(alpha = 0.22f),
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .align(Alignment.BottomCenter)
        )
    }
}
