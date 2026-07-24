package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.import_card_description
import rhythhaus.shared.generated.resources.import_card_title
import rhythhaus.shared.generated.resources.import_card_title_with_tracks

@Composable
fun importCardTitle(): String = stringResource(Res.string.import_card_title)

@Composable
fun importCardTitleWithTracks(): String =
    stringResource(Res.string.import_card_title_with_tracks)

@Composable
fun importCardDescription(): String =
    stringResource(Res.string.import_card_description)
