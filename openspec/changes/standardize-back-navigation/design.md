# Design: Standardized Back Navigation

## Back affordance

Use a single shared Compose back chip across the shared UI:

- Text: `‹ Back`
- Shape: same rounded chip style as existing controls
- Colors: active `HausColors.current.ink` background and `HausColors.current.paper` foreground
- Typography: small, bold, consistent letter spacing
- Accessibility: semantic content description such as `Back`

The copy intentionally avoids destination-specific labels (`Library`, `Search`, `Settings`) because routes can now be opened from multiple origins. The route stack is the source of truth for where back returns.

## System and gesture back

The existing explicit `LibraryNavigationStack` owns in-app pop behavior. Android system/predictive back, visible back chips, and shared left-edge swipe-back gestures should call the same pop callback.

Use Compose Multiplatform's `PredictiveBackHandler` at the navigation-stack owner level when `navigation.canPop` is true. The handler consumes completed predictive/system back and pops one route. It does not add custom progress animation in this change; Android's system predictive-back gesture pipeline is supported by registering the predictive handler and manifest opt-in.

The Android manifest should explicitly set `android:enableOnBackInvokedCallback="true"` on the main activity so Android 13+ predictive back is enabled for the app.

## Scope

- Shared UI files that render visible back labels.
- Root navigation owner where Android/system back is registered.
- Android manifest opt-in.
- No playback, scanner, library persistence, or theme behavior changes.

## Verification

Automated verification:

- `openspec validate standardize-back-navigation --strict`
- `./gradlew :shared:compileKotlinJvm --configuration-cache`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`

Manual follow-up:

- On Android 13+ emulator/device, open a nested route and use gesture back. The app should preview/complete an in-app pop instead of closing from nested routes.
