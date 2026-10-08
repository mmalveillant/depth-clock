package com.alijassib.depthclock.data

import org.json.JSONObject

enum class ClockLayout(val label: String) {
    /** «12:45» */
    Horizontal("أفقية"),
    /** «12» فوگ «45» */
    Stacked("عمودية"),
}

/** شكل الدقائق مقارنةً بالساعات (مثل ساعة سامسونگ «10 40» بلونين). */
enum class MinutesTone(val label: String) {
    Same("نفس الشي"),
    Dim("باهتة"),
    Outline("إطار"),
}

/**
 * كل شي يخص شكل الساعة. الأحجام نسبية لعرض الشاشة حتى المعاينة والقفل يطلعون نفس الشي.
 * القيم الافتراضية = الساعة القديمة بالضبط (حتى التحديث ما يغيّر شاشة القفل).
 */
data class ClockSpec(
    val fontId: String = ClockFonts.DEFAULT_ID,
    /** 100..1000؛ يشتغل بس على الخطوط الي بيها أوزان. */
    val weight: Int = 700,
    /** حجم الخط كنسبة من عرض الشاشة. */
    val size: Float = 0.30f,
    /** «العرض»: 1 = طبيعي. */
    val widthScale: Float = 1f,
    /** «الطول»: 1 = طبيعي. */
    val heightScale: Float = 1f,
    /** تباعد الأرقام (em). */
    val spacing: Float = 0f,
    /** المسافة بين سطري الساعة العمودية (em). */
    val lineGap: Float = 0.08f,
    val layout: ClockLayout = ClockLayout.Horizontal,
    val showColon: Boolean = true,
    val minutesTone: MinutesTone = MinutesTone.Same,
    /** إطار بس (بلا تعبئة). */
    val outline: Boolean = false,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    /** مركز الساعة كنسبة من الشاشة. */
    val centerX: Float = 0.5f,
    val centerY: Float = 0.24f,
) {
    fun clamped(): ClockSpec = copy(
        weight = weight.coerceIn(100, 1000),
        size = size.coerceIn(MIN_SIZE, MAX_SIZE),
        widthScale = widthScale.coerceIn(0.5f, 1.8f),
        heightScale = heightScale.coerceIn(0.5f, 2.2f),
        spacing = spacing.coerceIn(-0.15f, 0.5f),
        lineGap = lineGap.coerceIn(-0.4f, 0.6f),
        centerX = centerX.coerceIn(0.08f, 0.92f),
        centerY = centerY.coerceIn(0.04f, 0.9f),
    )

    /** نفس الشكل بس بمكان ولون المستخدم الحاليين (لمّا يختار شكل جاهز). */
    fun withPlacementOf(other: ClockSpec) = copy(centerX = other.centerX, centerY = other.centerY, colorArgb = other.colorArgb)

    fun toJson(): JSONObject = JSONObject()
        .put("font", fontId).put("weight", weight).put("size", size.toDouble())
        .put("w", widthScale.toDouble()).put("h", heightScale.toDouble())
        .put("spacing", spacing.toDouble()).put("lineGap", lineGap.toDouble())
        .put("layout", layout.name).put("colon", showColon).put("minutes", minutesTone.name)
        .put("outline", outline).put("color", colorArgb)
        .put("cx", centerX.toDouble()).put("cy", centerY.toDouble())

    companion object {
        const val MIN_SIZE = 0.08f
        const val MAX_SIZE = 0.9f

        fun fromJson(o: JSONObject): ClockSpec {
            val d = ClockSpec()
            return ClockSpec(
                fontId = o.optString("font", d.fontId).takeIf { ClockFonts.byId(it) != null } ?: d.fontId,
                weight = o.optInt("weight", d.weight),
                size = o.optDouble("size", d.size.toDouble()).toFloat(),
                widthScale = o.optDouble("w", 1.0).toFloat(),
                heightScale = o.optDouble("h", 1.0).toFloat(),
                spacing = o.optDouble("spacing", 0.0).toFloat(),
                lineGap = o.optDouble("lineGap", d.lineGap.toDouble()).toFloat(),
                layout = enumOr(o.optString("layout"), d.layout),
                showColon = o.optBoolean("colon", true),
                minutesTone = enumOr(o.optString("minutes"), d.minutesTone),
                outline = o.optBoolean("outline", false),
                colorArgb = o.optInt("color", d.colorArgb),
                centerX = o.optDouble("cx", 0.5).toFloat(),
                centerY = o.optDouble("cy", d.centerY.toDouble()).toFloat(),
            ).clamped()
        }

        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            runCatching { enumValueOf<E>(name ?: "") }.getOrDefault(fallback)
    }
}
