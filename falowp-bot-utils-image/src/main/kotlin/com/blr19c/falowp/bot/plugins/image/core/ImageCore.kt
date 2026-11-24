package com.blr19c.falowp.bot.plugins.image.core

import com.blr19c.falowp.bot.plugins.image.ImageUtils.Format
import com.blr19c.falowp.bot.plugins.image.spi.*
import java.awt.image.BufferedImage
import java.nio.file.Path

/**
 * 按操作选择图像引擎，只在原生库加载失败时尝试备用实现
 */
internal class ImageCore(
    private val loadProcessor: () -> ProcessingEngine,
    private val loadThumbnail: () -> ThumbnailEngine,
    private val loadInfo: () -> ImageInfoReader,
    private val loadPreview: () -> PreviewDecoder,
    private val loadJpeg: () -> JpegEncoder,
    private val fallbackThumbnail: ThumbnailEngine,
    private val fallbackPreview: PreviewDecoder,
    private val fallbackJpeg: JpegEncoder,
    private val requireNative: () -> Boolean,
    renderer: RasterRenderer,
    feature: ColorFeature,
    similarity: ColorSimilarity,
    png: PngEncoder
) : RasterRenderer by renderer, ColorFeature by feature, ColorSimilarity by similarity, PngEncoder by png, PreviewDecoder, JpegEncoder {

    private val logger = System.getLogger(ImageCore::class.java.name)
    private val preview by lazy { select(loadPreview) { fallbackPreview } }
    private val jpeg by lazy { select(loadJpeg) { fallbackJpeg } }

    fun warmUp() {
        loadProcessor()
    }

    fun thumbnail(bytes: ByteArray, size: Int, format: Format, quality: Int): ByteArray {
        require(bytes.isNotEmpty()) { "图片内容为空" }
        require(size > 0) { "边长必须大于0" }
        require(quality in 1..100) { "质量范围为1到100" }
        val engine = select(loadThumbnail) { fallbackThumbnail.takeIf { it.supports(bytes, format) } }
        return engine.thumbnail(bytes, size, format, quality)
    }

    fun process(bytes: ByteArray, steps: List<EditStep>, format: Format, quality: Int) =
        loadProcessor().process(bytes, steps, format, quality)

    fun info(bytes: ByteArray) = loadInfo().info(bytes)

    override fun readPreview(path: Path, size: Int, square: Boolean) = preview.readPreview(path, size, square)

    override fun writeJpeg(image: BufferedImage, output: Path, quality: Int) = jpeg.writeJpeg(image, output, quality)

    private fun <T : Any> select(load: () -> T, fallback: () -> T?): T {
        val cause = try {
            return load()
        } catch (e: LinkageError) {
            e
        } catch (e: Exception) {
            e
        }
        val engine = if (requireNative()) null else fallback()
        if (engine == null) throw IllegalStateException("加载原生图像引擎失败", cause)
        logger.log(System.Logger.Level.WARNING, "加载原生图像引擎失败，使用备用引擎", cause)
        return engine
    }
}
