"""Writes scenes.json for the Decoration Overlays page: one scene per overlay on the title screen.

The title screen keeps only Singleplayer, Multiplayer, Options and Quit Game, 14 GUI pixels apart, so overlays that pile things up
on elements or hang things under them have room. The logo, splash, branding, copyright and Realms notification icons don't take
part in overlays, so only the buttons do.
"""
import json
import os

DAY = "/config/fancymenu/assets/docs_bg.png"
NIGHT = "/config/fancymenu/assets/docs_bg_night.png"

# GUI pixels of a 480x270 screen
BUTTONS = [
    {"id": "mc_titlescreen_singleplayer_button", "x": 140, "y": 110, "w": 200, "h": 20},
    {"id": "mc_titlescreen_multiplayer_button", "x": 140, "y": 144, "w": 200, "h": 20},
    {"id": "mc_titlescreen_options_button", "x": 140, "y": 178, "w": 93, "h": 20},
    {"id": "mc_titlescreen_quit_button", "x": 247, "y": 178, "w": 93, "h": 20},
]
HIDDEN = ["mc_titlescreen_realms_button", "mc_titlescreen_language_button", "mc_titlescreen_accessibility_button",
          "titlescreen_mods_button", "454370"]
NOT_AFFECTED = ["minecraft_logo_widget", "minecraft_splash_widget", "minecraft_branding_widget", "title_screen_copyright_button",
                "minecraft_realms_notification_icons_widget"]

VANILLA = (BUTTONS
           + [{"id": i, "hidden": True} for i in HIDDEN]
           + [{"id": i, "props": {"should_be_affected_by_decoration_overlays": "false"}} for i in NOT_AFFECTED])

NORMAL = [5, 15, 30]
GROWING = [5, 20, 45]
BURSTS = [4, 7, 10, 13, 16, 19]

# overlay identifier, background, shot seconds, overlay properties
OVERLAYS = [
    ("snowfall", DAY, GROWING, {}),
    ("rainfall", DAY, NORMAL, {}),
    ("leaves", DAY, NORMAL, {}),
    ("cherry_blossoms", DAY, NORMAL, {}),
    ("fog", DAY, NORMAL, {}),
    ("frost", DAY, GROWING, {}),
    ("clouds", DAY, NORMAL, {}),
    ("sunbeams", DAY, NORMAL, {}),
    ("birds", DAY, NORMAL, {}),
    ("butterflies", DAY, NORMAL, {}),
    ("bubbles", DAY, NORMAL, {}),
    ("overgrowth", DAY, GROWING, {}),
    ("cobwebs", DAY, GROWING, {}),
    ("confetti", DAY, BURSTS, {}),
    ("buddy", DAY, NORMAL, {}),
    ("string_lights", NIGHT, NORMAL, {}),
    ("fireflies", NIGHT, NORMAL, {}),
    ("fireworks", NIGHT, BURSTS, {}),
    ("sky_lanterns", NIGHT, NORMAL, {}),
    ("aurora", NIGHT, NORMAL, {}),
    ("shooting_stars", NIGHT, BURSTS, {}),
    ("bats", NIGHT, NORMAL, {}),
    ("embers", NIGHT, GROWING, {}),
]

# Only the two short strings that hang down beside the logo, so the lights don't cross it
WINTER_LIGHTS = {"look": "detailed", "string_lights_pine_garland": "true"}
for position in ("left_center_to_top_center", "right_center_to_top_center", "top_left_to_top_right"):
    WINTER_LIGHTS[f"string_lights_{position}"] = "false"
for position in ("loose_left_top", "loose_right_top"):
    WINTER_LIGHTS[f"string_lights_{position}"] = "true"
    WINTER_LIGHTS[f"string_lights_{position}_christmas_mode"] = "true"

# scene name, background, shot seconds, overlays
SCENES = [(overlay, background, shots, [{"type": overlay, "props": props}]) for overlay, background, shots, props in OVERLAYS]
SCENES += [
    ("hero_winter", NIGHT, GROWING, [{"type": "snowfall", "props": {}}, {"type": "string_lights", "props": WINTER_LIGHTS}]),
    ("snowfall_night", NIGHT, GROWING, [{"type": "snowfall", "props": {}}]),
    ("rainfall_night", NIGHT, NORMAL, [{"type": "rainfall", "props": {}}]),
    # Dark bats don't show on the night background, and default fireflies and birds are tiny
    ("bats_day", DAY, NORMAL, [{"type": "bats", "props": {}}]),
    ("bats_day_2x", DAY, NORMAL, [{"type": "bats", "props": {"size": "2"}}]),
    ("fireflies_2x", NIGHT, [5, 10, 15, 20, 25, 30], [{"type": "fireflies", "props": {"look": "simple", "firefly_scale": "2.0"}}]),
    ("birds_2x", DAY, [5, 10, 15, 20, 25, 30], [{"type": "birds", "props": {"size": "2", "amount": "50"}}]),
    ("sky_lanterns_2x", NIGHT, [5, 10, 15, 20, 25, 30], [{"type": "sky_lanterns", "props": {"size": "2", "amount": "25"}}]),
]

scenes = []
for name, background, shots, overlays in SCENES:
    scenes.append({
        "name": name,
        "screen": "title",
        "background_image": background,
        "seed": 7,
        "pointer": [-40, -40],
        "shots": shots,
        "elements": [],
        "vanilla": VANILLA,
        "overlays": overlays,
    })

config = {"gui_width": 480, "gui_height": 270, "gui_scale": 2, "scenes": scenes}
with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "scenes.json"), "w") as f:
    json.dump(config, f, indent=1)
print(len(scenes), "scenes")
