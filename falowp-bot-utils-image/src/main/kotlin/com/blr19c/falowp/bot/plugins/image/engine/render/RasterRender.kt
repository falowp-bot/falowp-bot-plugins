package com.blr19c.falowp.bot.plugins.image.engine.render

import com.blr19c.falowp.bot.plugins.image.spi.RasterCanvas
import com.blr19c.falowp.bot.plugins.image.spi.RasterRenderer
import com.blr19c.falowp.bot.plugins.image.spi.Rgb
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.roundToInt

internal object RasterRender : RasterRenderer {

    override fun text(text: String, font: Font, color: Color) = TextWatermark.render(text, font, color)

    override fun canvas(image: BufferedImage): RasterCanvas = object : RasterCanvas {
        private val graphics = image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        }

        override fun draw(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, opacity: Float) {
            if (opacity != 1f) graphics.composite = AlphaComposite.SrcOver.derive(opacity)
            graphics.drawImage(image, x, y, width, height, null)
            if (opacity != 1f) graphics.composite = AlphaComposite.SrcOver
        }

        override fun close() = graphics.dispose()
    }

    override fun coverSquare(source: BufferedImage, size: Int): BufferedImage {
        val side = minOf(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        val tile = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val g = tile.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, size, size)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.drawImage(source, 0, 0, size, size, x, y, x + side, y + side, null)
        } finally {
            g.dispose()
        }
        return tile
    }

    //平移每个通道的颜色，保留头像本身的明暗起伏
    override fun tint(source: BufferedImage, dest: BufferedImage, mean: Rgb, target: Rgb) {
        fun channel(value: Int, offset: Int): Int {
            return (value + offset).coerceIn(value / 3, 255 - (255 - value) / 3)
        }

        val red = target.red - mean.red
        val green = target.green - mean.green
        val blue = target.blue - mean.blue
        val input = (source.raster.dataBuffer as DataBufferInt).data
        val output = (dest.raster.dataBuffer as DataBufferInt).data
        for (i in input.indices) {
            val rgb = input[i]
            output[i] = (channel(rgb ushr 16 and 255, red) shl 16) or
                    (channel(rgb ushr 8 and 255, green) shl 8) or channel(rgb and 255, blue)
        }
    }

    override fun fit(image: BufferedImage, maxSide: Int): BufferedImage {
        val scale = maxSide.toDouble() / maxOf(image.width, image.height)
        if (scale >= 1) return image
        val w = (image.width * scale).roundToInt().coerceAtLeast(1)
        val h = (image.height * scale).roundToInt().coerceAtLeast(1)
        val copy = BufferedImage(
            w,
            h,
            if (image.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )
        val graphics = copy.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.drawImage(image, 0, 0, w, h, null)
        } finally {
            graphics.dispose()
        }
        return copy
    }
}
