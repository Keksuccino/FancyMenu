# Docs Tools

Tools for screenshots, showcase GIFs and checks of the FancyMenu docs. Images get rendered in an off-screen test client of a FancyMenu workspace (see its `TESTING_GUIDELINES.md`).

# Folder Layout

| Path | What it is |
|---|---|
| `ingame_harness/DocsShowcase.java` | Renders the elements of a scene config and takes one screenshot per scene. |
| `ingame_harness/DocsShowcaseEditorGif.java` | Records a scripted layout editor workflow as GIF frames. |
| `ingame_harness/DocsShowcaseElements.java` | Builds the elements of both from JSON. |
| `postprocess/crop.py` | Crops scene screenshots to their elements. |
| `postprocess/sprites.py` | Cursor sprites (arrow, I-beam) for `compose.py`. |
| `postprocess/compose.py` | Draws cursor, click ripples and key badges onto GIF frames. |
| `postprocess/encode.sh` | Encodes composed frames into an optimized GIF. |
| `configs/text_pages/` | Configs of the Text Formatting and Text in Menus images. |
| `configs/scroll_area_page/` | Configs of the Scrollable Areas images: elements of any type, elements in areas, editor stills. |
| `checks/check_page.py` | Checks a page's UI labels, link format, links, anchors and images. |
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

When done, `git restore` `FancyMenu.java` and remove the copied files. If the harness needed fixes for a workspace, copy the fixed version back here.

# Screenshots

1. Write a scene config (template: `configs/text_pages/gen_scenes.py`):

```json
{"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
  {"name": "lists", "wait": 3, "elements": [
    {"x": 12, "y": 12, "w": 210, "h": 196, "look": "pixel", "content": "### Bullet List\n- Diamonds"}
  ]}
]}
```

   - One screenshot per scene (`<name>.png`). Elements are anchored top-left, in GUI pixels, with the defaults of new elements.
   - Element keys: `type` (element type identifier, default `text_v2`), `id`, `parent` (the `id` of a Scrollable Area: `x`/`y` are then relative to its content), `props` (saved properties as strings, like `{"layout_mode": "stack"}` or `{"label": "Survival"}`).
   - Text elements also take `content`, `look` (`pixel`, `classic`, `neutral`), `background` (`true`/`false`) and `scale`.
   - A scene without elements renders the clean background.

2. Render (from the workspace root, about a minute):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
FANCYMENU_TEST_RUN_DIR=run_client_docs FANCYMENU_DOCS_SHOWCASE=scenes FANCYMENU_DOCS_SHOWCASE_OUT=<out dir> FANCYMENU_DOCS_SHOWCASE_CONFIG=<scenes.json> sh offscreen_client.sh fabric
```

   `grep "\[CHECK\]"` the output. It ends with `docs showcase finished: PASS`.

3. Crop and optimize:

```bash
python3 postprocess/crop.py <scenes.json> <out dir> <cropped dir> [margin, default 10]
oxipng -o 4 --strip safe <cropped dir>/*.png
```

   `crop.py` skips elements with a `parent`. Scroll bars and backgrounds reach beyond their element, so give them a margin of about 22.

4. Look at every image. Tune element sizes (empty space, scroll bars) and render again if needed.

# Showcase GIFs

1. Write a config and pick a script with its `script` key, or add a new script to `buildScript()`:
   - `text_editor` (default): edits a Text element (`configs/text_pages/editor.json`).
   - `scroll_area`: drags buttons into a Scrollable Area, switches it to Stack, scrolls and reorders (`configs/scroll_area_page/gen_editor.py`).
   - `select_and_scroll`: selects an element and scrolls it, for editor stills. The last frame is the still: crop it with Pillow (`configs/scroll_area_page/gen_content_handle.py`).
   - The config's `elements` (same keys as in scene configs) are the layout the editor opens with.
   - Steps: `waitTicks`, `waitUntil`, `moveTo`, `click`, `press` (optionally with modifiers), `release`, `key` (with badge label), `badge`, `type`, `wheel`, `clickMenuEntry` (also in sub-menus), `scrollMenuUntilVisible`, `dragSelectWord`.
   - Resolve targets from the real widgets (`menuEntry`, `editorBox`, `wordEdge`, `lineEnd`, `doneButton`), never from fixed positions.
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
- Dragging several selected elements needs `Ctrl`: `press(..., InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)` and `badge("Ctrl + Drag")`.
- An empty Scrollable Area shows its guide as a big tooltip while hovered at the colossal UI scale. Give it a child, like a title.
- A Text element that's too small for its text shows its own scroll bar.
- Selected elements show their position labels in the editor. They can't be turned off.
- Anchor overlays while dragging and long menu scrolls change big parts of the frame. The Scrollable Areas GIF is 1.4 MB for 25 seconds.
- The recorder class has to be public, or its listener never runs and the game never stops.
- A stuck test client never stops on its own. Stop its Java process (`ps`, look for the run folder).

# Checking Pages

```bash
python3 checks/check_page.py <docs repo> <workspace> <page name> [more pages]
```

Lists bold text that isn't a UI label in `en_us.json`, links the docs site turns into `#` (anything not starting with `/`, `#`, `http(s):` or `mailto:`, like `./page`), links to missing pages and anchors, and missing images. Bold text that's no UI label (like "Strikethrough") shows up too, so read the list.

To check all pages:

```bash
ls <docs repo>/docs/*.md | sed 's|.*/||; s/\.md$//' | xargs python3 checks/check_page.py <docs repo> <workspace>
```
