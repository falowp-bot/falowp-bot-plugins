package com.blr19c.falowp.bot.plugins.image.spi

import java.awt.image.BufferedImage

internal fun interface ColorFeature {
    fun averageColor(image: BufferedImage, x: Int, y: Int, width: Int, height: Int): Rgb
}
