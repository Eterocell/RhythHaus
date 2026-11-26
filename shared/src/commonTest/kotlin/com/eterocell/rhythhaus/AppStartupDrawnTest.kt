package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStartupDrawnTest {
    private val pending = InitialLibraryPublicationState()
    private val emptyLibrary = LibraryContentState(emptyList(), emptyList())

    @Test
    fun pendingHomeIsNotFullyDrawnEvenIfAnotherPublicationWasApplied() {
        assertFalse(pending.isAppliedAt(0))
        assertFalse(pending.isAppliedAt(1))
    }

    @Test
    fun emptyHomeMustApplyItsTerminalPublication() {
        val initial = pending.complete(emptyLibrary).copy(publicationRevision = 2)
        assertFalse(initial.isAppliedAt(1))
        assertTrue(initial.isAppliedAt(2))
    }

    @Test
    fun recoverableHomeCanUseAnAppliedNewerPublication() {
        val initial =
            pending.failSafe(emptyLibrary, IllegalStateException("restore failed"))
                .copy(publicationRevision = 2)
        assertTrue(initial.isAppliedAt(3))
    }
}
