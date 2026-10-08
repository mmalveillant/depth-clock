package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.MinutesTone
import kotlin.math.roundToInt

/** ألوان ثابتة هادئة (بلا نيون) — تنضاف لألوان الصورة. */
private val FIXED = listOf(
    0xFFFFFFFF, 0xFFF5EBDD, 0xFFE9D8A6, 0xFFF2C230, 0xFFE8B4A0, 0xFFD98C8C,
    0xFFB9C7E6, 0xFF9EC9D9, 0xFFA8D5BA, 0xFFC9B8E8, 0xFF8E8E93, 0xFF1C1C1E,
).map { it.toInt() }

@Composable
fun ColorTab(clock: ClockSpec, photoSwatches: List<Int>, onClockChange: (ClockSpec) -> Unit, modifier: Modifier = Modifier) {
    val alpha = clock.colorArgb ushr 24
    val rgb = clock.colorArgb and 0xFFFFFF
    fun withRgb(c: Int) = clock.copy(colorArgb = (alpha shl 24) or (c and 0xFFFFFF))

    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(clock.colorArgb, it) }
    // الدرجة تنحفظ لحالها: إذا التشبّع صار صفر، اللون ما يعرف درجته ويرجع أحمر
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    LaunchedEffect(clock.colorArgb) { if (hsv[1] > 0.05f) hue = hsv[0] }

    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SwatchRow("من الصورة", photoSwatches, rgb) { onClockChange(withRgb(it)) }
        SwatchRow("ألوان", FIXED, rgb) { onClockChange(withRgb(it)) }
        LabeledSlider(
            label = "درجة اللون",
            value = hue,
            range = 0f..359f,
            valueText = "${hue.roundToInt()}°",
            onChange = {
                hue = it
                onClockChange(withRgb(android.graphics.Color.HSVToColor(floatArrayOf(it, hsv[1].coerceAtLeast(0.35f), hsv[2].coerceAtLeast(0.85f)))))
            },
        )
        LabeledSlider(
            label = "التشبّع",
            value = hsv[1],
            range = 0f..1f,
            valueText = "${(hsv[1] * 100).roundToInt()}%",
            onChange = { onClockChange(withRgb(android.graphics.Color.HSVToColor(floatArrayOf(hue, it, hsv[2].coerceAtLeast(0.3f))))) },
        )
        LabeledSlider(
            label = "الوضوح",
            value = alpha / 255f,
            range = 0.3f..1f,
            valueText = "${(alpha / 255f * 100).roundToInt()}%",
            onChange = { onClockChange(clock.copy(colorArgb = ((it * 255).roundToInt() shl 24) or rgb)) },
        )
        Text("الدقائق", style = MaterialTheme.typography.bodyMedium)
        SegmentedChoice(
            options = MinutesTone.entries.map { it to it.label },
            selected = clock.minutesTone,
            onSelect = { onClockChange(clock.copy(minutesTone = it)) },
        )
        ToggleRow("إطار بس (بلا تعبئة)", clock.outline, { onClockChange(clock.copy(outline = it)) })
    }
}

@Composable
private fun SwatchRow(title: String, colors: List<Int>, selectedRgb: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            colors.forEach { argb ->
                val isSel = (argb and 0xFFFFFF) == selectedRgb
                Box(
                    Modifier
                        .size(34.dp)
                        .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(argb or 0xFF000000.toInt()))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(role = Role.RadioButton) { onPick(argb) }
                        .semantics {
                            this.selected = isSel
                            contentDescription = "لون"
                        },
                )
            }
        }
    }
}
