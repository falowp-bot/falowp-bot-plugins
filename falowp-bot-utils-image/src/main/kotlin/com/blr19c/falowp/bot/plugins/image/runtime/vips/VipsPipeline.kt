package com.blr19c.falowp.bot.plugins.image.runtime.vips

import com.blr19c.falowp.bot.plugins.image.ImageUtils
import com.blr19c.falowp.bot.plugins.image.spi.EditStep
import com.blr19c.falowp.bot.plugins.image.spi.Preview
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.file.Path
import kotlin.math.roundToInt

/**
 * 单次编辑任务，所有中间图像随arena一起释放
 */
internal class VipsPipeline(private val ops: VipsOperations, private val arena: Arena) {

    private val out = arena.allocate(ADDRESS)
    private val end = MemorySegment.NULL

    private fun buffer(bytes: ByteArray) = arena.allocate(bytes.size.toLong()).copyFrom(MemorySegment.ofArray(bytes))

    private fun own(image: MemorySegment): MemorySegment {
        if (image == end) ops.checkResult(-1)
        return image.reinterpret(0, arena) { ops.unref.invoke(it) }
    }

    private fun result(status: Int): MemorySegment {
        ops.checkResult(status)
        return own(out.get(ADDRESS, 0))
    }

    private fun unary(operation: MethodHandle, image: MemorySegment, vararg args: Any): MemorySegment {
        return result(operation.invokeWithArguments(image, out, *args, end) as Int)
    }

    fun open(bytes: ByteArray): MemorySegment {
        val image = own(ops.open.invoke(buffer(bytes), bytes.size.toLong(), arena.allocateFrom(""), end) as MemorySegment)
        return unary(ops.autorot, image)
    }

    fun thumbnail(bytes: ByteArray, size: Int): MemorySegment =
        result(ops.thumbnail.invoke(buffer(bytes), bytes.size.toLong(), out, size, end) as Int)

    fun readPreview(path: Path, size: Int, square: Boolean): Preview? {
        val filename = arena.allocateFrom(path.toAbsolutePath().toString())
        if ((ops.findLoader.invoke(filename) as MemorySegment) == end) {
            ops.clearError.invoke()
            return null
        }
        val header = own(ops.openFile.invoke(filename, end) as MemorySegment)
        val rotated = (ops.orientation.invoke(header) as Int) in 5..8
        val w = if (rotated) height(header) else width(header)
        val h = if (rotated) width(header) else height(header)
        val edge = if (square) size else minOf(size, maxOf(w, h))
        val preview = result(ops.preview.invoke(filename, out, edge, arena.allocateFrom("height"), edge,
            arena.allocateFrom("crop"), if (square) 1 else 0, end) as Int)
        val rgb = if (square) apply(preview, EditStep.Flatten(Color.WHITE)) else unary(ops.colourspace, preview, SRGB)
        val alpha = hasAlpha(rgb)
        val bands = if (alpha) 4 else 3
        val length = arena.allocate(JAVA_LONG)
        val data = ops.pixels.invoke(unary(ops.cast, rgb, UCHAR), length) as MemorySegment
        if (data == end) ops.checkResult(-1)
        try {
            val memory = data.reinterpret(length.get(JAVA_LONG, 0))
            val image = BufferedImage(width(rgb), height(rgb), if (alpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
            val dest = (image.raster.dataBuffer as DataBufferInt).data
            check(memory.byteSize() == dest.size.toLong() * bands)
            val row = ByteArray(image.width * bands)
            for (y in 0 until image.height) {
                MemorySegment.copy(memory, JAVA_BYTE, y.toLong() * row.size, row, 0, row.size)
                for (x in 0 until image.width) {
                    val i = x * bands
                    val a = if (alpha) row[i + 3].toInt() else -1
                    dest[y * image.width + x] = (a shl 24) or ((row[i].toInt() and 255) shl 16) or
                            ((row[i + 1].toInt() and 255) shl 8) or (row[i + 2].toInt() and 255)
                }
            }
            return Preview(image, w, h)
        } finally {
            ops.free.invoke(data)
        }
    }

    fun writeJpeg(image: BufferedImage, path: Path, quality: Int) {
        val row = IntArray(image.width)
        val rgb = ByteArray(image.width * 3)
        val memory = arena.allocate(rgb.size.toLong() * image.height)
        for (y in 0 until image.height) {
            image.getRGB(0, y, image.width, 1, row, 0, image.width)
            for (x in row.indices) {
                rgb[3 * x] = (row[x] ushr 16).toByte()
                rgb[3 * x + 1] = (row[x] ushr 8).toByte()
                rgb[3 * x + 2] = row[x].toByte()
            }
            MemorySegment.copy(rgb, 0, memory, JAVA_BYTE, y.toLong() * rgb.size, rgb.size)
        }
        val input = own(ops.fromPixels.invoke(memory, memory.byteSize(), image.width, image.height, 3, UCHAR) as MemorySegment)
        ops.checkResult(ops.jpegSave.invoke(input, arena.allocateFrom(path.toAbsolutePath().toString()),
            arena.allocateFrom("Q"), quality, end) as Int)
    }

    private fun width(image: MemorySegment) = ops.width.invoke(image) as Int
    private fun height(image: MemorySegment) = ops.height.invoke(image) as Int
    private fun hasAlpha(image: MemorySegment) = (ops.hasAlpha.invoke(image) as Int) != 0

    fun info(image: MemorySegment) = ImageUtils.Info(width(image), height(image), hasAlpha(image))

    private fun resize(image: MemorySegment, x: Double, y: Double, kernel: Int = LANCZOS3) =
        unary(ops.resize, image, x, arena.allocateFrom("vscale"), y, arena.allocateFrom("kernel"), kernel)

    private fun withAlpha(image: MemorySegment) = if (hasAlpha(image)) image else unary(ops.addAlpha, image)

    //调色只作用于颜色通道，避免透明区域变成实色
    private fun colors(image: MemorySegment, transform: (MemorySegment) -> MemorySegment): MemorySegment {
        val rgb = unary(ops.colourspace, image, SRGB)
        if (!hasAlpha(rgb)) return transform(rgb)
        val n = arena.allocateFrom("n")
        val color = unary(ops.extract, rgb, 0, n, 3)
        val alpha = unary(ops.extract, rgb, 3, n, 1)
        return result(ops.bandjoin.invoke(transform(color), alpha, out, end) as Int)
    }

    fun apply(image: MemorySegment, step: EditStep): MemorySegment = when (step) {
        is EditStep.Resize -> resize(image, step.width.toDouble() / width(image), step.height.toDouble() / height(image))
        is EditStep.Fit -> {
            val scale = minOf(step.width.toDouble() / width(image), step.height.toDouble() / height(image))
            resize(image, scale, scale)
        }
        is EditStep.Cover -> {
            val scale = maxOf(step.width.toDouble() / width(image), step.height.toDouble() / height(image))
            val scaled = resize(image, scale, scale)
            unary(ops.crop, scaled, (width(scaled) - step.width) / 2, (height(scaled) - step.height) / 2, step.width, step.height)
        }
        is EditStep.Crop -> {
            require(step.width <= width(image) - step.x && step.height <= height(image) - step.y) { "裁剪范围超出图片" }
            unary(ops.crop, image, step.x, step.y, step.width, step.height)
        }
        is EditStep.Rotate -> {
            val angle = (step.degrees % 360 + 360) % 360
            if (angle % 90 == 0.0) unary(ops.rot, image, (angle / 90).toInt())
            else unary(ops.rotate, withAlpha(image), angle)
        }
        EditStep.FlipHorizontal -> unary(ops.flip, image, 0)
        EditStep.FlipVertical -> unary(ops.flip, image, 1)
        is EditStep.Pad -> unary(ops.pad, withAlpha(image), step.left, step.top,
            Math.addExact(width(image), Math.addExact(step.left, step.right)),
            Math.addExact(height(image), Math.addExact(step.top, step.bottom)))
        EditStep.Grayscale -> unary(ops.colourspace, image, GRAY)
        EditStep.Invert -> colors(image) { unary(ops.invert, it) }
        is EditStep.Adjust -> colors(image) {
            val adjusted = unary(ops.linear, it, step.contrast, step.brightness + 128 * (1 - step.contrast))
            unary(ops.cast, adjusted, UCHAR)
        }
        is EditStep.Threshold -> colors(image) {
            unary(ops.threshold, unary(ops.colourspace, it, GRAY), GREATER_OR_EQUAL, step.level.toDouble())
        }
        is EditStep.Flatten -> {
            val rgb = unary(ops.colourspace, image, SRGB)
            if (hasAlpha(rgb)) {
                val color = step.color
                val values = arena.allocateFrom(JAVA_DOUBLE, color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
                val background = ops.arrayDouble.invoke(values, 3) as MemorySegment
                try {
                    unary(ops.flatten, rgb, arena.allocateFrom("background"), background)
                } finally {
                    ops.areaUnref.invoke(background)
                }
            } else rgb
        }
        is EditStep.Tint -> colors(image) {
            val lch = unary(ops.colourspace, it, LCH)
            val factors = arena.allocateFrom(JAVA_DOUBLE, 1.0, step.saturation, 1.0)
            val offsets = arena.allocateFrom(JAVA_DOUBLE, 0.0, 0.0, step.hue % 360)
            unary(ops.colourspace, unary(ops.linearBands, lch, factors, offsets, 3), SRGB)
        }
        is EditStep.Opacity -> {
            val rgba = withAlpha(unary(ops.colourspace, image, SRGB))
            val factors = arena.allocateFrom(JAVA_DOUBLE, 1.0, 1.0, 1.0, step.factor)
            val offsets = arena.allocateFrom(JAVA_DOUBLE, 0.0, 0.0, 0.0, 0.0)
            unary(ops.cast, unary(ops.linearBands, rgba, factors, offsets, 4), UCHAR)
        }
        is EditStep.Blur -> unary(ops.blur, image, step.sigma)
        EditStep.Sharpen -> unary(ops.sharpen, image)
        is EditStep.Median -> unary(ops.median, image, step.size, step.size, step.size * step.size / 2)
        is EditStep.Pixelate -> {
            val w = width(image)
            val h = height(image)
            val cols = (w.toDouble() / step.blockSize).roundToInt().coerceAtLeast(1)
            val rows = (h.toDouble() / step.blockSize).roundToInt().coerceAtLeast(1)
            val small = resize(image, cols.toDouble() / w, rows.toDouble() / h)
            resize(small, w.toDouble() / cols, h.toDouble() / rows, NEAREST)
        }
        is EditStep.Overlay -> {
            val layer = open(step.image)
            result(ops.overlay.invoke(image, layer, out, OVER, arena.allocateFrom("x"), step.x,
                arena.allocateFrom("y"), step.y, end) as Int)
        }
        is EditStep.Join -> {
            val first = withAlpha(unary(ops.colourspace, image, SRGB))
            val second = withAlpha(unary(ops.colourspace, open(step.image), SRGB))
            result(ops.join.invoke(first, second, out, if (step.vertical) 1 else 0, arena.allocateFrom("expand"), 1, end) as Int)
        }
    }

    fun encode(image: MemorySegment, format: ImageUtils.Format, quality: Int): ByteArray {
        val suffix = when (format) {
            ImageUtils.Format.JPEG -> ".jpg[Q=$quality]"
            ImageUtils.Format.WEBP -> ".webp[Q=$quality]"
            ImageUtils.Format.PNG -> ".png"
            ImageUtils.Format.GIF -> ".gif"
            ImageUtils.Format.TIFF -> ".tif[compression=deflate]"
            ImageUtils.Format.AVIF -> ".avif[Q=$quality]"
        }
        val length = arena.allocate(JAVA_LONG)
        ops.checkResult(ops.encode.invoke(image, arena.allocateFrom(suffix), out, length, end) as Int)
        val bytes = out.get(ADDRESS, 0)
        return try {
            bytes.reinterpret(length.get(JAVA_LONG, 0)).toArray(JAVA_BYTE)
        } finally {
            ops.free.invoke(bytes)
        }
    }

    private companion object {
        const val SRGB = 22
        const val LCH = 19
        const val GRAY = 1
        const val UCHAR = 0
        const val GREATER_OR_EQUAL = 5
        const val OVER = 2
        const val NEAREST = 0
        const val LANCZOS3 = 5
    }
}
