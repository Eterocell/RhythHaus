package com.eterocell.gradle.architecture

public object ArchitectureAllowList {
    private data class ModulePolicy(val packageRoots: Set<String>)

    private val allowList: Map<String, Set<String>> =
        mapOf(
            ":androidApp" to setOf(":shared"),
            ":desktopApp" to setOf(":shared"),
            ":shared" to setOf(":taglib", ":core:model", ":core:database", ":core:platform", ":core:playback", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl"),
            ":feature:library:impl" to setOf(":feature:library:api"),
            ":feature:playlists:impl" to setOf(":feature:playlists:api"),
            ":feature:library:api" to setOf(":core:model"),
            ":feature:playlists:api" to setOf(":core:model"),
        )

    private val policies: Map<String, ModulePolicy> =
        mapOf(
            ":shared" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":androidApp" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":desktopApp" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":taglib" to ModulePolicy(setOf("com.eterocell.rhythhaus.taglib")),
            ":core:model" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":core:database" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":core:platform" to ModulePolicy(setOf("com.eterocell.rhythhaus.platform")),
            ":core:playback" to ModulePolicy(setOf("com.eterocell.rhythhaus.playback")),
            ":core:ui" to ModulePolicy(setOf("com.eterocell.rhythhaus.ui", "com.eterocell.rhythhaus.theme")),
            ":feature:library:api" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":feature:library:impl" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":feature:playlists:api" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":feature:playlists:impl" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
        )

    public fun isAllowed(from: String, to: String): Boolean = to in allowList[from].orEmpty()

    public fun ownsPackage(modulePath: String, packageName: String): Boolean =
        policy(modulePath).packageRoots.any { root -> packageName == root || packageName.startsWith("$root.") }

    public fun packageRoots(modulePath: String): Set<String> = policy(modulePath).packageRoots

    public fun requiresExplicitApi(modulePath: String): Boolean = modulePath.startsWith(":core:") || modulePath.endsWith(":api")

    public fun allowsIosExport(modulePath: String, exportedProjectPath: String): Boolean = false

    public fun isSqlDelightOwner(modulePath: String): Boolean = modulePath == ":shared"

    /** Compose custom roots are intentionally code-owned rather than inferred from task internals. */
    public fun composeResourceRoots(modulePath: String, sourceSet: String): Set<String> =
        composeResourceRoots[modulePath to sourceSet].orEmpty()

    private val composeResourceRoots: Map<Pair<String, String>, Set<String>> = emptyMap()

    private fun policy(modulePath: String): ModulePolicy = policies[modulePath] ?: ModulePolicy(emptySet())
}
