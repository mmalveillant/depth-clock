package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.render.ClockRenderer

/** رسّام واحد لكل البطاقات (Paint بس، رخيص). */
@Composable
fun rememberTileRenderer(): ClockRenderer {
    val ctx = LocalContext.current
    return remember { ClockRenderer(ctx.applicationContext.assets) }
}

/**
 * بطاقة صغيرة ترسم الساعة بنفس رسّام شاشة القفل — الي تشوفه بالبطاقة هو الي يطلع.
 * الحجم يتعدّل حتى الساعة تلزم البطاقة.
 */
@Composable
fun ClockTile(
    renderer: ClockRenderer,
    spec: ClockSpec,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    width: Dp = 104.dp,
    height: Dp = 76.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    val tileColor = MaterialTheme.colorScheme.surfaceContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier
                .size(width, height)
                .clip(shape)
                .background(tileColor)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape,
                )
                .clickable(role = Role.RadioButton, onClick = onClick)
                .semantics {
                    this.selected = selected
                    contentDescription = label
                },
        ) {
            val base = spec.copy(centerX = 0.5f, centerY = 0.5f, colorArgb = WHITE)
            val fit = base.copy(size = renderer.maxSize(base, size.width / size.height) * 0.8f)
            drawIntoCanvas { renderer.draw(it.nativeCanvas, fit, size.width, size.height, SAMPLE_TIME, 0f) }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val WHITE = 0xFFFFFFFF.toInt()
private const val SAMPLE_TIME = "12:45"
