# Design: Android notification permission recovery

`MainActivity` owns `checkSelfPermission`, `shouldShowRequestPermissionRationale`, `ActivityResultContracts.RequestPermission`, and `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. It maps those APIs to an Android-free Shared contract: `Unavailable`, `Granted`, `Requestable`, or `SettingsRequired`; the default controller is unavailable so iOS/desktop call sites remain unchanged.

The Activity issues one startup request only while none is active; permission results and `onResume` refresh state. Requestable invokes the launcher; permanent denial opens the app notification settings intent when resolvable.

Shared maps only denied states into an optional Settings recovery projection and forwards request/settings callbacks through its route composition. Settings receives scalar content/action mode, owns English/Chinese strings, and renders one accessible recovery card. It is not gated by library mutations because it changes no library or playback state.

Denied notifications never pause local playback, modify queues, or disable in-app transport. Returning from Android settings recomputes state and removes the card once granted.

Tests: pure Android policy; Shared route forwarding; Settings policy and JVM semantics for absence/request/settings-required state, action dispatch, and active library mutation. Manual Android 13+ acceptance remains a closeout gate.
