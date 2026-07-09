package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppScanCancellationTest {
    @Test
    fun requestScanCancellationMarksActiveScanAsCancellingImmediately() {
        val progress = ScanProgress(
            session = ScanSession(
                id = "scan-1",
                sourceId = "source-1",
                status = ScanStatus.Scanning,
                startedAtEpochMillis = 1L,
                filesVisited = 42,
            ),
        )

        val result = progress.requestScanCancellation()

        assertEquals(ScanStatus.Cancelling, result?.session?.status)
        assertEquals(42, result?.session?.filesVisited)
    }

    @Test
    fun requestScanCancellationLeavesTerminalScanUntouched() {
        val progress = ScanProgress(
            session = ScanSession(
                id = "scan-1",
                sourceId = "source-1",
                status = ScanStatus.Completed,
                startedAtEpochMillis = 1L,
            ),
        )

        val result = progress.requestScanCancellation()

        assertSame(progress, result)
    }
}
