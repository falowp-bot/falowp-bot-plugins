package com.blr19c.falowp.bot.plugins.image.engine.feature

import com.blr19c.falowp.bot.plugins.image.spi.ColorSimilarity
import com.blr19c.falowp.bot.plugins.image.spi.Rgb

internal object RgbDistance : ColorSimilarity {
    override fun distanceSquared(first: Rgb, second: Rgb): Int {
        val dr = first.red - second.red
        val dg = first.green - second.green
        val db = first.blue - second.blue
        return dr * dr + dg * dg + db * db
    }
}
