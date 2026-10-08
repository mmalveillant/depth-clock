package com.alijassib.depthclock.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArraySet

/** الصورة + الشخص المقصوص + الإعدادات، جاهزة للرسم. */
class DesignContent(
    val photo: Bitmap,
    /** نفس أبعاد [photo]؛ null إذا ما انلكه شخص. */
    val subject: Bitmap?,
    val design: DepthDesign,
    val version: Long,
)

/**
 * التصميم «المطبّق» على شاشة القفل. التطبيق يكتب، والخلفية الحية تقرا.
 * الاثنين بنفس العملية، فالتبليغ بالذاكرة يكفي؛ والملفات حتى تبقى بعد إعادة التشغيل.
 *
 * الحفظ «كله لو ماكو»: كل صورة بمجلد `assets_N` وكل إعدادات بملف `design_N.json`،
 * وملف `current` يأشّر عليهم ويتكتب آخر شي. إذا انقطع الحفظ بالنص (ذاكرة مليانة، إطفاء)
 * يبقى `current` يأشّر على النسخة القديمة الكاملة.
 */
object DesignStore {

    fun interface Listener { fun onDesignChanged() }

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var cached: DesignContent? = null

    private data class Pointer(val assets: Long, val design: Long)

    private fun dir(ctx: Context) = File(ctx.applicationContext.filesDir, "design").apply { mkdirs() }
    private fun pointerFile(ctx: Context) = File(dir(ctx), "current")
    private fun assetsDir(ctx: Context, n: Long) = File(dir(ctx), "assets_$n")
    private fun designFile(ctx: Context, n: Long) = File(dir(ctx), "design_$n.json")

    fun addListener(l: Listener) { listeners += l }
    fun removeListener(l: Listener) { listeners -= l }

    fun hasDesign(ctx: Context): Boolean = readPointer(ctx) != null

    /** يقرا من الكاش أو من القرص. لا تناديها على الـ main thread. */
    fun load(ctx: Context): DesignContent? {
        val p = readPointer(ctx) ?: return null
        synchronized(lock) { cached?.let { if (it.version == p.design) return it } }
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val assets = assetsDir(ctx, p.assets)
        val photo = BitmapFactory.decodeFile(File(assets, "photo.jpg").path, opts) ?: return null
        val subject = File(assets, "subject.png").takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.path, opts) }
            ?.takeIf { it.width == photo.width && it.height == photo.height }
        val design = runCatching { DepthDesign.fromJson(designFile(ctx, p.design).readText()) }.getOrDefault(DepthDesign())
        val content = DesignContent(photo, subject, design, p.design)
        synchronized(lock) { cached = content }
        return content
    }

    /**
     * يحفظ التصميم. [newPhoto] = false يعني «نفس الصورة السابقة، بس الإعدادات تغيّرت».
     * يرمي استثناء إذا فشل أي جزء، والنسخة القديمة تبقى سليمة. لا تناديها على الـ main thread.
     */
    fun save(ctx: Context, design: DepthDesign, photo: Bitmap, subject: Bitmap?, newPhoto: Boolean) {
        synchronized(lock) {
            val old = readPointer(ctx)
            val next = (old?.design ?: 0L) + 1
            val assets = if (newPhoto || old == null) {
                val n = next
                val d = assetsDir(ctx, n).apply { deleteRecursively(); mkdirs() }
                writeDurable(File(d, "photo.jpg")) { check(photo.compress(Bitmap.CompressFormat.JPEG, 95, it)) { "photo" } }
                if (subject != null) {
                    writeDurable(File(d, "subject.png")) { check(subject.compress(Bitmap.CompressFormat.PNG, 100, it)) { "subject" } }
                }
                n
            } else {
                old.assets
            }
            writeDurable(designFile(ctx, next)) { it.write(design.toJson().toByteArray()) }

            // الالتزام: من هنا وبعد، النسخة الجديدة هي المعتمدة
            val tmp = File(dir(ctx), "current.tmp")
            writeDurable(tmp) { it.write(JSONObject().put("assets", assets).put("design", next).toString().toByteArray()) }
            check(tmp.renameTo(pointerFile(ctx))) { "ما گدرت أعتمد الحفظ" }

            cleanup(ctx, keepAssets = assets, keepDesign = next)
            cached = DesignContent(photo, subject, design, next)
        }
        main.post { listeners.forEach { it.onDesignChanged() } }
    }

    private fun readPointer(ctx: Context): Pointer? = synchronized(lock) {
        migrateLegacy(ctx)
        readPointerFile(ctx)
    }

    private fun readPointerFile(ctx: Context): Pointer? = runCatching {
        val o = JSONObject(pointerFile(ctx).readText())
        Pointer(o.getLong("assets"), o.getLong("design"))
    }.getOrNull()?.takeIf { File(assetsDir(ctx, it.assets), "photo.jpg").exists() }

    /** النسخة الأولى من التطبيق كانت تحفظ الملفات مباشرة بمجلد design — ننقلها للشكل الجديد. */
    private fun migrateLegacy(ctx: Context) {
        val d = dir(ctx)
        val photo = File(d, "photo.jpg")
        if (pointerFile(ctx).exists() || !photo.exists()) return
        val assets = assetsDir(ctx, 1).apply { mkdirs() }
        photo.renameTo(File(assets, "photo.jpg"))
        File(d, "subject.png").takeIf { it.exists() }?.renameTo(File(assets, "subject.png"))
        File(d, "design.json").takeIf { it.exists() }?.renameTo(designFile(ctx, 1))
        pointerFile(ctx).writeText(JSONObject().put("assets", 1).put("design", 1).toString())
    }

    /** يكتب الملف ويتأكد إنه وصل للقرص قبل ما نعتمد عليه. */
    private fun writeDurable(target: File, block: (FileOutputStream) -> Unit) {
        try {
            FileOutputStream(target).use { out ->
                block(out)
                out.flush()
                out.fd.sync()
            }
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
    }

    private fun cleanup(ctx: Context, keepAssets: Long, keepDesign: Long) {
        dir(ctx).listFiles()?.forEach { f ->
            val stale = when {
                f.name.startsWith("assets_") -> f.name != "assets_$keepAssets"
                f.name.startsWith("design_") -> f.name != "design_$keepDesign.json"
                // ملفات النسخة الأولى من التطبيق
                f.name in setOf("photo.jpg", "subject.png", "design.json", "version") -> true
                else -> false
            }
            if (stale) f.deleteRecursively()
        }
    }
}
