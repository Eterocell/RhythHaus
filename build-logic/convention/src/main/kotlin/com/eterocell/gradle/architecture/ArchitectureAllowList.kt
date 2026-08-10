package com.eterocell.gradle.architecture

public object ArchitectureAllowList {
    private data class ModulePolicy(
        val packageRoots: Set<String>,
        val androidNamespace: String? = null,
        val composeNamespace: String? = null,
    )

    private val allowList: Map<String, Set<String>> =
        mapOf(
            ":androidApp" to setOf(":shared"),
            ":desktopApp" to setOf(":shared"),
            ":shared" to setOf(":taglib", ":core:model", ":core:database", ":core:platform", ":core:playback", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl", ":feature:nowplaying", ":feature:search", ":feature:settings"),
            ":feature:nowplaying" to setOf(":core:playback", ":core:ui"),
            ":feature:search" to setOf(":feature:library:api", ":core:ui"),
            ":feature:settings" to setOf(":core:ui"),
            ":feature:library:impl" to setOf(":feature:library:api", ":core:model", ":core:ui", ":core:database", ":core:platform", ":taglib"),
            ":feature:playlists:impl" to setOf(":feature:playlists:api", ":feature:library:api", ":core:model", ":core:playback", ":core:ui", ":core:platform", ":core:database"),
            ":feature:library:api" to setOf(":core:model"),
            ":core:playback" to setOf(":core:model", ":core:platform"),
        )

    private val policies: Map<String, ModulePolicy> =
        mapOf(
            ":shared" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":androidApp" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":desktopApp" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":taglib" to ModulePolicy(setOf("com.eterocell.rhythhaus.taglib")),
            ":core:model" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":core:database" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":core:platform" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":core:playback" to ModulePolicy(setOf("com.eterocell.rhythhaus")),
            ":core:ui" to ModulePolicy(setOf("com.eterocell.rhythhaus.ui", "com.eterocell.rhythhaus.theme")),
            ":feature:library:api" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":feature:library:impl" to ModulePolicy(
                packageRoots = setOf(
                    "com.eterocell.rhythhaus.library",
                    "com.eterocell.rhythhaus.library.impl",
                    "com.eterocell.rhythhaus.library.ui",
                ),
                androidNamespace = "com.eterocell.rhythhaus.library.impl",
                composeNamespace = "rhythhaus.feature.library.generated.resources",
            ),
            ":feature:playlists:api" to ModulePolicy(setOf("com.eterocell.rhythhaus.library")),
            ":feature:playlists:impl" to ModulePolicy(
                setOf(
                    "com.eterocell.rhythhaus.library",
                    "com.eterocell.rhythhaus.library.ui",
                    "com.eterocell.rhythhaus.playlistbackup",
                ),
            ),
            ":feature:nowplaying" to ModulePolicy(setOf("com.eterocell.rhythhaus.nowplaying", "com.eterocell.rhythhaus.ui")),
            ":feature:search" to ModulePolicy(
                packageRoots = setOf("com.eterocell.rhythhaus.search"),
                androidNamespace = "com.eterocell.rhythhaus.search",
                composeNamespace = "rhythhaus.feature.search.generated.resources",
            ),
            ":feature:settings" to ModulePolicy(
                packageRoots = setOf("com.eterocell.rhythhaus.settings"),
                androidNamespace = "com.eterocell.rhythhaus.settings",
                composeNamespace = "rhythhaus.feature.settings.generated.resources",
            ),
        )

    public fun isAllowed(from: String, configuration: String, to: String): Boolean =
        when {
            from == ":shared" && to in setOf(":feature:search", ":feature:settings") -> configuration == "commonMainImplementation"
            else -> to in allowList[from].orEmpty()
        }

    public fun ownsPackage(modulePath: String, packageName: String): Boolean =
        policy(modulePath).packageRoots.any { root -> packageName == root || packageName.startsWith("$root.") }

    public fun packageRoots(modulePath: String): Set<String> = policy(modulePath).packageRoots

    public fun expectedAndroidNamespace(modulePath: String): String? = policy(modulePath).androidNamespace

    public fun expectedComposeNamespace(modulePath: String): String? = policy(modulePath).composeNamespace

    public fun requiresExplicitApi(modulePath: String): Boolean = modulePath.startsWith(":core:") || modulePath.endsWith(":api")

    public fun allowsIosExport(modulePath: String, exportedProjectPath: String): Boolean =
        modulePath == ":shared" && exportedProjectPath == ":core:playback"

    public fun sqlDelightOwner(): String = ":core:database"

    public fun isSqlDelightOwner(modulePath: String): Boolean = modulePath == sqlDelightOwner()

    /** Compose custom roots are intentionally code-owned rather than inferred from task internals. */
    public fun composeResourceRoots(modulePath: String, sourceSet: String): Set<String> =
        composeResourceRoots[modulePath to sourceSet].orEmpty()

    private val composeResourceRoots: Map<Pair<String, String>, Set<String>> = emptyMap()

    private fun policy(modulePath: String): ModulePolicy = policies[modulePath] ?: ModulePolicy(emptySet())
}
