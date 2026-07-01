package com.eterocell.rhythhaus

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.decodeArtwork(): ImageBitmap? = try {
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
} catch (_: Exception) {
    null
}

actual fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = try {
    val target = maxPixelSize.coerceAtLeast(1)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, target)
    }
    BitmapFactory.decodeByteArray(this, 0, size, options)?.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= target && halfHeight / sampleSize >= target) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}
