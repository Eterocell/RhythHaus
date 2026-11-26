package com.eterocell.rhythhaus.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()

    @Test
    fun coldStartup() {
        val target = targetPackage()
        profile.collect(target, outputFilePrefix = "cold-startup", includeInStartupProfile = false) {
            pressHome()
            launchReadyLibrary()
        }
    }

    @Test
    fun firstSongsScroll() {
        val target = targetPackage()
        profile.collect(target, outputFilePrefix = "first-songs-scroll", includeInStartupProfile = false) {
            pressHome()
            launchReadyLibrary()
            prepareSongs()
            flingSongs()
        }
    }
}
