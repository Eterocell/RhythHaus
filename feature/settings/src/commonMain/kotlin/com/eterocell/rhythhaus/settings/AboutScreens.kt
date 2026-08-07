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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.settings.generated.resources.Res
import rhythhaus.feature.settings.generated.resources.about
import rhythhaus.feature.settings.generated.resources.about_app_name
import rhythhaus.feature.settings.generated.resources.about_logo_description
import rhythhaus.feature.settings.generated.resources.about_open_source_libraries
import rhythhaus.feature.settings.generated.resources.about_version_format
import rhythhaus.feature.settings.generated.resources.about_view_source
import rhythhaus.feature.settings.generated.resources.open_source_libraries_error
import rhythhaus.feature.settings.generated.resources.open_source_libraries_loading
import rhythhaus.feature.settings.generated.resources.open_source_libraries_retry
import rhythhaus.feature.settings.generated.resources.rhythhaus_logo
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

internal const val RhythHausSourceUrl = "https://github.com/Eterocell/RhythHaus"
internal const val AboutLoadingTestTag = "about-libraries-loading"
internal const val AboutRetryTestTag = "about-libraries-retry"
internal const val AboutSourceTestTag = "about-source"
internal const val AboutLibrariesTestTag = "about-libraries"
internal const val AboutLoadedTestTag = "about-libraries-loaded"

internal data class AboutLayoutPolicy(
    val maxContentWidth: androidx.compose.ui.unit.Dp,
    val logoImageSize: androidx.compose.ui.unit.Dp
)

internal val AboutScreenLayoutPolicy = AboutLayoutPolicy(720.dp, 80.dp)

internal sealed interface AboutLibrariesLoadState {
    data object Loading : AboutLibrariesLoadState

    data class Loaded(val libraries: Libs) : AboutLibrariesLoadState

    data class Failed(val cause: Throwable) : AboutLibrariesLoadState
}

private sealed interface AboutLibrariesLoadOutcome {
    data class Catalog(val json: String, val libraries: Libs) :
        AboutLibrariesLoadOutcome

    data class CallbackCancellation(val cancellation: CancellationException) :
        AboutLibrariesLoadOutcome
}

internal suspend fun loadAboutLibraries(
    readJson: suspend () -> String,
    parseJson: (String) -> Libs = { Libs.Builder().withJson(it).build() },
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): AboutLibrariesLoadState =
    try {
        val outcome =
            withContext(dispatcher) {
                val json =
                    try {
                        readJson()
                    } catch (cancellation: CancellationException) {
                        return@withContext AboutLibrariesLoadOutcome
                            .CallbackCancellation(cancellation)
                    }
                val libraries =
                    try {
                        parseJson(json)
                    } catch (cancellation: CancellationException) {
                        return@withContext AboutLibrariesLoadOutcome
                            .CallbackCancellation(cancellation)
                    }
                AboutLibrariesLoadOutcome.Catalog(json, libraries)
            }
        if (outcome is AboutLibrariesLoadOutcome.CallbackCancellation) {
            throw outcome.cancellation
        }
        outcome as AboutLibrariesLoadOutcome.Catalog
        if (outcome.json.isBlank() || outcome.libraries.libraries.isEmpty()) {
            AboutLibrariesLoadState.Failed(
                IllegalStateException(
                    "AboutLibraries catalog parsed to zero libraries; the source JSON is likely malformed or empty."))
        } else AboutLibrariesLoadState.Loaded(outcome.libraries)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        AboutLibrariesLoadState.Failed(failure)
    }

/**
 * Internal whole-load seam for production-content lifecycle tests; production
 * content defaults to [loadAboutLibraries].
 */
internal typealias AboutLibrariesLoader = suspend () -> AboutLibrariesLoadState

/**
 * Internal comparison observer for production-content lifecycle tests.
 * Implementations must not throw because it is invoked in the loading
 * coroutine.
 */
internal typealias AboutLibrariesLoadComparisonObserver =
    (AboutLibrariesLoadState, Boolean) -> Unit

/**
 * Renders the feature-owned About page and delegates route actions to Shared.
 */
@Composable
public fun SettingsAboutScreen(
    onOpenLibraries: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val policy = CompactSettingsLayoutPolicy
    val uriHandler = LocalUriHandler.current
    Surface(modifier.fillMaxSize(), color = HausColors.current.paper) {
        Scaffold(
            Modifier.fillMaxSize(),
            containerColor = HausColors.current.paper,
            contentWindowInsets = WindowInsets(0.dp)) {
                Box(
                    Modifier.fillMaxSize()
                        .safeContentPadding()
                        .padding(
                            horizontal = policy.horizontalPagePadding,
                            vertical = policy.verticalPagePadding),
                    contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            Modifier.fillMaxHeight()
                                .widthIn(
                                    max =
                                        AboutScreenLayoutPolicy.maxContentWidth)
                                .fillMaxWidth(),
                            contentPadding =
                                PaddingValues(
                                    bottom = policy.bottomContentPadding),
                            verticalArrangement =
                                Arrangement.spacedBy(policy.itemSpacing)) {
                                item {
                                    RhythHausTopAppBar(
                                        stringResource(Res.string.about),
                                        onDismiss,
                                        titlePadding =
                                            policy.topBarTitlePadding,
                                        navigationIconPadding =
                                            policy.topBarNavigationIconPadding)
                                }
                                item { AboutIdentityBlock() }
                                item {
                                    Button(
                                        onClick = {
                                            uriHandler.openUri(
                                                RhythHausSourceUrl)
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag(AboutSourceTestTag)
                                                .semantics {
                                                    role = Role.Button
                                                },
                                        cornerRadius = 16.dp,
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                color =
                                                    HausColors.current.panel,
                                                contentColor =
                                                    HausColors.current.ink)) {
                                            Icon(
                                                Icons.AutoMirrored.Filled
                                                    .OpenInNew,
                                                null,
                                                tint = HausColors.current.ink,
                                                modifier = Modifier.size(20.dp))
                                            Text(
                                                stringResource(
                                                    Res.string
                                                        .about_view_source),
                                                Modifier.padding(start = 10.dp),
                                                fontWeight = FontWeight.Bold)
                                        }
                                }
                                item {
                                    Button(
                                        onClick = onOpenLibraries,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag(AboutLibrariesTestTag)
                                                .semantics {
                                                    role = Role.Button
                                                },
                                        cornerRadius = 16.dp,
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                color = HausColors.current.ink,
                                                contentColor =
                                                    HausColors.current.paper)) {
                                            Icon(
                                                Icons.AutoMirrored.Filled
                                                    .LibraryBooks,
                                                null,
                                                tint = HausColors.current.paper,
                                                modifier = Modifier.size(20.dp))
                                            Text(
                                                stringResource(
                                                    Res.string
                                                        .about_open_source_libraries),
                                                Modifier.padding(start = 10.dp),
                                                fontWeight = FontWeight.Bold)
                                        }
                                }
                            }
                    }
            }
    }
}

@Composable
private fun AboutIdentityBlock() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(88.dp)
                    .background(
                        HausColors.current.panel,
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            20.dp)),
                contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(Res.drawable.rhythhaus_logo),
                        stringResource(Res.string.about_logo_description),
                        Modifier.size(AboutScreenLayoutPolicy.logoImageSize)
                            .testTag("about-logo"))
                }
            Text(
                stringResource(Res.string.about_app_name),
                color = HausColors.current.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black)
            Text(
                stringResource(
                    Res.string.about_version_format,
                    RhythHausBuildInfo.versionName),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium)
        }
}

/**
 * Reads and renders caller-supplied app-wide attribution JSON, retaining
 * retryable failures and preserving exact injected read/parse callback
 * cancellation identity (parse leaves dispatcher work as data), while
 * dispatcher rejection, prompt cancellation, and Job cancellation propagate
 * without `Loaded`/`Failed` publication or an identity promise.
 */
@Composable
public fun OpenSourceLibrariesScreen(
    readCatalogJson: suspend () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    OpenSourceLibrariesContent(readCatalogJson, onDismiss, modifier)
}

/**
 * Renders the production attribution content with internal whole-load test
 * seams.
 */
@Composable
internal fun OpenSourceLibrariesContent(
    readCatalogJson: suspend () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    loadLibraries: AboutLibrariesLoader? = null,
    onLoadCompared: AboutLibrariesLoadComparisonObserver = { _, _ -> }
) {
    val policy = CompactSettingsLayoutPolicy
    val currentLoader by rememberUpdatedState(readCatalogJson)
    val defaultLoader: AboutLibrariesLoader = remember {
        { loadAboutLibraries(currentLoader) }
    }
    val activeLoader = loadLibraries ?: defaultLoader
    val currentComparisonObserver by rememberUpdatedState(onLoadCompared)
    var retryGeneration by remember { mutableIntStateOf(0) }
    var retryState by remember {
        mutableStateOf<AboutLibrariesLoadState?>(null)
    }
    val token =
        remember(readCatalogJson, activeLoader, retryGeneration) { Any() }
    val currentToken by rememberUpdatedState(token)
    val state by
        produceState<AboutLibrariesLoadState>(
            AboutLibrariesLoadState.Loading, token) {
                value = AboutLibrariesLoadState.Loading
                val result = activeLoader()
                val isCurrent = token === currentToken
                currentComparisonObserver(result, isCurrent)
                if (isCurrent) {
                    retryState = null
                    value = result
                }
            }
    val visibleState = retryState ?: state
    Surface(modifier.fillMaxSize(), color = HausColors.current.paper) {
        Scaffold(
            Modifier.fillMaxSize(),
            containerColor = HausColors.current.paper,
            contentWindowInsets = WindowInsets(0.dp)) {
                Box(
                    Modifier.fillMaxSize()
                        .safeContentPadding()
                        .padding(
                            horizontal = policy.horizontalPagePadding,
                            vertical = policy.verticalPagePadding),
                    contentAlignment = Alignment.TopCenter) {
                        Column(
                            Modifier.fillMaxHeight()
                                .widthIn(
                                    max =
                                        AboutScreenLayoutPolicy.maxContentWidth)
                                .fillMaxWidth()) {
                                RhythHausTopAppBar(
                                    stringResource(
                                        Res.string.about_open_source_libraries),
                                    onDismiss,
                                    titlePadding = policy.topBarTitlePadding,
                                    navigationIconPadding =
                                        policy.topBarNavigationIconPadding)
                                Box(Modifier.fillMaxSize().weight(1f)) {
                                    when (val current = visibleState) {
                                        AboutLibrariesLoadState.Loading ->
                                            Text(
                                                stringResource(
                                                    Res.string
                                                        .open_source_libraries_loading),
                                                Modifier.align(Alignment.Center)
                                                    .testTag(
                                                        AboutLoadingTestTag),
                                                color =
                                                    HausColors.current.muted,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium)
                                        is AboutLibrariesLoadState.Failed ->
                                            Column(
                                                Modifier.align(Alignment.Center)
                                                    .padding(
                                                        horizontal =
                                                            policy
                                                                .horizontalPagePadding),
                                                horizontalAlignment =
                                                    Alignment
                                                        .CenterHorizontally,
                                                verticalArrangement =
                                                    Arrangement.spacedBy(
                                                        16.dp)) {
                                                    Text(
                                                        stringResource(
                                                            Res.string
                                                                .open_source_libraries_error),
                                                        color =
                                                            HausColors.current
                                                                .muted,
                                                        fontSize = 14.sp,
                                                        fontWeight =
                                                            FontWeight.Medium,
                                                        textAlign =
                                                            TextAlign.Center)
                                                    Button(
                                                        onClick = {
                                                            retryState =
                                                                AboutLibrariesLoadState
                                                                    .Loading
                                                            retryGeneration++
                                                        },
                                                        modifier =
                                                            Modifier.heightIn(
                                                                    min = 48.dp)
                                                                .testTag(
                                                                    AboutRetryTestTag),
                                                        cornerRadius = 16.dp,
                                                        colors =
                                                            ButtonDefaults
                                                                .buttonColors(
                                                                    color =
                                                                        HausColors
                                                                            .current
                                                                            .ink,
                                                                    contentColor =
                                                                        HausColors
                                                                            .current
                                                                            .paper)) {
                                                            Text(
                                                                stringResource(
                                                                    Res.string
                                                                        .open_source_libraries_retry),
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Bold)
                                                        }
                                                }
                                        is AboutLibrariesLoadState.Loaded ->
                                            Box(
                                                Modifier.fillMaxSize()
                                                    .testTag(
                                                        AboutLoadedTestTag)) {
                                                    LibrariesContainer(
                                                        libraries =
                                                            current.libraries,
                                                        modifier =
                                                            Modifier
                                                                .fillMaxSize(),
                                                        contentPadding =
                                                            PaddingValues(
                                                                top =
                                                                    policy
                                                                        .itemSpacing,
                                                                bottom =
                                                                    policy
                                                                        .bottomContentPadding),
                                                        colors =
                                                            LibraryDefaults
                                                                .libraryColors(
                                                                    libraryBackgroundColor =
                                                                        HausColors
                                                                            .current
                                                                            .paper,
                                                                    libraryContentColor =
                                                                        HausColors
                                                                            .current
                                                                            .ink,
                                                                    dialogBackgroundColor =
                                                                        HausColors
                                                                            .current
                                                                            .panel,
                                                                    dialogContentColor =
                                                                        HausColors
                                                                            .current
                                                                            .ink,
                                                                    dialogConfirmButtonColor =
                                                                        HausColors
                                                                            .current
                                                                            .pulse))
                                                }
                                    }
                                }
                            }
                    }
            }
    }
}
