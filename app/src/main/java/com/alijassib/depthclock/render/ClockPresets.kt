package com.alijassib.depthclock.render

import com.alijassib.depthclock.data.ClockLayout
import com.alijassib.depthclock.data.ClockSpec
import com.alijassib.depthclock.data.MinutesTone

data class ClockPreset(val id: String, val label: String, val spec: ClockSpec)

/** أشكال جاهزة؛ اختيار وحدة يغيّر الشكل بس ويخلي مكان الساعة ولونها مثل ما هم. */
object ClockPresets {

    private fun p(id: String, label: String, spec: ClockSpec) = ClockPreset(id, label, spec)

    val all: List<ClockPreset> = listOf(
        p("ios", "آيفون", ClockSpec(fontId = "roboto_flex", weight = 620, size = 0.27f, spacing = -0.02f)),
        p("classic", "كلاسيكي", ClockSpec(fontId = "roboto_flex", weight = 420, size = 0.26f)),
        p("tall_thin", "طويلة رفيعة", ClockSpec(fontId = "roboto_flex", weight = 220, size = 0.30f, widthScale = 0.55f, heightScale = 1.45f)),
        p("tall_bold", "طويلة عريضة", ClockSpec(fontId = "roboto_flex", weight = 820, size = 0.28f, widthScale = 0.6f, heightScale = 1.5f)),
        p("stacked", "عمودية", ClockSpec(fontId = "oneui", weight = 700, size = 0.42f, layout = ClockLayout.Stacked, lineGap = -0.02f, spacing = -0.03f)),
        p("stacked_thin", "عمودية رفيعة", ClockSpec(fontId = "roboto_flex", weight = 140, size = 0.46f, layout = ClockLayout.Stacked, lineGap = 0.02f)),
        p("two_tone", "لونين", ClockSpec(fontId = "oneui", weight = 800, size = 0.27f, minutesTone = MinutesTone.Dim, showColon = false)),
        p("two_tone_stacked", "لونين عمودية", ClockSpec(fontId = "secnum", weight = 800, size = 0.44f, layout = ClockLayout.Stacked, minutesTone = MinutesTone.Dim, lineGap = -0.04f)),
        p("outline", "إطار", ClockSpec(fontId = "roboto_flex", weight = 900, size = 0.27f, outline = true)),
        p("half_outline", "نص إطار", ClockSpec(fontId = "roboto_flex", weight = 900, size = 0.27f, minutesTone = MinutesTone.Outline)),
        p("wide_black", "عريضة ثقيلة", ClockSpec(fontId = "roboto_flex", weight = 1000, size = 0.22f, widthScale = 1.45f)),
        p("samsung", "سامسونگ", ClockSpec(fontId = "secnum", weight = 300, size = 0.28f)),
        p("bebas", "Bebas", ClockSpec(fontId = "bebas", size = 0.36f, heightScale = 1.15f, spacing = 0.02f)),
        p("teko", "Teko", ClockSpec(fontId = "teko", weight = 600, size = 0.5f, layout = ClockLayout.Stacked, lineGap = -0.03f)),
        p("sport", "رياضي", ClockSpec(fontId = "tourney", weight = 800, size = 0.24f, widthScale = 1.2f)),
        p("elegant", "أنيق", ClockSpec(fontId = "kalnia", weight = 250, size = 0.27f)),
        p("fat", "فاخر", ClockSpec(fontId = "abril", size = 0.27f)),
        p("ultra_tall", "نحيفة جدًا", ClockSpec(fontId = "tulpen", size = 0.42f, heightScale = 1.35f)),
        p("pixel", "بكسل", ClockSpec(fontId = "pixel", weight = 600, size = 0.25f)),
        p("digital", "ديجيتال", ClockSpec(fontId = "digital", size = 0.25f)),
        p("stencil", "ستنسل", ClockSpec(fontId = "stick", weight = 700, size = 0.3f)),
        p("hand", "يدوي", ClockSpec(fontId = "edu", weight = 600, size = 0.3f)),
        p("chalk", "طباشير", ClockSpec(fontId = "fredericka", size = 0.27f)),
        p("prism", "منشور", ClockSpec(fontId = "prism", size = 0.28f)),
    )
}
