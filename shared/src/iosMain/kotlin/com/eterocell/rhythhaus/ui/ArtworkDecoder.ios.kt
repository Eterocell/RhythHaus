package com.eterocell.rhythhaus.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.min
import kotlin.math.roundToInt
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.decodeArtwork(): ImageBitmap? = try {
    SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) {
    null
}

actual fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = decodeSkiaArtworkThumbnail(maxPixelSize)

private fun ByteArray.decodeSkiaArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = try {
    val source = SkiaImage.makeFromEncoded(this)
    val target = maxPixelSize.coerceAtLeast(1)
    val scale = min(target.toFloat() / source.width, target.toFloat() / source.height).coerceAtMost(1f)
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(
        image = source,
        src = Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
        dst = Rect.makeWH(width.toFloat(), height.toFloat()),
        samplingMode = SamplingMode.LINEAR,
        paint = null,
        strict = false,
    )
    surface.makeImageSnapshot().toComposeImageBitmap()
} catch (_: Exception) {
    null
}
