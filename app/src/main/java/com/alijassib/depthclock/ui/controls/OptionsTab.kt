package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockLayout
import com.alijassib.depthclock.data.DepthDesign

@Composable
fun OptionsTab(
    design: DepthDesign,
    hasSubject: Boolean,
    onDesignChange: ((DepthDesign) -> DepthDesign) -> Unit,
    onShowGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = design.clock
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedChoice(
            options = ClockLayout.entries.map { it to it.label },
            selected = clock.layout,
            onSelect = { l -> onDesignChange { it.copy(clock = it.clock.copy(layout = l)) } },
        )
        if (clock.layout == ClockLayout.Horizontal) {
            ToggleRow("النقطتين «:»", clock.showColon, { v -> onDesignChange { it.copy(clock = it.clock.copy(showColon = v)) } })
        }
        ToggleRow(
            title = "الساعة ورا الشخص",
            checked = design.depthEnabled && hasSubject,
            onChange = { v -> onDesignChange { it.copy(depthEnabled = v) } },
            subtitle = if (hasSubject) null else "ما انلگه شخص بهالصورة",
            enabled = hasSubject,
        )
        ToggleRow("التاريخ فوگ الساعة", design.showDate, { v -> onDesignChange { it.copy(showDate = v) } })
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onDesignChange { it.copy(clock = it.clock.copy(centerX = 0.5f)) } }) { Text("رجّع الساعة للنص") }
            TextButton(onClick = onShowGuide) { Text("ساعة سامسونگ طالعة؟") }
        }
    }
}
