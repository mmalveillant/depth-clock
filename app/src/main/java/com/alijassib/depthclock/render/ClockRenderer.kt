package com.alijassib.depthclock.render

import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.alijassib.depthclock.data.ClockFonts
import com.alijassib.depthclock.data.ClockLayout
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.MinutesTone
import kotlin.math.max
import kotlin.math.min

/**
 * يرسم الساعة حسب [ClockSpec]. الرسم يصير بإحداثيات محلية مركزها وسط الساعة،
 * وبعدين ننقل للمكان ونمدّد (الطول/العرض) — فالحدود واللمس والرسم كلهم من نفس الحساب.
 */
class ClockRenderer(private val assets: AssetManager) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minutesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val digitRect = Rect()

    /** نتيجة الترتيب: كل شي يحتاجه الرسم والحدود. */
    private class Layout {
        var hours = ""
        var colon = ""
        var minutes = ""
        var textSize = 0f
        var digitH = 0f
        var gap = 0f
        var hoursW = 0f
        var colonW = 0f
        var minutesW = 0f
        var sx = 1f
        var sy = 1f
        var cx = 0f
        var cy = 0f
        val local = RectF()
    }

    private val layout = Layout()

    /** مستطيل الساعة على الشاشة (بعد التمديد والتثبيت). */
    fun bounds(spec: ClockSpec, w: Float, h: Float, time: String, dateAbove: Float, out: RectF = RectF()): RectF {
        val l = measure(spec, w, h, time, dateAbove)
        out.set(
            l.cx + l.local.left * l.sx, l.cy + l.local.top * l.sy,
            l.cx + l.local.right * l.sx, l.cy + l.local.bottom * l.sy,
        )
        return out
    }

    fun draw(canvas: Canvas, spec: ClockSpec, w: Float, h: Float, time: String, dateAbove: Float) {
        val l = measure(spec, w, h, time, dateAbove)
        // الرسم يتمدّد بـ canvas.scale؛ نعوّض سُمك الإطار حتى ما يطلع أثخن بالطول من العرض
        val strokeFix = 1f / kotlin.math.sqrt(l.sx * l.sy)
        if (spec.outline) fill.strokeWidth *= strokeFix
        configureMinutes(spec, l.textSize)
        if (spec.minutesTone == MinutesTone.Outline && !spec.outline) minutesPaint.strokeWidth *= strokeFix

        canvas.save()
        canvas.translate(l.cx, l.cy)
        canvas.scale(l.sx, l.sy)
        when (spec.layout) {
            ClockLayout.Horizontal -> {
                val baseline = l.digitH / 2f
                val simple = spec.minutesTone == MinutesTone.Same && spec.showColon
                if (simple) {
                    // نص واحد بنفس الطريقة القديمة (نفس الـ kerning بالضبط)
                    fill.textAlign = Paint.Align.CENTER
                    canvas.drawText("${l.hours}:${l.minutes}", 0f, baseline, fill)
                } else {
                    fill.textAlign = Paint.Align.LEFT
                    minutesPaint.textAlign = Paint.Align.LEFT
                    var x = -(l.hoursW + l.colonW + l.minutesW) / 2f
                    canvas.drawText(l.hours, x, baseline, fill)
                    x += l.hoursW
                    if (l.colon.isNotBlank()) canvas.drawText(l.colon, x, baseline, fill)
                    x += l.colonW
                    canvas.drawText(l.minutes, x, baseline, minutesPaint)
                }
            }
            ClockLayout.Stacked -> {
                fill.textAlign = Paint.Align.CENTER
                minutesPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(l.hours, 0f, -l.gap / 2f, fill)
                canvas.drawText(l.minutes, 0f, l.gap / 2f + l.digitH, minutesPaint)
            }
        }
        canvas.restore()
    }

    /**
     * أكبر حجم يخلي الساعة تلزم بالشاشة بأعرض أرقام ممكنة (نقيس كل رقم 0-9 مو بس «0»،
     * لأن خطوط مثل Abril أرقامها مو متساوية). [aspect] = عرض/ارتفاع الشاشة.
     */
    fun maxSize(spec: ClockSpec, aspect: Float): Float {
        configureFill(spec, 100f)
        val digitW = (0..9).maxOf { fill.measureText(it.toString()) }
        fill.getTextBounds("0", 0, 1, digitRect)
        val digitH = digitRect.height().toFloat()
        val sx = widthResidual(spec)
        val sy = spec.heightScale
        return when (spec.layout) {
            ClockLayout.Horizontal -> {
                val unitW = (4 * digitW + (if (spec.showColon) fill.measureText(":") else 0.25f * 100f)) / 100f
                val unitH = digitH / 100f
                min(0.96f / (unitW * sx), 0.5f / (aspect * unitH * sy))
            }
            ClockLayout.Stacked -> {
                val unitW = 2 * digitW / 100f
                val unitH = (2 * digitH + spec.lineGap * 100f) / 100f
                min(0.96f / (unitW * sx), 0.62f / (aspect * max(unitH, 0.1f) * sy))
            }
        }.coerceIn(ClockSpec.MIN_SIZE, ClockSpec.MAX_SIZE)
    }

    /** أعرض وقت ممكن بهالخط: أعرض رقم مكرّر («00:00» بخطوط، «88:88» بغيرها). */
    fun widestTime(spec: ClockSpec): String {
        configureFill(spec, 100f)
        val d = (0..9).maxBy { fill.measureText(it.toString()) }
        return "$d$d:$d$d"
    }

    private fun measure(spec: ClockSpec, w: Float, h: Float, time: String, dateAbove: Float): Layout {
        val l = layout
        val parts = time.split(':')
        l.hours = parts.getOrElse(0) { "" }
        l.minutes = parts.getOrElse(1) { "" }
        if (spec.layout == ClockLayout.Stacked) l.hours = l.hours.padStart(2, '0')

        l.textSize = spec.size * w
        configureFill(spec, l.textSize)
        fill.getTextBounds("0", 0, 1, digitRect)
        l.digitH = digitRect.height().toFloat()
        l.gap = spec.lineGap * l.textSize
        l.sx = widthResidual(spec)
        l.sy = spec.heightScale

        when (spec.layout) {
            ClockLayout.Horizontal -> {
                l.colon = if (spec.showColon) ":" else " "
                l.hoursW = fill.measureText(l.hours)
                l.colonW = if (spec.showColon) fill.measureText(":") else 0.25f * l.textSize
                l.minutesW = fill.measureText(l.minutes)
                val half = (l.hoursW + l.colonW + l.minutesW) / 2f
                l.local.set(-half, -l.digitH / 2f, half, l.digitH / 2f)
            }
            ClockLayout.Stacked -> {
                l.colon = ""
                l.hoursW = fill.measureText(l.hours)
                l.colonW = 0f
                l.minutesW = fill.measureText(l.minutes)
                val half = max(l.hoursW, l.minutesW) / 2f
                l.local.set(-half, -l.gap / 2f - l.digitH, half, l.gap / 2f + l.digitH)
            }
        }

        l.cx = spec.centerX * w
        // ما نخلي الساعة (والتاريخ فوگها) تطلع برّا الشاشة من فوگ
        val minCy = h * 0.05f + dateAbove - l.local.top * l.sy
        l.cy = max(spec.centerY * h, minCy)
        return l
    }

    private fun configureFill(spec: ClockSpec, textSize: Float) {
        fill.typeface = FontLoader.typeface(assets, spec.fontId, spec.weight, spec.widthScale)
        fill.textSize = textSize
        fill.letterSpacing = spec.spacing
        fill.color = spec.colorArgb
        if (spec.outline) {
            fill.style = Paint.Style.STROKE
            fill.strokeWidth = max(1.5f, textSize * 0.022f)
            fill.strokeJoin = Paint.Join.ROUND
        } else {
            fill.style = Paint.Style.FILL
        }
    }

    private fun configureMinutes(spec: ClockSpec, textSize: Float) {
        minutesPaint.set(fill)
        when (spec.minutesTone) {
            MinutesTone.Same -> Unit
            MinutesTone.Dim -> minutesPaint.alpha = (fill.alpha * 0.45f).toInt()
            MinutesTone.Outline -> {
                minutesPaint.style = Paint.Style.STROKE
                minutesPaint.strokeWidth = max(1.5f, textSize * 0.022f)
                minutesPaint.strokeJoin = Paint.Join.ROUND
            }
        }
    }

    /** الجزء من «العرض» الي ما غطّاه محور wdth بالخط — نطبّقه كتمديد أفقي. */
    private fun widthResidual(spec: ClockSpec): Float {
        val def = ClockFonts.byId(spec.fontId) ?: return spec.widthScale
        val axis = FontLoader.wdthAxisFor(def, spec.widthScale) ?: return spec.widthScale
        return spec.widthScale / (axis / 100f)
    }

    companion object {
        /** الحجم الأقصى بالنسخة الأولى: «00:00» يلزم 92% من العرض (للتحويل بس). */
        fun legacyMaxSize(spec: ClockSpec): Float {
            val def = ClockFonts.byId(spec.fontId) ?: return spec.size
            val family = (def.source as? com.alijassib.depthclock.data.FontSource.Family)?.family ?: return spec.size
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.create(family, Typeface.NORMAL), spec.weight, false)
                textSize = 100f
            }
            return 0.92f * 100f / p.measureText("00:00")
        }
    }
}
