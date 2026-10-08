package com.alijassib.depthclock.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.alijassib.depthclock.data.DepthDesign
import kotlin.math.max

/**
 * الرسّام الوحيد: نفس الكود يرسم المعاينة داخل التطبيق وشاشة القفل الحقيقية.
 * الترتيب: الصورة ← الساعة ← الشخص (إذا العمق شغّال) ← التاريخ.
 */
class LockRenderer(context: Context) {

    val clock = ClockRenderer(context.applicationContext.assets)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val dateTypeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val clockRect = RectF()

    /** مكان الصورة على شاشة بحجم w×h. */
    fun photoMatrix(pw: Int, ph: Int, w: Float, h: Float, d: DepthDesign, out: Matrix = matrix): Matrix {
        val s = max(w / pw, h / ph) * d.zoom
        val overflowX = pw * s - w
        val overflowY = ph * s - h
        out.setScale(s, s)
        out.postTranslate(-overflowX / 2f + d.panX * overflowX / 2f, -overflowY / 2f + d.panY * overflowY / 2f)
        return out
    }

    /** مستطيل الساعة (للمس بالمعاينة ولحساب التغطية). */
    fun clockBounds(w: Float, h: Float, d: DepthDesign, time: String, out: RectF = RectF()): RectF =
        clock.bounds(d.clock, w, h, time, dateAbove(w, d), out)

    fun draw(
        canvas: Canvas,
        w: Float,
        h: Float,
        photo: Bitmap?,
        subject: Bitmap?,
        d: DepthDesign,
        time: String,
        date: String,
        drawClock: Boolean,
    ) {
        canvas.drawColor(Color.BLACK)
        if (photo != null) canvas.drawBitmap(photo, photoMatrix(photo.width, photo.height, w, h, d), bitmapPaint)
        if (!drawClock) return

        clock.draw(canvas, d.clock, w, h, time, dateAbove(w, d))

        if (d.depthEnabled && subject != null && photo != null) {
            canvas.drawBitmap(subject, photoMatrix(subject.width, subject.height, w, h, d), bitmapPaint)
        }

        if (d.showDate && date.isNotEmpty()) {
            val r = clockBounds(w, h, d, time, clockRect)
            datePaint.color = d.clock.colorArgb
            datePaint.textSize = w * DATE_SIZE
            datePaint.typeface = dateTypeface
            // التاريخ أعرض من الساعة أحيانًا — نخليه داخل الشاشة لو الساعة مسحوبة للحافة
            val half = datePaint.measureText(date) / 2f
            val margin = w * 0.03f
            val x = if (half * 2 > w - 2 * margin) w / 2f else r.centerX().coerceIn(margin + half, w - margin - half)
            canvas.drawText(date, x, r.top - w * DATE_GAP, datePaint)
        }
    }

    /**
     * شكد من مستطيل الساعة مغطّى بالشخص (0..1). iOS يلغي العمق إذا الشخص غطّى أغلب الساعة،
     * إحنا بس ننبّه.
     */
    fun subjectCoverage(subject: Bitmap?, w: Float, h: Float, d: DepthDesign, time: String): Float {
        if (subject == null) return 0f
        val r = clockBounds(w, h, d, time, clockRect)
        photoMatrix(subject.width, subject.height, w, h, d, matrix).invert(inverse)
        val pt = FloatArray(2)
        var covered = 0
        var total = 0
        val cols = 32
        val rows = 12
        for (iy in 0 until rows) for (ix in 0 until cols) {
            pt[0] = r.left + (ix + 0.5f) / cols * r.width()
            pt[1] = r.top + (iy + 0.5f) / rows * r.height()
            inverse.mapPoints(pt)
            val x = pt[0].toInt()
            val y = pt[1].toInt()
            total++
            if (x in 0 until subject.width && y in 0 until subject.height &&
                Color.alpha(subject.getPixel(x, y)) > 128
            ) covered++
        }
        return if (total == 0) 0f else covered.toFloat() / total
    }

    private fun dateAbove(w: Float, d: DepthDesign) = if (d.showDate) w * (DATE_SIZE + DATE_GAP) else 0f

    companion object {
        private const val DATE_SIZE = 0.048f
        private const val DATE_GAP = 0.035f
    }
}
