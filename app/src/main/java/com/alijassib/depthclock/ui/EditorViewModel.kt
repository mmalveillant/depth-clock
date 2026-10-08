package com.alijassib.depthclock.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import android.view.WindowManager
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.DepthDesign
import com.alijassib.depthclock.data.DesignStore
import com.alijassib.depthclock.image.PhotoLoader
import com.alijassib.depthclock.image.SubjectCutter
import com.alijassib.depthclock.render.LockRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Stage {
    data object Loading : Stage
    data object Empty : Stage
    data class Processing(val message: String) : Stage
    data object Editing : Stage
}

data class EditorState(
    val stage: Stage = Stage.Loading,
    val photo: Bitmap? = null,
    val subject: Bitmap? = null,
    val design: DepthDesign = DepthDesign(),
    /** ألوان مقترحة من الصورة (أولها أبيض). */
    val swatches: List<Int> = listOf(WHITE),
    /** فيه تعديلات ما انحفظت على شاشة القفل. */
    val dirty: Boolean = false,
    /** الصورة نفسها تبدّلت (لازم نكتبها للقرص بالحفظ الجاي). */
    val photoChanged: Boolean = false,
    val saving: Boolean = false,
    /** انحفظ والخلفية مو مطبّقة بعد — الواجهة تفتح شاشة «تعيين الخلفية» وتصفّره. */
    val pickerRequested: Boolean = false,
    /** رسالة قصيرة تطلع (خطأ أو تأكيد). */
    val notice: String? = null,
) {
    companion object { const val WHITE = 0xFFFFFFFF.toInt() }
}

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    private val initJob: Job = viewModelScope.launch {
        val saved = withContext(Dispatchers.IO) { runCatching { DesignStore.load(app) }.getOrNull() }
        val swatches = saved?.let { withContext(Dispatchers.Default) { swatchesOf(it.photo) } }
        _state.update {
            if (saved == null) it.copy(stage = Stage.Empty)
            else it.copy(
                stage = Stage.Editing,
                photo = saved.photo,
                subject = saved.subject,
                design = saved.design,
                swatches = swatches ?: it.swatches,
            )
        }
    }
    private var photoJob: Job? = null

    fun onPhotoPicked(uri: Uri) {
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            // صورة مشاركة من الاستوديو ممكن توصل قبل ما يخلص تحميل التصميم المحفوظ
            initJob.join()
            _state.update { it.copy(stage = Stage.Processing("دا أفتح الصورة…"), notice = null) }
            try {
                val photo = withContext(Dispatchers.IO) { PhotoLoader.load(getApplication(), uri) }
                var notice: String? = null
                val subject = try {
                    SubjectCutter(getApplication()).cut(photo) { msg ->
                        _state.update { it.copy(stage = Stage.Processing(msg)) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SubjectCutter.NoSubjectException) {
                    notice = "ما لگيت شخص واضح — الساعة راح تطلع فوگ الصورة"
                    null
                } catch (t: Throwable) {
                    Log.w(TAG, "segmentation failed", t)
                    notice = "ما گدرت أفصل الشخص (أول مرة يحتاج نت لتنزيل الموديل). جرّب مرة لخ"
                    null
                }
                val swatches = withContext(Dispatchers.Default) { swatchesOf(photo) }
                _state.update {
                    it.copy(
                        stage = Stage.Editing,
                        photo = photo,
                        subject = subject,
                        // صورة جديدة = تأطير جديد، بس نخلي ستايل الساعة الي اختاره
                        design = it.design.copy(zoom = 1f, panX = 0f, panY = 0f, depthEnabled = subject != null),
                        swatches = swatches,
                        dirty = true,
                        photoChanged = true,
                        notice = notice,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "photo failed", t)
                _state.update {
                    it.copy(stage = if (it.photo == null) Stage.Empty else Stage.Editing, notice = "ما گدرت أفتح هالصورة")
                }
            }
        }
    }

    private val renderer by lazy { LockRenderer(app) }
    private val screenAspect: Float by lazy {
        val b = app.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        minOf(b.width(), b.height()).toFloat() / maxOf(b.width(), b.height())
    }

    fun maxClockSize(spec: ClockSpec): Float = renderer.clock.maxSize(spec, screenAspect)

    /**
     * [stopAtEdge] = true للسلايدرات المستمرّة (الطول، العرض، السُمك، التباعد): توقف عند حافة الشاشة.
     * غيرها (الحجم، الأشكال، الأزرار، المفاتيح) يتنفّذ دائمًا ويصغّر الحجم إذا لازم.
     */
    fun updateDesign(transform: (DepthDesign) -> DepthDesign, stopAtEdge: Boolean = false) {
        _state.update {
            val prev = it.design
            var next = transform(prev).clamped()
            val maxNext = maxClockSize(next.clock)
            if (next.clock.size > maxNext + 1e-4f) {
                val overNext = next.clock.size / maxNext
                val overPrev = prev.clock.size / maxClockSize(prev.clock)
                if (stopAtEdge && overNext > overPrev + 1e-4f) return@update it
                next = next.copy(clock = next.clock.copy(size = maxNext))
            }
            next = next.copy(clock = keepOnScreen(next))
            if (next == prev) it else it.copy(design = next, dirty = true)
        }
    }

    /** للـ callbacks الي تاخذ تحويل واحد بس. */
    fun updateDesign(transform: (DepthDesign) -> DepthDesign) = updateDesign(transform, stopAtEdge = false)

    /** يزحزح الساعة حتى تبقى كلها (ويّا التاريخ) داخل الشاشة، وين ما انسحبت. */
    private fun keepOnScreen(d: DepthDesign): ClockSpec {
        val w = 1080f
        val h = w / screenAspect
        // أعرض وقت ممكن بهالخط (مو «10:00») حتى ولا ساعة باليوم تطلع برّا
        val r = renderer.clockBounds(w, h, d, renderer.clock.widestTime(d.clock))
        var c = d.clock
        // الرسّام يمنع الساعة تطلع من فوگ؛ نخلي القيمة المحفوظة تطابق المرسوم حتى السحب ما يصير بيه «منطقة ميتة»
        if (r.centerY() > c.centerY * h + 0.5f) c = c.copy(centerY = r.centerY() / h)
        val margin = w * 0.02f
        val dx = when {
            r.width() > w - 2 * margin -> w / 2f - r.centerX()
            r.left < margin -> margin - r.left
            r.right > w - margin -> (w - margin) - r.right
            else -> 0f
        }
        if (dx != 0f) c = c.copy(centerX = c.centerX + dx / w)
        if (r.bottom > h * 0.97f) c = c.copy(centerY = c.centerY - (r.bottom - h * 0.97f) / h)
        return c
    }

    /**
     * يحفظ التصميم؛ الخلفية الحية (إذا مطبّقة) تتحدّث فورًا.
     * [openPickerAfter]: إذا الخلفية مو على شاشة القفل، نطلب من الواجهة تفتح «تعيين الخلفية».
     */
    fun save(openPickerAfter: Boolean) {
        val s = _state.value
        val photo = s.photo ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val ctx = getApplication<Application>()
                    DesignStore.save(ctx, s.design, photo, s.subject, newPhoto = s.photoChanged || !DesignStore.hasDesign(ctx))
                }.onFailure { Log.e(TAG, "save failed", it) }.isSuccess
            }
            _state.update {
                // إذا تعدّل شي أثناء الحفظ يبقى dirty
                val samePhoto = it.photo === s.photo
                it.copy(
                    saving = false,
                    dirty = if (ok) it.design != s.design || !samePhoto else true,
                    photoChanged = if (ok) !samePhoto else it.photoChanged,
                    pickerRequested = ok && openPickerAfter,
                    notice = when {
                        !ok -> "ما انحفظ التصميم، جرّب مرة لخ"
                        !openPickerAfter -> "تحدّثت شاشة القفل"
                        else -> null
                    },
                )
            }
        }
    }

    /** يرجّع لآخر تصميم محفوظ (المطبّق على شاشة القفل) ويلغي التعديلات. */
    fun discardChanges() {
        photoJob?.cancel()
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { DesignStore.load(getApplication()) }.getOrNull() } ?: return@launch
            val swatches = withContext(Dispatchers.Default) { swatchesOf(saved.photo) }
            _state.update {
                it.copy(
                    stage = Stage.Editing, photo = saved.photo, subject = saved.subject, design = saved.design,
                    swatches = swatches, dirty = false, photoChanged = false, notice = null,
                )
            }
        }
    }

    fun onPickerOpened() = _state.update { it.copy(pickerRequested = false) }

    fun showNotice(text: String?) = _state.update { it.copy(notice = text) }

    private fun swatchesOf(photo: Bitmap): List<Int> {
        val p = Palette.from(photo).maximumColorCount(16).generate()
        val picks = listOfNotNull(
            p.lightVibrantSwatch, p.vibrantSwatch, p.lightMutedSwatch,
            p.mutedSwatch, p.darkVibrantSwatch, p.dominantSwatch,
        ).map { it.rgb or 0xFF000000.toInt() }
        return (listOf(EditorState.WHITE) + picks + listOf(0xFF111111.toInt())).distinct().take(7)
    }

    companion object { private const val TAG = "DepthEditor" }
}
