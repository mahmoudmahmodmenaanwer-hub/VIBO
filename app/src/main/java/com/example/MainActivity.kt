package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.util.HomeShortcutHelper
import com.example.data.util.ThemeMode
import com.example.data.util.ThemePreferences
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.ViboTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingStartTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HomeShortcutHelper.registerDynamicShortcuts(applicationContext)
        pendingStartTab = intent?.getStringExtra(HomeShortcutHelper.EXTRA_START_TAB)

        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val scope = rememberCoroutineScope()

            ViboTheme(themeMode = themeMode) {
                AppNavigation(
                    themeMode = themeMode,
                    onThemeModeChange = { selectedMode ->
                        scope.launch {
                            themePreferences.setThemeMode(selectedMode)
                        }
                    },
                    pendingStartTab = pendingStartTab,
                    onConsumeStartTab = { pendingStartTab = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tab = intent.getStringExtra(HomeShortcutHelper.EXTRA_START_TAB)
        if (!tab.isNullOrBlank()) {
            pendingStartTab = tab
        }
    }
}
