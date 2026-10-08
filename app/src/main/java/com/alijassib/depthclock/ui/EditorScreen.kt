package com.alijassib.depthclock.ui

import android.content.ActivityNotFoundException
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.DepthDesign
import com.alijassib.depthclock.render.LockRenderer
import com.alijassib.depthclock.ui.controls.ControlsPanel
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(vm: EditorViewModel) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var placement by rememberSaveable { mutableStateOf(LockWallpaper.placement(ctx)) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    // فتحنا «تعيين الخلفية» والخلفية مو على القفل — يبقى محفوظ حتى لو انعادت الشاشة
    var awaitingApply by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        placement = LockWallpaper.placement(ctx)
        onPauseOrDispose { }
    }

    // الاختيار من «المعرض» مال سامسونگ؛ إذا ماكو نرجع لمنتقي الملفات، وبعده منتقي الصور مال النظام
    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.data?.let(vm::onPhotoPicked)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onPhotoPicked(uri)
    }
    val launchPick: () -> Unit = {
        try {
            pickFromGallery.launch(PhotoSources.samsungGallery())
        } catch (_: ActivityNotFoundException) {
            try {
                pickFromGallery.launch(PhotoSources.anyGallery())
            } catch (_: ActivityNotFoundException) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    val setWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val before = placement
        placement = LockWallpaper.placement(ctx)
        if (awaitingApply) {
            awaitingApply = false
            when {
                placement.onLock -> showGuide = true
                placement == Placement.Home && before != Placement.Home ->
                    vm.showNotice("انحطّت على الشاشة الرئيسية بس — اختار «شاشة القفل»")
            }
        }
    }

    // الحفظ يصير بالـ ViewModel؛ فتح شاشة النظام يصير هنا بس، بالشاشة الحالية
    LaunchedEffect(state.pickerRequested) {
        if (state.pickerRequested) {
            vm.onPickerOpened()
            awaitingApply = true
            setWallpaper.launch(LockWallpaper.pickerIntent(ctx))
        }
    }

    // الرسائل تختفي لحالها
    LaunchedEffect(state.notice) {
        if (state.notice != null) { delay(4500); vm.showNotice(null) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val stage = state.stage) {
                Stage.Loading -> Unit
                Stage.Empty -> EmptyState(onPick = launchPick)
                is Stage.Processing -> ProcessingView(stage.message)
                Stage.Editing -> {
                    val photo = state.photo ?: return@Box
                    EditingContent(
                        state = state,
                        photo = photo,
                        placement = placement,
                        screenAspect = rememberScreenAspect(),
                        onPick = launchPick,
                        onDesignChange = vm::updateDesign,
                        onDesignChangeAtEdge = { t -> vm.updateDesign(t, stopAtEdge = true) },
                        maxClockSize = vm::maxClockSize,
                        onApply = { vm.save(openPickerAfter = !placement.onLock) },
                        onDiscard = vm::discardChanges,
                        onShowGuide = { showGuide = true },
                    )
                }
            }
            state.notice?.let { NoticeBar(it, Modifier.align(Alignment.TopCenter).padding(top = 56.dp)) }
        }
    }

    if (showGuide) HideClockGuide(onDismiss = { showGuide = false })
}

@Composable
private fun EditingContent(
    state: EditorState,
    photo: android.graphics.Bitmap,
    placement: Placement,
    screenAspect: Float,
    onPick: () -> Unit,
    onDesignChange: ((DepthDesign) -> DepthDesign) -> Unit,
    onDesignChangeAtEdge: ((DepthDesign) -> DepthDesign) -> Unit,
    maxClockSize: (ClockSpec) -> Float,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    onShowGuide: () -> Unit,
) {
    val ctx = LocalContext.current
    val coverageRenderer = remember { LockRenderer(ctx) }
    val coverage = remember(state.design, state.subject) {
        if (!state.design.depthEnabled) 0f
        // نقيس على وقت بأربع أرقام — أعرض حالة، وما تتغيّر كل دقيقة
        else coverageRenderer.subjectCoverage(state.subject, 1080f, 1080f / screenAspect, state.design, "10:00")
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ساعة العمق",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onPick) { Text("غيّر الصورة") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            LockPreview(
                photo = photo,
                subject = state.subject,
                design = state.design,
                screenAspect = screenAspect,
                onDesignChange = onDesignChange,
            )
        }
        PreviewHint(coverage = coverage)
        ControlsPanel(
            design = state.design,
            swatches = state.swatches,
            hasSubject = state.subject != null,
            maxClockSize = maxClockSize,
            onDesignChange = onDesignChange,
            onDesignChangeAtEdge = onDesignChangeAtEdge,
            onShowGuide = onShowGuide,
            modifier = Modifier.padding(top = 6.dp),
        )
        ApplyBar(
            placement = placement,
            dirty = state.dirty,
            saving = state.saving,
            onApply = onApply,
            onDiscard = onDiscard,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun PreviewHint(coverage: Float) {
    val (text, warn) = if (coverage > 0.55f) {
        "الشخص مغطّي أغلب الساعة — نزّلها أو صغّر الصورة حتى تنقرا" to true
    } else {
        "اسحب الساعة لفوگ وجوّه، واسحب الصورة أو قرّصها حتى تضبطها" to false
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (warn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun rememberScreenAspect(): Float {
    val ctx = LocalContext.current
    return remember {
        val b = ctx.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val w = minOf(b.width(), b.height()).toFloat()
        val h = maxOf(b.width(), b.height()).toFloat()
        w / h
    }
}

@Composable
private fun NoticeBar(text: String, modifier: Modifier = Modifier) {
    // Box مو Surface: الرسالة ما تبلع اللمسات من الي تحتها
    Box(
        modifier
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
