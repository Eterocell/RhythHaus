package com.eterocell.rhythhaus

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

internal fun desktopDroppedFilePaths(
    transferable: Transferable
): List<String>? {
    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        return null
    val files =
        transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
    return if (files != null && files.all { it is File }) {
        files.filterIsInstance<File>().map(File::getPath)
    } else {
        null
    }
}

internal fun desktopDropCanStart(
    transferable: Transferable,
    isAvailable: Boolean,
): Boolean =
    isAvailable &&
        runCatching {
                transferable.isDataFlavorSupported(
                    DataFlavor.javaFileListFlavor)
            }
            .getOrDefault(false)

internal fun admitDesktopDrop(
    transferable: Transferable,
    isAvailable: Boolean,
    onFilesDropped: (List<String>) -> Boolean,
): Boolean {
    if (!isAvailable) return false
    return runCatching {
            desktopDroppedFilePaths(transferable)?.let(onFilesDropped) ?: false
        }
        .getOrDefault(false)
}
