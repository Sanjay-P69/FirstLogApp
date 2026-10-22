package com.Sanjay.firstlogapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================================
// Custom Semantic Palette for Utility Tracking
// =====================================================================
@Immutable
data class SemanticColors(
    val success: Color = MintSage,
    val onSuccess: Color = Color.White,
    val successContainer: Color = MintSageContainerDark,
    val alert: Color = WarmCoral,
    val onAlert: Color = Color.White,
    val alertContainer: Color = WarmCoralContainerDark,
    val neutralMuted: Color = MutedSlate,
    val primaryAccent: Color = SoftIndigoPrimaryDark,
    val skyAccent: Color = SkyBlueAccent,
    val amberAccent: Color = AmberGoldAccent
)

val LocalSemanticColors = staticCompositionLocalOf { SemanticColors() }

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current

// =====================================================================
// M3 Dark Color Scheme (Tonal surfaces, no harsh pure #000000)
// =====================================================================
private val DarkColorScheme = darkColorScheme(
    primary = SoftIndigoPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = SoftIndigoContainerDark,
    onPrimaryContainer = SoftIndigoOnContainerDark,

    secondary = SkyBlueAccent,
    onSecondary = Color(0xFF003549),
    secondaryContainer = SkyBlueContainerDark,
    onSecondaryContainer = Color(0xFFC2E8FF),

    tertiary = MintSage,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = MintSageContainerDark,
    onTertiaryContainer = MintSageOnContainerDark,

    background = M3DarkSurface,
    onBackground = M3DarkOnSurface,

    surface = M3DarkSurface,
    onSurface = M3DarkOnSurface,
    surfaceVariant = M3DarkSurfaceContainer,
    onSurfaceVariant = M3DarkOnSurfaceVariant,

    surfaceContainer = M3DarkSurfaceContainer,
    surfaceContainerHigh = M3DarkSurfaceContainerHigh,
    surfaceContainerHighest = M3DarkSurfaceContainerHighest,
    surfaceContainerLow = M3DarkSurfaceLow,
    surfaceContainerLowest = M3DarkSurfaceDim,

    outline = M3DarkOutline,
    outlineVariant = M3DarkOutlineVariant,

    error = WarmCoral,
    onError = Color(0xFF690005),
    errorContainer = WarmCoralContainerDark,
    onErrorContainer = WarmCoralOnContainerDark
)

// =====================================================================
// M3 Light Color Scheme
// =====================================================================
private val LightColorScheme = lightColorScheme(
    primary = SoftIndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = SoftIndigoContainerLight,
    onPrimaryContainer = SoftIndigoOnContainerLight,

    secondary = Color(0xFF006782),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBE9FF),
    onSecondaryContainer = Color(0xFF001F29),

    tertiary = MintSageLight,
    onTertiary = Color.White,
    tertiaryContainer = MintSageContainerLight,
    onTertiaryContainer = MintSageOnContainerLight,

    background = M3LightSurface,
    onBackground = M3LightOnSurface,

    surface = M3LightSurface,
    onSurface = M3LightOnSurface,
    surfaceVariant = M3LightSurfaceContainer,
    onSurfaceVariant = M3LightOnSurfaceVariant,

    surfaceContainer = M3LightSurfaceContainer,
    surfaceContainerHigh = M3LightSurfaceContainerHigh,
    surfaceContainerHighest = M3LightSurfaceContainerHighest,
    surfaceContainerLow = M3LightSurfaceLow,

    outline = M3LightOutline,
    outlineVariant = M3LightOutlineVariant,

    error = WarmCoralLight,
    onError = Color.White,
    errorContainer = WarmCoralContainerLight,
    onErrorContainer = WarmCoralOnContainerLight
)

@Composable
fun FirstLogAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (darkTheme) {
        SemanticColors(
            success = MintSage,
            onSuccess = Color.White,
            successContainer = MintSageContainerDark,
            alert = WarmCoral,
            onAlert = Color.White,
            alertContainer = WarmCoralContainerDark,
            neutralMuted = MutedSlate,
            primaryAccent = SoftIndigoPrimaryDark,
            skyAccent = SkyBlueAccent,
            amberAccent = AmberGoldAccent
        )
    } else {
        SemanticColors(
            success = MintSageLight,
            onSuccess = Color.White,
            successContainer = MintSageContainerLight,
            alert = WarmCoralLight,
            onAlert = Color.White,
            alertContainer = WarmCoralContainerLight,
            neutralMuted = MutedSlateLight,
            primaryAccent = SoftIndigoPrimaryLight,
            skyAccent = Color(0xFF006782),
            amberAccent = Color(0xFFE65100)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Modern Edge-to-Edge window scrim
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}