package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?
