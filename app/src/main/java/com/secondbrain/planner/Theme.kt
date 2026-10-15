package com.secondbrain.planner

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalSettings = staticCompositionLocalOf<AppSettings> { error("no settings") }

fun hsv(h: Float, s: Float, v: Float): Color =
    Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        )
    )

fun buildScheme(h: Float, dark: Boolean, amoled: Boolean): ColorScheme {
    return if (dark) {
        val bg = if (amoled) Color.Black else hsv(h, .38f, .075f)
        val surf = if (amoled) Color.Black else hsv(h, .35f, .10f)
        darkColorScheme(
            primary = hsv(h, .42f, 1f), onPrimary = hsv(h, .9f, .22f),
            primaryContainer = hsv(h, .68f, .46f), onPrimaryContainer = hsv(h, .12f, 1f),
            secondary = hsv(h + 35, .38f, 1f), onSecondary = hsv(h + 35, .9f, .2f),
            secondaryContainer = hsv(h + 35, .6f, .38f), onSecondaryContainer = hsv(h + 35, .12f, 1f),
            tertiary = hsv(h + 85, .4f, 1f), onTertiary = hsv(h + 85, .9f, .2f),
            tertiaryContainer = hsv(h + 85, .62f, .4f), onTertiaryContainer = hsv(h + 85, .12f, 1f),
            background = bg, onBackground = hsv(h, .05f, .96f),
            surface = surf, onSurface = hsv(h, .05f, .96f),
            surfaceVariant = if (amoled) hsv(h, .25f, .14f) else hsv(h, .30f, .20f),
            onSurfaceVariant = hsv(h, .12f, .82f),
            outline = hsv(h, .2f, .55f), outlineVariant = hsv(h, .3f, .30f),
            error = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary = hsv(h, .72f, .72f), onPrimary = Color.White,
            primaryContainer = hsv(h, .16f, 1f), onPrimaryContainer = hsv(h, .9f, .30f),
            secondary = hsv(h + 35, .6f, .70f), onSecondary = Color.White,
            secondaryContainer = hsv(h + 35, .14f, 1f), onSecondaryContainer = hsv(h + 35, .9f, .3f),
            tertiary = hsv(h + 85, .6f, .70f), onTertiary = Color.White,
            tertiaryContainer = hsv(h + 85, .14f, 1f), onTertiaryContainer = hsv(h + 85, .9f, .3f),
            background = hsv(h, .03f, .99f), onBackground = hsv(h, .5f, .14f),
            surface = Color.White, onSurface = hsv(h, .5f, .14f),
            surfaceVariant = hsv(h, .07f, .95f), onSurfaceVariant = hsv(h, .2f, .35f),
            outline = hsv(h, .15f, .60f), outlineVariant = hsv(h, .10f, .85f),
            error = Color(0xFFD93B3B)
        )
    }
}

@Composable
fun rememberFont(): FontFamily {
    val ctx = LocalContext.current
    return remember {
        try {
            val am = ctx.assets
            val files = am.list("fonts")?.toList() ?: emptyList()
            if (files.contains("Vazirmatn-Regular.ttf") && files.contains("Vazirmatn-Bold.ttf")) {
                FontFamily(
                    Font("fonts/Vazirmatn-Regular.ttf", am, FontWeight.Normal),
                    Font("fonts/Vazirmatn-Regular.ttf", am, FontWeight.Medium),
                    Font("fonts/Vazirmatn-Bold.ttf", am, FontWeight.SemiBold),
                    Font("fonts/Vazirmatn-Bold.ttf", am, FontWeight.Bold)
                )
            } else FontFamily.Default
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}

fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun AppTheme(s: AppSettings, content: @Composable () -> Unit) {
    val sysDark = isSystemInDarkTheme()
    val mode = s.themeMode.value
    val dark = when (mode) {
        1 -> false
        2, 3 -> true
        else -> sysDark
    }
    val scheme = buildScheme(s.hue.value, dark, mode == 3)
    val r = s.radius.value
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((r * 0.3f).dp),
        small = RoundedCornerShape((r * 0.5f).dp),
        medium = RoundedCornerShape((r * 0.7f).dp),
        large = RoundedCornerShape(r.dp),
        extraLarge = RoundedCornerShape((r * 1.3f).dp)
    )
    val typo = Typography().withFont(rememberFont())
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, d.fontScale * s.fontScale.value),
        LocalLayoutDirection provides (if (s.rtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        MaterialTheme(colorScheme = scheme, shapes = shapes, typography = typo, content = content)
    }
}
