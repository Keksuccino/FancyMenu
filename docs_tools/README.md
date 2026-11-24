# Docs Tools

Tools for screenshots and showcase GIFs of the FancyMenu docs. Everything gets rendered in an off-screen test client of a FancyMenu workspace (see its `TESTING_GUIDELINES.md`).

# Folder Layout

| Path | What it is |
|---|---|
| `ingame_harness/DocsShowcase.java` | Renders Text elements from a scene config and takes one screenshot per scene. |
| `ingame_harness/DocsShowcaseEditorGif.java` | Records a scripted layout editor workflow as GIF frames. |
| `postprocess/crop.py` | Crops scene screenshots to their elements. |
| `postprocess/sprites.py` | Cursor sprites (arrow, I-beam) for `compose.py`. |
| `postprocess/compose.py` | Draws cursor, click ripples and key badges onto GIF frames. |
| `postprocess/encode.sh` | Encodes composed frames into an optimized GIF. |
| `configs/text_pages/` | Configs of the Text Formatting and Text in Menus images. Use them as templates. |
| `assets/` | Cursor sprites and `docs_bg.png`, a blurred panorama background for editor GIFs. |

# Requirements

- A FancyMenu workspace. The tools were made in `fancymenu-26.3.0` (Java 25).
- Python 3 with Pillow and numpy, `ffmpeg`, `gifsicle`, `oxipng`.
- macOS for the key badge font (`HelveticaNeue.ttc`).

# Setup

## Game Instance

Record in a separate instance in the workspace root, never in `run_client`:

```bash
cd fancymenu-26.3.0
mkdir run_client_docs
cp -cR run_client/rinku-libraries run_client_docs/
printf 'onboardAccessibility:false\npanoramaScrollSpeed:0.0\n' > run_client_docs/options.txt
```

For editor GIFs, after the first launch:

- Set `S:ui_scale_v2 = 'colossal';` in `run_client_docs/config/fancymenu/options.txt`, so the UI is readable at 800 px.
- Copy `assets/docs_bg.png` to `run_client_docs/config/fancymenu/assets/`.

`run_client_docs` isn't git-ignored. Never commit it, and remove it when you're done.

## Harness

The harness is no part of the mod. Install it only while recording:

1. Copy `ingame_harness/*.java` to `common/src/main/java/de/keksuccino/fancymenu/util/window/preview/`.
2. In `FancyMenu.java`, add this after `WindowPreview.startIfRequested();`:
   `de.keksuccino.fancymenu.util.window.preview.DocsShowcase.startIfRequested();`

When done, `git restore` `FancyMenu.java` and remove both files. If the harness needed fixes for a workspace, copy the fixed version back here.

# Screenshots

1. Write a scene config (template: `configs/text_pages/gen_scenes.py`):

```json
{"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
  {"name": "lists", "wait": 3, "elements": [
    {"x": 12, "y": 12, "w": 210, "h": 196, "look": "pixel", "content": "### Bullet List\n- Diamonds"}
  ]}
]}
```

   - One screenshot per scene (`<name>.png`). Elements are Text elements anchored top-left, in GUI pixels, with the defaults of new elements.
   - Optional element keys: `look` (`pixel`, `classic`, `neutral`), `background` (`true`/`false`), `scale`.
   - A scene without elements renders the clean background.

2. Render (from the workspace root, about a minute):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
FANCYMENU_TEST_RUN_DIR=run_client_docs FANCYMENU_DOCS_SHOWCASE=scenes FANCYMENU_DOCS_SHOWCASE_OUT=<out dir> FANCYMENU_DOCS_SHOWCASE_CONFIG=<scenes.json> sh offscreen_client.sh fabric
```

   `grep "\[CHECK\]"` the output. It ends with `docs showcase finished: PASS`.

3. Crop and optimize:

```bash
python3 postprocess/crop.py <scenes.json> <out dir> <cropped dir>
oxipng -o 4 --strip safe <cropped dir>/*.png
```

4. Look at every image. Tune element sizes (empty space, scroll bars) and render again if needed.

# Showcase GIFs

1. Adapt `DocsShowcaseEditorGif`:
   - `openEditor()`: the layout the editor opens with.
   - `buildScript()`: the workflow. Steps: `waitTicks`, `waitUntil`, `moveTo`, `click`, `press`, `release`, `key` (with badge label), `type`, `scrollMenuUntilVisible`, `dragSelectWord`.
   - Resolve targets from the real widgets (`contextMenuEntry`, `wordEdge`, `lineEnd`, `doneButton`), never from fixed positions.
   - Texts come from the JSON config (template: `configs/text_pages/editor.json`).
2. Record:

```bash
FANCYMENU_TEST_RUN_DIR=run_client_docs FANCYMENU_DOCS_SHOWCASE=editor FANCYMENU_DOCS_SHOWCASE_OUT="$PWD/run_client_docs/gif_frames" FANCYMENU_DOCS_SHOWCASE_CONFIG=<editor.json> sh offscreen_client.sh fabric
```

3. Look at raw frames around clicks and key presses (`grep events frames.jsonl`).
4. Compose and encode:

```bash
python3 postprocess/compose.py run_client_docs/gif_frames run_client_docs/gif_composed
sh postprocess/encode.sh run_client_docs/gif_composed <path>/showcase.gif
```

5. Check frames of the GIF itself: `ffmpeg -ss <seconds> -i showcase.gif -frames:v 1 check.png`.

# Gotchas

- Text elements replace placeholders everywhere, even in code blocks.
- Element-specific entries of an element's right-click menu are below its visible part. Scroll first (`scrollMenuUntilVisible`).
- The text editor reads the real SDL state for drag and Shift+Arrow selection, which simulated input can't set. Use `dragSelectWord`.
- PiP windows fade in, so what's behind them shows through for a few frames. That's real.
- Don't raise `FREEZE` / `FREEZE_DRIFT` in `compose.py`, or faded-out content stays as a ghost.
- Label key badges with `Ctrl`, even though the harness presses Cmd on macOS.
- The recorder class has to be public, or its listener never runs and the game never stops.
- A stuck test client never stops on its own. Stop its Java process (`ps`, look for the run folder).
