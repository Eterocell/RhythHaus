package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Publishes dismissible feature content into an app-owned window overlay. */
public fun interface HausWindowOverlayPublisher {
    /**
     * Replaces the host's current modal content; `null` clears the overlay.
     * [restoreInvokerFocus] runs after the background child is restored and
     * returns `true` when it successfully focuses the modal invoker.
     */
    public fun publish(
        content: (@Composable () -> Unit)?,
        restoreInvokerFocus: (() -> Boolean)?,
    )
}

/**
 * Feature surfaces publish modal visuals here while Shared retains Back
 * ownership.
 */
public val LocalHausWindowOverlayPublisher:
    ProvidableCompositionLocal<HausWindowOverlayPublisher?> =
    staticCompositionLocalOf<HausWindowOverlayPublisher?> { null }

/** Whether a window-level modal currently covers the host's background. */
public val LocalHausWindowOverlayVisible: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf {
        false
    }

/**
 * Draws feature modal content above the full app window, outside narrow panes.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
public fun HausWindowOverlayHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var overlayContent by remember {
        mutableStateOf<(@Composable () -> Unit)?>(null, neverEqualPolicy())
    }
    var overlayVisible by remember { mutableStateOf(false) }
    val backgroundFocusRequester = remember { FocusRequester() }
    var restoreInvokerFocus by remember {
        mutableStateOf<(() -> Boolean)?>(null, neverEqualPolicy())
    }
    val publisher =
        remember(backgroundFocusRequester) {
            HausWindowOverlayPublisher { next, invokerFocusRestorer ->
                if (next == null) {
                    overlayContent = null
                    overlayVisible = false
                } else {
                    if (!overlayVisible) {
                        backgroundFocusRequester.saveFocusedChild()
                    }
                    overlayContent = next
                    restoreInvokerFocus = invokerFocusRestorer
                    overlayVisible = true
                }
            }
        }
    var overlayWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            withFrameNanos {}
        } else if (overlayWasVisible) {
            withFrameNanos {}
            val backgroundRestored =
                backgroundFocusRequester.restoreFocusedChild()
            val invokerRestored = restoreInvokerFocus?.invoke() == true
            restoreInvokerFocus = null
            if (!backgroundRestored && !invokerRestored) {
                backgroundFocusRequester.requestFocus()
            }
        }
        overlayWasVisible = overlayVisible
    }
    Box(modifier) {
        Box(
            modifier =
                Modifier.focusRequester(backgroundFocusRequester)
                    .focusRestorer()
                    .focusGroup()
                    .then(
                        if (overlayVisible) {
                            Modifier.focusProperties { canFocus = false }
                                .clearAndSetSemantics {}
                        } else {
                            Modifier
                        },
                    ),
        ) {
            CompositionLocalProvider(
                LocalHausWindowOverlayPublisher provides publisher,
                LocalHausWindowOverlayVisible provides overlayVisible,
                content = content,
            )
        }
        Box(
            Modifier.fillMaxSize()
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
        ) {
            val content = overlayContent
            if (content != null) PublishedOverlayContent(content)
        }
    }
}

@NonSkippableComposable
@Composable
private fun PublishedOverlayContent(content: @Composable () -> Unit) {
    content()
}
