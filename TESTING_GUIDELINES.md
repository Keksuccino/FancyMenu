# Testing Guidelines

## Off-screen test clients

Test clients must never show their window on screen or take focus away from the user. FancyMenu has a dev-only
off-screen mode for that, so always launch test clients through `offscreen_client.sh`:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) sh offscreen_client.sh fabric
JAVA_HOME=$(/usr/libexec/java_home -v 21) sh offscreen_client.sh neoforge
```

- The script runs the loader's client task (`runClient`, or `Client` for Forge) with the `FANCYMENU_TEST_OFFSCREEN=true` environment variable.
  Extra arguments get passed on to Gradle.
- It sets `use-cache=false` in `run_client/config/rinku/rinku.properties` for the run, so Rinku's Chromium uses CEF's default profile
  instead of the user's persistent Rinku profile, and it clears the crash mark a killed test run leaves in that profile.
  Chromium would otherwise open a visible "Restore pages?" window.
- Off-screen clients of all workspaces run one at a time, guarded by a lock in `$TMPDIR`. A Chromium starting while another one uses
  the same profile makes the running one open a visible "New Tab" window. The script compiles the loader before waiting for the lock.
- For (Neo)Forge, it also sets `earlyWindowControl = false` in `run_client/config/fml.toml` for the run.
  The early loading window opens before any mod code runs, so the FML config is the only way to keep it closed.
- Both files get restored when the script exits.
- The game log contains `Off-screen test client mode is active!` when the mode works.

### How it works

- `util/window/OffscreenTestClient` reads the environment variable. The mode only works in a development environment.
- `MixinGLX` disables the macOS menu bar before GLFW starts, so the game gets no Dock icon and can't get activated.
- `MixinWindow` creates the game window hidden and unfocused, and keeps it windowed, because GLFW always shows fullscreen windows.

### Writing in-game tests

- The game still renders into its render targets, so screenshots (`Screenshot.grab()`, `Screenshot.takeScreenshot()`) work as usual.
- The hidden window gets no input from the OS. Simulate input through the game's real input handlers instead,
  for example `KeyboardHandler#keyPress` with the key of the `KeyMapping` you want to trigger.
- There is no window to close, so test code has to call `Minecraft#stop()` when it's done. If a run hangs, stop its Gradle process.
