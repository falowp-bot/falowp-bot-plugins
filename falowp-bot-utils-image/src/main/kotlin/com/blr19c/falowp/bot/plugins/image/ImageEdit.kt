package com.blr19c.falowp.bot.plugins.image

import com.blr19c.falowp.bot.plugins.image.core.ImageEngines
import com.blr19c.falowp.bot.plugins.image.spi.EditStep
import java.awt.Color
import java.awt.Font

/**
 * 按调用顺序编辑静态图片，调用 [encode] 时统一处理和编码
 */
@Suppress("unused")
class ImageEdit internal constructor(private val bytes: ByteArray) {

    private val steps = mutableListOf<EditStep>()

    /**
     * 缩放到指定宽高
     *
     * @param width 目标宽度
     * @param height 目标高度
     * @return 当前编辑任务，可继续添加操作
     */
    fun resize(width: Int, height: Int): ImageEdit = apply {
        require(width > 0 && height > 0)
        steps += EditStep.Resize(width, height)
    }

    /**
     * 等比缩放，宽高都落在目标范围内
     *
     * @param width 最大宽度
     * @param height 最大高度
     * @return 当前编辑任务，可继续添加操作
     */
    fun fit(width: Int, height: Int): ImageEdit = apply {
        require(width > 0 && height > 0)
        steps += EditStep.Fit(width, height)
    }

    /**
     * 等比缩放后居中裁剪，填满指定尺寸
     *
     * @param width 目标宽度
     * @param height 目标高度
     * @return 当前编辑任务，可继续添加操作
     */
    fun cover(width: Int, height: Int): ImageEdit = apply {
        require(width > 0 && height > 0)
        steps += EditStep.Cover(width, height)
    }

    /**
     * 截取指定矩形
     *
     * @param x 左上角横坐标
     * @param y 左上角纵坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     * @return 当前编辑任务，可继续添加操作
     */
    fun crop(x: Int, y: Int, width: Int, height: Int): ImageEdit = apply {
        require(x >= 0 && y >= 0 && width > 0 && height > 0)
        steps += EditStep.Crop(x, y, width, height)
    }

    /**
     * 顺时针旋转，画布随图片扩展
     *
     * @param degrees 旋转角度，负数表示逆时针
     * @return 当前编辑任务，可继续添加操作
     */
    fun rotate(degrees: Double): ImageEdit = apply {
        require(degrees.isFinite())
        steps += EditStep.Rotate(degrees)
    }

    /**
     * 左右翻转
     *
     * @return 当前编辑任务，可继续添加操作
     */
    fun flipHorizontal(): ImageEdit = apply {
        steps += EditStep.FlipHorizontal
    }

    /**
     * 上下翻转
     *
     * @return 当前编辑任务，可继续添加操作
     */
    fun flipVertical(): ImageEdit = apply {
        steps += EditStep.FlipVertical
    }

    /**
     * 在四周增加透明留白
     *
     * @param left 左边距
     * @param top 上边距
     * @param right 右边距
     * @param bottom 下边距
     * @return 当前编辑任务，可继续添加操作
     */
    fun pad(left: Int, top: Int, right: Int, bottom: Int): ImageEdit = apply {
        require(left >= 0 && top >= 0 && right >= 0 && bottom >= 0)
        steps += EditStep.Pad(left, top, right, bottom)
    }

    /**
     * 转为灰度图
     *
     * @return 当前编辑任务，可继续添加操作
     */
    fun grayscale(): ImageEdit = apply {
        steps += EditStep.Grayscale
    }

    /**
     * 反转颜色，保留透明度
     *
     * @return 当前编辑任务，可继续添加操作
     */
    fun invert(): ImageEdit = apply {
        steps += EditStep.Invert
    }

    /**
     * 调整亮度和对比度
     *
     * @param brightness 亮度偏移，范围 -255 到 255
     * @param contrast 对比度倍率，1 为原始对比度
     * @return 当前编辑任务，可继续添加操作
     */
    @JvmOverloads
    fun adjust(brightness: Int = 0, contrast: Double = 1.0): ImageEdit = apply {
        require(brightness in -255..255 && contrast.isFinite() && contrast >= 0)
        steps += EditStep.Adjust(brightness, contrast)
    }

    /**
     * 按灰度阈值转为黑白图
     *
     * @param level 灰度阈值，范围 0 到 255，大于等于此值的像素变白
     * @return 当前编辑任务，可继续添加操作
     */
    fun threshold(level: Int): ImageEdit = apply {
        require(level in 0..255)
        steps += EditStep.Threshold(level)
    }

    /**
     * 高斯模糊
     *
     * @param sigma 模糊半径参数，范围 0.2 到 1000
     * @return 当前编辑任务，可继续添加操作
     */
    fun blur(sigma: Double): ImageEdit = apply {
        require(sigma in 0.2..1000.0)
        steps += EditStep.Blur(sigma)
    }

    /**
     * 增强边缘细节
     *
     * @return 当前编辑任务，可继续添加操作
     */
    fun sharpen(): ImageEdit = apply {
        steps += EditStep.Sharpen
    }

    /**
     * 用中值滤波去除噪点
     *
     * @param size 窗口边长，取 3 到 31 的奇数
     * @return 当前编辑任务，可继续添加操作
     */
    fun median(size: Int): ImageEdit = apply {
        require(size in 3..31 && size % 2 == 1)
        steps += EditStep.Median(size)
    }

    /**
     * 将画面处理为像素块
     *
     * @param blockSize 像素块的近似边长，单位为像素
     * @return 当前编辑任务，可继续添加操作
     */
    fun pixelate(blockSize: Int): ImageEdit = apply {
        require(blockSize > 0)
        steps += EditStep.Pixelate(blockSize)
    }

    /**
     * 叠加图片或透明水印，超出底图的部分裁掉
     *
     * @param image 叠加图的编码字节
     * @param x 叠加图左上角横坐标
     * @param y 叠加图左上角纵坐标
     * @return 当前编辑任务，可继续添加操作
     */
    fun overlay(image: ByteArray, x: Int, y: Int): ImageEdit = apply {
        require(image.isNotEmpty())
        steps += EditStep.Overlay(image, x, y)
    }

    /**
     * 拼接两张图片，沿上边或左边对齐
     *
     * @param image 第二张图的编码字节
     * @param vertical true 表示上下拼接，默认左右拼接
     * @return 当前编辑任务，可继续添加操作
     */
    @JvmOverloads
    fun join(image: ByteArray, vertical: Boolean = false): ImageEdit = apply {
        require(image.isNotEmpty())
        steps += EditStep.Join(image, vertical)
    }

    /**
     * 调整饱和度和色相，保留透明度
     *
     * @param saturation 饱和度倍率，0 为去色，1 为原色
     * @param hue 色相偏移角度，默认 0
     * @return 当前编辑任务
     */
    @JvmOverloads
    fun tint(saturation: Double, hue: Double = 0.0): ImageEdit = apply {
        require(saturation.isFinite() && saturation >= 0 && hue.isFinite())
        steps += EditStep.Tint(saturation, hue)
    }

    /**
     * 调整整张图片的透明度
     *
     * @param factor 不透明度倍率，范围 0 到 1
     * @return 当前编辑任务
     */
    fun opacity(factor: Double): ImageEdit = apply {
        require(factor in 0.0..1.0)
        steps += EditStep.Opacity(factor)
    }

    /**
     * 用底色填充透明区域，适合导出 JPEG 前处理透明图
     *
     * @param color 不透明的背景颜色，默认白色
     * @return 当前编辑任务
     */
    @JvmOverloads
    fun flatten(color: Color = Color.WHITE): ImageEdit = apply {
        require(color.alpha == 255)
        steps += EditStep.Flatten(color)
    }

    /**
     * 在图片上添加单行文字水印
     *
     * @param text 水印文字
     * @param x 文字左边缘的横坐标
     * @param y 文字上边缘的纵坐标
     * @param font 字体和字号，可传入应用自带的字体
     * @param color 文字颜色，支持透明度
     * @return 当前编辑任务
     */
    @JvmOverloads
    fun text(
        text: String,
        x: Int,
        y: Int,
        font: Font = Font(Font.SANS_SERIF, Font.PLAIN, 24),
        color: Color = Color.WHITE
    ): ImageEdit {
        require(text.isNotBlank() && text.none { it == '\n' || it == '\r' } && font.size2D > 0)
        return overlay(ImageEngines.core.text(text, font, color), x, y)
    }

    /**
     * 执行已添加的操作并编码，直接调用可转换图片格式
     *
     * @param format 输出格式，默认 PNG
     * @param quality JPEG、WebP 和 AVIF 的画质，范围 1 到 100
     * @return 输出图片的完整字节
     */
    @JvmOverloads
    fun encode(format: ImageUtils.Format = ImageUtils.Format.PNG, quality: Int = 90): ByteArray {
        require(quality in 1..100)
        return ImageEngines.core.process(bytes, steps, format, quality)
    }
}
