package com.eterocell.rhythhaus.ui

import android.graphics.Bitmap
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
    val sampled = BitmapFactory.decodeByteArray(this, 0, size, options) ?: return null
    val (scaledWidth, scaledHeight) = scaledThumbnailDimension(sampled.width, sampled.height, target)
    val thumbnail = if (sampled.width == scaledWidth && sampled.height == scaledHeight) {
        sampled
    } else {
        Bitmap.createScaledBitmap(sampled, scaledWidth, scaledHeight, true)
    }
    thumbnail.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    val safeTarget = target.coerceAtLeast(1)
    val largestDimension = maxOf(width, height).coerceAtLeast(1)
    var sampleSize = 1
    while (largestDimension / (sampleSize * 2) >= safeTarget) {
        sampleSize *= 2
    }
    return sampleSize
}
