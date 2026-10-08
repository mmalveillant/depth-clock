package com.alijassib.depthclock.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.alijassib.depthclock.wallpaper.DepthWallpaperService

/** وين خلفيتنا مطبّقة هسه. */
enum class Placement {
    None, Lock, Home, Both;

    val onLock: Boolean get() = this == Lock || this == Both
}

object LockWallpaper {

    fun component(ctx: Context) = ComponentName(ctx, DepthWallpaperService::class.java)

    fun placement(ctx: Context): Placement {
        val wm = WallpaperManager.getInstance(ctx)
        val ours = component(ctx)
        val home = wm.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM)?.component == ours
        val lockInfo = wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK)
        // إذا ماكو خلفية قفل منفصلة، القفل يعرض خلفية الرئيسية
        val lock = if (lockInfo != null) lockInfo.component == ours else home && wm.getWallpaperId(WallpaperManager.FLAG_LOCK) < 0
        return when {
            lock && home -> Placement.Both
            lock -> Placement.Lock
            home -> Placement.Home
            else -> Placement.None
        }
    }

    /** يفتح شاشة «تعيين الخلفية» مال النظام؛ المستخدم يختار «شاشة القفل». */
    fun pickerIntent(ctx: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(ctx))
}
