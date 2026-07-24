package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.selection_add_to_playlist
import rhythhaus.shared.generated.resources.selection_cancel
import rhythhaus.shared.generated.resources.selection_count_many
import rhythhaus.shared.generated.resources.selection_count_one
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

data class TrackSelectionBarSemantics(
    val selectedCountDescription: String,
    val cancelDescription: String,
    val addToPlaylistDescription: String,
)

fun trackSelectionBarSemantics(
    selectedCountDescription: String,
    cancelDescription: String,
    addToPlaylistDescription: String,
): TrackSelectionBarSemantics =
    TrackSelectionBarSemantics(
        selectedCountDescription = selectedCountDescription,
        cancelDescription = cancelDescription,
        addToPlaylistDescription = addToPlaylistDescription,
    )

@Composable
fun TrackSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onAddToPlaylist: () -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    require(selectedCount > 0)
    val countDescription =
        stringResource(
            if (selectedCount == 1) Res.string.selection_count_one
            else Res.string.selection_count_many,
            selectedCount,
        )
    val cancelDescription = stringResource(Res.string.selection_cancel)
    val addDescription = stringResource(Res.string.selection_add_to_playlist)
    val semantics =
        trackSelectionBarSemantics(
            countDescription, cancelDescription, addDescription)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .background(HausColors.current.panel, RoundedCornerShape(20.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = countDescription,
            modifier =
                Modifier.semantics {
                    contentDescription = semantics.selectedCountDescription
                },
            color = HausColors.current.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (interactive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionBarButton(
                    text = cancelDescription,
                    contentDescription = semantics.cancelDescription,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    emphasized = false,
                )
                SelectionBarButton(
                    text = addDescription,
                    contentDescription = semantics.addToPlaylistDescription,
                    onClick = onAddToPlaylist,
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun SelectionBarButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    emphasized: Boolean,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier.heightIn(min = 44.dp).semantics {
                this.contentDescription = contentDescription
            },
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (emphasized) HausColors.current.ink
                    else HausColors.current.panelStrong,
                contentColor =
                    if (emphasized) HausColors.current.paper
                    else HausColors.current.ink,
            ),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
