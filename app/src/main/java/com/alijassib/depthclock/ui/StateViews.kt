package com.alijassib.depthclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(onPick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ساعة العمق", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "اختار صورة من المعرض بيها شخص، وأنا أفصله عن الخلفية وأحط ساعة شاشة القفل وراه — مثل الآيفون.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
        )
        PrimaryButton("اختار صورة من المعرض", onClick = onPick)
    }
}

@Composable
fun ProcessingView(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
fun ApplyBar(
    placement: Placement,
    dirty: Boolean,
    saving: Boolean,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onLock = placement == Placement.Lock || placement == Placement.Both
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val label = when {
            saving -> "دا أحفظ…"
            !onLock -> "طبّقها على شاشة القفل"
            dirty -> "حدّث شاشة القفل"
            else -> "مطبّقة على شاشة القفل"
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { PrimaryButton(label, enabled = !saving && (!onLock || dirty), onClick = onApply) }
            // التراجع يبيّن بس إذا أكو تصميم مطبّق وتعديلات عليه
            if (onLock && dirty && !saving) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onDiscard,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.height(52.dp),
                ) { Text("رجّع") }
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

