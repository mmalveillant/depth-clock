package com.alijassib.depthclock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** ألوان مصمتة وهادئة: خلفية داكنة، الأبيض هو اللون الأساسي، بلا توهّج ولا تدرّجات. */
private val Scheme = darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF0E0F11),
    secondary = Color(0xFFF2C230),
    onSecondary = Color(0xFF0E0F11),
    background = Color(0xFF0E0F11),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF0E0F11),
    onSurface = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFF17181C),
    surfaceContainerHigh = Color(0xFF1F2025),
    onSurfaceVariant = Color(0xFF9A9BA3),
    outline = Color(0xFF34353C),
    outlineVariant = Color(0xFF26272D),
    error = Color(0xFFFF8A80),
)

@Composable
fun DepthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme) {
        // الواجهة عربية حتى لو لغة التلفون إنگليزي
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
