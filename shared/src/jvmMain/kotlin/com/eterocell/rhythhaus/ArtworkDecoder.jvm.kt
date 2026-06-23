package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.decodeArtwork(): ImageBitmap? = try {
    SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) {
    null
}
