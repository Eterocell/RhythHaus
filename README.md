# RhythHaus

RhythHaus is a local-first music player for Android, iOS, and macOS/desktop JVM. It scans and plays local audio, manages playlists, and keeps playback and library data on-device. Windows and Linux are not currently supported targets.

## Project layout

| Area | Responsibility |
| --- | --- |
| `androidApp`, `iosApp`, `desktopApp` | Thin platform entry points and packaging. |
| `shared` | App composition, cross-feature shell/navigation, lifecycle, and iOS framework facade. |
| `core/model`, `core/ui`, `core/platform`, `core/playback`, `core/database` | Cross-feature models, shared UI/theme, platform contracts, playback, and physical database ownership. |
| `feature/library`, `feature/playlists`, `feature/search`, `feature/nowplaying`, `feature/settings`, `feature/onboarding` | Feature contracts and implementations, owned by their respective modules. |
| `taglib`, `architecture-processor`, `build-logic` | Audio metadata support, architecture verification, and shared Gradle conventions. |
| `openspec` | Durable product/architecture requirements and active change tasks. |

See [architecture guidance](docs/architecture.md) and [ADRs](docs/adr/) for module ownership and dependency rules.

## Run the apps

- Android: open the project in Android Studio or assemble a debug APK with `./gradlew :androidApp:assembleDebug`.
- Desktop: `./gradlew :desktopApp:run`.
- iOS: open [`iosApp`](iosApp/) in Xcode and run the `iosApp` scheme.

## Verify changes

The repository's first-platform verification entry point is:

```bash
./init.sh
```

It runs the Shared JVM tests, desktop compilation, Android debug assembly, Xcode version check, and Shared iOS Simulator tests. For focused work, run the owning module's test task plus the affected app compile/build task. Examples:

```bash
./gradlew :shared:jvmTest --configuration-cache
./gradlew :feature:library:impl:jvmTest --configuration-cache
./gradlew :desktopApp:compileKotlin --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

A successful compile or automated test does not establish user-only device, picker, accessibility, system-media, or audible-playback acceptance. Current priorities and exact outstanding release evidence are in [`roadmap.md`](roadmap.md); workflow and handoff requirements are in [`AGENTS.md`](AGENTS.md) and [`docs/harness-engineering.md`](docs/harness-engineering.md).
