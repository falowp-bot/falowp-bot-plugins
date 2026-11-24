package com.blr19c.falowp.bot.plugins.image.engine.render

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal object TextWatermark {

    fun render(text: String, font: Font, color: Color): ByteArray {
        val context = FontRenderContext(null, true, true)
        val layout = TextLayout(text, font, context)
        val bounds = layout.getPixelBounds(context, 0f, 0f)
        val image = BufferedImage(bounds.width.coerceAtLeast(1), bounds.height.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = color
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            layout.draw(graphics, -bounds.x.toFloat(), -bounds.y.toFloat())
        } finally {
            graphics.dispose()
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }
}
