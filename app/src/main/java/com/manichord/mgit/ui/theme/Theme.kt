package com.manichord.mgit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import me.sheimi.android.utils.Profile

/**
 * Brand color by default (a full tonal palette generated from [BrandSeedColor] via
 * MaterialKolor, so the app always looks like itself), with an opt-in escape hatch to
 * system wallpaper-based Material You color for users who want that instead -- see
 * Settings > "Use wallpaper colors" (Profile.useDynamicColor).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = Profile.useDynamicColor(LocalContext.current),
    content: @Composable () -> Unit
) {
    val systemDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        useDynamicColor && systemDynamicColorAvailable && useDarkTheme ->
            dynamicDarkColorScheme(LocalContext.current)
        useDynamicColor && systemDynamicColorAvailable && !useDarkTheme ->
            dynamicLightColorScheme(LocalContext.current)
        else ->
            rememberDynamicColorScheme(
                seedColor = BrandSeedColor,
                isDark = useDarkTheme,
                style = PaletteStyle.TonalSpot
            )
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
