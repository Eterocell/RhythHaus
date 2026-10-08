package com.eterocell.rhythhaus

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel

class DesktopDropTargetTest {
    @Test
    fun readsDroppedFilePathsWithoutOpeningOrCopyingFiles() {
        val file =
            Files.createTempFile("rhythhaus-desktop-drop", ".mp3").toFile()
        try {
            val paths =
                desktopDroppedFilePaths(FileListTransferable(listOf(file)))

            assertEquals(listOf(file.path), paths)
            assertTrue(file.exists())
        } finally {
            file.delete()
        }
    }

    @Test
    fun ignoresTransferablesThatDoNotContainFiles() {
        assertNull(desktopDroppedFilePaths(TextTransferable))
        assertFalse(desktopDropCanStart(TextTransferable, isAvailable = true))
        var filesDropped = false
        assertFalse(
            admitDesktopDrop(
                transferable = TextTransferable,
                isAvailable = true,
                onFilesDropped = {
                    filesDropped = true
                    true
                },
            ),
        )
        assertFalse(filesDropped)
    }

    @Test
    fun admitsNativeFileTransferablesThroughTheExistingChannelOnlyWhenAvailable() {
        val file =
            Files.createTempFile("rhythhaus-desktop-drop", ".mp3").toFile()
        val drops = Channel<List<String>>(capacity = 1)
        try {
            assertFalse(
                desktopDropCanStart(
                    FileListTransferable(listOf(file)),
                    isAvailable = false,
                ),
            )
            assertTrue(
                desktopDropCanStart(
                    FileListTransferable(listOf(file)),
                    isAvailable = true,
                ),
            )
            assertFalse(
                admitDesktopDrop(
                    transferable = FileListTransferable(listOf(file)),
                    isAvailable = false,
                    onFilesDropped = { drops.trySend(it).isSuccess },
                ),
            )
            assertTrue(
                admitDesktopDrop(
                    transferable = FileListTransferable(listOf(file)),
                    isAvailable = true,
                    onFilesDropped = { drops.trySend(it).isSuccess },
                ),
            )
            assertEquals(listOf(file.path), drops.tryReceive().getOrThrow())
            assertTrue(file.exists())
        } finally {
            drops.close()
            file.delete()
        }
    }

    private class FileListTransferable(
        private val files: List<java.io.File>,
    ) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> =
            arrayOf(DataFlavor.javaFileListFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            flavor == DataFlavor.javaFileListFlavor

        override fun getTransferData(flavor: DataFlavor): Any {
            require(isDataFlavorSupported(flavor))
            return files
        }
    }

    private object TextTransferable : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> =
            arrayOf(DataFlavor.stringFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            flavor == DataFlavor.stringFlavor

        override fun getTransferData(flavor: DataFlavor): Any {
            require(isDataFlavorSupported(flavor))
            return "not a file list"
        }
    }
}
