package com.manichord.mgit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.sheimi.sgit.R

// All bundled as single variable-weight font files (Google Fonts only publishes these as
// variable, no static per-weight files), each referenced once per FontWeight below. Android
// applies the weight via font variation settings on API 26+; on the minSdk 23-25 range it
// falls back to one fixed instance for all weights -- graceful degradation, not broken
// rendering. All OFL-1.1, see README -- Third-party fonts.
private fun variableFontFamily(fontResId: Int) = FontFamily(
    Font(fontResId, FontWeight.Normal),
    Font(fontResId, FontWeight.Medium),
    Font(fontResId, FontWeight.SemiBold),
    Font(fontResId, FontWeight.Bold),
)

/**
 * The fonts a user can pick between in Settings > App font. [id] is the persisted preference
 * value (Profile/PreferenceHelper.getAppFont()), so it must stay stable across releases even if
 * [displayName] changes.
 */
enum class FontOption(val id: String, val displayName: String, val fontFamily: FontFamily) {
    SYSTEM_DEFAULT("system_default", "System default", FontFamily.Default),
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta Sans", variableFontFamily(R.font.plus_jakarta_sans)),
    INTER("inter", "Inter", variableFontFamily(R.font.inter)),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", variableFontFamily(R.font.jetbrains_mono));

    companion object {
        val DEFAULT = PLUS_JAKARTA_SANS

        fun fromId(id: String?): FontOption = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

fun gitlingTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
