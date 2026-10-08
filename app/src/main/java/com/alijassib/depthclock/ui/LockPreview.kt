package com.alijassib.depthclock.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.alijassib.depthclock.data.DepthDesign
import com.alijassib.depthclock.render.ClockText
import com.alijassib.depthclock.render.LockRenderer
import kotlinx.coroutines.delay
import kotlin.math.max

private enum class DragTarget { Photo, Clock }

/** مسافة اللزگة على نص الشاشة (نسبة من العرض). */
private const val SNAP = 0.025f

/**
 * معاينة شاشة القفل بنفس نسبة الشاشة الحقيقية. نفس [LockRenderer] الي يرسم الخلفية الحية.
 * السحب على الساعة يحرّكها، والقرص عليها يكبّرها؛ برّاها يحرّك/يكبّر الصورة.
 */
@Composable
fun LockPreview(
    photo: Bitmap,
    subject: Bitmap?,
    design: DepthDesign,
    screenAspect: Float,
    onDesignChange: ((DepthDesign) -> DepthDesign) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val renderer = remember { LockRenderer(ctx) }
    val clockRect = remember { RectF() }
    var active by remember { mutableStateOf<DragTarget?>(null) }
    /** الساعة لازگة عالنص أثناء السحب (نرسم خط إرشاد). */
    var snapped by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // نحدّث المعاينة على رأس كل دقيقة، مثل شاشة القفل
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
            now = System.currentTimeMillis()
        }
    }

    val time = ClockText.time(ctx, java.util.Date(now))
    val date = ClockText.date(java.util.Date(now))
    val latestDesign by rememberUpdatedState(design)
    val latestTime by rememberUpdatedState(time)
    val latestChange by rememberUpdatedState(onDesignChange)
    val shape = RoundedCornerShape(28.dp)
    val outline = MaterialTheme.colorScheme.outline

    Canvas(
        modifier
            .aspectRatio(screenAspect)
            .clip(shape)
            .border(1.dp, outline, shape)
            .pointerInput(photo) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    // نوسّع منطقة لمس الساعة شوية — الأرقام الصغيرة صعب تنمسك
                    val pad = w * 0.04f
                    val hit = renderer.clockBounds(w, h, latestDesign, latestTime, clockRect)
                    val target = if (down.position.x in hit.left - pad..hit.right + pad &&
                        down.position.y in hit.top - pad..hit.bottom + pad
                    ) DragTarget.Clock else DragTarget.Photo
                    var started = false
                    // مكان الساعة الأفقي «الخام» بلا لزگة، حتى نعرف متى يفلت من النص
                    var rawX = latestDesign.clock.centerX
                    var travel = Offset.Zero
                    var scale = 1f
                    val slop = viewConfiguration.touchSlop
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        if (!started) {
                            // لمسة عادية ما تحرّك شي — لازم تعبر الـ slop أول
                            travel += pan
                            scale *= zoom
                            if (travel.getDistance() > slop || kotlin.math.abs(scale - 1f) > 0.04f) {
                                started = true
                                active = target
                                if (target == DragTarget.Clock) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        } else if (pan != Offset.Zero || zoom != 1f) {
                            // نقطة الصورة الي جانت تحت الأصابع (الموضع السابق) تلحگ الأصابع لموضعها الحالي
                            val before = event.calculateCentroid(useCurrent = false)
                            val after = event.calculateCentroid(useCurrent = true)
                            if (target == DragTarget.Clock) {
                                // إذا مو لازگة بالنص، نبني على القيمة المحفوظة (بعد تثبيت الحواف) حتى الرجعة تتحرّك فورًا
                                rawX = (if (snapped) rawX else latestDesign.clock.centerX) + pan.x / w
                                val snap = kotlin.math.abs(rawX - 0.5f) < SNAP
                                if (snap && !snapped) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                snapped = snap
                                val x = if (snap) 0.5f else rawX
                                latestChange { d ->
                                    d.copy(clock = d.clock.copy(centerX = x, centerY = d.clock.centerY + pan.y / h, size = d.clock.size * zoom))
                                }
                            } else {
                                latestChange { d -> movePhoto(d, photo, w, h, zoom, before, after) }
                            }
                        }
                        if (started) event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                    active = null
                    snapped = false
                }
            }
    ) {
        drawIntoCanvas { c ->
            renderer.draw(
                canvas = c.nativeCanvas,
                w = size.width, h = size.height,
                photo = photo, subject = subject, d = design,
                time = time, date = date, drawClock = true,
            )
        }
        if (active == DragTarget.Clock && snapped) {
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        if (active == DragTarget.Clock) {
            val r = renderer.clockBounds(size.width, size.height, design, time, clockRect)
            val pad = size.width * 0.03f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(r.left - pad, r.top - pad),
                size = androidx.compose.ui.geometry.Size(r.width() + pad * 2, r.height() + pad * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pad),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

/** تحريك/تكبير الصورة: النقطة الي جانت تحت [from] تصير تحت [to]، وتتحوّل لقيم نسبية (pan -1..1). */
private fun movePhoto(d: DepthDesign, photo: Bitmap, w: Float, h: Float, zoom: Float, from: Offset, to: Offset): DepthDesign {
    val pw = photo.width.toFloat()
    val ph = photo.height.toFloat()
    val base = max(w / pw, h / ph)
    val s = base * d.zoom
    val oxOld = pw * s - w
    val oyOld = ph * s - h
    val tx = -oxOld / 2f + d.panX * oxOld / 2f
    val ty = -oyOld / 2f + d.panY * oyOld / 2f

    val newZoom = (d.zoom * zoom).coerceIn(1f, DepthDesign.MAX_ZOOM)
    val s2 = base * newZoom
    val ix = (from.x - tx) / s
    val iy = (from.y - ty) / s
    val tx2 = to.x - ix * s2
    val ty2 = to.y - iy * s2
    val ox = pw * s2 - w
    val oy = ph * s2 - h
    return d.copy(
        zoom = newZoom,
        panX = if (ox > 0.5f) (1f + 2f * tx2 / ox).coerceIn(-1f, 1f) else 0f,
        panY = if (oy > 0.5f) (1f + 2f * ty2 / oy).coerceIn(-1f, 1f) else 0f,
    )
}
