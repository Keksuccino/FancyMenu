# FancyMenu Video Player

This package contains a Minecraft video player implementation based on MCEF (Minecraft Chromium Embedded Framework).

## Features

- Play videos within Minecraft using Chromium
- No visible UI controls (clean video playback)
- Java hook interface for controlling videos programmatically
- Configurable options via URL parameters or API:
  - Volume control
  - Loop control
  - Aspect ratio preservation with fill screen mode

## Requirements

- MCEF must be installed and loaded
- The HTML/JS player files must be available in the assets

## Usage Example

```java
// Check if video playback is available
VideoManager videoManager = VideoManager.getInstance();
if (!videoManager.isVideoPlaybackAvailable()) {
    // MCEF is not loaded
    return;
}

// Create a video player
String playerId = videoManager.createPlayer(100, 100, 640, 360);
MCEFVideoPlayer videoPlayer = videoManager.getPlayer(playerId);

// Configure player
videoPlayer.setVolume(0.7f);  // 70% volume
videoPlayer.setLooping(true); // Loop the video
videoPlayer.setFillScreen(true); // Fill the screen while preserving aspect ratio

// Load and play a video
videoPlayer.loadVideo("path/to/video.mp4");
videoPlayer.play();

// Later when done with the player
videoManager.removePlayer(playerId);
```

## Rendering

In your rendering code:

```java
// In your render method
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    // Render other UI elements...
    
    // Render the video player
    videoPlayer.render(graphics, mouseX, mouseY, partialTick);
    
    // Continue rendering...
}
```

## Video Controls API

The video player supports various control methods:

- `play()` - Start or resume playback
- `pause()` - Pause playback
- `stop()` - Stop playback and reset to beginning
- `setVolume(float)` - Set volume (0.0 to 1.0)
- `setLooping(boolean)` - Enable/disable looping
- `setFillScreen(boolean)` - Enable/disable fill screen mode
- `setCurrentTime(double)` - Seek to a specific time (in seconds)
- `getDuration()` - Get the total duration of the video
- `getCurrentTime()` - Get the current playback position
- `isPlaying()` - Check if the video is currently playing
- `getVideoWidth()` - Get the natural width of the video
- `getVideoHeight()` - Get the natural height of the video

## URL Parameters

When creating a video player, you can provide initial settings via URL parameters:

- `volume` - Initial volume (0.0 to 1.0)
- `loop` - Initial loop state (true/false)
- `fillScreen` - Initial fill screen mode (true/false)
- `autoPlay` - Whether to auto-play videos (true/false)
- `video` - URL of video to load initially

These parameters are automatically applied when the player loads.
