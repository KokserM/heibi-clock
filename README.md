# Markus Clock

A beautiful and functional timer application for Android that visualizes time using an elegant analog clock interface.

<img src="screenshots/main.jpg" width="300">

## Overview

Markus Clock is a unique timer application that helps you visualize time periods using a traditional analog clock face. Instead of using a countdown or progress bar, Markus Clock shows your scheduled time as arcs on a clock face, making it easier to understand the time period in context with your day.

## Features

- **Visual Time Representation**: See your scheduled time period as colored arcs on an analog clock face
- **Activity Types**: Set different activities (Play or Sleep) with different color schemes
- **Customizable Wallpapers**: Choose from various background wallpapers to personalize your experience
- **Intuitive Interface**: Easy-to-use time selectors and controls
- **End Time Notification**: Subtle zoom animation and clear notification when your timer ends
- **Real-time Clock**: Always shows the current time with smooth animated clock hands

## How It Works

1. **Select Activity Type**: Choose between "Play" or "Sleep" modes (or customize with your own activities)
2. **Set Time Range**: Set your start and end times using the intuitive time picker
3. **Start Timer**: Press the start button to begin tracking your time period
4. **Visual Feedback**: Watch as the colored arcs show your progress through the time period
5. **Timer End**: When the time is up, the clock will animate with a subtle zoom effect and display "TIME'S UP!"

## Technical Details

Markus Clock is built with:
- Kotlin and Jetpack Compose for modern, declarative UI
- MVVM architecture pattern for clean separation of concerns
- Coroutines for smooth animations and asynchronous operations
- Material Design 3 components for a modern look and feel

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Build and run on your Android device or emulator

## Customization

You can extend Markus Clock by:
- Adding new activity types in `ActivityType.kt`
- Creating new wallpapers by adding images to the resources and updating `Wallpapers.kt`
- Customizing animations and colors in the theme files

## Future Enhancements

- Multiple simultaneous timers
- Calendar integration
- Notification support
- Custom sounds for timer completion
- Additional themes and visual styles

## License

[MIT License](LICENSE)

## Credits

A beautiful way to visualize and manage your time. 