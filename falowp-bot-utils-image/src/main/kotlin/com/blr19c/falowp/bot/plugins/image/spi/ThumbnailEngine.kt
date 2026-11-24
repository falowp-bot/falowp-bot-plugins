package com.blr19c.falowp.bot.plugins.image.spi

import com.blr19c.falowp.bot.plugins.image.ImageUtils.Format

/**
 * 缩略图处理引擎
 */
internal interface ThumbnailEngine {

    fun supports(bytes: ByteArray, format: Format): Boolean = true

    fun thumbnail(bytes: ByteArray, size: Int, format: Format, quality: Int): ByteArray
}
