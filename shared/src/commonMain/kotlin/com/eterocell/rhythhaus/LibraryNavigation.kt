package com.eterocell.rhythhaus

sealed interface LibraryRoute {
    data object Home : LibraryRoute
    data class AlbumDetail(val album: String) : LibraryRoute
    data class ArtistDetail(val artist: String) : LibraryRoute
    data object NowPlaying : LibraryRoute
    data object Search : LibraryRoute
    data object Settings : LibraryRoute
    data object ClearLibraryDialog : LibraryRoute
}

data class LibraryNavigationStack(
    val routes: List<LibraryRoute> = listOf(LibraryRoute.Home),
) {
    val current: LibraryRoute = routes.lastOrNull() ?: LibraryRoute.Home
    val canPop: Boolean = routes.size > 1

    fun push(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        route == current -> this
        else -> copy(routes = normalizedRoutes(routes + route))
    }

    fun replaceTop(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        routes.size <= 1 -> push(route)
        else -> copy(routes = normalizedRoutes(routes.dropLast(1) + route))
    }

    fun pop(): LibraryNavigationStack = if (canPop) {
        copy(routes = routes.dropLast(1))
    } else {
        this
    }

    fun popToRoot(): LibraryNavigationStack = copy(routes = listOf(LibraryRoute.Home))

    private fun normalizedRoutes(candidate: List<LibraryRoute>): List<LibraryRoute> = when {
        candidate.isEmpty() -> listOf(LibraryRoute.Home)
        candidate.first() != LibraryRoute.Home -> listOf(LibraryRoute.Home) + candidate.filterNot { it == LibraryRoute.Home }
        else -> candidate
    }
}
