package com.blr19c.falowp.bot.plugins.image.business.mosaic

import com.blr19c.falowp.bot.plugins.image.core.ImageCore
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry
import kotlin.math.abs
import kotlin.math.sqrt
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile

internal sealed interface MosaicOriginal {
    class File(val path: Path) : MosaicOriginal
    class Image(val name: String, val image: BufferedImage) : MosaicOriginal
}

internal class MosaicFrame(val image: BufferedImage, val originals: List<MosaicOriginal> = emptyList())

internal fun writeMosaic(
    core: ImageCore,
    output: Path,
    tilePixels: Int,
    expectedBytes: Long,
    render: (Int) -> MosaicFrame
): Path {
    val dir = Files.createTempDirectory(Files.createDirectories(output.toAbsolutePath().parent), ".falowp-mosaic-")
    val jpeg = dir.resolve("mosaic.jpg")
    val assets = dir.resolve("tiles.zip")
    val packed = dir.resolve("packed.jpg")
    val best = dir.resolve("best.jpg")
    val limit = Math.multiplyExact(expectedBytes, 2)
    try {
        var pixels = tilePixels
        var frame = render(pixels)
        val originals = frame.originals
        var assetPixels: Int? = null
        while (true) {
            if (originals.isNotEmpty()) writeOriginals(core, originals, assets, assetPixels)
            val archiveBytes = if (originals.isEmpty()) 0L else Files.size(assets)
            var bestDistance = Long.MAX_VALUE
            if (archiveBytes < limit) {
                while (true) {
                    fun measure(quality: Int): Long {
                        core.writeJpeg(frame.image, jpeg, quality)
                        val estimate = Files.size(jpeg) + archiveBytes
                        if (estimate > limit) return estimate
                        val candidate = if (originals.isEmpty()) jpeg else {
                            pack(jpeg, assets, packed)
                            packed
                        }
                        val size = Files.size(candidate)
                        val distance = abs(size - expectedBytes)
                        if (size <= limit && distance < bestDistance) {
                            Files.move(candidate, best, REPLACE_EXISTING)
                            bestDistance = distance
                        }
                        return size
                    }

                    //素材本身已超过期望时，在两倍上限内优先保留拼图细节
                    val goal = if (archiveBytes >= expectedBytes) limit else expectedBytes
                    if (measure(96) <= goal) break
                    val smallest = measure(50)
                    if (smallest <= goal) {
                        var low = 51
                        var high = 95
                        while (low <= high) {
                            val middle = (low + high) / 2
                            if (measure(middle) < expectedBytes) low = middle + 1 else high = middle - 1
                        }
                        break
                    }
                    if (pixels == 1) break
                    val ratio = sqrt((goal - archiveBytes).toDouble() / (smallest - archiveBytes))
                    pixels = (pixels * ratio * 0.98).toInt().coerceIn(1, pixels - 1)
                    frame = render(pixels)
                }
                if (bestDistance != Long.MAX_VALUE) return Files.move(best, output, ATOMIC_MOVE, REPLACE_EXISTING)
            }

            //先试完整素材，再缩小包内副本。格数和入选素材始终保持一致
            check(originals.isNotEmpty() && assetPixels != 1) {
                "每格和素材均已降到一个像素，JPEG 与 ZIP 的最小体积仍超过期望值的两倍"
            }
            val previous = assetPixels
            assetPixels = if (previous == null) {
                val perTile = expectedBytes.toDouble() / originals.size
                (sqrt(perTile) * 0.85).toInt().coerceIn(1, 2048)
            } else {
                val ratio = sqrt(expectedBytes.toDouble() / archiveBytes.coerceAtLeast(1))
                (previous * ratio * 0.8).toInt().coerceIn(1, previous - 1)
            }
            if (pixels != tilePixels) {
                pixels = tilePixels
                frame = render(pixels)
            }
        }
    } finally {
        for (path in listOf(best, packed, assets, jpeg, dir)) Files.deleteIfExists(path)
    }
}

private fun writeOriginals(core: ImageCore, originals: List<MosaicOriginal>, path: Path, maxSide: Int?) {
    ZipArchiveOutputStream(path.toFile()).use { zip ->
        val names = mutableSetOf<String>()
        for (source in originals) {
            val originalName = when (source) {
                is MosaicOriginal.File -> source.path.fileName.toString()
                is MosaicOriginal.Image -> source.name
            }
            val resized = maxSide?.let {
                when (source) {
                    is MosaicOriginal.Image -> core.fit(source.image, it)
                    is MosaicOriginal.File -> requireNotNull(core.readPreview(source.path, it)).image
                }
            }
            val name = (if (resized == null) originalName else originalName.substringBeforeLast('.') + ".png")
                .replace("%", "%25").replace("\\", "%5C").replace(":", "%3A")
            var entryName = name
            var suffix = 2
            while (!names.add(entryName.lowercase())) {
                val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
                entryName = "${name.take(dot)}-${suffix++}${name.drop(dot)}"
            }
            val raw = path.resolveSibling("entry.raw")
            try {
                when {
                    resized != null -> core.writePng(resized, raw)
                    source is MosaicOriginal.File -> Files.copy(source.path, raw)
                    source is MosaicOriginal.Image -> core.writePng(source.image, raw)
                }
                writeSmallestEntry(zip, entryName, raw)
            } finally {
                Files.deleteIfExists(raw)
            }
        }
    }
}

private fun writeSmallestEntry(zip: ZipArchiveOutputStream, name: String, raw: Path) {
    val compressed = raw.resolveSibling("entry.deflate")
    val checksum = CRC32()
    try {
        Deflater(Deflater.BEST_COMPRESSION, true).use { deflater ->
            Files.newOutputStream(compressed).use { output ->
                DeflaterOutputStream(output, deflater, 65536).use { stream ->
                    CheckedInputStream(Files.newInputStream(raw), checksum).use { it.copyTo(stream, 65536) }
                }
            }
        }
        val entry = ZipArchiveEntry(name)
        entry.size = Files.size(raw)
        entry.crc = checksum.value
        val compressedSize = Files.size(compressed)
        val useDeflate = compressedSize < entry.size
        entry.method = if (useDeflate) ZipEntry.DEFLATED else ZipEntry.STORED
        entry.compressedSize = if (useDeflate) compressedSize else entry.size
        Files.newInputStream(if (useDeflate) compressed else raw).use { zip.addRawArchiveEntry(entry, it) }
    } finally {
        Files.deleteIfExists(compressed)
    }
}

private fun pack(jpeg: Path, assets: Path, output: Path) {
    ZipArchiveOutputStream(output.toFile()).use { zip ->
        Files.newInputStream(jpeg).use { input ->
            val buffer = ByteArray(65536)
            var length = input.read(buffer)
            while (length != -1) {
                zip.writePreamble(buffer, 0, length)
                length = input.read(buffer)
            }
        }
        ZipFile.builder().setPath(assets).get().use { it.copyRawEntries(zip) { true } }
    }
}
