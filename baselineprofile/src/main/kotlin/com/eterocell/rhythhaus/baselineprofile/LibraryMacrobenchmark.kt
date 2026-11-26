package com.eterocell.rhythhaus.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryMacrobenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test fun coldStartupNone() = coldStartup(CompilationMode.None())

    @Test fun coldStartupWithProfile() = coldStartup(profileRequired())

    @Test fun firstScrollNone() = firstScroll(CompilationMode.None())

    @Test fun firstScrollWithProfile() = firstScroll(profileRequired())

    private fun coldStartup(compilation: CompilationMode) =
        benchmark.measureRepeated(
            packageName = targetPackage(),
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
        ) { launchReadyLibrary() }

    private fun firstScroll(compilation: CompilationMode) =
        benchmark.measureRepeated(
            packageName = targetPackage(),
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilation,
            iterations = 5,
            setupBlock = {
                killProcess()
                launchReadyLibrary()
                prepareSongs()
            },
        ) { flingSongs() }

    private fun profileRequired(): CompilationMode =
        CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 0,
        )
}
