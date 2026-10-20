package com.eterocell.rhythhaus.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Shared text styles for Studio surfaces. */
public object HausTypography {
    /** Large page title. */
    public val pageTitle: TextStyle =
        TextStyle(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
        )

    /** Compact page title. */
    public val compactTitle: TextStyle =
        TextStyle(
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )

    /** Section heading. */
    public val section: TextStyle =
        TextStyle(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )

    /** Song or content title. */
    public val title: TextStyle =
        TextStyle(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        )

    /** Standard body text. */
    public val body: TextStyle =
        TextStyle(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
        )

    /** Secondary descriptive text. */
    public val secondary: TextStyle =
        TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        )

    /** Supporting labels. */
    public val label: TextStyle =
        TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
}
