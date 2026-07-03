# Design: Navigation Animations

## Route transition state

Extend the shared navigation layer with a small transition classification that records the last navigation action. The route stack remains the source of truth; the transition state only tells the UI how to animate the current route change.

The transition kinds are:

- `Push`: a route was added on top of the stack.
- `Pop`: the stack returned to the previous route.
- `Replace`: the top route changed without growing the stack.
- `Root`: navigation returned to Home.
- `None`: no route change.

This logic belongs next to `LibraryNavigationStack` so it can be tested without Compose runtime.

## Root animation wrapper

Wrap the `LibraryHomeScreen` route switch in a root-level `AnimatedContent` keyed by `navigation.current`. The transition spec chooses enter/exit animation from the last transition kind:

- `Push`: fade in plus slide in from the right; old route fades/slides slightly left.
- `Pop`: fade in plus slide in from the left; dismissed route fades/slides right.
- `Replace`: fade transition with minimal movement.
- `Root`: pop-like return toward Home.
- `None`: no route-motion special case beyond Compose's stable state.

Use conservative durations around 220-260ms. This is fast enough for frequent route changes but visible enough to add spatial continuity.

## Existing behavior preservation

Visible back chips, left-edge swipe-back, and Android system/predictive back continue to call the same pop function. Push callbacks continue to call the same route push function. The only new behavior is the visual transition between route states.

Do not add dependencies. Use Compose animation APIs already available through existing Compose Multiplatform dependencies.

## Verification

Automated verification:

- `openspec validate navigation-animations --strict`
- Focused common navigation tests for transition classification.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`

Manual follow-up:

- On Android/iOS/macOS, navigate Home → album/artist detail → Search/Settings/Now Playing and back. Pushes should move forward; backs should reverse direction. No route should feel stuck or lose playback state.
