package com.eterocell.rhythhaus.onboarding

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
internal actual fun currentOnboardingPlatform(): OnboardingPlatform =
    if (Platform.osFamily == OsFamily.IOS) OnboardingPlatform.IOS
    else OnboardingPlatform.MacOS
