package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HausDialogTest {
    @Test
    fun darkDialogPresentationUsesSolidPanelAndLightScrim() {
        val presentation = hausDialogPresentation(DarkHausPalette)

        assertEquals(DarkHausPalette.panel, presentation.panelColor)
        assertTrue(presentation.scrimColor.luminance() > DarkHausPalette.paper.luminance())
        assertEquals(24.dp, presentation.outerPadding)
        assertEquals(24.dp, presentation.cornerRadius)
    }

    @Test
    fun lightDialogPresentationUsesSolidPanelAndRestrainedInkScrim() {
        val presentation = hausDialogPresentation(LightHausPalette)

        assertEquals(LightHausPalette.panel, presentation.panelColor)
        assertEquals(LightHausPalette.ink.copy(alpha = 0.36f), presentation.scrimColor)
    }

    @Test
    fun dialogPresentationBoundsScrollableContentForCompactWindows() {
        val presentation = hausDialogPresentation(LightHausPalette)

        assertEquals(480.dp, presentation.maxPanelHeight)
    }
}

@Composable
private fun hausDialogSeparateSlotsContract() {
    HausDialog(
        title = "Dialog",
        onDismiss = {},
        body = {},
        actions = {},
    )
}
