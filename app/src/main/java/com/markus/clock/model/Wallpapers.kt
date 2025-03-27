package com.markus.clock.model

import com.markus.clock.R

/**
 * Contains a list of all available wallpapers for the app
 */
object Wallpapers {
    // Get all available wallpapers
    fun getWallpapers(): List<BackgroundImage> = listOf(
        BackgroundImage(
            id = 1,
            name = "Gaming",
            resourceId = R.drawable.gaming
        ),
        BackgroundImage(
            id = 2,
            name = "Kirby",
            resourceId = R.drawable.kirby
        ),
        BackgroundImage(
            id = 3,
            name = "Mario Party",
            resourceId = R.drawable.mario_party
        ),
        BackgroundImage(
            id = 4,
            name = "Letters",
            resourceId = R.drawable.letters
        )
    )
    
    // Get a wallpaper by its ID
    fun getWallpaperById(id: Int): BackgroundImage {
        return getWallpapers().find { it.id == id } ?: getWallpapers().first()
    }
} 