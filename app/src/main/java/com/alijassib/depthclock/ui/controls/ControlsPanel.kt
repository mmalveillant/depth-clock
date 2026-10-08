package com.alijassib.depthclock.ui.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.DepthDesign

private val TABS = listOf("الأشكال", "الخط", "الأبعاد", "اللون", "خيارات")

/** لوحة التحكم تحت المعاينة. ارتفاع ثابت حتى المعاينة ما «تنط» لمّا تتبدّل التبويبات. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsPanel(
    design: DepthDesign,
    swatches: List<Int>,
    hasSubject: Boolean,
    maxClockSize: (ClockSpec) -> Float,
    onDesignChange: ((DepthDesign) -> DepthDesign) -> Unit,
    onDesignChangeAtEdge: ((DepthDesign) -> DepthDesign) -> Unit,
    onShowGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val onClockChange: (ClockSpec) -> Unit = { c -> onDesignChange { it.copy(clock = c) } }
    val onSliderChange: (ClockSpec) -> Unit = { c -> onDesignChangeAtEdge { it.copy(clock = c) } }

    Column(modifier.fillMaxWidth()) {
        SecondaryTabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tab),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            TABS.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(PANEL_HEIGHT).padding(top = 12.dp)) {
            val fill = Modifier.fillMaxSize()
            when (tab) {
                0 -> StylesTab(design.clock, onClockChange, fill)
                1 -> FontsTab(design.clock, onClockChange, fill)
                2 -> SizeTab(design.clock, maxClockSize(design.clock), onClockChange, onSliderChange, fill)
                3 -> ColorTab(design.clock, swatches, onClockChange, fill)
                else -> OptionsTab(design, hasSubject, onDesignChange, onShowGuide, fill)
            }
        }
    }
}

private val PANEL_HEIGHT = 228.dp
