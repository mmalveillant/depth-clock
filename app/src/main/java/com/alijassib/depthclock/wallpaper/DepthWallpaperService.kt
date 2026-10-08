package com.alijassib.depthclock.wallpaper

import android.app.KeyguardManager
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.alijassib.depthclock.data.DesignContent
import com.alijassib.depthclock.data.DesignStore
import com.alijassib.depthclock.render.ClockText
import com.alijassib.depthclock.render.LockRenderer
import java.util.concurrent.Executors

/**
 * الخلفية الحية الي تنحط على شاشة القفل: ترسم الصورة، الساعة، وبعدين الشخص فوگها.
 * ما بيها أنيميشن — ترسم مرة بالدقيقة (TIME_TICK) فما تاكل بطارية.
 */
class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    private inner class DepthEngine : Engine(), DesignStore.Listener {

        private val renderer by lazy { LockRenderer(applicationContext) }
        private val main = Handler(Looper.getMainLooper())
        private val loader = Executors.newSingleThreadExecutor()
        private var content: DesignContent? = null
        private var visible = false
        private var tickRegistered = false
        private var keyguardRegistered = false
        private var destroyed = false

        /** يتحدّث الوقت كل دقيقة، ولمّا يتغيّر الوقت/المنطقة/اللغة. */
        private val tickReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = drawFrame()
        }

        /** إذا انحطّت على الشاشتين: الساعة تطلع بس والتلفون مقفول. */
        private val keyguardReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                drawFrame()
                // القفل ممكن يتأخر شوية بعد إطفاء الشاشة
                main.postDelayed(::drawFrame, 400)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            DesignStore.addListener(this)
            registerReceiver(keyguardReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            })
            keyguardRegistered = true
            reload()
        }

        override fun onDestroy() {
            destroyed = true
            DesignStore.removeListener(this)
            // النظام ممكن يهدم محرّك ما انبنى (onCreate ما انندهت) — فنتأكد قبل الإلغاء
            if (keyguardRegistered) {
                unregisterReceiver(keyguardReceiver)
                keyguardRegistered = false
            }
            setTickEnabled(false)
            main.removeCallbacksAndMessages(null)
            loader.shutdownNow()
            super.onDestroy()
        }

        override fun onDesignChanged() = reload()

        private fun reload() {
            loader.execute {
                val loaded = runCatching { DesignStore.load(applicationContext) }
                    .onFailure { Log.e(TAG, "load failed", it) }
                    .getOrNull()
                main.post {
                    if (destroyed) return@post
                    content = loaded
                    notifyColorsChanged()
                    drawFrame()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            setTickEnabled(visible)
            if (visible) drawFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) = drawFrame()

        override fun onWallpaperFlagsChanged(which: Int) = drawFrame()

        override fun onComputeColors(): WallpaperColors? {
            val photo = content?.photo ?: return null
            val small = Bitmap.createScaledBitmap(photo, 112, (112f * photo.height / photo.width).toInt().coerceAtLeast(1), true)
            return WallpaperColors.fromBitmap(small)
        }

        private fun setTickEnabled(enabled: Boolean) {
            if (enabled == tickRegistered) return
            if (enabled) {
                registerReceiver(tickReceiver, IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_LOCALE_CHANGED)
                })
            } else {
                unregisterReceiver(tickReceiver)
            }
            tickRegistered = enabled
        }

        private fun shouldDrawClock(): Boolean {
            if (isPreview) return true
            val flags = wallpaperFlags
            val onLock = flags and WallpaperManager.FLAG_LOCK != 0
            val onHome = flags and WallpaperManager.FLAG_SYSTEM != 0
            if (!onLock && !onHome) return true
            if (!onLock) return false
            if (!onHome) return true
            return getSystemService(KeyguardManager::class.java).isKeyguardLocked
        }

        private fun drawFrame() {
            if (destroyed) return
            val holder = surfaceHolder
            if (!holder.surface.isValid) return
            val canvas = runCatching { holder.lockHardwareCanvas() }.getOrNull() ?: return
            try {
                val frame = holder.surfaceFrame
                val c = content
                renderer.draw(
                    canvas = canvas,
                    w = frame.width().toFloat(),
                    h = frame.height().toFloat(),
                    photo = c?.photo,
                    subject = c?.subject,
                    d = c?.design ?: com.alijassib.depthclock.data.DepthDesign(),
                    time = ClockText.time(applicationContext),
                    date = ClockText.date(),
                    drawClock = shouldDrawClock(),
                )
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    companion object { private const val TAG = "DepthWallpaper" }
}
