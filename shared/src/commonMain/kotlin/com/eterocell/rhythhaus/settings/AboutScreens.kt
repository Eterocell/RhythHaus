package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.about
import rhythhaus.shared.generated.resources.about_app_name
import rhythhaus.shared.generated.resources.about_logo_description
import rhythhaus.shared.generated.resources.about_open_source_libraries
import rhythhaus.shared.generated.resources.about_version_format
import rhythhaus.shared.generated.resources.about_view_source
import rhythhaus.shared.generated.resources.open_source_libraries_error
import rhythhaus.shared.generated.resources.open_source_libraries_loading
import rhythhaus.shared.generated.resources.open_source_libraries_retry
import rhythhaus.shared.generated.resources.rhythhaus_logo
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

/**
 * Exact source repository URL for the About screen's source action.
 * Must remain identical to the project's canonical GitHub location.
 */
internal const val RhythHausSourceUrl: String = "https://github.com/Eterocell/RhythHaus"

internal data class AboutLayoutPolicy(
    val maxContentWidth: Dp,
    val logoImageSize: Dp,
)

internal val AboutScreenLayoutPolicy = AboutLayoutPolicy(
    maxContentWidth = 720.dp,
    logoImageSize = 80.dp,
)

/**
 * Reads the common attribution catalog resource. Shared by [OpenSourceLibrariesScreen] and by
 * regression tests so both consume the exact same resource path and encoding.
 */
internal suspend fun readAboutLibrariesCatalogJson(): String =
    Res.readBytes("files/aboutlibraries.json").decodeToString()

/**
 * Explicit, Compose-independent loading state for the AboutLibraries catalog. Kept as a plain
 * sealed interface so [loadAboutLibraries] can be unit tested without a Composable host.
 */
internal sealed interface AboutLibrariesLoadState {
    data object Loading : AboutLibrariesLoadState
    data class Loaded(val libraries: Libs) : AboutLibrariesLoadState
    data class Failed(val cause: Throwable) : AboutLibrariesLoadState
}

/**
 * Reads and parses the AboutLibraries catalog via [readJson], converting any resource-read or
 * JSON-parse failure into [AboutLibrariesLoadState.Failed] instead of letting it escape.
 * Coroutine cancellation is rethrown unchanged so structured concurrency keeps working.
 */
internal suspend fun loadAboutLibraries(
    readJson: suspend () -> String = ::readAboutLibrariesCatalogJson,
): AboutLibrariesLoadState =
    try {
        val json = readJson()
        val libraries = Libs.Builder().withJson(json).build()
        if (libraries.libraries.isEmpty()) {
            AboutLibrariesLoadState.Failed(
                IllegalStateException(
                    "AboutLibraries catalog parsed to zero libraries; the source JSON is likely malformed or empty.",
                ),
            )
        } else {
            AboutLibrariesLoadState.Loaded(libraries)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        AboutLibrariesLoadState.Failed(failure)
    }

@Composable
fun SettingsAboutScreen(
    onOpenLibraries: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutPolicy = CompactSettingsLayoutPolicy
    val uriHandler = LocalUriHandler.current

    Surface(modifier = modifier.fillMaxSize(), color = HausColors.current.paper) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = HausColors.current.paper,
            contentWindowInsets = WindowInsets(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(
                        horizontal = layoutPolicy.horizontalPagePadding,
                        vertical = layoutPolicy.verticalPagePadding,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = AboutScreenLayoutPolicy.maxContentWidth)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = layoutPolicy.bottomContentPadding),
                    verticalArrangement = Arrangement.spacedBy(layoutPolicy.itemSpacing),
                ) {
                    item {
                        RhythHausTopAppBar(
                            title = stringResource(Res.string.about),
                            onBack = onDismiss,
                            titlePadding = layoutPolicy.topBarTitlePadding,
                            navigationIconPadding = layoutPolicy.topBarNavigationIconPadding,
                        )
                    }

                    item {
                        AboutIdentityBlock()
                    }

                    item {
                        Button(
                            onClick = { uriHandler.openUri(RhythHausSourceUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { role = Role.Button },
                            cornerRadius = 16.dp,
                            colors = ButtonDefaults.buttonColors(
                                color = HausColors.current.panel,
                                contentColor = HausColors.current.ink,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = HausColors.current.ink,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(Res.string.about_view_source),
                                modifier = Modifier.padding(start = 10.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = onOpenLibraries,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { role = Role.Button },
                            cornerRadius = 16.dp,
                            colors = ButtonDefaults.buttonColors(
                                color = HausColors.current.ink,
                                contentColor = HausColors.current.paper,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null,
                                tint = HausColors.current.paper,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(Res.string.about_open_source_libraries),
                                modifier = Modifier.padding(start = 10.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutIdentityBlock() {
    val logoDescription = stringResource(Res.string.about_logo_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(HausColors.current.panel, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.rhythhaus_logo),
                contentDescription = logoDescription,
                modifier = Modifier.size(AboutScreenLayoutPolicy.logoImageSize),
            )
        }
        Text(
            text = stringResource(Res.string.about_app_name),
            color = HausColors.current.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(Res.string.about_version_format, RhythHausBuildInfo.versionName),
            color = HausColors.current.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun OpenSourceLibrariesScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutPolicy = CompactSettingsLayoutPolicy
    var retryGeneration by remember { mutableIntStateOf(0) }
    val loadState by produceState<AboutLibrariesLoadState>(
        initialValue = AboutLibrariesLoadState.Loading,
        retryGeneration,
    ) {
        value = AboutLibrariesLoadState.Loading
        value = withContext(Dispatchers.Default) { loadAboutLibraries() }
    }

    Surface(modifier = modifier.fillMaxSize(), color = HausColors.current.paper) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = HausColors.current.paper,
            contentWindowInsets = WindowInsets(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(
                        horizontal = layoutPolicy.horizontalPagePadding,
                        vertical = layoutPolicy.verticalPagePadding,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = AboutScreenLayoutPolicy.maxContentWidth)
                        .fillMaxWidth(),
                ) {
                    RhythHausTopAppBar(
                        title = stringResource(Res.string.about_open_source_libraries),
                        onBack = onDismiss,
                        titlePadding = layoutPolicy.topBarTitlePadding,
                        navigationIconPadding = layoutPolicy.topBarNavigationIconPadding,
                    )

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (val currentState = loadState) {
                        is AboutLibrariesLoadState.Loading -> {
                            Text(
                                text = stringResource(Res.string.open_source_libraries_loading),
                                modifier = Modifier.align(Alignment.Center),
                                color = HausColors.current.muted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        is AboutLibrariesLoadState.Failed -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = layoutPolicy.horizontalPagePadding),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.open_source_libraries_error),
                                    color = HausColors.current.muted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = { retryGeneration++ },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .semantics { role = Role.Button },
                                    cornerRadius = 16.dp,
                                    colors = ButtonDefaults.buttonColors(
                                        color = HausColors.current.ink,
                                        contentColor = HausColors.current.paper,
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.open_source_libraries_retry),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        is AboutLibrariesLoadState.Loaded -> {
                            LibrariesContainer(
                                libraries = currentState.libraries,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = layoutPolicy.itemSpacing,
                                    bottom = layoutPolicy.bottomContentPadding,
                                ),
                                colors = LibraryDefaults.libraryColors(
                                    libraryBackgroundColor = HausColors.current.paper,
                                    libraryContentColor = HausColors.current.ink,
                                    dialogBackgroundColor = HausColors.current.panel,
                                    dialogContentColor = HausColors.current.ink,
                                    dialogConfirmButtonColor = HausColors.current.pulse,
                                ),
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}
