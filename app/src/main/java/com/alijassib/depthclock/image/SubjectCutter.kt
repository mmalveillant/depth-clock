package com.alijassib.depthclock.image

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * يقص الشخص (أو أي شي واضح بالصورة) بـ ML Kit Subject Segmentation على الجهاز.
 * الناتج بتماب بنفس أبعاد الصورة: بكسلات الشخص وباقي الصورة شفاف.
 */
class SubjectCutter(private val ctx: Context) {

    class NoSubjectException : Exception("ما لكيت شخص واضح بالصورة")

    suspend fun cut(photo: Bitmap, onStatus: (String) -> Unit): Bitmap {
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build()
        )
        try {
            ensureModule(segmenter, onStatus)
            onStatus("دا أفصل الشخص عن الخلفية…")
            val result = segmenter.process(InputImage.fromBitmap(photo, 0)).await()
            val mask = result.foregroundConfidenceMask ?: throw NoSubjectException()
            return withContext(Dispatchers.Default) { applyMask(photo, mask) }
        } finally {
            segmenter.close()
        }
    }

    /** الموديل ينزل من Play services أول مرة (~بضع ميگات). */
    private suspend fun ensureModule(segmenter: SubjectSegmenter, onStatus: (String) -> Unit) {
        val client = ModuleInstall.getClient(ctx)
        if (client.areModulesAvailable(segmenter).await().areModulesAvailable()) return

        onStatus("دا أنزّل موديل القص لأول مرة…")
        val done = CompletableDeferred<Unit>()
        val listener = InstallStatusListener { update ->
            when (update.installState) {
                InstallState.STATE_COMPLETED -> done.complete(Unit)
                InstallState.STATE_FAILED, InstallState.STATE_CANCELED ->
                    done.completeExceptionally(IllegalStateException("فشل تنزيل موديل القص (${update.errorCode})"))
                else -> Unit
            }
        }
        val request = ModuleInstallRequest.newBuilder().addApi(segmenter).setListener(listener).build()
        try {
            val response = client.installModules(request).await()
            if (!response.areModulesAlreadyInstalled()) withTimeout(120_000) { done.await() }
        } finally {
            client.unregisterListener(listener)
        }
    }

    private fun applyMask(photo: Bitmap, mask: java.nio.FloatBuffer): Bitmap {
        val w = photo.width
        val h = photo.height
        val pixels = IntArray(w * h)
        photo.getPixels(pixels, 0, w, 0, 0, w, h)
        mask.rewind()
        var any = false
        for (i in pixels.indices) {
            val a = edge(mask.get())
            if (a > 0) any = true
            pixels[i] = (a shl 24) or (pixels[i] and 0x00FFFFFF)
        }
        if (!any) throw NoSubjectException()
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    /** الثقة 0..1 ← شفافية. نشد الحافة شوية حتى ما تطلع هالة من الخلفية حول الشعر. */
    private fun edge(confidence: Float): Int {
        val t = ((confidence - 0.30f) / 0.45f).coerceIn(0f, 1f)
        val s = t * t * (3f - 2f * t)
        return (s * 255f + 0.5f).toInt()
    }
}
