package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockFonts
import com.alijassib.depthclock.data.ClockLayout
import com.alijassib.depthclock.data.ClockSpec
import kotlin.math.roundToInt

/**
 * «الأبعاد»: الحجم، الطول، العرض، السُمك، التباعد.
 * [maxSize] = أكبر حجم يلزم بالشاشة بهالإعدادات (السلايدر يوقف عنده).
 */
@Composable
fun SizeTab(
    clock: ClockSpec,
    maxSize: Float,
    onClockChange: (ClockSpec) -> Unit,
    /** للسلايدرات الي توقف عند حافة الشاشة بدل ما تصغّر الحجم. */
    onSliderChange: (ClockSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    val font = ClockFonts.byId(clock.fontId)
    val wght = font?.wght
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LabeledSlider(
            label = "الحجم",
            value = clock.size,
            range = ClockSpec.MIN_SIZE..maxSize.coerceAtLeast(ClockSpec.MIN_SIZE + 0.01f),
            valueText = "${(clock.size / maxSize * 100).roundToInt()}%",
            onChange = { onClockChange(clock.copy(size = it)) },
        )
        LabeledSlider(
            label = "الطول",
            value = clock.heightScale,
            range = 0.5f..2.2f,
            valueText = times(clock.heightScale),
            onChange = { onSliderChange(clock.copy(heightScale = snapOne(it))) },
        )
        LabeledSlider(
            label = "العرض",
            value = clock.widthScale,
            range = 0.5f..1.8f,
            valueText = times(clock.widthScale),
            onChange = { onSliderChange(clock.copy(widthScale = snapOne(it))) },
        )
        LabeledSlider(
            label = "السُمك",
            value = clock.weight.toFloat(),
            range = (wght?.min ?: 100f)..(wght?.max ?: 1000f),
            valueText = if (wght != null) clock.weight.coerceIn(wght.min.toInt(), wght.max.toInt()).toString() else "—",
            onChange = { onSliderChange(clock.copy(weight = it.roundToInt())) },
            enabled = wght != null,
            hint = if (wght == null) "خط بوزن واحد" else null,
        )
        LabeledSlider(
            label = "التباعد",
            value = clock.spacing,
            range = -0.15f..0.5f,
            valueText = "${(clock.spacing * 100).roundToInt()}",
            onChange = { onSliderChange(clock.copy(spacing = snapZero(it))) },
        )
        if (clock.layout == ClockLayout.Stacked) {
            LabeledSlider(
                label = "بين السطرين",
                value = clock.lineGap,
                range = -0.4f..0.6f,
                valueText = "${(clock.lineGap * 100).roundToInt()}",
                onChange = { onSliderChange(clock.copy(lineGap = it)) },
            )
        }
        TextButton(onClick = {
            onClockChange(clock.copy(widthScale = 1f, heightScale = 1f, spacing = 0f, lineGap = ClockSpec().lineGap))
        }) { Text("رجّع الطول والعرض والتباعد") }
    }
}

private fun times(v: Float) = "×" + ((v * 100).roundToInt() / 100f).toString()

/** لزگة خفيفة على القيمة الطبيعية حتى سهل ترجعلها. */
private fun snapOne(v: Float) = if (kotlin.math.abs(v - 1f) < 0.03f) 1f else v
private fun snapZero(v: Float) = if (kotlin.math.abs(v) < 0.01f) 0f else v
