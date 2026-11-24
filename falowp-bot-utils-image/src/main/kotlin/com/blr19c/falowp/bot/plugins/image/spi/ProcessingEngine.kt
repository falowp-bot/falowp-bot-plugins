package com.blr19c.falowp.bot.plugins.image.spi

import com.blr19c.falowp.bot.plugins.image.ImageUtils

internal fun interface ProcessingEngine {

    fun process(bytes: ByteArray, steps: List<EditStep>, format: ImageUtils.Format, quality: Int): ByteArray
}

internal fun interface ImageInfoReader {
    fun info(bytes: ByteArray): ImageUtils.Info
}
