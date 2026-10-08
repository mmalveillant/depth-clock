package com.alijassib.depthclock.data

/** محور متغيّر بالخط (وزن/عرض). */
data class AxisRange(val min: Float, val max: Float)

sealed interface FontSource {
    /** عائلة نظام (sans-serif, serif…) — الوزن يختار أقرب ملف. */
    data class Family(val family: String) : FontSource
    /** ملف بـ /system/fonts — موجود بتلفونات سامسونگ؛ إذا ماكو نرجع للخط الافتراضي. */
    data class SystemFile(val path: String) : FontSource
    /** ملف داخل التطبيق (assets/fonts) — خطوط OFL. */
    data class Asset(val path: String) : FontSource
}

data class ClockFontDef(
    val id: String,
    val label: String,
    val source: FontSource,
    /** null = وزن واحد (السلايدر يتعطّل). */
    val wght: AxisRange? = null,
    val wdth: AxisRange? = null,
    /** محاور ثابتة نضبطها دائمًا (مثل opsz=144 حتى RobotoFlex يطلع بشكل «عناوين»). */
    val fixedAxes: String = "",
)

object ClockFonts {
    const val SYS_SANS = "sys_sans"
    const val SYS_CONDENSED = "sys_condensed"
    const val SYS_SERIF = "sys_serif"
    const val SYS_MONO = "sys_mono"
    const val DEFAULT_ID = SYS_SANS

    private val FAMILY_WEIGHTS = AxisRange(100f, 900f)

    val all: List<ClockFontDef> = listOf(
        // خطوط التلفون
        ClockFontDef("roboto_flex", "Flex", FontSource.SystemFile("/system/fonts/RobotoFlex-Regular.ttf"),
            wght = AxisRange(100f, 1000f), wdth = AxisRange(25f, 151f), fixedAxes = "'opsz' 144"),
        ClockFontDef("oneui", "One UI", FontSource.SystemFile("/system/fonts/OneUISans-VF.ttf"), wght = AxisRange(100f, 900f)),
        ClockFontDef("secnum", "أرقام سامسونگ", FontSource.SystemFile("/system/fonts/SECNum-VF.ttf"), wght = AxisRange(100f, 900f)),
        ClockFontDef(SYS_SANS, "النظام", FontSource.Family("sans-serif"), wght = FAMILY_WEIGHTS),
        ClockFontDef(SYS_CONDENSED, "ضيّق", FontSource.Family("sans-serif-condensed"), wght = FAMILY_WEIGHTS),
        ClockFontDef(SYS_SERIF, "كلاسيكي", FontSource.Family("serif"), wght = FAMILY_WEIGHTS),
        ClockFontDef(SYS_MONO, "رقمي", FontSource.Family("monospace"), wght = FAMILY_WEIGHTS),
        ClockFontDef("dancing", "مكتوب", FontSource.SystemFile("/system/fonts/DancingScript-Regular.ttf"), wght = AxisRange(400f, 700f)),
        ClockFontDef("cutive", "آلة كاتبة", FontSource.SystemFile("/system/fonts/CutiveMono.ttf")),
        ClockFontDef("coming_soon", "قلم", FontSource.SystemFile("/system/fonts/ComingSoon.ttf")),
        // خطوط OFL داخل التطبيق
        ClockFontDef("bebas", "Bebas", FontSource.Asset("fonts/BebasNeue-Bold.ttf")),
        ClockFontDef("teko", "Teko", FontSource.Asset("fonts/Teko-VariableFont_wght.ttf"), wght = AxisRange(300f, 700f)),
        ClockFontDef("tourney", "رياضي", FontSource.Asset("fonts/Tourney-VariableFont_wdth,wght.ttf"),
            wght = AxisRange(100f, 900f), wdth = AxisRange(75f, 125f)),
        ClockFontDef("kalnia", "أنيق", FontSource.Asset("fonts/Kalnia-VariableFont_wdth,wght.ttf"),
            wght = AxisRange(100f, 700f), wdth = AxisRange(100f, 125f)),
        ClockFontDef("abril", "فاخر", FontSource.Asset("fonts/AbrilFatface-Regular.ttf")),
        ClockFontDef("marvel", "نحيف", FontSource.Asset("fonts/Marvel-Bold.ttf")),
        ClockFontDef("tulpen", "طويل", FontSource.Asset("fonts/TulpenOne-Regular.ttf")),
        ClockFontDef("tektur", "تقني", FontSource.Asset("fonts/Tektur-Medium.ttf")),
        ClockFontDef("stick", "ستنسل", FontSource.Asset("fonts/StickNoBills-VariableFont_wght.ttf"), wght = AxisRange(200f, 800f)),
        ClockFontDef("pixel", "بكسل", FontSource.Asset("fonts/PixelifySans-VariableFont_wght.ttf"), wght = AxisRange(400f, 700f)),
        ClockFontDef("digital", "ديجيتال", FontSource.Asset("fonts/DigitalNumbers-Regular.ttf")),
        ClockFontDef("marhey", "مدوّر", FontSource.Asset("fonts/Marhey-VariableFont_wght.ttf"), wght = AxisRange(300f, 700f)),
        ClockFontDef("edu", "يدوي", FontSource.Asset("fonts/EduSABeginner-VariableFont_wght.ttf"), wght = AxisRange(400f, 700f)),
        ClockFontDef("fredericka", "طباشير", FontSource.Asset("fonts/FrederickatheGreat-Regular.ttf")),
        ClockFontDef("sevillana", "زخرفة", FontSource.Asset("fonts/Sevillana-Regular.ttf")),
        ClockFontDef("glitch", "Glitch", FontSource.Asset("fonts/RubikGlitch-Regular.ttf")),
        ClockFontDef("prism", "منشور", FontSource.Asset("fonts/TiltPrism-Regular-VariableFont_XROT,YROT.ttf")),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): ClockFontDef? = byId[id]
}
