# Background Scanning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move folder scanning off the main thread using Kotlin coroutines, show scan progress in UI

**Architecture:** Launch `scanner.scan()` on `Dispatchers.Default`, feed `ScanProgress` as mutable state for reactive UI, reload tracks on main thread when done, support cancellation

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Kotlin Coroutines, Miuix UI

## Global Constraints

- Kotlin 2.4.0, Compose Multiplatform 1.11.1
- Existing `CoroutineScope` pattern: use `rememberCoroutineScope()` in `App()` composable
- Existing `ScanProgress` model in `LibraryModels.kt` (has `session`, `latestItem`, `isActive`)
- `LibraryScanner.scan()` already accepts `isCancelled: () -> Boolean` lambda
- Scanner runs synchronously — `scanner.scan(source)` is a blocking call; wrap in coroutine

---

### Task 1: Background scanning with progress indicator

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Consumes:** `LibraryScanner` (existing), `ScanProgress` (existing), `CoroutineScope` (from `rememberCoroutineScope`)

**Produces:** Non-blocking scan with progress bar, cancellation support

- [ ] **Step 1: Add scan state variables in App()**

In `App()`, add state variables for scan progress and a scan job handle:

```kotlin
var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
var scanJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
val scope = rememberCoroutineScope()
```

- [ ] **Step 2: Replace synchronous scanner call in folder picker callback**

Replace the synchronous block in `rememberPlatformFolderPickerLauncher` callback:

Old code (sync, blocking):
```kotlin
is PlatformFolderPickResult.Success -> {
    val session = scanner.scan(result.source)
    libraryTracks = repository.tracks()
}
```

New code (async, non-blocking):
```kotlin
is PlatformFolderPickResult.Success -> {
    val source = result.source
    scanJob = scope.launch(Dispatchers.Default) {
        var progress = ScanProgress(
            session = ScanSession(id = "", sourceId = source.id, status = ScanStatus.Scanning, startedAtEpochMillis = 0L)
        )
        withContext(Dispatchers.Main) { scanProgress = progress }

        val session = scanner.scan(source) { scanJob?.isActive != true }

        withContext(Dispatchers.Main) {
            scanProgress = ScanProgress(session = session)
            libraryTracks = repository.tracks()
        }
    }
}
```

- [ ] **Step 3: Add progress card to LibraryHomeScreen**

In `LibraryHomeScreen`, above the browsing content, check `scanProgress`:

```kotlin
if (scanProgress?.isActive == true) {
    ScanningCard(
        foldersVisited = scanProgress!!.session!!.foldersVisited,
        filesVisited = scanProgress!!.session!!.filesVisited,
        tracksAdded = scanProgress!!.session!!.tracksAdded,
        onCancel = { scanJob?.cancel() },
    )
}
```

- [ ] **Step 4: Create ScanningCard composable at end of App.kt**

```kotlin
@Composable
private fun ScanningCard(
    foldersVisited: Int,
    filesVisited: Int,
    tracksAdded: Int,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        cornerRadius = 12.dp,
        colors = CardDefaults.defaultColors(color = HausPanel),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Scanning…", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HausInk)
            Spacer(Modifier.height(6.dp))
            Text("$foldersVisited folders • $filesVisited files • $tracksAdded tracks", fontSize = 12.sp, color = HausInk.copy(alpha = 0.7f))
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 8.dp,
                colors = ButtonDefaults.buttonColors(color = HausInk, contentColor = HausPaper),
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}
```

Requires import: `import androidx.compose.material3.LinearProgressIndicator` — but we use Miuix. Check if Miuix has a progress indicator. If not, use a simple animated Row or add material3 back just for `LinearProgressIndicator` (minimal, single-use dependency addition to `shared/build.gradle.kts`).

- [ ] **Step 5: Hide import card while scanning**

In `LibraryHomeScreen`, conditionally hide `ImportAudioCard` when scan is active:

```kotlin
if (scanProgress?.isActive != true) {
    ImportAudioCard(...)
}
```

- [ ] **Step 6: Add imports**

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.LinearProgressIndicator // If Miuix lacks one
```

- [ ] **Step 7: Test compilation**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: move scanner to background thread with progress indicator"
```
