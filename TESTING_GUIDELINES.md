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
JAVA_HOME=$(/usr/libexec/java_home -v 21) sh offscreen_client.sh fabric
JAVA_HOME=$(/usr/libexec/java_home -v 21) sh offscreen_client.sh forge
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
- `MouseHandler#onMove` passes mouse moves and drags to the screen also while the window has no focus, so simulated moves and drags
  work as they are (only turning the camera needs `Minecraft#isWindowActive()`). From Minecraft 1.21 on, moves and drags need it too.
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
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
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
- Clicking "Quit Game" stops the game, so don't. The title screen that `title` opens gets the pointer where the last `move` or click
  left it, because screens get the pointer position of the game's mouse handler every frame.
- Overlays get their real frame times (at most 0.1 seconds per update), so they move at the same speed at any frame rate.
  Below 10 fps they slow down, so check the `fps` of the frame checks when motion looks off.

## Window preview

A dev-only harness in `util/window/preview/` opens one FancyMenu window on the title screen of an off-screen test client, simulates mouse
and keyboard input, takes screenshots and stops the game. Use it to look at windows, value editors of actions and requirements, and
anything else that opens in a PiP window. It only runs in off-screen test clients and is controlled by environment variables:

| Variable | Meaning |
| --- | --- |
| `FANCYMENU_WINDOW_PREVIEW` | **Required.** What to open: `manage_variables` (the Manage Variables window), `action:<identifier>` (the value editor of an action, for example `action:set_variable`), `requirement:<identifier>` (the value editor of a requirement) or `opener:<fully.qualified.Class>#<method>` (calls a public static method without parameters that opens a window, for anything else). |
| `FANCYMENU_WINDOW_PREVIEW_OUT` | **Required.** Absolute folder for the screenshots, outside of the game directory. |
| `FANCYMENU_WINDOW_PREVIEW_VALUE` | Value of the action or requirement instance whose editor gets opened. Default: the preset of the action or requirement. |
| `FANCYMENU_WINDOW_PREVIEW_VARIABLES` | JSON object that replaces **all** variables of the game directory before the window opens. Arrays become list variables, everything else text variables, for example `{"fruits":["apple","banana"],"name":"Steve"}`. Default: the variables stay as they are. |
| `FANCYMENU_WINDOW_PREVIEW_SHOTS` | Comma-separated seconds after the window opened, for example `1,3.5`. Default: `2`. |
| `FANCYMENU_WINDOW_PREVIEW_INPUT` | Simulated input, entries `<seconds>:<action>[:<arguments>]` separated by `;`: `move:x,y`, `click:x,y`, `right_click:x,y`, `double_click:x,y`, `drag:x1,y1,x2,y2` (left button), `scroll:x,y,amount` (positive scrolls up), `key:<key>` (letters, digits, `enter`, `escape`, `tab`, `backspace`, `delete`, `space`, `up`, `down`, `left`, `right`, `home`, `end`, `page_up`, `page_down`, `f1` to `f12`, with `shortcut+` (Cmd on macOS, Ctrl elsewhere), `ctrl+`, `shift+`, `alt+` or `cmd+` in front, like `shortcut+z`; letters and digits press the key that types them in the system's keyboard layout, like UIs match shortcuts) and `type:<text>` (types into whatever has the focus, write a `;` in the text as `\;`). Positions are GUI pixels. An input starts once the one before it is done. |
| `FANCYMENU_WINDOW_PREVIEW_SIZE` | Screen size in GUI pixels. Default: `960x540`. |
| `FANCYMENU_WINDOW_PREVIEW_GUI_SCALE` | GUI scale. Default: `2`. The hidden window gets sized to size times scale (1920x1080 by default). |
| `FANCYMENU_WINDOW_PREVIEW_NAME` | Start of the screenshot file names. Default: the target. |

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
FANCYMENU_TEST_RUN_DIR=run_client_test \
FANCYMENU_WINDOW_PREVIEW=manage_variables \
FANCYMENU_WINDOW_PREVIEW_OUT="$TMPDIR/window-preview" \
FANCYMENU_WINDOW_PREVIEW_VARIABLES='{"fruits":["apple","banana","cherry"],"player_name":"Steve"}' \
FANCYMENU_WINDOW_PREVIEW_SHOTS=1,3 \
FANCYMENU_WINDOW_PREVIEW_INPUT="2:right_click:300,200" \
sh offscreen_client.sh fabric
```

- Screenshots land in the output folder at framebuffer size as `<name>_<seconds>s.png`, for example `manage_variables_002.00s.png`.
- The log gets `[CHECK]` lines: the setup, the variables, the opened window (its title, GUI size and scale), every input (the open windows
  afterward, front to back, with a `*` at the focused one), every screenshot, the result of an action or requirement editor when it gets
  closed with Done or canceled (`editor result`), and `window preview finished: PASS` or `FAIL <n> failed checks` at the end. Then the game
  stops on its own. `grep "\[CHECK\]"` the run's output, it's all there.
- Variables you replace stay in the game directory, so use a separate game instance (`FANCYMENU_TEST_RUN_DIR`) for previews.
- Input goes through the game's own input handlers, so it reaches windows like real input. Checks that read the real key state from GLFW
  (`Screen#hasShiftDown()`, `#hasControlDown()`, `#hasAltDown()`) don't see simulated modifiers. Only the modifiers of the key events
  carry them, so code that should be testable reads modifiers from its events.
- Opener methods are for temporary test code: remove them when you are done.
