package com.example.data.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

enum class PinShortcutResult {
    REQUESTED,
    UNSUPPORTED,
    ERROR
}

enum class ShortcutTargetTab(val extraValue: String) {
    FEED("feed"),
    DISCOVER("discover"),
    UPLOAD("upload")
}

object HomeShortcutHelper {
    const val EXTRA_START_TAB = "vibo_start_tab"

    private const val SHORTCUT_ID_FEED = "vibo_pin_feed"
    private const val SHORTCUT_ID_UPLOAD = "vibo_pin_upload"
    private const val SHORTCUT_ID_DISCOVER = "vibo_pin_discover"

    fun buildShortcutIntent(context: Context, targetTab: ShortcutTargetTab): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_START_TAB, targetTab.extraValue)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun registerDynamicShortcuts(context: Context) {
        try {
            val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
            val shortcuts = listOf(
                ShortcutInfoCompat.Builder(context, SHORTCUT_ID_FEED)
                    .setShortLabel(context.getString(R.string.shortcut_feed_short))
                    .setLongLabel(context.getString(R.string.shortcut_feed_long))
                    .setIcon(icon)
                    .setIntent(buildShortcutIntent(context, ShortcutTargetTab.FEED))
                    .build(),
                ShortcutInfoCompat.Builder(context, SHORTCUT_ID_UPLOAD)
                    .setShortLabel(context.getString(R.string.shortcut_upload_short))
                    .setLongLabel(context.getString(R.string.shortcut_upload_long))
                    .setIcon(icon)
                    .setIntent(buildShortcutIntent(context, ShortcutTargetTab.UPLOAD))
                    .build(),
                ShortcutInfoCompat.Builder(context, SHORTCUT_ID_DISCOVER)
                    .setShortLabel(context.getString(R.string.shortcut_discover_short))
                    .setLongLabel(context.getString(R.string.shortcut_discover_long))
                    .setIcon(icon)
                    .setIntent(buildShortcutIntent(context, ShortcutTargetTab.DISCOVER))
                    .build()
            )
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {
        }
    }

    fun pinShortcutToHomeScreen(
        context: Context,
        targetTab: ShortcutTargetTab
    ): PinShortcutResult {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return PinShortcutResult.UNSUPPORTED
            }
            val (id, shortLabel, longLabel) = when (targetTab) {
                ShortcutTargetTab.FEED -> Triple(
                    SHORTCUT_ID_FEED,
                    context.getString(R.string.app_name),
                    context.getString(R.string.shortcut_feed_long)
                )
                ShortcutTargetTab.UPLOAD -> Triple(
                    SHORTCUT_ID_UPLOAD,
                    context.getString(R.string.shortcut_upload_short),
                    context.getString(R.string.shortcut_upload_long)
                )
                ShortcutTargetTab.DISCOVER -> Triple(
                    SHORTCUT_ID_DISCOVER,
                    context.getString(R.string.shortcut_discover_short),
                    context.getString(R.string.shortcut_discover_long)
                )
            }
            val shortcut = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(buildShortcutIntent(context, targetTab))
                .build()

            val requested = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            if (requested) PinShortcutResult.REQUESTED else PinShortcutResult.UNSUPPORTED
        } catch (_: Exception) {
            PinShortcutResult.ERROR
        }
    }
}
