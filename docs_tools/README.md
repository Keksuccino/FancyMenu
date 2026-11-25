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
| `configs/decoration_overlays_page/` | Configs of the Decoration Overlays images: overlays on the title screen, vanilla elements moved and hidden. |
| `configs/action_scripts_page/` | Configs of the Action Scripts GIFs: making a button's action script in the editor, and clicking the button on the title screen. |
| `configs/elements_page/` | Configs of the Elements images: one screenshot per visible element type, and the GIF of adding a Button through New Element. |
| `configs/visibility_page/` | Config of the Element Visibility GIF: a welcome text with Delay Appearance and fade-in, and a button that toggles a rules panel through a variable. |
| `configs/variables_page/` | Configs of the Variables GIFs: making variables in Manage Variables, a list action on a button, and the button in use. |
| `configs/listeners_page/` | Configs of the Listeners images: making a listener in Manage Listeners, the listener running on the title screen, and stills of Manage Listeners and the listener chooser. |
| `configs/schedulers_page/` | Configs of the Schedulers images: a scheduler that shows a random tip every 4 seconds, making that scheduler in Manage Schedulers (adding it, then its settings and starting it), and a still of Manage Schedulers. |
| `configs/custom_guis_page/` | Configs of the Custom GUIs GIFs: a Modpack Info button on the title screen that opens a Custom GUI as a popup, which its Back button closes again, and making that Custom GUI in Manage Custom GUIs. `make_emblem.py` draws the Frostfall emblem pixel art of the popup. |
| `configs/layout_editor_page/` | Configs of the Layout Editor images: the GIF of the editor basics (moving, widening and nudging a title, selecting buttons with a rectangle, copying, pasting, dragging the copies and undoing) a still of the editor with its Layers widget, and the Layers GIF (fixing the order of a news panel, grouping, hiding and folding). |
| `configs/player_entities_page/` | Configs of the Player Entities images: five players that show the main settings, the GIF of heads and bodies following the mouse, a still of the Entity Pose window, and the Player Entity image of the Elements page. |
| `configs/requirements_page/` | Configs of the Requirements GIFs: giving an info box an Is Element Hovered requirement in the editor, hovering the Multiplayer button on the title screen, and an Or group with two requirements. |
| `configs/placeholders_page/` | Configs of the Placeholders images: the GIF of adding a placeholder in the text editor, the welcome panel, and a check scene (no docs image) that shows the real output of placeholders whose format the page states. |
| `configs/export_for_ai_page/` | The chat GIF of the Export for AI page, a mockup of a support chat with Alice drawn with Pillow. The only image that isn't an in-game render, because it shows something outside of the game. Its avatar and emotes come from the user's Alice emotes folder. |
| `configs/global_customizations_page/` | Configs of the Global Customizations GIF: a tour through the Title screen, Options and Music & Sounds without layouts, with global wood button and brass slider textures (`make_textures.py` draws them) and a night menu background. |
| `configs/getting_started_page/` | Configs and pixel art of the Getting Started GIFs, which build the Wildwood title screen step by step: `make_assets.py` draws its scene (also as three parallax layers), logo and wooden buttons (`wildwood/`), `gen_configs.py` writes the configs of the finished menu, making a first layout from the menu bar, setting the background, adding the logo and saving, `make_stills.py` crops the menu bar still. |
| `configs/parallax_page/` | Configs and pixel art of the Parallax Effect images: `make_layers.py` draws a landscape at dusk as a sky background and three layers, `gen_configs.py` writes the configs of the title screen with the pointer moving around, of turning on the effect of the foreground layer, and of the still of the background's parallax settings, which `make_stills.py` crops. Nearly every pixel of the scene GIF changes, so `half_size.py` halves its frames before `compose.py` (640x360, about 1.3 MB). |
| `checks/check_page.py` | Checks a page's UI labels, link format, links, anchors and images. |
| `assets/` | Cursor sprites, `docs_bg.png` (a blurred panorama background) and `docs_bg_night.png` (the same at night). |

# Requirements

- A FancyMenu workspace. The tools were made in `fancymenu-26.3.0` (Java 25).
- Python 3 with Pillow and numpy, `ffmpeg`, `gifsicle`, `oxipng`, `pngquant`.
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
- Copy `assets/docs_bg.png` and `assets/docs_bg_night.png` to `run_client_docs/config/fancymenu/assets/`.

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
   - Element keys: `type` (element type identifier, default `text_v2`), `id`, `parent` (the `id` of a Scrollable Area: `x`/`y` are then relative to its content), `props` (saved properties as strings, like `{"layout_mode": "stack"}` or `{"label": "Survival"}`), `actions` (the action script of elements that have one, like buttons: `[{"action": "show_toast", "value": "..."}]`, and IF statements with `if`, `actions` and an optional `else`, like in `listeners`), `requirements` (Loading Requirements: `[{"requirement": "<identifier>", "value": "...", "mode": "opposite"}]`, `mode` is optional).
   - Text elements also take `content`, `look` (`pixel`, `classic`, `neutral`), `background` (`true`/`false`) and `scale`.
   - A scene without elements renders the clean background.
   - More scene keys (template: `configs/decoration_overlays_page/gen_scenes.py`):
     - `screen`: `title` for the title screen, instead of an empty screen.
     - `vanilla`: changes vanilla elements, by widget identifier (`id`): `hidden`, `x`/`y`/`w`/`h` (moves it, anchored top-left) and `props`.
     - `background_image` (like `/config/fancymenu/assets/docs_bg.png`) or `background_color` (`#RRGGBB`): the menu background.
     - `overlays`: decoration overlays, each with `type` (like `snowfall`), optional `layers` and `props`.
     - `seed`: makes overlays build the same random setup every run.
     - `pointer`: `[x, y]` in GUI pixels. `[-40, -40]` keeps it off the screen.
     - `shots`: seconds for several screenshots of a scene that changes over time, named `<name>_<seconds>s.png`.
     - `variables`: text variables to set before the scene opens, like `{"show_tips": "true"}` for a checkbox in Variable Mode or a linked text input field.

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

   Full-screen shots with a background image are about 650 KB. `pngquant --quality 80-95` before `oxipng` makes them about 150 KB without a visible change (template: `configs/decoration_overlays_page/make_assets.py`).

4. Look at every image. Tune element sizes (empty space, scroll bars) and render again if needed.

# Showcase GIFs

1. Write a config and pick a script with its `script` key, or add a new script to `buildScript()`:
   - `text_editor` (default): edits a Text element (`configs/text_pages/editor.json`).
   - `scroll_area`: drags buttons into a Scrollable Area, switches it to Stack, scrolls and reorders (`configs/scroll_area_page/gen_editor.py`).
   - `select_and_scroll`: selects an element and scrolls it, for editor stills. The last frame is the still: crop it with Pillow (`configs/scroll_area_page/gen_content_handle.py`).
   - `action_script`: opens a button's action script editor, presses A, searches and picks an action, fills in its value window and clicks Done (`configs/action_scripts_page/gen_configs.py`).
   - `click_element`: clicks an element outside the editor and holds the result, like a toast (needs `start: screen`). `clicks` and `click_gap_ticks` click it several times, `start_wait_ticks` waits longer before the pointer moves.
   - `manage_variables`: opens **Customization -> Manage Variables** from the menu bar, adds a text variable and sets its value, then adds a list and types its entries in the list editor (`configs/variables_page/gen_configs.py`, needs `start: screen` and `menu_bar: true`).
   - `manage_listeners`: opens **Customization -> Manage Listeners** from the menu bar, clicks **Add Listener**, searches and picks a listener (`listener`, `listener_search`), gives it an action like `action_script`, selects it, clicks **Done** and presses `keys` (`configs/listeners_page/gen_configs.py`, needs `start: screen`, `menu_bar: true` and `listeners: []`).
   - `listeners_still`: opens Manage Listeners, selects a listener (`select`) and scrolls its description down (`description_wheel_turns`), then picks a type in the listener chooser. The frames of both stills have a `mark` event (`manage`, `choose`) in frames.jsonl: `configs/listeners_page/make_stills.py` picks and optimizes them.
   - `add_element`: right-clicks a free spot (`right_click`), opens **New Element** and picks `element_type`, drags the element to `drag_to`, widens it by `widen_by` at its right edge, sets its label through `label_entry` (like `edit_label`) to `label`, and clicks `free_spot` (`configs/elements_page/gen_configs.py`).
   - `add_placeholder`: opens the text editor of the Text element (like `text_editor`), clicks behind its last line, opens **Placeholders**, clicks the `category`, rests on the `placeholder` so its tooltip shows, adds it, types `typed_after` and clicks **Done** (`configs/placeholders_page/gen_configs.py`).
   - `manage_requirements`: in the editor, optionally copies the ID of the `copy_id` element through its right-click menu, opens **Loading Requirements** of the `target` element (or opens **Manage Requirements** right away with `open_directly: true`) and runs `requirement_steps`: `add` (a requirement identifier, with `search`, `via: "menu"` or the `A` key, and `value`, `paste: true` or `fields` for its value), `add_group` (`menu` or `key`), `group_mode`, `open_group` (`from_open_menu: true` right after `group_mode`), `done` (closes the group window) and `rest`. Then it clicks **Done** and the `free_spot`, unless `close: false` (`configs/requirements_page/gen_configs.py`).
   - `context_entry_still`: in the editor, right-clicks the `target` element, scrolls its right-click menu to the `entry` (an entry identifier, like `entity_pose`) and clicks it, rests the pointer at `rest_point` and marks the frame of the still (`still`) (`configs/player_entities_page/gen_configs.py`).
   - `hover_elements`: outside the editor, moves the pointer over `hovers` (each with `target`, an element or Vanilla element id, or `point`, plus `ticks`) to show hover effects like requirements.
   - `manage_schedulers`: makes a scheduler in two parts, so each GIF stays short (`part`). `add` opens **Customization -> Manage Schedulers** from the menu bar, clicks **Add Scheduler**, gives the new scheduler an action like `action_script` and selects it (needs `start: screen`, `menu_bar: true` and `schedulers: []`). `settings` opens Manage Schedulers, selects the scheduler `select`, fills in **Edit Settings** (`settings_fields`, same format as `fields`), clicks **Start Now** and **Done**, and rests the pointer at `rest_point` to watch it work. Both hold for `hold_ticks` (`configs/schedulers_page/gen_configs.py`).
   - `schedulers_still`: opens Manage Schedulers and selects a scheduler (`select`). Its frame has a `mark` event (`manage`): `configs/schedulers_page/make_stills.py` picks, crops and optimizes it.
   - `click_through`: outside the editor, clicks `clicks` one after another (each with `target`, an element id on the screen that is open at that time, and `wait_ticks`), like a button that opens a Custom GUI and its Back button. Then it rests at `rest_point` (`configs/custom_guis_page/gen_configs.py`, needs `start: screen`).
   - `manage_custom_guis`: opens **Customization -> Custom GUIs -> Manage Custom GUIs** from the menu bar, clicks **New GUI**, types the `identifier` (and an optional `title`), clicks the cycle buttons of `cycles` (label keys) once each and clicks **Done**. Then it selects the new Custom GUI, clicks **Open GUI** and **Done**, and rests at `rest_point` (`configs/custom_guis_page/gen_configs.py`, needs `start: screen`, `screen: title`, `menu_bar: true` and `custom_guis: []`).
   - `editor_basics`: selects and drags `target` by `move`, widens it at its right edge by `widen_by`, presses Arrow Right `nudge` times, selects the `buttons` (first and last id) with a selection rectangle, copies and pastes them, drags the copies by `copies_move` with Ctrl, undoes twice and clicks `free_spot` (`configs/layout_editor_page/gen_configs.py`).
   - `layers_widget`: in the Layers widget (needs `show_widgets: true`), drags the layer of `cover` (an element that hides `covered`) right below the layer of `covered` and clicks `free_spot` to unselect it, adds a group through the right-click menu of Layers, selects both with a selection rectangle in the editor, drags them onto the group, hides and shows the group with its eye and folds it up (`configs/layout_editor_page/gen_configs.py`).
   - `screen_tour`: outside the editor, walks through vanilla screens like a player, by the labels of their buttons and sliders (`tour` steps: `hover`, `click` with `opens: true` for buttons that open a screen, `drag` of a slider `from`/`to` parts of its width, `point`, `wait`). Labels are lang keys and match from their start, so `options.fov` finds `FOV: 70`. Widgets in lists count too (`configs/global_customizations_page/gen_configs.py`, needs `start: screen`).
   - `first_layout`: outside the editor with the menu bar (`start: screen`, `menu_bar: true`, `no_layout: true`, `customization: false`), opens **Customization**, turns on **Current Screen Customization** and clicks **Layouts -> New -> For Current Screen**, then holds in the editor. Its frame with the open Customization menu has a `mark` event (`menu`) for a still (`configs/getting_started_page/gen_configs.py`).
   - `menu_background`: right-clicks `right_click`, opens **Menu Backgrounds -> Image**, turns on **Show Background**, clicks **Choose Image** and picks `file` (in the assets sub-folder `folder`) through the resource chooser: **Source Type** to Local, **Choose Local Resource File**, a double-click on the folder, a click on the file and **OK**. Then it clicks `free_spot`.
   - `add_image`: deletes the vanilla elements of `remove` (click and `Delete`), adds an Image element through **Element -> New** of the editor's menu bar, picks its `file` through **Image Source** (like `menu_background`), clicks **Restore Aspect Ratio**, drags its center to `drag_to` and clicks `free_spot`. It scrolls the right-click menu of the image until the entry is inside its visible part, two wheel steps per frame.
   - `save_layout`: presses `Ctrl + S`, clicks **Save** in the Save File window (keeping its file name), closes the editor with the X of its menu bar and rests on the screen (`hovers`: `label` of a vanilla widget or `point`). It saves a real layout file: move it to the trash after recording.
   - `element_settings`: right-clicks the element at `right_click` and goes through `settings`: `toggle` (an entry identifier, like `enable_parallax`) clicks it, `number` (the entry of a number setting, like `menu_entry_parallax_intensity_x`) opens its number picker, types `value` after `Ctrl + A` and clicks **Done**. The number picker closes the menu, so the next setting right-clicks again. Optional `rest_point` and `hold_ticks` (`configs/parallax_page/gen_configs.py`).
   - `menu_path_still`: right-clicks `right_click`, opens the sub-menus of `path` (like `menu_backgrounds`, `menu_background_image`) and rests on the entry `rest_on` of the last one. Its frame has a `mark` event (`still`): `configs/parallax_page/make_stills.py` crops it.
   - `button_textures`: selects the `buttons` (first and last id) with a selection rectangle, so the right-click menu of `target` changes all of them at once, then sets **Button Textures -> Set -> Normal Background** (`normal`) and **Hover Background** (`hover`) through the resource chooser (like `menu_background`, with `folder`), turns on **Button Textures -> Nine-Slice Custom Background**, closes the menu with `Esc`, clicks `free_spot` and rests on `rest_on`. The selection must hold only buttons: the menu of several elements only has the entries all of them have.
   - `press_keys`: presses `keys` (each with `key` and `badge`) with `key_gap_ticks` between them, like a player trying a listener. `pointer: [-40, -40]` keeps the cursor out of the picture.
   - Fields of `action_script` value windows: `{"label", "text"}` types into a text field, `{"cycle", "clicks"}` clicks a cycle button (by the localization key of its label, like `fancymenu.actions.variables.arrange_list.edit.arrangement`).
   - The config's `elements`, `vanilla` and `background_image`/`background_color` (same keys as in scene configs) are the layout the editor opens with. `screen: title` opens it for the title screen instead of an empty Custom GUI.
   - `start: screen` opens the screen itself with that layout instead of the editor, like a player sees it. The menu bar stays hidden, unless `menu_bar: true`.
   - `variables` sets up FancyMenu's variables first: all others get deleted, a text value makes a text variable, an array a list. `shuffle_seed` makes the Shuffle of Arrange List the same every run.
   - `listeners` replaces the listeners of the instance until the recording ends: each with `type`, optional `name`, and a `script` of actions (`action`, `value`) and IF statements (`if`: requirements with `requirement` and `value`, `actions`: the script inside). `[]` leaves none. The old listeners come back at the end, so a crashed recording can leave its listeners in `run_client_docs/config/fancymenu/listener_instances.txt`.
   - `schedulers` replaces the schedulers of the instance until the recording ends, like `listeners`: each with `id`, optional `start_delay`, `tick_delay`, `ticks_to_run` and `start_on_launch`, a `script` like the one of listeners, and `running: true` to start it right away. They get set up after `variables` and `shuffle_seed`, because running schedulers can change variables right away.
   - `custom_guis` replaces the Custom GUIs of the instance until the recording ends, like `listeners`: each with `identifier`, optional `title`, `allow_esc`, `pause_game`, `world_background`, `world_background_overlay`, `popup_mode` and `popup_mode_background_overlay`, and its own layout with `elements`, `vanilla` and `background_image`/`background_color`. These layouts only live in memory. Only with `screen: title`, because the default start screen is the harness's own Custom GUI.
   - `fancymenu_options` sets FancyMenu options by their key in `options.txt` until the recording ends, like the Global Customizations: `{"global_button_label_hover_color": "#FFD35C", "global_button_background_nine_slice": true}`. The old values come back at the end.
   - `no_layout: true` (with `start: screen`) opens the screen without a layout, like a player sees it without customizations.
   - `customization` turns **Current Screen Customization** of the screen on (`true`) or off (`false`) until the recording ends.
   - `elements_behind_vanilla: true` turns on the layout's **Render Custom Elements Behind Vanilla**, like for full-screen image layers that would cover the version and copyright texts.
   - `overlays` and `seed` work like in scene configs. `background_parallax: [x, y]` turns on the **Parallax Effect** of `background_image` with these intensities (scene configs too).
   - `show_widgets: true` keeps the editor widgets, like Layers, which the harness hides by default. At the colossal UI scale, Layers covers the right part of the screen, so keep elements left of it.
   - `window_height` (default 720) records in a taller window, for stills of windows that are too cramped at 720, like the description of Manage Schedulers. Only for stills: `compose.py` and `encode.sh` expect 1280x720 frames.
   - Steps: `waitTicks`, `waitUntil`, `moveTo`, `click`, `press` (optionally with modifiers), `release`, `key` (with badge label), `badge`, `type`, `typeQuickly` (for long texts), `wheel`, `clickMenuEntry` (also in sub-menus), `scrollMenuUntilVisible` (optionally to a given Y), `dragSelectWord`.
   - Resolve targets from the real widgets, never from fixed positions: `menuEntry`, `editorBox`, `screenBox` (outside the editor), `wordEdge`, `lineEnd`, `doneButton`, and for windows `findWindow`/`requireWindow` (by body class), `windowWidgetCenter` (a widget field like `doneButton`), `windowButtonCenter` (a button by its label key), `chooserEntry` (an action in the action chooser), `cellInputAfterLabel` (a text field of a value window by its label key), `cellWidgetWithLabel` (a button of a value window by its label key), `menuBarEntry` (an entry of the menu bar outside the editor, like `screen` for Customization), `variableRow`, `variablesListFreeSpot` and `addEntryRow` (Manage Variables and its list editor), `listenerCell`, `listenerListFreeSpot`, `listenerChooserEntry` and `listenerChooserFreeSpot` (Manage Listeners and its listener chooser), `schedulerCell` (by ID, or `null` for the one that got added with a random ID) and `schedulerListFreeSpot` (Manage Schedulers), `newElementEntry` (an element type in the New Element menu), `newestElementBox` (the element that got added last), `layerRow`, `layerGroupRow`, `layerGroupButton` and `layersFreeSpot` (the Layers widget, which renders at the UI scale with its own coordinates), `requirementRow`, `requirementsListFreeSpot` and `requirementChooserEntry` (Manage Requirements, also of an open group, and its requirement chooser) and `placeholderMenuEntry` (a category or placeholder in the placeholder menu of the text editor, by its label), `customGuiCell` (a Custom GUI in Manage Custom GUIs) and `menuEntryRightPart` (the right part of a context menu entry, away from the info icon after its name).
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

   Over a background image, add `none` as third argument: no dithering makes the GIF about 10% smaller without visible banding.

5. Check frames of the GIF itself: `ffmpeg -ss <seconds> -i showcase.gif -frames:v 1 check.png`.

# Gotchas

- Text elements replace placeholders everywhere, even in code blocks.
- Overlays only react to elements outside of the editor, so record them in scenes, not in the editor.
- Title screen widget identifiers: `mc_titlescreen_singleplayer_button`, `..._multiplayer_button`, `..._realms_button`, `..._options_button`, `..._quit_button`, `..._language_button`, `..._accessibility_button`, `titlescreen_mods_button`, `454370` (the skin icon), `minecraft_logo_widget`, `minecraft_splash_widget`, `minecraft_branding_widget`, `title_screen_copyright_button`, `minecraft_realms_notification_icons_widget`.
- Overlay screenshots: keep at least 13 pixels between buttons, and turn off **Should Be Affected By Decoration Overlays** (`should_be_affected_by_decoration_overlays`) for the logo, splash, branding, copyright and Realms icons.
- Sub-menus of context menus open with a click on their entry. On entries with an info icon after their name, like **Custom GUIs**, `menuEntry` can land on the icon, whose tooltip keeps the sub-menu closed. Click their right part (`menuEntryRightPart`).
- Element-specific entries of an element's right-click menu are below its visible part. Scroll first (`scrollMenuUntilVisible`).
- **Manage Variables** sits below the visible part of the Customization menu too. That menu ends above the usual scroll target, so scroll it to Y 230. Moving over Custom GUIs on the way opens its sub-menu for a moment, which is real.
- The text editor reads the real SDL state for drag and Shift+Arrow selection, which simulated input can't set. Use `dragSelectWord`.
- PiP windows fade in, so what's behind them shows through for a few frames. That's real.
- Don't raise `FREEZE` / `FREEZE_DRIFT` in `compose.py`, or faded-out content stays as a ghost.
- Label key badges with `Ctrl`, even though the harness presses Cmd on macOS.
- The badge font has no arrow symbols like `→` (they show as an empty box). Write `Arrow Right` and so on.
- Dragging several selected elements needs `Ctrl`: `press(..., InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)` and `badge("Ctrl + Drag")`.
- An empty Scrollable Area shows its guide as a big tooltip while hovered at the colossal UI scale. Give it a child, like a title.
- A Text element that's too small for its text shows its own scroll bar.
- Selected elements show their position labels in the editor. They can't be turned off.
- The rotation control of a selected element is a circle around it, which reaches far above it. Select the lowest element for stills, or keep space above it.
- `Shift` while resizing, rotating or tilting reads the real keyboard, which simulated input can't set.
- Anchor overlays while dragging and long menu scrolls change big parts of the frame. The Scrollable Areas GIF is 1.4 MB for 25 seconds, the Action Scripts making GIF too (a button's right-click menu needs 21 wheel turns to reach **Edit Action Script**).
- Value windows of actions focus their first text field with its text selected. Click a field and press `Ctrl + A` before typing, so the GIF shows what gets replaced.
- Manage Listeners is a cell window like the value windows of actions. It hides while its action script editor is open, so value windows get found as the visible cell window (`findValueWindow`).
- FER adds a **FER Test** button to the top-right corner of the title screen. For title screen images without a layout, park its jar outside of `mods` while recording (like `run_client_docs/fer_parked/`) and put it back after.
- Player Entity elements need the Fancy Entity Renderer mod (FER). `run_client_docs/mods` has the FER build for 26.3 from Modrinth; without it, they render an error box. Keep it there.
- With FER 0.6.1 on 26.3, standing players never show their name tag (it's cut off above the head) and shoulder parrots don't render, so the images turn name tags off and show no parrot.
- Auto Skin needs a real Minecraft account name, like `jeb_`. A name without an account keeps the default skin.
- The default checkmark of Checkbox elements is a white square.
- **Copy Element ID** writes the real clipboard of the computer. Save it before such a recording (`pbpaste`) and put it back after (`pbcopy`).
- Scripts get built before they run, so a step can't look at what's open at that time. Say it in the config instead, like `from_open_menu`.
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
