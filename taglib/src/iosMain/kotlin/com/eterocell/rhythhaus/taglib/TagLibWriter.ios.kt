package com.eterocell.rhythhaus.taglib

actual fun createTagLibWriter(): TagLibWriter = IosNativeTagLibWriter()

private class IosNativeTagLibWriter : TagLibWriter {
    override fun writePath(path: String, meta: WriteMeta): WriteResult =
        WriteResult.Unsupported("Write not yet wired for iOS cinterop")
}
