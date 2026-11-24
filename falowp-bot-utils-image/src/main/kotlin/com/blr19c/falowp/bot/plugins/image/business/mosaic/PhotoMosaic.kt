package com.blr19c.falowp.bot.plugins.image.business.mosaic

import com.blr19c.falowp.bot.plugins.image.core.ImageCore
import com.blr19c.falowp.bot.plugins.image.spi.Preview
import com.blr19c.falowp.bot.plugins.image.spi.Rgb
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path

/**
 * 千图成像
 */
internal class PhotoMosaic(private val core: ImageCore) {

    private class Tile(val color: Rgb, val original: MosaicOriginal)

    fun render(edit: MosaicRequest): BufferedImage = prepare(edit, null).let { it.render(it.startPixels).image }

    fun save(edit: MosaicRequest, output: Path): Path {
        val extension = output.fileName.toString().substringAfterLast('.').lowercase()
        require(extension in setOf("jpg", "jpeg")) { "请选择 .jpg 或 .jpeg 输出文件" }
        val target = edit.subject
        require(target !is MosaicTarget.File || !Files.exists(output) || !Files.isSameFile(target.path, output)) {
            "输出文件不能覆盖目标原图"
        }
        val job = prepare(edit, output)
        return writeMosaic(core, output, job.startPixels, edit.expectedBytes, job::render)
    }

    private fun prepare(edit: MosaicRequest, output: Path?): Grid {
        val target = when (val input = requireNotNull(edit.subject) { "请选择目标图" }) {
            is MosaicTarget.Image -> Preview(input.image, input.image.width, input.image.height)
            is MosaicTarget.File -> requireNotNull(core.readPreview(input.path, maxOf(1400, edit.cells ?: 0))) {
                "无法读取目标图: ${input.path}"
            }
        }
        val palette = mutableListOf<Tile>()
        val seenFiles = mutableSetOf<Path>()
        val seenImages = mutableSetOf<BufferedImage>()
        val previousOutput = output?.takeIf { Files.exists(it) }

        fun addFile(path: Path, required: Boolean) {
            if (previousOutput != null && Files.isSameFile(path, previousOutput)) {
                require(!required) { "输出文件不能覆盖素材: $path" }
                return
            }
            val realPath = path.toRealPath()
            if (realPath in seenFiles) return
            val preview = core.readPreview(path, 32, square = true)?.image
            if (preview == null) {
                require(!required) { "无法读取素材图: $path" }
                return
            }
            seenFiles.add(realPath)
            palette.add(Tile(core.averageColor(preview, 0, 0, 32, 32), MosaicOriginal.File(path)))
        }

        for (source in edit.materials) {
            when (source) {
                is MosaicTiles.File -> addFile(source.path, required = true)
                is MosaicTiles.Directory -> files(source.path).forEach { addFile(it, required = false) }
                is MosaicTiles.Image -> {
                    val image = source.image
                    if (seenImages.add(image)) {
                        val color = core.averageColor(image, 0, 0, image.width, image.height)
                        palette.add(Tile(color, MosaicOriginal.Image("${seenImages.size}.png", image)))
                    }
                }
            }
        }
        require(palette.isNotEmpty()) { "没有可读取的素材图" }
        val count = edit.cells ?: minOf(maxOf(target.width, target.height), (8400 / edit.pixels).coerceAtLeast(1))
        val archive = output != null && edit.archiveTiles
        return Grid(target, palette, count, edit.pixels, edit.opacity, archive)
    }

    private inner class Grid(
        private val target: Preview,
        private val palette: List<Tile>,
        count: Int,
        pixels: Int,
        private val opacity: Float,
        private val archive: Boolean
    ) {

        private val longest = maxOf(target.width, target.height).toLong()
        private val cols = Math.ceilDiv(target.width.toLong() * count, longest).toInt()
        private val rows = Math.ceilDiv(target.height.toLong() * count, longest).toInt()
        private val cellCount = cols.toLong() * rows

        // 大画布先限制单格像素，始终保留用户指定的格数
        private val pixelBudget = minOf(100_000_000L, Runtime.getRuntime().maxMemory() / 12)
        val startPixels: Int

        init {
            require(cellCount <= pixelBudget) { "当前内存不足以为每个格子分配一个像素" }
            startPixels = minOf(pixels, 65500 / count, kotlin.math.sqrt(pixelBudget.toDouble() / cellCount).toInt())
        }

        fun render(size: Int): MosaicFrame {
            fun dimension(length: Int, cells: Int) =
                maxOf(cells, ((length.toLong() * maxOf(rows, cols) * size + longest / 2) / longest).toInt())
            val w = dimension(target.width, cols)
            val h = dimension(target.height, rows)
            val result = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val used = linkedMapOf<Int, MosaicOriginal>()
            val previous = IntArray(cols) { -1 }
            val random = java.util.Random(0)
            val tinted = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
            val cache = object : LinkedHashMap<Int, BufferedImage>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, BufferedImage>) =
                    this.size.toLong() * tinted.width * tinted.height * 4 > 32L * 1024 * 1024
            }
            core.canvas(result).use { canvas ->
                for (row in 0 until rows) {
                    val top = (row.toLong() * h / rows).toInt()
                    val bottom = ((row + 1L) * h / rows).toInt()
                    val sy = (row.toLong() * target.image.height / rows).toInt()
                    val ey = ((row + 1L) * target.image.height / rows).toInt()
                    var left = -1
                    for (col in 0 until cols) {
                        val sx = (col.toLong() * target.image.width / cols).toInt()
                        val ex = ((col + 1L) * target.image.width / cols).toInt()
                        val color = core.averageColor(target.image, sx, sy, maxOf(1, ex - sx), maxOf(1, ey - sy))
                        val i = palette.indices.minBy { index ->
                            val distance = core.distanceSquared(palette[index].color, color)
                            distance + repeatPenalty(index, left, previous[col]) + random.nextInt(12000)
                        }
                        val tile = palette[i]
                        val original = tile.original
                        val small = cache.getOrPut(i) {
                            when (original) {
                                is MosaicOriginal.Image -> core.coverSquare(original.image, size)
                                is MosaicOriginal.File -> requireNotNull(core.readPreview(original.path, size, square = true)?.image)
                            }
                        }
                        if (archive) {
                            used[i] = original
                        }
                        core.tint(small, tinted, tile.color, color)
                        val x = (col.toLong() * w / cols).toInt()
                        val end = ((col + 1L) * w / cols).toInt()
                        canvas.draw(tinted, x, top, end - x, bottom - top)
                        left = i
                        previous[col] = i
                    }
                }
                if (opacity > 0f) {
                    canvas.draw(target.image, 0, 0, w, h, opacity)
                }
            }
            return MosaicFrame(result, used.values.toList())
        }
    }

    private fun files(directory: Path): List<Path> {
        return Files.list(directory).use { files ->
            files.filter { Files.isRegularFile(it) }
                .sorted().toList()
        }
    }

    private fun repeatPenalty(index: Int, left: Int, above: Int): Int {
        val horizontal = if (index == left) 18000 else 0
        val vertical = if (index == above) 18000 else 0
        return horizontal + vertical
    }
}
