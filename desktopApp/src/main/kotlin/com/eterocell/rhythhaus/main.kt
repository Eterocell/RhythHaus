package com.eterocell.rhythhaus

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.eterocell.rhythhaus.di.startRhythHausKoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startRhythHausKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RhythHaus",
        ) {
            val drops = remember { Channel<List<String>>(capacity = 1) }
            val events = remember(drops) { drops.receiveAsFlow() }
            var dropActive by remember { mutableStateOf(false) }
            var desktopDropAvailable by remember { mutableStateOf(false) }
            val currentDesktopDropAvailable by
                rememberUpdatedState(desktopDropAvailable)
            val dropTarget =
                remember(drops) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean =
                            try {
                                if (!currentDesktopDropAvailable) {
                                    false
                                } else {
                                    runCatching {
                                            admitDesktopDrop(
                                                transferable =
                                                    event.awtTransferable,
                                                isAvailable =
                                                    currentDesktopDropAvailable,
                                                onFilesDropped = {
                                                    drops.trySend(it).isSuccess
                                                },
                                            )
                                        }
                                        .getOrDefault(false)
                                }
                            } finally {
                                dropActive = false
                            }

                        override fun onEntered(event: DragAndDropEvent) {
                            dropActive =
                                currentDesktopDropAvailable &&
                                    runCatching {
                                            desktopDropCanStart(
                                                transferable =
                                                    event.awtTransferable,
                                                isAvailable = true,
                                            )
                                        }
                                        .getOrDefault(false)
                        }

                        override fun onExited(event: DragAndDropEvent) {
                            dropActive = false
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            dropActive = false
                        }
                    }
                }
            DisposableEffect(drops) {
                onDispose {
                    drops.close()
                }
            }
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                currentDesktopDropAvailable &&
                                    runCatching {
                                            desktopDropCanStart(
                                                transferable =
                                                    event.awtTransferable,
                                                isAvailable = true,
                                            )
                                        }
                                        .getOrDefault(false)
                            },
                            target = dropTarget,
                        ),
            ) {
                App(
                    desktopDropEvents = events,
                    desktopDropActive = desktopDropAvailable && dropActive,
                    onDesktopDropAvailabilityChanged = { available ->
                        desktopDropAvailable = available
                        if (!available) dropActive = false
                    },
                )
            }
        }
    }
}
