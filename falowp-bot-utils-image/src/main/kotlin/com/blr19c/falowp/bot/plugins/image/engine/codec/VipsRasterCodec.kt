package com.blr19c.falowp.bot.plugins.image.engine.codec

import com.blr19c.falowp.bot.plugins.image.runtime.vips.VipsOperations
import com.blr19c.falowp.bot.plugins.image.runtime.vips.VipsPipeline
import com.blr19c.falowp.bot.plugins.image.spi.JpegEncoder
import com.blr19c.falowp.bot.plugins.image.spi.PreviewDecoder
import java.awt.image.BufferedImage
import java.lang.foreign.Arena
import java.nio.file.Path

internal class VipsRasterCodec(private val ops: VipsOperations) : PreviewDecoder, JpegEncoder {

    override fun readPreview(path: Path, size: Int, square: Boolean) = Arena.ofConfined().use { arena ->
        VipsPipeline(ops, arena).readPreview(path, size, square)
    }

    override fun writeJpeg(image: BufferedImage, output: Path, quality: Int) = Arena.ofConfined().use { arena ->
        VipsPipeline(ops, arena).writeJpeg(image, output, quality)
    }
}
