package com.markus.clock.model

import androidx.annotation.DrawableRes

/**
 * Data class representing a wallpaper background image for the clock
 */
data class BackgroundImage(
    val id: Int,
    val name: String,
    @DrawableRes val resourceId: Int
) 