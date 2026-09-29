package com.seequid.app.overlay

import androidx.annotation.ColorInt

/**
 * Colours are fully opaque on purpose: translucency comes from the overlay
 * window's alpha, which is what Android checks for touch occlusion.
 */
enum class LiquidSkin(
    val displayName: String,
    @param:ColorInt val front: Int,
    @param:ColorInt val back: Int,
    val isPro: Boolean,
) {
    WATER("Water", 0xFF3D9BFF.toInt(), 0xFF00D4F0.toInt(), isPro = false),
    MATCHA("Matcha", 0xFF7CB342.toInt(), 0xFF4E7A26.toInt(), isPro = true),
    COFFEE("Cold brew", 0xFF7B5236.toInt(), 0xFF4E342E.toInt(), isPro = true),
    BOBA("Boba tea", 0xFFD4A373.toInt(), 0xFFA8652A.toInt(), isPro = true),
    LAGOON("Night lagoon", 0xFF3949AB.toInt(), 0xFF00897B.toInt(), isPro = true);

    companion object {
        fun fromName(name: String?): LiquidSkin = entries.firstOrNull { it.name == name } ?: WATER
    }
}
