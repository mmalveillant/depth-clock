package com.alijassib.depthclock.render

import android.content.res.AssetManager
import android.graphics.Typeface
import android.util.Log
import android.util.LruCache
import com.alijassib.depthclock.data.ClockFontDef
import com.alijassib.depthclock.data.ClockFonts
import com.alijassib.depthclock.data.FontSource
import java.io.File
import kotlin.math.roundToInt

/**
 * يبني Typeface لكل (خط، وزن، عرض) مرة وحدة. السلايدرات تتحرّك بخطوات صغيرة،
 * فنقرّب الوزن لأقرب 10 والعرض لأقرب 1 حتى الكاش يشتغل وما نبني خط بكل فريم.
 */
object FontLoader {

    private val cache = LruCache<String, Typeface>(160)

    /** قيمة محور العرض الي نطبّقها، والباقي من [ClockSpec.widthScale] يصير تمديد بالرسم. */
    fun wdthAxisFor(def: ClockFontDef, widthScale: Float): Float? {
        val r = def.wdth ?: return null
        // إذا ملف الخط مو موجود (تلفون غير سامسونگ) رجعنا للخط الافتراضي بلا محور عرض
        if (!isAvailable(def)) return null
        return (100f * widthScale).coerceIn(r.min, r.max).roundToInt().toFloat()
    }

    fun typeface(assets: AssetManager, fontId: String, weight: Int, widthScale: Float): Typeface {
        val def = ClockFonts.byId(fontId) ?: ClockFonts.byId(ClockFonts.DEFAULT_ID)!!
        val w = def.wght?.let { (weight.coerceIn(it.min.toInt(), it.max.toInt()) / 10f).roundToInt() * 10 }
        val wdth = wdthAxisFor(def, widthScale)
        val key = "${def.id}|$w|$wdth"
        cache.get(key)?.let { return it }
        val tf = runCatching { build(assets, def, w, wdth) }
            .onFailure { Log.w(TAG, "font ${def.id} failed", it) }
            .getOrNull() ?: Typeface.DEFAULT
        cache.put(key, tf)
        return tf
    }

    private val available = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun isAvailable(def: ClockFontDef): Boolean = available.getOrPut(def.id) {
        when (val src = def.source) {
            is FontSource.SystemFile -> File(src.path).canRead()
            else -> true
        }
    }

    private fun build(assets: AssetManager, def: ClockFontDef, weight: Int?, wdth: Float?): Typeface {
        val axes = buildList {
            if (def.fixedAxes.isNotEmpty()) add(def.fixedAxes)
            if (weight != null && def.source !is FontSource.Family) add("'wght' $weight")
            if (wdth != null) add("'wdth' $wdth")
        }.joinToString(", ")

        return when (val src = def.source) {
            // نفس طريقة النسخة الأولى بالضبط (حتى التصاميم القديمة تطلع نفسها)
            is FontSource.Family -> Typeface.create(Typeface.create(src.family, Typeface.NORMAL), weight ?: 400, false)
            is FontSource.SystemFile -> {
                val f = File(src.path)
                if (!f.canRead()) Typeface.create(Typeface.DEFAULT, weight ?: 400, false)
                else Typeface.Builder(f).apply { if (axes.isNotEmpty()) setFontVariationSettings(axes) }.build()
            }
            is FontSource.Asset -> Typeface.Builder(assets, src.path).apply { if (axes.isNotEmpty()) setFontVariationSettings(axes) }.build()
        }
    }

    private const val TAG = "FontLoader"
}
