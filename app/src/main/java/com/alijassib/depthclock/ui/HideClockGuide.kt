package com.alijassib.depthclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * سامسونگ ما تسمح لتطبيق يخفي ساعة القفل، بس لونها يقبل شفافية 0%.
 * فنخليها شفافة وساعتنا هي الي تبين.
 */
private val Steps = listOf(
    "افتح محرّر شاشة القفل: ضغطة مطوّلة على الساعة بشاشة القفل ثم البصمة — أو أسهل: Good Lock ← LockStar (نفس المحرّر).",
    "دوس على الساعة ← «الخط واللون»: اختار الخط الثالث (عادي)، ونزّل السُمك لـ «رفيع». الساعات المتحرّكة ما تقبل الشفافية.",
    "أول زر بالألوان (القطرة) هو «Blur» — طفّيه. الضبابية تخلي الساعة تبين حتى لو شفافة.",
    "روح لآخر الألوان ← «منتقي الألوان» ← نزّل «الشفافية» لـ 0% ← تم.",
    "صغّر إطار الساعة من زاويته وسحبه لفوگ يم الشريط العلوي — سامسونگ تحط ظل خفيف تحت الساعة، وهناك ما يبين.",
    "دوس «تم» فوگ حتى ينحفظ.",
    "إذا الإشعارات تغطي الساعة: الإعدادات ← شاشة القفل و AOD ← إشعارات شاشة القفل ← اختار «نقطة» أو «أيقونات».",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideClockGuide(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("خفّي ساعة سامسونگ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "ساعتنا هي الي تطلع ورا الشخص. ساعة النظام لازم تصير شفافة حتى ما تطلع فوگها — مرة وحدة بس.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
            PrimaryButton("تمام", onClick = onDismiss)
        }
    }
}
