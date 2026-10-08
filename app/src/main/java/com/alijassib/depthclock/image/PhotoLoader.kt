package com.alijassib.depthclock.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoLoader {

    /** أطول ضلع نحتفظ بيه: يكفي للتكبير على شاشة QHD+ وما ياكل ذاكرة هواية. */
    private const val MAX_SIDE = 2560

    /**
     * يفك الصورة (JPEG/HEIC/…) ويطبّق دوران EXIF تلقائيًا (ImageDecoder يسوّيها)،
     * يصغّرها، ويحوّلها لـ sRGB بلا gainmap حتى تطلع نفس الشي بالمعاينة وبشاشة القفل.
     */
    fun load(ctx: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(ctx.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = MAX_SIDE.toFloat() / max(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            decoder.isMutableRequired = false
        }
        if (bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.hasGainmap()) return bitmap
        // صور سامسونگ الـ Ultra HDR بيها gainmap — نشيله حتى ما يتوهّج الشخص فوق الخلفية
        return bitmap.copy(Bitmap.Config.ARGB_8888, true).apply { if (hasGainmap()) gainmap = null }
    }
}
