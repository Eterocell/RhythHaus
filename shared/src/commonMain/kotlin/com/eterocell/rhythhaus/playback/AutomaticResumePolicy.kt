package com.eterocell.rhythhaus.playback

/**
 * Controls whether playback may follow a platform's interruption resume
 * recommendation.
 */
public enum class AutomaticResumePolicy {
    FollowSystemRecommendation,
    Never,
}
