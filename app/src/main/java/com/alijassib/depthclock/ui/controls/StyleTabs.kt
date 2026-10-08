package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.alijassib.depthclock.render.FontLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockFonts
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.render.ClockPresets

/** الشكل بلا المكان واللون والحجم — حتى نعرف أي شكل جاهز هو المختار. */
private fun ClockSpec.shapeKey() = copy(size = 0f, centerX = 0f, centerY = 0f, colorArgb = 0)

/** «الأشكال»: أشكال جاهزة؛ الاختيار يحتفظ بمكان الساعة ولونها. */
@Composable
fun StylesTab(clock: ClockSpec, onClockChange: (ClockSpec) -> Unit, modifier: Modifier = Modifier) {
    val renderer = rememberTileRenderer()
    val current = clock.shapeKey()
    WarmFonts(ClockPresets.all.map { it.spec })
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ClockPresets.all, key = { it.id }) { preset ->
            ClockTile(
                renderer = renderer,
                spec = preset.spec,
                label = preset.label,
                selected = preset.spec.shapeKey() == current,
                onClick = { onClockChange(preset.spec.withPlacementOf(clock)) },
            )
        }
    }
}

/** «الخط»: نفس ساعتك بكل خط، حتى تشوف الفرق قبل ما تختار. */
@Composable
fun FontsTab(clock: ClockSpec, onClockChange: (ClockSpec) -> Unit, modifier: Modifier = Modifier) {
    val renderer = rememberTileRenderer()
    WarmFonts(ClockFonts.all.map { clock.copy(fontId = it.id) })
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ClockFonts.all, key = { it.id }) { font ->
            ClockTile(
                renderer = renderer,
                spec = clock.copy(fontId = font.id),
                label = font.label,
                selected = clock.fontId == font.id,
                onClick = { onClockChange(clock.copy(fontId = font.id)) },
            )
        }
    }
}

/** نبني خطوط البطاقات بالخلفية حتى أول تمرير ما يتقطّع. */
@Composable
private fun WarmFonts(specs: List<ClockSpec>) {
    val ctx = LocalContext.current
    LaunchedEffect(specs) {
        withContext(Dispatchers.Default) {
            specs.forEach { FontLoader.typeface(ctx.applicationContext.assets, it.fontId, it.weight, it.widthScale) }
        }
    }
}
