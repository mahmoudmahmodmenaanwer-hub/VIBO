package com.example.ui.profile

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.util.HomeShortcutHelper
import com.example.data.util.PinShortcutResult
import com.example.data.util.ShortcutTargetTab
import com.example.data.util.ThemeMode
import com.example.ui.auth.signOutUser
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ViboAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVideoClick: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val creatorVideos by viewModel.creatorVideos.collectAsStateWithLifecycle()
    val likedVideos by viewModel.likedVideos.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        when (val state = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViboCoral)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        title = "تعذر تحميل الحساب",
                        subtitle = state.message
                    )
                }
            }

            is UiState.Success -> {
                val profile = state.data
                if (profile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            title = "الملف الشخصي غير متوفر",
                            subtitle = "لم يتم العثور على بيانات هذا المستخدم."
                        )
                    }
                } else {
                    val displayedVideos = when (selectedTab) {
                        ProfileTab.VIDEOS -> creatorVideos
                        ProfileTab.LIKED -> likedVideos
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            ProfileTopHeader(
                                profile = profile,
                                isOwnProfile = viewModel.isOwnProfile,
                                isFollowing = isFollowing,
                                themeMode = themeMode,
                                onBack = onBack,
                                onEditClick = { showEditDialog = true },
                                onFollowToggle = viewModel::toggleFollow,
                                onThemeModeChange = onThemeModeChange,
                                onPinShortcutClick = { targetTab ->
                                    val result = HomeShortcutHelper.pinShortcutToHomeScreen(context, targetTab)
                                    val message = when (result) {
                                        PinShortcutResult.REQUESTED ->
                                            context.getString(R.string.pin_shortcut_requested)
                                        PinShortcutResult.UNSUPPORTED ->
                                            context.getString(R.string.pin_shortcut_unsupported)
                                        PinShortcutResult.ERROR ->
                                            context.getString(R.string.pin_shortcut_error)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                onShareAppClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text))
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.share_app_chooser)
                                        )
                                    )
                                },
                                onSignOutClick = {
                                    signOutUser(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }

                        item {
                            val tabs = if (viewModel.isOwnProfile) {
                                listOf(ProfileTab.VIDEOS, ProfileTab.LIKED)
                            } else {
                                listOf(ProfileTab.VIDEOS)
                            }
                            TabRow(
                                selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = ViboCoral,
                                indicator = { tabPositions ->
                                    val idx = tabs.indexOf(selectedTab).coerceAtLeast(0)
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                                        color = ViboCoral
                                    )
                                }
                            ) {
                                Tab(
                                    selected = selectedTab == ProfileTab.VIDEOS,
                                    onClick = { viewModel.selectTab(ProfileTab.VIDEOS) },
                                    icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                                    text = { Text(stringResource(R.string.profile_videos_tab)) }
                                )
                                if (viewModel.isOwnProfile) {
                                    Tab(
                                        selected = selectedTab == ProfileTab.LIKED,
                                        onClick = { viewModel.selectTab(ProfileTab.LIKED) },
                                        icon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                                        text = { Text(stringResource(R.string.profile_liked_tab)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (displayedVideos.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = if (selectedTab == ProfileTab.VIDEOS) {
                                        "لا توجد فيديوهات منشورة"
                                    } else {
                                        "لا توجد فيديوهات معجب بها"
                                    },
                                    subtitle = if (selectedTab == ProfileTab.VIDEOS) {
                                        "ستظهر الفيديوهات المنشورة هنا فور رفعها."
                                    } else {
                                        "المقاطع التي تعجب بها ستُحفظ هنا للرجوع إليها."
                                    }
                                )
                            }
                        } else {
                            val rows = displayedVideos.chunked(3)
                            items(rows) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { video ->
                                        ProfileVideoTile(
                                            video = video,
                                            canDelete = viewModel.isOwnProfile && selectedTab == ProfileTab.VIDEOS,
                                            onClick = { onVideoClick(video.id) },
                                            onDeleteClick = { viewModel.deleteVideo(video.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (showEditDialog && viewModel.isOwnProfile) {
                        EditProfileDialog(
                            profile = profile,
                            onDismiss = { showEditDialog = false },
                            onSave = { username, displayName, bio ->
                                viewModel.updateProfile(username, displayName, bio)
                                showEditDialog = false
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ProfileTopHeader(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    themeMode: ThemeMode,
    onBack: (() -> Unit)?,
    onEditClick: () -> Unit,
    onFollowToggle: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPinShortcutClick: (ShortcutTargetTab) -> Unit,
    onShareAppClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isOwnProfile) {
                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = stringResource(R.string.profile_sign_out),
                        tint = ViboCoral
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ViboAvatar(
            avatarUrl = profile.avatarUrl,
            displayName = profile.displayName,
            size = 92.dp,
            borderWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (profile.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(value = profile.videosCount, label = stringResource(R.string.profile_stat_videos))
                StatColumn(value = profile.followersCount, label = stringResource(R.string.profile_stat_followers))
                StatColumn(value = profile.followingCount, label = stringResource(R.string.profile_stat_following))
                StatColumn(value = profile.likesCount, label = stringResource(R.string.profile_stat_likes))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isOwnProfile) {
            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("edit_profile_button")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_edit_button), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_theme_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        }
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size
                            ),
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "تلقائي"
                                        ThemeMode.DARK -> "ليلي"
                                        ThemeMode.LIGHT -> "نهاري"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("theme_mode_${mode.key}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PhoneInstallCard(
                onPinAppHomeClick = { onPinShortcutClick(ShortcutTargetTab.FEED) },
                onPinUploadShortcutClick = { onPinShortcutClick(ShortcutTargetTab.UPLOAD) },
                onShareAppClick = onShareAppClick
            )
        } else {
            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowToggle,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_follow_button")
                ) {
                    Text(stringResource(R.string.following_label), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onFollowToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = ViboCoral, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_follow_button")
                ) {
                    Text(stringResource(R.string.follow_label), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PhoneInstallCard(
    onPinAppHomeClick: () -> Unit,
    onPinUploadShortcutClick: () -> Unit,
    onShareAppClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_install_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddToHomeScreen,
                    contentDescription = null,
                    tint = ViboCoral
                )
                Text(
                    text = stringResource(R.string.profile_install_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.profile_install_section_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onPinAppHomeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ViboCoral,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pin_app_home_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddToHomeScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.profile_pin_home_button),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPinUploadShortcutClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pin_upload_shortcut_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoCall,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.profile_pin_upload_button),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onShareAppClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("share_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.profile_share_app_button),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.profile_share_app_button),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCompactCount(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileVideoTile(
    video: Video,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.74f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1F1635), Color(0xFF121829)))
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (canDelete) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC07070E))))
                .padding(8.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = ViboCyan,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatCompactCount(video.likesCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var username by remember(profile) { mutableStateOf(profile.username) }
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.profile_display_name_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.profile_username_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.profile_bio_label)) },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, bio) },
                colors = ButtonDefaults.buttonColors(containerColor = ViboCoral, contentColor = Color.White)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
