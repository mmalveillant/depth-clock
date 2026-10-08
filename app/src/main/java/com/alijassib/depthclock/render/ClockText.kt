package com.alijassib.depthclock.render

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** نص الوقت والتاريخ حسب إعدادات التلفون (12/24 ساعة واللغة). */
object ClockText {

    fun time(ctx: Context, now: Date = Date()): String {
        // مثل الآيفون: بلا ص/م حتى بنظام 12 ساعة.
        // أرقام لاتينية دائمًا: خطوط الساعة الست ما بيها أرقام هندية، فبلغة عربية جان الخط ما يتغيّر.
        val pattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
        return SimpleDateFormat(pattern, Locale.US).format(now)
    }

    fun date(now: Date = Date()): String {
        val locale = Locale.getDefault()
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(now)
    }
}
