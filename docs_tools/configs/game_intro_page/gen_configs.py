"""Writes the configs of the Game Intro page GIFs.

Usage: python3 gen_configs.py <out dir>

- play.json: the Skyhaven intro plays and fades into the Title screen, whose background is the intro's last scene (the hero GIF, no pointer).
- setup.json: on the same Title screen, sets the intro through Customization -> Global Customizations -> Game Intro -> Set Game Intro, plays it with Re-Trigger
  Game Intro and skips it with Space. Its frame with the open Game Intro menu has a mark event (menu) for a still.

Both need skyhaven_intro.apng and skyhaven_menu.png (make_intro.py) in run_client_docs/config/fancymenu/assets/ while recording. Move them to the trash after,
so the file choosers of other recordings don't list it."""
import json, os, sys

INTRO = "skyhaven_intro.apng"
# The Title screen of the Skyhaven modpack: the intro's last scene as background, so the intro fades into its own menu, and GIF frames
# stay small (Minecraft's detailed panorama makes every full frame big)
BASE = {"start": "screen", "screen": "title", "customization": True, "script": "game_intro", "elements": [],
        "background_image": "/config/fancymenu/assets/skyhaven_menu.png"}
# Default settings of the Game Intro, so the GIFs show what players get without changing anything
DEFAULTS = {"allow_game_intro_skip": True, "game_intro_fade_out": True, "game_intro_ignore_animation_loops": True, "custom_game_intro_skip_text": ""}

CONFIGS = {
    "play": dict(BASE, part="play", hold_ticks=70,
                 fancymenu_options=dict(DEFAULTS, game_intro_animation_name="[source:local]/config/fancymenu/assets/" + INTRO)),
    "setup": dict(BASE, part="setup", menu_bar=True, file=INTRO, menu_ticks=44, skip_after_ticks=46, hold_ticks=56,
                  skip_key={"key": "space", "badge": "Space"},
                  fancymenu_options=dict(DEFAULTS, game_intro_animation_name="")),
}

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for name, config in CONFIGS.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(config, f, indent=1)
    print(name + ".json")
