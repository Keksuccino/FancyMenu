# Docs Tools

Tools and assets for making screenshots and showcase GIFs for the FancyMenu docs ([docs_fancymenu](https://github.com/Keksuccino/docs_fancymenu), branch `wiki-2026`).

Everything gets rendered in a real game, in an **off-screen test client** of a FancyMenu workspace (see `TESTING_GUIDELINES.md` of the workspace), so nothing ever shows on screen, takes focus or makes sound.

# Folder Layout

| Path | What it is |
|---|---|
| `ingame_harness/DocsShowcase.java` | In-game harness. Mode `scenes` renders Text elements from a JSON scene config on an empty Custom GUI and takes one screenshot per scene. Mode `editor` hands over to `DocsShowcaseEditorGif`. |
| `ingame_harness/DocsShowcaseEditorGif.java` | In-game recorder for layout editor showcase GIFs. Runs a scripted workflow with simulated mouse and keyboard input and saves every second tick as a frame (10 fps) plus `frames.jsonl` with the pointer state, clicks and key badges. |
| `postprocess/crop.py` | Crops scene screenshots to their elements (plus a small margin of background). |
| `postprocess/sprites.py` | Builds the cursor sprites (arrow and I-beam) with their hotspots. `compose.py` imports it. |
| `postprocess/compose.py` | Draws the cursor, click ripples and key badges (like `Ctrl + B`) onto recorded frames, and freezes tiny pixel changes. |
| `postprocess/encode.sh` | Encodes composed frames into an optimized GIF (ffmpeg palette + gifsicle). |
| `configs/text_pages/` | The configs used for the Text Formatting and Text in Menus pages: `gen_scenes.py` (writes `scenes.json`), `editor.json` (editor GIF), `bg.json` (clean background). Good examples to copy. |
| `assets/cursor_arrow.png`, `assets/cursor_ibeam.png` | The cursor sprites as images, for reference. `sprites.py` makes them. |
| `assets/docs_bg.png` | A clean blurred panorama background (1920x1080), used as layout background in editor GIFs. |

# Requirements

- A FancyMenu workspace with an off-screen test client (all workspaces have one). The tools were made in `fancymenu-26.3.0` (Java 25).
- Python 3 with Pillow and numpy, `ffmpeg`, `gifsicle` and `oxipng` (all in Homebrew).
- macOS for the key badge font (`/System/Library/Fonts/HelveticaNeue.ttc`, see `compose.py`).

# Setup

## 1. The docs game instance

Always record in a separate, clean game instance in the workspace root (here called `run_client_docs`), so the dev instance stays untouched:

```bash
cd fancymenu-26.3.0
mkdir run_client_docs
cp -cR run_client/rinku-libraries run_client_docs/
printf 'onboardAccessibility:false\npanoramaScrollSpeed:0.0\n' > run_client_docs/options.txt
```

- `cp -c` makes an APFS clone of Rinku's Chromium libraries, so it takes no extra space.
- A static panorama keeps screenshots identical and GIFs small.
- For editor GIFs, set `S:ui_scale_v2 = 'colossal';` in `run_client_docs/config/fancymenu/options.txt` (exists after the first launch). Otherwise FancyMenu's UI is too small to read once the GIF is shown at 800 px width.
- For editor GIFs with a background, copy `assets/docs_bg.png` to `run_client_docs/config/fancymenu/assets/docs_bg.png`.
- The folder is **not** git-ignored. Never commit it, and move it to the trash when you're done.

## 2. Installing the harness

The harness is no part of the mod, so it only goes into a workspace while recording:

1. Copy both files from `ingame_harness/` to `common/src/main/java/de/keksuccino/fancymenu/util/window/preview/` of the workspace.
2. In `FancyMenu.java`, add this line right after `WindowPreview.startIfRequested();`:
   `de.keksuccino.fancymenu.util.window.preview.DocsShowcase.startIfRequested();`

Afterwards, restore `FancyMenu.java` (`git restore` after checking that the hook is its only change) and move both files to the trash. **Never commit them.**

The harness uses internal APIs of FancyMenu (some through reflection). If a workspace changed them, fix the harness and copy the fixed version back here.

# Screenshots

1. Write a scene config. Use `configs/text_pages/gen_scenes.py` as a template:

```json
{"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
  {"name": "lists", "wait": 3, "elements": [
    {"x": 12, "y": 12, "w": 210, "h": 196, "look": "pixel", "content": "### Bullet List\n- Diamonds"}
  ]}
]}
```

   - Every scene is one screenshot (`<name>.png`, 1920x1080). Elements are Text elements anchored top-left, in GUI pixels.
   - Optional element keys: `look` (`pixel`, `classic`, `neutral`), `background` (`true`/`false`), `scale` (text scale).
   - New elements get the defaults of new elements (Pixel look, background, numbered lists, patch note markers and callouts on).
   - A scene without elements renders the clean background (see `bg.json`).

2. Render it (from the workspace root):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
FANCYMENU_TEST_RUN_DIR=run_client_docs FANCYMENU_DOCS_SHOWCASE=scenes FANCYMENU_DOCS_SHOWCASE_OUT=<out dir> FANCYMENU_DOCS_SHOWCASE_CONFIG=<scenes.json> sh offscreen_client.sh fabric
```

   A run takes about a minute. `grep "\[CHECK\]"` the output: every scene and screenshot logs a line, and `docs showcase finished: PASS` ends it.

3. Crop and optimize:

```bash
python3 postprocess/crop.py <scenes.json> <out dir> <cropped dir>
oxipng -o 4 --strip safe <cropped dir>/*.png
```

4. Look at every image before using it, then tune the element sizes (empty space, scroll bars) and render again.

# Showcase GIFs

1. Adapt `DocsShowcaseEditorGif` to the showcase:
   - `openEditor()` builds the layout the editor opens with (elements, background).
   - `buildScript()` is the workflow itself. Building blocks:
     - `waitTicks`, `waitUntil` (with timeout)
     - `moveTo` (eased, slightly curved pointer path, the target gets resolved when the move starts)
     - `click`, `press`, `release`, `key` (with an optional badge label, like `"Ctrl + B"`), `type` (one character per tick)
     - `scrollMenuUntilVisible` (scrolls a long right-click menu to an entry)
     - `dragSelectWord` (selects text in the text editor while the pointer drags over it)
   - Targets get resolved from the real widgets, never from fixed positions: `contextMenuEntry`, `wordEdge`, `lineEnd`, `doneButton`.
   - Texts come from the JSON config (see `configs/text_pages/editor.json`).
2. Record (frames go to the external drive, never to `/tmp`):

```bash
FANCYMENU_TEST_RUN_DIR=run_client_docs FANCYMENU_DOCS_SHOWCASE=editor FANCYMENU_DOCS_SHOWCASE_OUT="$PWD/run_client_docs/gif_frames" FANCYMENU_DOCS_SHOWCASE_CONFIG=<editor.json> sh offscreen_client.sh fabric
```

3. Look at some raw frames (`f_*.png`), especially around clicks and key presses (`grep events frames.jsonl`).
4. Compose and encode:

```bash
python3 postprocess/compose.py run_client_docs/gif_frames run_client_docs/gif_composed
sh postprocess/encode.sh run_client_docs/gif_composed <path>/showcase.gif
```

5. Check the GIF itself: extract a few frames with `ffmpeg -ss <seconds> -i showcase.gif -frames:v 1 check.png`. A 25 second showcase ends up at about 1 MB.

# Using the Images in the Docs

- Put them in `assets/` of the docs repo, with clear names like `markdown_lists.png` or `text_editor_showcase.gif`.
- Docs pages get read from GitHub but shown on docs.fancymenu.net, so always use full URLs:

```html
<img width="640" alt="Markdown callouts" src="https://raw.githubusercontent.com/Keksuccino/docs_fancymenu/refs/heads/wiki-2026/assets/markdown_callouts.png?raw=true">
```

- Use the native image width, but at most 800. After pushing, check that every URL returns 200.

# Gotchas

- **Placeholders get replaced everywhere in Text elements, also inside code blocks.** Don't use placeholders in example texts that should show as code.
- **The layout editor's right-click menu of an element is long.** Element-specific entries sit below the visible part, so the script has to scroll the menu first (`scrollMenuUntilVisible`).
- **The text editor reads the real SDL state** for drag selection (mouse button) and Shift+Arrow selection, which simulated input can't set. `dragSelectWord` applies the same selection through the editor's own `selectBetweenOffsets` while the pointer drags.
- **Hide distractions in editor GIFs:** the recorder turns off the editor grid and hides the editor widgets (the Layers widget grows over elements at big UI scales).
- **PiP windows fade in.** For a few frames, what's behind them shows through. That's real and fine.
- **Freezing tiny changes can leave ghosts** after fades. `compose.py` only keeps a pixel while it barely changed and still matches the real frame (`FREEZE` / `FREEZE_DRIFT`). Don't raise these values.
- **Key badges:** label them with `Ctrl`, even though the harness presses Cmd on macOS. The docs always say "`Ctrl` (`Cmd` on macOS)".
- **The recorder class has to be public**, or FancyMenu's event system can't call its listener method and the game never stops.
- **A stuck test client never stops on its own.** Find its Java process (`ps` and the run folder name) and stop it, then check what went wrong.
- Use `trash` instead of `rm` for old takes and frames.
