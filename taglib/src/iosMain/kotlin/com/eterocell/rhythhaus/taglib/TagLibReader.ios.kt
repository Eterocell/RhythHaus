package com.eterocell.rhythhaus.taglib

actual fun createTagLibReader(): TagLibReader = IosUnlinkedNativeTagLibReader

private object IosUnlinkedNativeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = TagReadResult.Unsupported(
        IOS_TAGLIB_NOT_PACKAGED_MESSAGE,
    )
}

private const val IOS_TAGLIB_NOT_PACKAGED_MESSAGE =
    "Native TagLib iOS binding is scaffolded, but no iOS TagLib static library or XCFramework is packaged yet; " +
        "add taglib/third_party/taglib-ios/TagLib.xcframework or equivalent device/simulator static libraries " +
        "and wire Kotlin/Native cinterop before enabling metadata reads"
