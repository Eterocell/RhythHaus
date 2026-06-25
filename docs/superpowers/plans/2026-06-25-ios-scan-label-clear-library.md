# iOS Scan Label + Clear Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename iOS import button to "Scan for Music", add "Clear Library" button with confirmation dialog

**Architecture:** Platform-aware label via expect/actual, AlertDialog for confirmation, clearAll() on repository

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Miuix UI, Kermit logger

## Global Constraints

- Kotlin 2.4.0, Compose Multiplatform 1.11.1
- App.kt is the only UI file — all changes here
- Repository already has interface `LibraryRepository` and impl `SqlDelightLibraryRepository`
- Use Miuix components for dialog (or Compose foundation AlertDialog)
- No new dependencies

---

### Task 1: Add platform-aware import button labels

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ImportLabels.kt`
- Create: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ImportLabels.android.kt`
- Create: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ImportLabels.ios.kt`
- Create: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ImportLabels.jvm.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Produces:** expect/actual strings for button labels, iOS uses "Scan for Music"

- [ ] **Step 1: Create ImportLabels.kt (commonMain expect)**

```kotlin
package com.eterocell.rhythhaus

expect val importCardTitle: String
expect val importCardTitleWithTracks: String
expect val importCardDescription: String
```

- [ ] **Step 2: Create platform actuals**

**Android:**
```kotlin
package com.eterocell.rhythhaus
actual val importCardTitle: String = "Add music folder"
actual val importCardTitleWithTracks: String = "Manage music folders"
actual val importCardDescription: String = "Choose a music folder on this device to build your local library."
```

**iOS:**
```kotlin
package com.eterocell.rhythhaus
actual val importCardTitle: String = "Scan for Music"
actual val importCardTitleWithTracks: String = "Manage Music"
actual val importCardDescription: String = "Scan the app's music folder for tracks."
```

**JVM:**
```kotlin
package com.eterocell.rhythhaus
actual val importCardTitle: String = "Add music folder"
actual val importCardTitleWithTracks: String = "Manage music folders"
actual val importCardDescription: String = "Choose a music folder on this device to build your local library."
```

- [ ] **Step 3: Replace hardcoded strings in App.kt ImportAudioCard**

Replace line 430:
```kotlin
text = if (hasImportedTracks) "Manage music folders" else "Add music folder",
```
with:
```kotlin
text = if (hasImportedTracks) importCardTitleWithTracks else importCardTitle,
```

Replace line 437-440 description with:
```kotlin
text = importMessage ?: importCardDescription,
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :shared:compileKotlinJvm :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Expected: SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: platform-aware import button labels, iOS shows 'Scan for Music'"
```

---

### Task 2: Add clearAll() to repository

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`

**Produces:** `clearAll()` method on repository interface + SQLDelight impl

- [ ] **Step 1: Add clearAll() to LibraryRepository interface**

```kotlin
interface LibraryRepository {
    // ... existing methods ...
    fun clearAll()
}
```

- [ ] **Step 2: Implement in InMemoryLibraryRepository**

```kotlin
override fun clearAll() {
    sources.clear()
    tracks.clear()
    scanSessions.clear()
    scanErrors.clear()
}
```

- [ ] **Step 3: Implement in SqlDelightLibraryRepository**

Add SQL queries to .sq files or use raw SQL. Simplest: add DELETE statements to the existing .sq files.

Add to `LibraryTrack.sq`:
```sql
clearAllTracks:
DELETE FROM library_track;
```

Add to `LibrarySource.sq`:
```sql
clearAllSources:
DELETE FROM library_source;
```

Add to `ScanSession.sq`:
```sql
clearAllSessions:
DELETE FROM scan_session;
```

Add to `ScanError.sq`:
```sql
clearAllErrors:
DELETE FROM scan_error;
```

Then in `SqlDelightLibraryRepository.kt`:
```kotlin
override fun clearAll() {
    database.libraryTrackQueries.clearAllTracks()
    database.librarySourceQueries.clearAllSources()
    database.scanSessionQueries.clearAllSessions()
    database.scanErrorQueries.clearAllErrors()
}
```

- [ ] **Step 4: Verify compilation + tests**

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Expected: SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add clearAll() to LibraryRepository for database reset"
```

---

### Task 3: Add "Clear Library" button with confirmation dialog

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Consumes:** `repository.clearAll()` from Task 2

**Produces:** "Clear Library" button + AlertDialog confirmation

- [ ] **Step 1: Add showClearDialog state in App()**

```kotlin
var showClearDialog by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Add "Clear Library" button below ImportAudioCard**

In `LibraryHomeScreen`, below the `ImportAudioCard` block, add:

```kotlin
if (libraryTracks.isNotEmpty()) {
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { showClearDialog = true },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        cornerRadius = 12.dp,
        colors = ButtonDefaults.buttonColors(
            color = HausPulse.copy(alpha = 0.15f),
            contentColor = HausPulse,
        ),
    ) {
        Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
```

- [ ] **Step 3: Add AlertDialog for confirmation**

In `LibraryHomeScreen`, at the end of the composable (before closing brace):

```kotlin
if (showClearDialog) {
    AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("Clear Library") },
        text = { Text("This will remove all scanned tracks. Your music files are not deleted. Continue?") },
        confirmButton = {
            TextButton(onClick = {
                repository.clearAll()
                libraryTracks = emptyList()
                showClearDialog = false
            }) {
                Text("Clear", color = HausPulse)
            }
        },
        dismissButton = {
            TextButton(onClick = { showClearDialog = false }) {
                Text("Cancel")
            }
        },
    )
}
```

Requires imports: add `import androidx.compose.ui.window.AlertDialog` and `import com.eterocell.rhythhaus.library.TextButton`.

Actually, check if Miuix has a dialog. If not, add `implementation(libs.compose.material3)` back to `shared/build.gradle.kts` for AlertDialog only, OR use a simple `Dialog` from Compose foundation.

Simpler: use Compose foundation `Dialog` composable which doesn't need material3:

```kotlin
import androidx.compose.ui.window.Dialog
```

Use `Dialog(onDismissRequest = ...) { Card { ... } }` instead of AlertDialog.

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Clear Library button with confirmation dialog"
```
