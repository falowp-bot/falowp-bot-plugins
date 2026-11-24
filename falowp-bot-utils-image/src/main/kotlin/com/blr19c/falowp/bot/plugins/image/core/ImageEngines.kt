package com.blr19c.falowp.bot.plugins.image.core

import com.blr19c.falowp.bot.plugins.image.ImageConfig
import com.blr19c.falowp.bot.plugins.image.engine.codec.ImageIoCodec
import com.blr19c.falowp.bot.plugins.image.engine.codec.JvmThumbnail
import com.blr19c.falowp.bot.plugins.image.engine.codec.VipsRasterCodec
import com.blr19c.falowp.bot.plugins.image.engine.feature.ColorFeatures
import com.blr19c.falowp.bot.plugins.image.engine.feature.RgbDistance
import com.blr19c.falowp.bot.plugins.image.engine.processing.VipsEngine
import com.blr19c.falowp.bot.plugins.image.engine.render.RasterRender
import com.blr19c.falowp.bot.plugins.image.runtime.VipsRuntime
import com.blr19c.falowp.bot.plugins.image.runtime.vips.VipsOperations

/**
 * 内置图像能力及其实现
 */
internal object ImageEngines {
    private val ops by lazy { VipsOperations(VipsRuntime.prepareLibrary()) }
    private val processor by lazy { VipsEngine(ops) }
    private val codec by lazy { VipsRasterCodec(ops) }

    val core = ImageCore(
        loadProcessor = { processor },
        loadThumbnail = { processor },
        loadInfo = { processor },
        loadPreview = { codec },
        loadJpeg = { codec },
        fallbackThumbnail = JvmThumbnail,
        fallbackPreview = ImageIoCodec,
        fallbackJpeg = ImageIoCodec,
        requireNative = { ImageConfig.requireNative },
        renderer = RasterRender,
        feature = ColorFeatures,
        similarity = RgbDistance,
        png = ImageIoCodec
    )
}
