package com.blr19c.falowp.bot.plugins.image.spi

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage

internal interface RasterRenderer {
    fun coverSquare(source: BufferedImage, size: Int): BufferedImage
    fun fit(image: BufferedImage, maxSide: Int): BufferedImage
    fun tint(source: BufferedImage, dest: BufferedImage, mean: Rgb, target: Rgb)
    fun text(text: String, font: Font, color: Color): ByteArray
    fun canvas(image: BufferedImage): RasterCanvas
}

internal interface RasterCanvas : AutoCloseable {
    fun draw(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, opacity: Float = 1f)
}
