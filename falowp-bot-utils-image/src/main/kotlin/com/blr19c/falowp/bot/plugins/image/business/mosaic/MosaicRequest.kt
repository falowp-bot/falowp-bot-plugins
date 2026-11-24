package com.blr19c.falowp.bot.plugins.image.business.mosaic

import java.awt.image.BufferedImage
import java.nio.file.Path

internal class MosaicRequest {
    var subject: MosaicTarget? = null

    val materials = mutableListOf<MosaicTiles>()

    var cells: Int? = null

    var pixels = 192

    var expectedBytes = 30_000_000L

    var archiveTiles = false

    var opacity = 0.18f
}

internal sealed interface MosaicTarget {
    class File(val path: Path) : MosaicTarget
    class Image(val image: BufferedImage) : MosaicTarget
}

internal sealed interface MosaicTiles {
    class Directory(val path: Path) : MosaicTiles
    class File(val path: Path) : MosaicTiles
    class Image(val image: BufferedImage) : MosaicTiles
}
