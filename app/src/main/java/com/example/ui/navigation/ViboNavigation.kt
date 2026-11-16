package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.VideoRepository
import com.example.data.util.ShortcutTargetTab
import com.example.data.util.ThemeMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.discover.DiscoverScreen
import com.example.ui.discover.DiscoverViewModel
import com.example.ui.feed.FeedScreen
import com.example.ui.feed.FeedViewModel
import com.example.ui.feed.SingleVideoViewerScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.notifications.NotificationsViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan
import com.example.ui.upload.UploadScreen
import com.example.ui.upload.UploadViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable data object FeedRoute
@Serializable data object DiscoverRoute
@Serializable data object UploadRoute
@Serializable data object NotificationsRoute
@Serializable data object ProfileRoute
@Serializable data class UserProfileRoute(val userId: String)
@Serializable data class VideoDetailRoute(val videoId: String)

private data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val isUploadCta: Boolean = false
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(FeedRoute, R.string.nav_feed, Icons.Rounded.Home, Icons.Outlined.Home, "nav_feed"),
    TopLevelTab(DiscoverRoute, R.string.nav_discover, Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_discover"),
    TopLevelTab(UploadRoute, R.string.nav_upload, Icons.Rounded.Add, Icons.Rounded.Add, "nav_upload", isUploadCta = true),
    TopLevelTab(NotificationsRoute, R.string.nav_notifications, Icons.Rounded.Notifications, Icons.Outlined.Notifications, "nav_notifications"),
    TopLevelTab(ProfileRoute, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun AppNavigation(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    pendingStartTab: String? = null,
    onConsumeStartTab: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen()
    } else {
        key(user.uid) {
            AuthenticatedViboRoot(
                currentUserId = user.uid,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                pendingStartTab = pendingStartTab,
                onConsumeStartTab = onConsumeStartTab
            )
        }
    }
}

@Composable
private fun AuthenticatedViboRoot(
    currentUserId: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    pendingStartTab: String?,
    onConsumeStartTab: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    }
    var profileReady by remember(currentUserId) { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        try {
            UserRepository(db).ensureCurrentUserProfile()
        } catch (_: Exception) {
        } finally {
            profileReady = true
        }
    }

    if (!profileReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ViboCoral)
        }
        return
    }

    val feedViewModel: FeedViewModel = viewModel(
        key = "feed_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                FeedViewModel(VideoRepository(firestore), UserRepository(firestore), currentUserId)
            }
        }
    )

    val discoverViewModel: DiscoverViewModel = viewModel(
        key = "discover_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                DiscoverViewModel(VideoRepository(firestore), UserRepository(firestore), currentUserId)
            }
        }
    )

    val uploadViewModel: UploadViewModel = viewModel(
        key = "upload_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                UploadViewModel(VideoRepository(firestore), UserRepository(firestore), currentUserId)
            }
        }
    )

    val notificationsViewModel: NotificationsViewModel = viewModel(
        key = "notifs_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                NotificationsViewModel(NotificationRepository(firestore), currentUserId)
            }
        }
    )

    val ownProfileViewModel: ProfileViewModel = viewModel(
        key = "profile_${currentUserId}_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ProfileViewModel(UserRepository(firestore), VideoRepository(firestore), currentUserId, currentUserId)
            }
        }
    )

    val unreadCount by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomOrRailNav = currentDestination?.let { dest ->
        !dest.hasRoute<UserProfileRoute>() && !dest.hasRoute<VideoDetailRoute>()
    } ?: true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingStartTab) {
        when (pendingStartTab) {
            ShortcutTargetTab.DISCOVER.extraValue -> {
                navigateTopLevel(DiscoverRoute)
                onConsumeStartTab()
            }
            ShortcutTargetTab.UPLOAD.extraValue -> {
                navigateTopLevel(UploadRoute)
                onConsumeStartTab()
            }
            ShortcutTargetTab.FEED.extraValue -> {
                navigateTopLevel(FeedRoute)
                onConsumeStartTab()
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (showBottomOrRailNav && !useRail) {
                    val isFeedActive = currentDestination?.hasRoute<FeedRoute>() == true
                    NavigationBar(
                        containerColor = if (isFeedActive) Color(0xFF0A0A12) else MaterialTheme.colorScheme.surface,
                        contentColor = if (isFeedActive) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    TabIcon(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = {
                                    if (!tab.isUploadCta) {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ViboCoral,
                                    selectedTextColor = ViboCoral,
                                    indicatorColor = ViboCoral.copy(alpha = 0.14f),
                                    unselectedIconColor = if (isFeedActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = if (isFeedActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomOrRailNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    TabIcon(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = FeedRoute,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable<FeedRoute> {
                        FeedScreen(
                            viewModel = feedViewModel,
                            currentUserId = currentUserId,
                            onNavigateToUpload = { navigateTopLevel(UploadRoute) },
                            onNavigateToDiscover = { navigateTopLevel(DiscoverRoute) },
                            onNavigateToUserProfile = { targetUid ->
                                if (targetUid == currentUserId) {
                                    navigateTopLevel(ProfileRoute)
                                } else {
                                    navController.navigate(UserProfileRoute(targetUid))
                                }
                            }
                        )
                    }

                    composable<DiscoverRoute> {
                        DiscoverScreen(
                            viewModel = discoverViewModel,
                            onVideoClick = { videoId ->
                                navController.navigate(VideoDetailRoute(videoId))
                            },
                            onCreatorClick = { targetUid ->
                                if (targetUid == currentUserId) {
                                    navigateTopLevel(ProfileRoute)
                                } else {
                                    navController.navigate(UserProfileRoute(targetUid))
                                }
                            }
                        )
                    }

                    composable<UploadRoute> {
                        UploadScreen(
                            viewModel = uploadViewModel,
                            onVideoPublished = {
                                navigateTopLevel(FeedRoute)
                            }
                        )
                    }

                    composable<NotificationsRoute> {
                        NotificationsScreen(
                            viewModel = notificationsViewModel,
                            onOpenVideo = { videoId ->
                                navController.navigate(VideoDetailRoute(videoId))
                            },
                            onOpenUserProfile = { targetUid ->
                                if (targetUid == currentUserId) {
                                    navigateTopLevel(ProfileRoute)
                                } else {
                                    navController.navigate(UserProfileRoute(targetUid))
                                }
                            }
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            viewModel = ownProfileViewModel,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onVideoClick = { videoId ->
                                navController.navigate(VideoDetailRoute(videoId))
                            }
                        )
                    }

                    composable<UserProfileRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<UserProfileRoute>()
                        val otherProfileViewModel: ProfileViewModel = viewModel(
                            key = "profile_${currentUserId}_${route.userId}",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY]) as Application
                                    val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                    ProfileViewModel(
                                        UserRepository(firestore),
                                        VideoRepository(firestore),
                                        currentUserId,
                                        route.userId
                                    )
                                }
                            }
                        )
                        ProfileScreen(
                            viewModel = otherProfileViewModel,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onVideoClick = { videoId ->
                                navController.navigate(VideoDetailRoute(videoId))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<VideoDetailRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<VideoDetailRoute>()
                        SingleVideoViewerScreen(
                            videoId = route.videoId,
                            viewModel = feedViewModel,
                            currentUserId = currentUserId,
                            onBack = { navController.popBackStack() },
                            onNavigateToUserProfile = { targetUid ->
                                if (targetUid == currentUserId) {
                                    navigateTopLevel(ProfileRoute)
                                } else {
                                    navController.navigate(UserProfileRoute(targetUid))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(
    tab: TopLevelTab,
    selected: Boolean,
    unreadCount: Int
) {
    if (tab.isUploadCta) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(ViboCoral, ViboCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(tab.labelRes),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    } else if (tab.route == NotificationsRoute && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = ViboCoral, contentColor = Color.White) {
                    Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                }
            }
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = stringResource(tab.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(tab.labelRes)
        )
    }
}
