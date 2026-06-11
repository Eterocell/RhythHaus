package com.eterocell.rhythhaus.taglib

actual fun createTagLibReader(): TagLibReader = UnlinkedNativeTagLibReader

private object UnlinkedNativeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = TagReadResult.Unsupported(
        "Native TagLib reader is not linked yet for Android",
    )
}
