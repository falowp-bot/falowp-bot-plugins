package com.blr19c.falowp.bot.plugins.image.engine.feature

import com.blr19c.falowp.bot.plugins.image.spi.Rgb
import com.blr19c.falowp.bot.plugins.image.spi.ColorFeature
import java.awt.image.BufferedImage

internal object ColorFeatures : ColorFeature {

    override fun averageColor(image: BufferedImage, x: Int, y: Int, width: Int, height: Int): Rgb {
        var red = 0L
        var green = 0L
        var blue = 0L
        for (row in y until y + height) {
            for (column in x until x + width) {
                val pixel = image.getRGB(column, row)
                val alpha = pixel ushr 24
                val white = 255 * (255 - alpha)
                red += ((pixel ushr 16 and 255) * alpha + white)
                green += ((pixel ushr 8 and 255) * alpha + white)
                blue += ((pixel and 255) * alpha + white)
            }
        }
        val divisor = 255L * width * height
        return Rgb((red / divisor).toInt(), (green / divisor).toInt(), (blue / divisor).toInt())
    }
}
