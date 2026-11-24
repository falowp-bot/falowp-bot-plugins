package com.blr19c.falowp.bot.plugins.image.engine.codec

import com.blr19c.falowp.bot.plugins.image.ImageUtils.Format
import com.blr19c.falowp.bot.plugins.image.spi.ThumbnailEngine
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.roundToInt

internal object JvmThumbnail : ThumbnailEngine {

    /**
     * 普通8位PNG可以在本地缩放，带色彩配置或方向信息的图片交给libvips
     */
    override fun supports(bytes: ByteArray, format: Format): Boolean {
        if (format != Format.PNG) return false
        val input = ByteBuffer.wrap(bytes)
        if (input.remaining() < 8 || input.long != 0x89504e470d0a1a0auL.toLong()) return false
        var hasHeader = false
        while (input.remaining() >= 12) {
            val length = input.int
            val type = String(bytes, input.position(), 4, Charsets.US_ASCII)
            input.position(input.position() + 4)
            if (length < 0 || length > input.remaining() - 4) return false
            when (type) {
                "IHDR" -> {
                    if (hasHeader || length != 13 || input.get(input.position() + 8).toInt() != 8) return false
                    hasHeader = true
                }
                "IEND" -> return hasHeader && length == 0
                "IDAT", "PLTE", "tRNS" -> Unit
                else -> return false
            }
            input.position(input.position() + length + 4)
        }
        return false
    }

    override fun thumbnail(bytes: ByteArray, size: Int, format: Format, quality: Int): ByteArray {
        val source = ImageIO.read(bytes.inputStream())
        val scale = size.toDouble() / maxOf(source.width, source.height)
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.drawImage(source, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, "png", output)) { "缺少PNG编码器" }
        return output.toByteArray()
    }
}
