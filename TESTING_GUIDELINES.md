# Testing Guidelines

## Staying off the screen

Tests must never get in the way of the user. No test, unit test or in-game test, may open a window, put an icon in the Dock,
take the window focus, move or lock the mouse cursor, play audible sound or open anything in the browser.

- **Unit tests** run headless: the root `build.gradle` gives every `Test` task `java.awt.headless=true` and `apple.awt.UIElement=true`.
  Without headless mode, macOS puts the test JVM in the Dock as soon as a test loads AWT (fonts, images). Never remove these properties,
  and never start GLFW, Swing or anything else that opens a window from a unit test.
- **In-game tests** only run in off-screen test clients, see below. Never launch a normal client (`runClient`) for testing.
- To check a new kind of test, watch `lsappinfo list` while it runs. No process of the test may show up there with `type="Foreground"`,
  which is what gets a Dock icon, and `lsappinfo front` must never switch to it.

## Off-screen test clients

Test clients must never show their window on screen, take focus away from the user or touch the mouse cursor. FancyMenu has a dev-only
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
- It sets `soundCategory_master:0.0` in `<game directory>/options.txt` for the run, so test clients stay silent.
- All of these files get restored when the script exits.
- The game log contains `Off-screen test client mode is active!` when the mode works.
- **Important:** Test clients must never open anything in the browser of the user. Closing the "Welcome to FancyMenu!" window in any way
  (its button, its close button, or `PiPWindow#close()`) opens the FancyMenu docs there, so off-screen clients never open that window,
  and `WebUtils#openWebLink` only logs links in them. Never close the Welcome window from test code, never close all open PiP windows
  in bulk to get rid of it, and never open links some other way.

### How it works

- `util/window/OffscreenTestClient` reads the environment variable. The mode only works in a development environment.
- `MixinGLX` disables the macOS menu bar before GLFW starts, so the game gets no Dock icon and can't get activated.
- `MixinMinecraft` does not open the Welcome window, and `WebUtils#openWebLink` does not open links.
- `MixinWindow` creates the game window hidden and unfocused, and keeps it windowed, because GLFW always shows fullscreen windows.
- `MixinInputConstants` skips the game's mouse grab and release, so the real cursor never gets moved or locked,
  for example when a test enters or leaves a world.

### Writing in-game tests

- The game still renders into its render targets, so screenshots (`Screenshot.grab()`, `Screenshot.takeScreenshot()`) work as usual.
- The hidden window gets no input from the OS. Simulate input through the game's real input handlers instead,
  for example `KeyboardHandler#keyPress` with the key of the `KeyMapping` you want to trigger.
- Mouse moves and drags only reach the screen while `Minecraft#isWindowActive()` is true (`MouseHandler#onMove` and
  `#handleAccumulatedMovement` check it). It reads `Window#isFocused()`, which starts as `true` and only changes through focus events
  of the window, so simulated moves and drags work as they are. If a test sees them ignored, check `isWindowActive()` first.
  Mouse buttons and scrolling don't depend on it.
- The game still keeps track of the mouse being grabbed or released, only the real cursor stays untouched.
  Never call window or cursor functions of GLFW from test code (showing, raising or focusing the window, moving the cursor).
- There is no window to close, so test code has to call `Minecraft#stop()` when it's done. If a run hangs, stop its Gradle process.

## Decoration overlay preview

A dev-only harness in `customization/decorationoverlay/preview/` shows one decoration overlay on the title screen of an off-screen
test client, takes screenshots and stops the game. Use it to compare an overlay with its mockup and to check the Layers setting.
It only runs in off-screen test clients and is controlled by environment variables:

| Variable | Meaning |
| --- | --- |
| `FANCYMENU_OVERLAY_PREVIEW` | **Required.** Identifier of the overlay type (the builder's identifier), for example `snowfall`. A wrong one logs the known types. |
| `FANCYMENU_OVERLAY_PREVIEW_OUT` | **Required.** Absolute folder for the screenshots, outside of the game directory (for example your temp folder). |
| `FANCYMENU_OVERLAY_PREVIEW_LAYERS` | `background_and_foreground`, `only_background` or `only_foreground`. Default: the overlay's default. |
| `FANCYMENU_OVERLAY_PREVIEW_PROPERTIES` | Overlay properties as in layout files, `key=value` pairs separated by `;`, for example `amount=200;size=2;look=simple`. |
| `FANCYMENU_OVERLAY_PREVIEW_SHOTS` | Comma-separated seconds after the overlay first rendered, for example `1,3.5,8`. Default: `3`. |
| `FANCYMENU_OVERLAY_PREVIEW_INPUT` | Simulated input, entries `<seconds>:<action>[:<arguments>]` separated by `;`: `move:x,y` moves the pointer to a GUI position, `click:x,y` moves there and left-clicks, `click_widget:<id or label>` clicks the middle of a widget (FancyMenu widget identifier like `mc_titlescreen_options_button`, or its label), `title` opens the preview title screen again (after a click opened another screen), `set:key=value` sets an overlay property while it runs, like switching an option in the editor (for example `set:grass=false`). |
| `FANCYMENU_OVERLAY_PREVIEW_SIZE` | Screen size in GUI pixels. Default: `480x270`, the size of the mockup stage. |
| `FANCYMENU_OVERLAY_PREVIEW_GUI_SCALE` | GUI scale. Default: `4`. The hidden window gets sized to size times scale (1920x1080 by default). |
| `FANCYMENU_OVERLAY_PREVIEW_BACKGROUND` | `#RRGGBB` color of a plain backdrop (default `#1B2231`), or `panorama` for the vanilla title background. |
| `FANCYMENU_OVERLAY_PREVIEW_SHOT_SCALE` | `gui` (default): one image pixel per GUI pixel, so the images compare 1:1 with `node shot.js <id> out.png --scale 1` of the mockup. `full`: framebuffer size. |
| `FANCYMENU_OVERLAY_PREVIEW_SEED` | Seed for `PixelRandom.create()`, so overlays build the same random setup every run. |
| `FANCYMENU_OVERLAY_PREVIEW_NAME` | Start of the screenshot file names. Default: `<overlay>_<layers>`. |

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
FANCYMENU_TEST_RUN_DIR=run_client_test \
FANCYMENU_OVERLAY_PREVIEW=snowfall \
FANCYMENU_OVERLAY_PREVIEW_LAYERS=only_background \
FANCYMENU_OVERLAY_PREVIEW_OUT="$TMPDIR/overlay-preview" \
FANCYMENU_OVERLAY_PREVIEW_SHOTS=2,5 \
FANCYMENU_OVERLAY_PREVIEW_INPUT="3:move:240,100;6:click_widget:mc_titlescreen_options_button" \
sh offscreen_client.sh fabric
```

- Screenshots land in the output folder as `<name>_<seconds>s.png`, for example `snowfall_only_background_002.00s.png`.
- The log gets `[CHECK]` lines: the setup (overlay, Layers, properties), the preview screen once the overlay first rendered
  (GUI size, GUI scale, framebuffer), every screenshot (file, screen, quads per frame of pixel overlays, live toolkit textures),
  every click (the widget under the pointer and the screen afterwards; `click_widget` fails when the screen stayed the same, so use it
  on widgets that open another screen to prove that clicks still reach widgets), `update once per frame` and
  `background and foreground pass per frame` (update, frame and pass counts of the run, and the frame rate on the preview screen),
  and `overlay preview finished: PASS` or `FAIL <n> failed checks` at the end. Then the game stops on its own.
  `grep "\[CHECK\]"` the run's output, it's all there.
- The title screen shows only a temporary in-memory layout: the overlay, the backdrop and a forced GUI scale. The preview hides the
  other layouts of the title screen and never saves anything, so it leaves the layouts and the customizable screens of the game
  directory as they were. Global customizations of the instance still apply, so a clean instance (`FANCYMENU_TEST_RUN_DIR`, see above)
  gives the cleanest pictures.
- Clicking "Quit Game" stops the game, so don't. After `title`, send a `move` again before checking anything that reacts to the pointer:
  the new screen doesn't get the old pointer position.
- Overlays get their real frame times (at most 0.1 seconds per update), so they move at the same speed at any frame rate.
  Below 10 fps they slow down, so check the `fps` of the frame checks when motion looks off.
