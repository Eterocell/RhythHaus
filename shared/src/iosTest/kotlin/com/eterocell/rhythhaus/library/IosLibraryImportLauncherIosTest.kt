package com.eterocell.rhythhaus.library

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosLibraryImportLauncherIosTest {
    @AfterTest
    fun clearProvider() {
        IOSLibraryImportBridge.provider = null
    }

    @Test
    fun secondLaunchRejectedUntilFirstTerminalCompletion() {
        val results = mutableListOf<PlatformFolderPickResult>()
        val provider = RecordingProvider()
        IOSLibraryImportBridge.provider = provider
        val launcher =
            IosLibraryImportLauncher("prepare failed") { results += it }

        launcher.launch()
        launcher.launch()

        assertEquals(1, provider.calls.size)
        assertTrue(launcher.isImportActive)
        provider.calls.single().completion.complete(
            status = IOSLibraryImportStatus.CANCELLED,
            imported = 0,
            duplicates = 0,
            unsupported = 0,
            failed = 0,
            message = null,
        )
        assertFalse(launcher.isImportActive)
        assertEquals(
            listOf<PlatformFolderPickResult>(PlatformFolderPickResult.Cancelled),
            results,
        )
    }

    private class RecordingProvider : IOSLibraryImportProvider {
        data class Call(val destinationPath: String, val completion: IOSLibraryImportCompletion)
        val calls = mutableListOf<Call>()

        override fun importAudio(
            destinationPath: String,
            completion: IOSLibraryImportCompletion,
        ) {
            calls += Call(destinationPath, completion)
        }
    }
}
