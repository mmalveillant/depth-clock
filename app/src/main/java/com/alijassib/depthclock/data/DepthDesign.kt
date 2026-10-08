package com.alijassib.depthclock.data

import com.alijassib.depthclock.render.ClockRenderer
import org.json.JSONObject

/**
 * إعدادات التصميم. كل القيم نسبية (مو بكسلات) حتى تطلع نفس النتيجة
 * بالمعاينة الصغيرة داخل التطبيق وبشاشة القفل الحقيقية.
 */
data class DepthDesign(
    /** تكبير الصورة فوق «تغطية الشاشة»، 1 = تغطية بالضبط. */
    val zoom: Float = 1f,
    /** إزاحة الصورة: -1..1 (1 = الحافة اليسرى/العليا على حافة الشاشة). */
    val panX: Float = 0f,
    val panY: Float = 0f,
    val clock: ClockSpec = ClockSpec(),
    /** الشخص فوق الساعة (تأثير العمق). */
    val depthEnabled: Boolean = true,
    val showDate: Boolean = true,
) {
    fun toJson(): String = JSONObject()
        .put("v", 2)
        .put("zoom", zoom.toDouble())
        .put("panX", panX.toDouble())
        .put("panY", panY.toDouble())
        .put("clock", clock.toJson())
        .put("depth", depthEnabled)
        .put("date", showDate)
        .toString()

    fun clamped(): DepthDesign = copy(
        zoom = zoom.coerceIn(1f, MAX_ZOOM),
        panX = panX.coerceIn(-1f, 1f),
        panY = panY.coerceIn(-1f, 1f),
        clock = clock.clamped(),
    )

    companion object {
        const val MAX_ZOOM = 4f

        fun fromJson(json: String): DepthDesign {
            val o = JSONObject(json)
            val clock = o.optJSONObject("clock")?.let { ClockSpec.fromJson(it) } ?: legacyClock(o)
            return DepthDesign(
                zoom = o.optDouble("zoom", 1.0).toFloat(),
                panX = o.optDouble("panX", 0.0).toFloat(),
                panY = o.optDouble("panY", 0.0).toFloat(),
                clock = clock,
                depthEnabled = o.optBoolean("depth", true),
                showDate = o.optBoolean("date", true),
            ).clamped()
        }

        /**
         * النسخة الأولى كانت تخزّن خط من ست خطوط وحجم يتصغّر لحاله حتى «00:00» يلزم 92% من العرض.
         * نحوّلها لنفس الشكل بالضبط حتى شاشة القفل ما تتغيّر بالتحديث.
         */
        private fun legacyClock(o: JSONObject): ClockSpec {
            val (fontId, weight) = when (o.optString("font")) {
                "Heavy" -> ClockFonts.SYS_SANS to 900
                "Light" -> ClockFonts.SYS_SANS to 300
                "Condensed" -> ClockFonts.SYS_CONDENSED to 700
                "Serif" -> ClockFonts.SYS_SERIF to 600
                "Mono" -> ClockFonts.SYS_MONO to 500
                else -> ClockFonts.SYS_SANS to 700
            }
            val size = o.optDouble("clockSize", 0.30).toFloat()
            val base = ClockSpec(fontId = fontId, weight = weight)
            return base.copy(
                size = size.coerceAtMost(ClockRenderer.legacyMaxSize(base)),
                colorArgb = o.optInt("color", base.colorArgb),
                centerY = o.optDouble("clockCenterY", base.centerY.toDouble()).toFloat(),
            )
        }
    }
}
