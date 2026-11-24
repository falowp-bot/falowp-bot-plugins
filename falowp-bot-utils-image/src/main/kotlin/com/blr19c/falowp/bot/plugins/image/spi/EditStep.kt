package com.blr19c.falowp.bot.plugins.image.spi

import java.awt.Color

internal sealed interface EditStep {
    data class Resize(val width: Int, val height: Int) : EditStep
    data class Fit(val width: Int, val height: Int) : EditStep
    data class Cover(val width: Int, val height: Int) : EditStep
    data class Crop(val x: Int, val y: Int, val width: Int, val height: Int) : EditStep
    data class Rotate(val degrees: Double) : EditStep
    data object FlipHorizontal : EditStep
    data object FlipVertical : EditStep
    data class Pad(val left: Int, val top: Int, val right: Int, val bottom: Int) : EditStep
    data object Grayscale : EditStep
    data object Invert : EditStep
    data class Adjust(val brightness: Int, val contrast: Double) : EditStep
    data class Threshold(val level: Int) : EditStep
    data class Blur(val sigma: Double) : EditStep
    data class Flatten(val color: Color) : EditStep
    data class Tint(val saturation: Double, val hue: Double) : EditStep
    data class Opacity(val factor: Double) : EditStep
    data object Sharpen : EditStep
    data class Median(val size: Int) : EditStep
    data class Pixelate(val blockSize: Int) : EditStep
    class Overlay(val image: ByteArray, val x: Int, val y: Int) : EditStep
    class Join(val image: ByteArray, val vertical: Boolean) : EditStep
}
