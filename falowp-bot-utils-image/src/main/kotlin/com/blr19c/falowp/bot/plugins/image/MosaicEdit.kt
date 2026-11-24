package com.blr19c.falowp.bot.plugins.image

import com.blr19c.falowp.bot.plugins.image.business.mosaic.MosaicRequest
import com.blr19c.falowp.bot.plugins.image.business.mosaic.MosaicTarget
import com.blr19c.falowp.bot.plugins.image.business.mosaic.MosaicTiles
import com.blr19c.falowp.bot.plugins.image.business.mosaic.PhotoMosaic
import com.blr19c.falowp.bot.plugins.image.core.ImageEngines
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path

/**
 * 千图成像，按目标图的长宽比排列素材
 */
@Suppress("unused")
class MosaicEdit internal constructor(private val mosaic: PhotoMosaic = PhotoMosaic(ImageEngines.core)) {

    private val request = MosaicRequest()

    /**
     * 选择目标图文件
     *
     * @param file 目标图片路径
     * @return 当前拼图任务
     */
    fun target(file: Path): MosaicEdit = apply {
        request.subject = MosaicTarget.File(file)
    }

    /**
     * 使用内存中的目标图
     *
     * @param image 要拼出的图片
     * @return 当前拼图任务
     */
    fun target(image: BufferedImage): MosaicEdit = apply {
        request.subject = MosaicTarget.Image(image)
    }

    /**
     * 追加图片文件或目录，目录只读取当前层
     *
     * @param path 素材路径
     * @return 当前拼图任务
     */
    fun addTiles(path: Path): MosaicEdit = apply {
        request.materials.add(if (Files.isDirectory(path)) MosaicTiles.Directory(path) else MosaicTiles.File(path))
    }

    /**
     * 追加一张内存图片
     *
     * @param image 素材图
     * @return 当前拼图任务
     */
    fun addTiles(image: BufferedImage): MosaicEdit = apply {
        request.materials.add(MosaicTiles.Image(image))
    }

    /**
     * 按列表顺序追加素材
     *
     * @param images 素材图列表
     * @return 当前拼图任务
     */
    fun addTiles(images: List<BufferedImage>): MosaicEdit = apply {
        images.forEach { addTiles(it) }
    }

    /**
     * 设置拼图密度
     *
     * @param count 长边的格数，范围 1 到 65500，短边按比例向上取整
     * @return 当前拼图任务
     */
    fun density(count: Int): MosaicEdit = apply {
        require(count in 1..65500) { "长边格数应在 1 到 65500 之间" }
        request.cells = count
    }

    /**
     * 设置每格的期望尺寸
     *
     * @param size 格子边长，单位为像素，范围 1 到 2048，默认 192
     * @return 当前拼图任务
     */
    fun tilePixels(size: Int): MosaicEdit = apply {
        require(size in 1..2048) { "子图边长应在 1 到 2048 之间" }
        request.pixels = size
    }

    /**
     * 设置成品的期望大小，实际文件最多为期望值的两倍
     *
     * @param mb 期望大小，默认 30，每 MB 按 100 万字节计算
     * @return 当前拼图任务
     */
    fun expectedSizeMb(mb: Int): MosaicEdit = apply {
        require(mb > 0) { "期望大小必须大于 0" }
        request.expectedBytes = mb * 1_000_000L
    }

    /**
     * 调整原图在拼图中的叠加强度
     *
     * @param value 不透明度，范围 0 到 1，设为 0 时只绘制素材
     * @return 当前拼图任务
     */
    fun targetOpacity(value: Float): MosaicEdit = apply {
        require(value in 0f..1f)
        request.opacity = value
    }

    /**
     * 将用到的素材附在 JPEG 中，优先保留原文件，空间不足时缩小包内副本
     *
     * @param enabled 是否附带素材，调用此方法时默认开启
     * @return 当前拼图任务
     */
    @JvmOverloads
    fun includeTiles(enabled: Boolean = true): MosaicEdit = apply {
        request.archiveTiles = enabled
    }

    /**
     * 按当前设置生成拼图
     *
     * @return 生成的 RGB 图片
     */
    fun render(): BufferedImage = mosaic.render(request)

    /**
     * 将拼图保存为 JPEG
     *
     * @param output 输出文件路径，扩展名为 .jpg 或 .jpeg
     * @return 保存好的文件路径
     */
    fun save(output: Path): Path = mosaic.save(request, output)
}
