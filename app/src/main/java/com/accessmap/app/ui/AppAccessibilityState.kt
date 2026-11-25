package com.accessmap.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.util.Locale

private const val PREFS_NAME = "accessmap_prefs"
private const val KEY_DARK = "dark_theme"
private const val KEY_CONTRAST = "high_contrast"
private const val KEY_LARGE_TEXT = "large_text"
private const val KEY_VOICE = "voice_guidance"
private const val KEY_ONBOARDING = "onboarding_seen"
private const val KEY_FAVORITES = "favorite_nodes"

@Stable
class AppSettingsState(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkTheme by mutableStateOf(prefs.getBoolean(KEY_DARK, false))
        private set
    var highContrast by mutableStateOf(prefs.getBoolean(KEY_CONTRAST, false))
        private set
    var largeText by mutableStateOf(prefs.getBoolean(KEY_LARGE_TEXT, false))
        private set
    var voiceGuidance by mutableStateOf(prefs.getBoolean(KEY_VOICE, true))
        private set
    var onboardingSeen by mutableStateOf(prefs.getBoolean(KEY_ONBOARDING, false))
        private set
    var favoriteIds by mutableStateOf(prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet())
        private set

    fun toggleDarkTheme(enabled: Boolean) {
        darkTheme = enabled
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }

    fun toggleHighContrast(enabled: Boolean) {
        highContrast = enabled
        prefs.edit().putBoolean(KEY_CONTRAST, enabled).apply()
    }

    fun toggleLargeText(enabled: Boolean) {
        largeText = enabled
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply()
    }

    fun toggleVoiceGuidance(enabled: Boolean) {
        voiceGuidance = enabled
        prefs.edit().putBoolean(KEY_VOICE, enabled).apply()
    }

    fun markOnboardingSeen() {
        onboardingSeen = true
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun toggleFavorite(nodeId: String) {
        val next = favoriteIds.toMutableSet()
        if (!next.add(nodeId)) next.remove(nodeId)
        favoriteIds = next
        prefs.edit().putStringSet(KEY_FAVORITES, next).apply()
    }

    fun isFavorite(nodeId: String): Boolean = nodeId in favoriteIds
}

val LocalAppSettings = staticCompositionLocalOf<AppSettingsState> {
    error("AppSettingsState not provided")
}

val LocalVoiceGuidance = staticCompositionLocalOf<VoiceGuidanceHelper> {
    error("VoiceGuidanceHelper not provided")
}

/**
 * Built-in Android TextToSpeech wrapper (100% free, offline, no API key).
 */
class VoiceGuidanceHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isReady = true
            }
        }
    }

    fun speak(text: String, enabled: Boolean = true) {
        if (!enabled || !isReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "accessmap_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

@Composable
fun AccessMapTheme(
    settings: AppSettingsState,
    content: @Composable () -> Unit
) {
    val useDark = settings.darkTheme || (settings.highContrast && isSystemInDarkTheme())

    val colorScheme = when {
        settings.highContrast && settings.darkTheme -> darkColorScheme(
            primary = Color(0xFFFFEA00),
            onPrimary = Color.Black,
            secondary = Color(0xFF00E5FF),
            onSecondary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = Color(0xFFFFEA00)
        )
        settings.highContrast -> lightColorScheme(
            primary = Color(0xFF000080),
            onPrimary = Color.White,
            secondary = Color(0xFF004D40),
            onSecondary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFEEEEEE),
            onSurfaceVariant = Color.Black
        )
        useDark -> darkColorScheme(
            primary = Color(0xFF64B5F6),
            onPrimary = Color(0xFF002147),
            secondary = Color(0xFF00E676),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFECEFF1)
        )
        else -> lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            secondary = Color(0xFF2E7D32),
            background = Color(0xFFF5F5F5),
            surface = Color.White
        )
    }

    val scale = if (settings.largeText) 1.22f else 1.0f
    val base = Typography()
    fun TextStyle.scaled() = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale
    )
    val typography = Typography(
        displayLarge = base.displayLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled()
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
