package com.blr19c.falowp.bot.plugins.image.engine.codec

import com.blr19c.falowp.bot.plugins.image.spi.*
import com.blr19c.falowp.bot.plugins.image.engine.render.RasterRender
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Files
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

internal object ImageIoCodec : PreviewDecoder, JpegEncoder, PngEncoder {
    override fun readPreview(path: Path, size: Int, square: Boolean): Preview? {
        ImageIO.createImageInputStream(path.toFile()).use { input ->
            if (input == null) return null
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.setInput(input, true, true)
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val side = minOf(width, height)
                val param = reader.defaultReadParam
                if (square) param.sourceRegion = Rectangle((width - side) / 2, (height - side) / 2, side, side)
                val longest = if (square) side else maxOf(width, height)
                val sample = (longest / size / 2).coerceAtLeast(1)
                param.setSourceSubsampling(sample, sample, 0, 0)
                val image = reader.read(0, param)
                val preview = if (square) RasterRender.coverSquare(image, size) else RasterRender.fit(image, size)
                return Preview(preview, width, height)
            } finally {
                reader.dispose()
            }
        }
    }

    override fun writeJpeg(image: BufferedImage, output: Path, quality: Int) {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        try {
            val param = (writer.defaultWriteParam as JPEGImageWriteParam).apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality / 100f
                optimizeHuffmanTables = true
            }
            Files.newOutputStream(output).use { file ->
                ImageIO.createImageOutputStream(file).use { stream ->
                    writer.output = stream
                    writer.write(null, IIOImage(image, null, null), param)
                }
            }
        } finally {
            writer.dispose()
        }
    }

    override fun writePng(image: BufferedImage, output: Path) {
        check(ImageIO.write(image, "png", output.toFile()))
    }
}
