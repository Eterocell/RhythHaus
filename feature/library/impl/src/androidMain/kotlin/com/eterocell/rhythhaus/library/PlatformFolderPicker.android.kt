package com.eterocell.rhythhaus.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.folder_picker_error_access

/**
 * Creates the Android SAF tree folder picker launcher.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val context = LocalContext.current
    val couldNotAccessMessage =
        stringResource(Res.string.folder_picker_error_access)
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val result = runCatching {
                    persistTreePermission(context, uri)
                    PlatformFolderPickResult.Success(
                        uri.toAndroidSafSource(context))
                }
                    .getOrElse { throwable ->
                        PlatformFolderPickResult.Failure(
                            message = couldNotAccessMessage)
                    }
                onResult(result)
            }

    return remember(launcher) {
        object : PlatformFolderPickerLauncher {
            override val isAvailable: Boolean = true
            override val supportsAdditionalSources: Boolean = true

            override fun launch() {
                launcher.launch(null)
            }
        }
    }
}

private fun persistTreePermission(context: Context, uri: Uri) {
    context.contentResolver.takePersistableUriPermission(
        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun Uri.toAndroidSafSource(context: Context): LibrarySource {
    val document = DocumentFile.fromTreeUri(context, this)
    val stableUri = toString()
    val displayName = document?.name.orEmpty()
    return LibrarySource(
        id = androidSafSourceId(stableUri),
        platformKind = LibraryPlatformKind.AndroidSafTree,
        displayName = displayName,
        handle = stableUri,
        createdAtEpochMillis = System.currentTimeMillis(),
    )
}
