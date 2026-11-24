package com.blr19c.falowp.bot.plugins.image.spi

import java.awt.image.BufferedImage
import java.nio.file.Path

internal class Preview(val image: BufferedImage, val width: Int, val height: Int)

internal interface PreviewDecoder {
    fun readPreview(path: Path, size: Int, square: Boolean = false): Preview?
}

internal fun interface PngEncoder {
    fun writePng(image: BufferedImage, output: Path)
}

internal fun interface JpegEncoder {
    fun writeJpeg(image: BufferedImage, output: Path, quality: Int)
}
