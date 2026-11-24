package com.blr19c.falowp.bot.plugins.image.spi

internal data class Rgb(val red: Int, val green: Int, val blue: Int)

internal fun interface ColorSimilarity {
    fun distanceSquared(first: Rgb, second: Rgb): Int
}
