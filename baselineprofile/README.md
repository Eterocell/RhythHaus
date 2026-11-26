# Android Baseline Profiles

`:baselineprofile` records cold startup into a restored Library and the first fling in Library → Songs. Collection uses `nonMinifiedRelease`; paired measurements use `benchmarkRelease` (R8 on, app non-debuggable). Both local variants keep the release application ID, are shell-profileable, and use the workstation's debug key. The normal release neither uses that key nor needs a device to consume checked-in rules.

## Prepare a disposable device

1. Use a single disposable API 34+ target with no personal RhythHaus installation. A stock emulator can collect rules and validate journeys; report timings only from a low-end physical device or `aosp_cf_x86_64_phone-userdebug` (label the latter a Cuttlefish reference).
2. Build `:androidApp:assembleNonMinifiedRelease`, then install only that variant. Run the Gradle commands below through the repository's `gradle-run` workflow when using an agent. For all ADB commands, set `serial` to the disposable device and keep any other targets disconnected for the connected Gradle tasks.

   ```bash
   sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
   apk=$(find androidApp/build/outputs/apk/nonMinifiedRelease -name '*.apk')
   target=$("$sdk/cmdline-tools/latest/bin/apkanalyzer" manifest application-id "$apk")
   adb -s "$serial" install "$apk"
   ```

3. Generate and transfer the fixture into a fresh `Music/RhythHausBenchmark` folder. The script refuses to overwrite a host directory.

   ```bash
   fixture=$(mktemp -d)
   python3 baselineprofile/prepare-fixture.py "$fixture/RhythHausBenchmark"
   adb -s "$serial" shell mkdir -p /sdcard/Music
   adb -s "$serial" push "$fixture/RhythHausBenchmark" /sdcard/Music/
   ```

4. Launch the app, handle any notification prompt, and finish or skip onboarding. In Library, add the fixture folder through the system folder picker; wait for the scan. The Library must show exactly 256 tracks, with `RhythHausBenchmark 001` first in Songs at default title order. Do not clear app data between locally signed variants. Android device selection and SAF permissions must refer to the disposable fixture only.

## Collect and verify app-owned rules

```bash
./gradlew :androidApp:generateReleaseBaselineProfile \
  "-Pandroid.testInstrumentationRunnerArguments.rhythhausTargetPackage=$target"
```

The output is `androidApp/src/release/generated/baselineProfiles/baseline-prof.txt`. Preserve per-journey `cold-startup-baseline-prof.txt` and `first-songs-scroll-baseline-prof.txt` from the instrumentation output. Confirm app-owned startup and scroll descriptors in those captures. The generator does not emit build-time Startup Profile rules. Missing fixture, an obstructing system dialog, or a fling that reveals no new fixture track fails the run rather than producing accepted coverage.

With no device, run `:androidApp:verifyReleaseApks`, `:androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true`, and `:androidApp:verifyReleaseAab` as separate Gradle invocations. Keep the ordinary release APK and its matching R8 `mapping.txt` before building another variant. Check `assets/dexopt/baseline.prof` and `assets/dexopt/baseline.profm` in the APK and profile entries under `BUNDLE-METADATA/com.android.tools.build.profiles/` in the AAB. Decode the ordinary release profile with the SDK's Profgen:

```bash
unzip -p "$release_apk" assets/dexopt/baseline.prof > "$output/baseline.prof"
"$sdk/cmdline-tools/latest/bin/profgen" dumpProfile \
  --apk "$release_apk" --profile "$output/baseline.prof" \
  --map androidApp/build/outputs/mapping/release/mapping.txt \
  --output "$output/release-profile.txt"
```

Record the SDK command-line tools version, APK sha256 and matched RhythHaus method descriptors from both journeys. An asset listing or dependency-only profile is not app-owned coverage.

## Compare compiled and uncompiled runs

Use a qualifying device and the same prepared catalog for both modes. Assemble the R8 benchmark APK, read its application ID, and verify the APK includes the generated profile. Then run:

```bash
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.rhythhausTargetPackage=$target" \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

Keep the JSON and Perfetto traces from `baselineprofile/build/outputs/connected_android_test_additional_output/` outside version control. Report APK and rule-file sha256, device/API/build, artifact paths, 10 TTID and TTFD samples per mode with their medians, and P50/P90/P95/P99 for frame CPU duration and frame overrun across five fling iterations per mode. Missing TTFD, no scroll motion, or an unsuccessful profile-required run invalidates the comparison. These numbers compare the **merged** profile with no AOT; they do not isolate app-owned rules from library rules. Report the observed direction, including slower or inconclusive results.

## Initial collection evidence — 2026-11-20

- `RhythHausProfileApi34` (Android 14, arm64, Google Play emulator): imported all 256 tracks through SAF. Both `BaselineProfileGenerator` tests passed. An empty-Library control failed with `Expected only the 256-track RhythHausBenchmark folder; visible count label: '0 tracks'.`
- Per-journey captures are under `build/outputs/connected_android_test_additional_output/nonMinifiedRelease/connected/` in this module. SHA-256: `cold-startup-baseline-prof.txt` = `3f74254c49210341e60c9ecd0a399e052316b756d15950223734d6136f159e31`; `first-songs-scroll-baseline-prof.txt` = `22602cb57373bc55be3cf5f6f247d5c998fc6178ad2caf65316ebcfa698f05c1`. The versioned merged file has 25,097 rules and SHA-256 `18b761e2aecdf99fe936a0b0b6f932e52f485a8b7ad8cecc020446a819aca102`.
- Profgen (Android SDK command-line tools 23.0), using `dumpProfile` as above, decoded ordinary release APK SHA-256 `57f89e9df6b3476348f42ed59310124bcc875beaa3bcc1d5ee021d868cc7e248`. Of the decoded RhythHaus method descriptors, 364 matched the startup capture and merged input; five matched the scroll capture and merged input but not the startup capture. Examples: `MainActivity.onCreate(Bundle)` and `LibraryScrollPosition.equals(Object)` respectively. The ordinary APK, split APK and AAB release contract checks passed with no online device.
- No qualifying measurement device was available after collection. The user waived paired timing for this change. The Pixel 7 Pro attempt below did not produce timing data.

## Pixel 7 Pro benchmark attempt — 2026-11-20

- Authorized device-specific run: Pixel 7 Pro (`cheetah`), Android 17 / API 37, build `CP4A.261105.040.A1`, build ID `16581829`, `user/release-keys`. The normal system picker imported all 256 tracks. The R8 benchmark APK SHA-256 was `088fe7609ecb188ec2311d90c5dc322c97eed5bcd3bb8779189145ebaf3f95e`; it contained both binary profile assets and used the versioned rules above.
- The connected Macrobenchmark command above exited 1. `coldStartupWithProfile` failed before measurement while `androidx.benchmark.ShellImpl` initialized `UiAutomation`: `IllegalStateException: UiAutomationService … already registered!`. The other benchmark cases failed with `NoClassDefFoundError: androidx.benchmark.ShellImpl`; the instrumentation process crashed. Neither compilation mode produced valid timing samples. The binary assets do not establish that profile-required compilation ran.
- Raw JUnit XML and per-test logcat are retained under `baselineprofile/build/pixel7pro-EzFgTb/failed-connected/` from the repository root. After USB was restored, both fresh `am instrument` runs selecting only `LibraryMacrobenchmark#coldStartupWithProfile` reproduced the same registration failure. The second used no UI inspection immediately beforehand. Both crashed before sampling and produced no JSON or traces. Instrumentation output: `baselineprofile/build/pixel7pro-EzFgTb/retry-cold-profile.txt` and `retry-cold-profile-quiescent.txt` in the same directory. No valid result was recorded for either compilation mode.
