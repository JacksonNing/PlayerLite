package com.wxy.playerlite.feature.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun shouldUseLightStatusBarContent(backgroundColor: Color): Boolean {
    return backgroundColor.luminance() < 0.45f
}

fun detailHeroContentColor(backgroundColor: Color): Color {
    return if (shouldUseLightStatusBarContent(backgroundColor)) {
        Color.White
    } else {
        Color.Black
    }
}
