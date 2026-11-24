package com.blr19c.falowp.bot.plugins.image.engine.processing

import com.blr19c.falowp.bot.plugins.image.ImageUtils
import com.blr19c.falowp.bot.plugins.image.runtime.vips.VipsOperations
import com.blr19c.falowp.bot.plugins.image.runtime.vips.VipsPipeline
import com.blr19c.falowp.bot.plugins.image.spi.EditStep
import com.blr19c.falowp.bot.plugins.image.spi.ImageInfoReader
import com.blr19c.falowp.bot.plugins.image.spi.ProcessingEngine
import com.blr19c.falowp.bot.plugins.image.spi.ThumbnailEngine
import java.lang.foreign.Arena

/**
 * libvips图像处理
 */
internal class VipsEngine(private val ops: VipsOperations) : ProcessingEngine, ThumbnailEngine, ImageInfoReader {

    override fun thumbnail(bytes: ByteArray, size: Int, format: ImageUtils.Format, quality: Int): ByteArray =
        Arena.ofConfined().use { arena ->
            val task = VipsPipeline(ops, arena)
            task.encode(task.thumbnail(bytes, size), format, quality)
        }

    override fun process(bytes: ByteArray, steps: List<EditStep>, format: ImageUtils.Format, quality: Int): ByteArray =
        Arena.ofConfined().use { arena ->
            val task = VipsPipeline(ops, arena)
            val image = steps.fold(task.open(bytes), task::apply)
            task.encode(image, format, quality)
        }

    override fun info(bytes: ByteArray): ImageUtils.Info = Arena.ofConfined().use { arena ->
        val task = VipsPipeline(ops, arena)
        task.info(task.open(bytes))
    }
}
