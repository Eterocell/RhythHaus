package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner

/** Commits one validated interchange plan through the existing owner mutex. */
internal suspend fun importPlaylistInteroperability(
    owner: PlaylistStateOwner,
    plan: PlaylistInteroperabilityImportPlan,
) = owner.importPlaylists(listOf(plan.toMutation()))
