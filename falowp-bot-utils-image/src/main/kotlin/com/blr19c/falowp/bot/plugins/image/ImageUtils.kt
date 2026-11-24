package com.blr19c.falowp.bot.plugins.image

import com.blr19c.falowp.bot.plugins.image.core.ImageEngines

/**
 * 图像处理
 */
@Suppress("unused")
object ImageUtils {

    /**
     * 图片信息，宽高为应用 EXIF 方向后的像素尺寸
     */
    data class Info(val width: Int, val height: Int, val hasAlpha: Boolean)

    enum class Format { JPEG, PNG, WEBP, GIF, TIFF, AVIF }

    /**
     * 提前加载 libvips，供图片编辑、预览和编码使用
     * 缓存可用时直接加载，否则先下载运行库
     *
     * @return 无返回值，方法结束时引擎已就绪
     */
    @JvmStatic
    fun warmUp() {
        ImageEngines.core.warmUp()
    }

    /**
     * 等比缩放图片，按指定格式输出
     *
     * @param bytes 原图文件的字节内容
     * @param size 缩放后最长边的长度，单位为像素，必须大于 0
     * @param format 输出格式，默认 JPEG
     * @param quality JPEG、WebP 和 AVIF 的画质，范围 1 到 100，默认 90
     * @return 缩放并编码后的图片字节，可直接写入文件
     */
    @JvmStatic
    @JvmOverloads
    fun thumbnail(
        bytes: ByteArray,
        size: Int,
        format: Format = Format.JPEG,
        quality: Int = 90
    ): ByteArray {
        return ImageEngines.core.thumbnail(bytes, size, format, quality)
    }

    /**
     * 创建编辑任务，可连续缩放、裁剪、调色或叠加水印
     *
     * @param bytes 原图文件的字节内容，编码完成前请保持内容不变
     * @return 编辑任务，调用 [ImageEdit.encode] 得到结果
     */
    @JvmStatic
    fun edit(bytes: ByteArray): ImageEdit {
        require(bytes.isNotEmpty()) { "图片内容为空" }
        return ImageEdit(bytes)
    }

    /**
     * 读取静态图或多帧图片首帧的尺寸和透明通道信息
     *
     * @param bytes 原图文件的字节内容
     * @return 图片宽度、高度及是否包含透明通道
     */
    @JvmStatic
    fun info(bytes: ByteArray): Info {
        require(bytes.isNotEmpty()) { "图片内容为空" }
        return ImageEngines.core.info(bytes)
    }

    /**
     * 创建千图成像任务，设置目标图和素材后可渲染或保存
     *
     * @return 可继续设置密度、子图尺寸和文件大小的拼图任务
     */
    @JvmStatic
    fun photoMosaic() = MosaicEdit()
}
