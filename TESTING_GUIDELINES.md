# Testing Guidelines

## Off-screen test clients

Test clients must never show their window on screen or take focus away from the user. FancyMenu has a dev-only
off-screen mode for that, so always launch test clients through `offscreen_client.sh`:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) sh offscreen_client.sh fabric
JAVA_HOME=$(/usr/libexec/java_home -v 25) sh offscreen_client.sh neoforge
```

- The script runs the loader's client task (`runClient`, or `Client` for Forge) with the `FANCYMENU_TEST_OFFSCREEN=true` environment variable.
  Extra arguments get passed on to Gradle.
- By default the client uses the `run_client` game directory. To test with a separate, clean game instance, set `FANCYMENU_TEST_RUN_DIR`
  to a folder name in the project root, for example `FANCYMENU_TEST_RUN_DIR=run_client_test`. The script passes it to the loader build
  scripts as the `fancymenu_client_run_dir` Gradle property. Rinku keeps its Chromium libraries (~360 MB) in the game directory and
  rejects a symlinked `rinku-libraries` folder as unsafe, so clone it into the new folder before its first launch:
  `cp -cR run_client/rinku-libraries <new folder>/` (an APFS clone, which takes no extra space).
- It sets `use-cache=false` in `<game directory>/config/rinku/rinku.properties` for the run, so Rinku's Chromium uses CEF's default profile
  instead of the user's persistent Rinku profile, and it clears the crash mark a killed test run leaves in that profile.
  Chromium would otherwise open a visible "Restore pages?" window.
- Off-screen clients of all workspaces run one at a time, guarded by a lock in `$TMPDIR`. A Chromium starting while another one uses
  the same profile makes the running one open a visible "New Tab" window. The script compiles the loader before waiting for the lock.
- For (Neo)Forge, it also sets `earlyWindowControl = false` in `<game directory>/config/fml.toml` for the run.
  The early loading window opens before any mod code runs, so the FML config is the only way to keep it closed.
- Both files get restored when the script exits.
- The game log contains `Off-screen test client mode is active!` when the mode works.

### How it works

- `util/window/OffscreenTestClient` reads the environment variable. The mode only works in a development environment.
- `MixinRenderSystem` makes SDL run the game as macOS background app, so it gets no Dock icon and can't get activated.
- `MixinWindow` creates the game window with the `SDL_WINDOW_HIDDEN` flag, and keeps it windowed, because SDL shows fullscreen windows.

### Writing in-game tests

- The game still renders into its render targets, so screenshots (`Screenshot.grab()`, `Screenshot.takeScreenshot()`) work as usual.
- The hidden window gets no input from the OS. Simulate input through the game's real input handlers instead,
  for example `KeyboardHandler#keyPress` with the key of the `KeyMapping` you want to trigger.
- There is no window to close, so test code has to call `Minecraft#stop()` when it's done. If a run hangs, stop its Gradle process.

## Recording showcase GIFs

Showcase GIFs for the docs (like the one of the Profiles feature) get recorded by a temporary automation class in an off-screen client,
then the cursor gets drawn onto the frames and the frames get encoded. The automation is no permanent part of the mod, so write a new one
for each showcase, following what worked before:

### Instance

- Record in a separate game instance (`FANCYMENU_TEST_RUN_DIR`, see above), so the showcase starts clean and the dev instance stays untouched.
  Give its `options.txt` `onboardAccessibility:false`, `panoramaScrollSpeed:0.0` (a moving panorama changes every pixel of every
  frame, which makes GIFs huge) and `soundCategory_master:0.0`.
- The first launch of an instance opens the "Welcome to FancyMenu!" window, which blocks all other input. Close all PiP windows
  (`PiPWindowHandler.INSTANCE.getOpenWindows()`) before recording starts.
- A take changes the instance (profiles, layouts, options). After the first launch, clone `config/fancymenu` and `fancymenu_data`
  into a snapshot folder and restore them before every take, so all takes start from the same state.
- If the showcase switches to other FancyMenu options (profiles bring their own `options.txt`), give the instance the same
  `ui_scale_v2` and `enable_ui_blur` values, otherwise the whole UI jumps to another size mid-GIF.

### Automation

- Register a listener for `ClientTickEvent.Pre` from the client init when an environment variable is set, and run a script of small
  steps (wait, wait until, move, click, type, drag) on it. Log every step as `[CHECK] name: PASS/FAIL`, so failed takes are easy to spot.
- Size the window with `Window#setWindowed(1280, 720)`. On Retina displays its framebuffer is 2560x1440, so set the GUI scale to
  `2 * Window#getPixelDensity()` and call `Minecraft#resizeGui()` to get the GUI of a 1280x720 window at GUI scale 2.
- Simulate input through the real handlers, with positions in window coordinates (GUI coordinates times `getScreenWidth()` divided by
  `getGuiScaledWidth()`):
  - `MouseHandler#onMove(handle, x, y, dx, dy)` every tick while the cursor moves, on an eased and slightly curved path.
  - `MouseHandler#onButton(handle, new MouseButtonInfo(button, 0), 1 or 0)`, with SDL button numbers: 1 is left, 3 is right.
    Release the button one tick after pressing it.
  - `KeyboardHandler#charTyped(handle, new CharacterEvent(codepoint))` for typing. Select all is
    `keyPress(handle, 1, new KeyEvent(InputConstants.KEY_A, 'a', InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER))`, then the same with action 0.
  - File drag and drop: `FileDragTracker.onDragBegin()`, then `onDragPosition(x, y)` every tick, then `onDragEnd()` and
    `MouseHandler#onDrop(handle, paths)`. Don't move the mouse while dragging, because SDL doesn't either.
- Resolve click targets from the widget objects when the move starts, never from hardcoded positions. Reflection is fine in temporary code:
  - Menu bar entries (`MenuBar#getEntry`) have `x`, `y`, `height` and `getWidth()` in the space of `MenuBar.getRenderScale()`.
  - Context menu entries (`ContextMenu#getEntry`) have `x`, `y` and `width` in the space of `UIBase.calculateFixedRenderScale(menu.getScale())`.
    The open context menu is `ContextMenuHandler.INSTANCE.getCurrent()`.
  - Widgets of a PiP window body are in body coordinates: GUI position = `getBodyX()` + local position * `getScreenRenderScaleFactor()`
    (private). The window close button is at `getCloseButtonX()`/`getButtonY()` (private).
  - FancyMenu's `UIIconButton` is no `AbstractWidget`, so read the bounds through its own `getX()`, `getY()`, `getWidth()`, `getHeight()`.

### Frames

- Capture in `ClientTickEvent.Pre`: at that point the main render target still holds the last completely rendered frame, including
  the deferred GUI pass. Capturing during screen rendering misses the GUI. Every second tick gives 10 fps, which is enough for UI showcases.
- `Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), callback)` reads back asynchronously, so write the PNGs on
  a thread pool. Store the cursor state that was active when the captured frame got rendered (the state at the end of the previous tick)
  as one JSON line per frame.
- The readbacks finish in later frames. Calling `Minecraft#stop()` while one is pending crashes the game on close
  ("Cannot close a surface while it is acquired"), so keep ticking until all frames got written, then stop.

### Post-processing and encoding

- Draw the cursor afterwards, with hand-made pixel art sprites: scale the frames down to 1280x720 first (Lanczos), then paste the sprites
  at integer positions, so they stay crisp. Click ripples and the dragged file (icon, copy badge, file name) get drawn there too.
- GIFs store the changed rectangle of every frame. Animated menus with moving parts all over the screen make that rectangle the whole
  frame, which was most of the size of the first Profiles takes (9.9 MB). Keep those parts short and put them at the end.
- Freeze pixels that change less than a small threshold (~7 of 255) between frames, so blur shimmer doesn't count as change.
- Encode with one palette for the whole GIF and small per-frame rectangles, then let gifsicle optimize:

```bash
ffmpeg -framerate 10 -i c_%05d.png -vf "palettegen=max_colors=256:stats_mode=diff:reserve_transparent=1" palette.png
ffmpeg -framerate 10 -i c_%05d.png -i palette.png -lavfi "paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" -loop 0 raw.gif
gifsicle -O3 --lossy=20 raw.gif -o showcase.gif
```

- A 35-second 1280x720 showcase ends up at about 4 MB this way. Higher `--lossy` values barely save more and blur text edges.
