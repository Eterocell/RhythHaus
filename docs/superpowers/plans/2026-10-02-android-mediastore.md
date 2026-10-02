# Implementation plan: Android MediaStore source

1. Establish the `AndroidMediaStoreAudio` contract and permission projection with failing common tests, then add the enum/source identity without affecting SAF.
2. Implement the Android `ContentResolver` adapter using a narrow query projection and cursor-safe scan sequence. Add host tests for permission API branching, filtering, stable row identity, cancellation and resource closure.
3. Add Shared orchestration for explicit registration and scanning, plus Android host callbacks for permission request and app settings. Preserve no startup prompt and source-manager mutation gates.
4. Add localized Settings UI and accessibility semantics for unavailable/available/recovery states; verify compact and split layouts.
5. Run focused database/library/Shared/Android checks, formatting, Detekt, architecture, and OpenSpec validation. Perform independent review, sync specs, archive, update `roadmap.md` and `progress.md`, and commit.

Non-goals remain SAF replacement, media mutation, broad permission requests, and non-Android platform support.
