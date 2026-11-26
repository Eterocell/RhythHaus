package com.eterocell.rhythhaus.baselineprofile

import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

private const val TIMEOUT_MS = 10_000L
private val fixtureTitlePattern = Pattern.compile("RhythHausBenchmark[ -][0-9]{3}")
private val firstTitlePattern = Pattern.compile("RhythHausBenchmark[ -]001")

internal fun targetPackage(): String {
    check(Build.VERSION.SDK_INT >= 34) { "Use a disposable Android API 34+ target." }
    return checkNotNull(
        InstrumentationRegistry.getArguments().getString("rhythhausTargetPackage")
            ?.takeIf(String::isNotBlank),
    ) { "Pass rhythhausTargetPackage from the target APK's application ID; see baselineprofile/README.md." }
}

internal fun MacrobenchmarkScope.launchReadyLibrary() {
    startActivityAndWait()
    awaitResource("rhythhaus-library-ready", "Finish onboarding and resolve permission prompts.")
}

internal fun MacrobenchmarkScope.prepareSongs() {
    val countLabel = awaitResource("library-track-count", "Open the prepared Library.").text.orEmpty()
    val count = countLabel.mapNotNull { Character.digit(it, 10).takeIf { digit -> digit >= 0 } }
    check(count == listOf(2, 5, 6)) {
        "Expected only the 256-track RhythHausBenchmark folder; visible count label: '$countLabel'."
    }
    awaitResource("rhythhaus-library-songs", "The Songs browse control is not visible.").click()
    val list = awaitSongsList()
    check(list.wait(Until.hasObject(By.text(firstTitlePattern)), TIMEOUT_MS) == true) {
        "The first fixture track is not visible. Import Music/RhythHausBenchmark and start at the top of Songs."
    }
}

internal fun MacrobenchmarkScope.flingSongs() {
    val list = awaitSongsList()
    val before = list.visibleFixtureTitles()
    check(before.isNotEmpty()) { "The Songs list is not showing any fixture tracks before the fling." }
    list.setGestureMargin(list.visibleBounds.width() / 5)
    list.fling(Direction.DOWN)
    check(device.wait({ device ->
        if (device.currentPackageName != packageName) return@wait false
        try {
            device.findObject(By.res("rhythhaus-library-songs-list").pkg(packageName))
                ?.visibleFixtureTitles()?.any { it !in before } == true
        } catch (_: StaleObjectException) {
            false // Lazy layout may replace the node during the fling.
        }
    }, TIMEOUT_MS) == true) {
        "The Songs fling did not reveal another fixture track. Check the list and device overlays."
    }
}

private fun MacrobenchmarkScope.awaitSongsList(): UiObject2 =
    awaitResource("rhythhaus-library-songs-list", "Select Songs in the prepared Library.")

private fun MacrobenchmarkScope.awaitResource(id: String, recovery: String): UiObject2 {
    val node = device.wait(Until.findObject(By.res(id).pkg(packageName)), TIMEOUT_MS)
    check(node != null && device.currentPackageName == packageName) {
        "Expected $id in $packageName; foreground is ${device.currentPackageName}. $recovery"
    }
    return node
}

private fun UiObject2.visibleFixtureTitles(): Set<String> =
    findObjects(By.text(fixtureTitlePattern)).mapNotNullTo(mutableSetOf()) { it.text }
