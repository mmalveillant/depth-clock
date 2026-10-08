package com.alijassib.depthclock.ui

import android.content.Intent
import android.provider.MediaStore

/** من وين نختار الصورة. */
object PhotoSources {
    private const val SAMSUNG_GALLERY = "com.sec.android.gallery3d"

    /** «المعرض» مال سامسونگ مباشرة (بلا قائمة اختيار). */
    fun samsungGallery(): Intent =
        Intent(Intent.ACTION_PICK)
            .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            .setPackage(SAMSUNG_GALLERY)

    /** لأي تلفون ثاني: أي تطبيق صور يرد. */
    fun anyGallery(): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),
            "اختار صورة",
        )
}
