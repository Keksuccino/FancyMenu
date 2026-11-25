"""Writes the configs of the Rotating and Tilting Elements page images: a still of what rotation and the two tilts do (light boxes show
where the elements would be without them), the GIF of rotating a sticker and tilting a news panel with their controls in the editor,
and the GIF of the pointer on a rotated button, which only reacts where the button shows."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"


def panel(pid, x, y, title, value, props):
    return {"id": pid, "x": x, "y": y, "w": 150, "h": 84, "look": "pixel", "background": True,
            "content": f"^^^\n### {title}\n{value}\n^^^", "props": props}


def ghost(pid, x, y):
    return {"type": "shape", "id": pid, "x": x, "y": y, "w": 150, "h": 84, "props": {"color": "#FFFFFF38"}}


# Rotation reaches about 18 GUI pixels above and below its box, so the rows keep 34 pixels apart
CELLS = [
    ("rotation", 300, 150, "Rotation", "15 degrees", {"rotation_degrees": "15.0"}),
    ("vertical", 490, 150, "Vertical Tilt", "45 degrees", {"vertical_tilt_degrees": "45.0"}),
    ("horizontal", 300, 268, "Horizontal Tilt", "45 degrees", {"horizontal_tilt_degrees": "45.0"}),
    ("both", 490, 268, "Both Tilts", "35 degrees each", {"vertical_tilt_degrees": "35.0", "horizontal_tilt_degrees": "35.0"}),
]
axes_elements = [ghost(f"{c[0]}_ghost", c[1], c[2]) for c in CELLS] + [panel(*c) for c in CELLS]
# Check scene, no docs image: the spinning example of the page turns the element once every 3.6 seconds, so the two shots differ by about 90 degrees
SPIN = '{"placeholder":"calc","values":{"expression":"{"placeholder":"unix_time"} / 10 % 360"}}'
spin = {"id": "spin_check", "x": 405, "y": 230, "w": 150, "h": 84, "look": "pixel", "background": True,
        "content": "^^^\n### Spinning\n^^^", "props": {"advanced_rotation_mode": "true", "advanced_rotation_degrees": SPIN}}
axes = {"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
    {"name": "axes", "wait": 3, "background_image": BG, "pointer": [-40, -40], "elements": axes_elements},
    {"name": "spin_check", "wait": 2, "shots": [1.0, 1.9], "background_image": BG, "pointer": [-40, -40], "elements": [spin]},
]}

TITLE = {"id": "pack_title", "x": 30, "y": 40, "w": 320, "h": 44, "look": "pixel", "background": False, "scale": 2,
         "content": "^^^\n**Skyblock Adventures**\n^^^"}
PLAY = {"type": "custom_button", "id": "play_button", "x": 115, "y": 150, "w": 150, "h": 20, "props": {"label": "Play"}}
SERVERS = {"type": "custom_button", "id": "servers_button", "x": 115, "y": 176, "w": 150, "h": 20, "props": {"label": "Join Server"}}
NEWS = {"id": "news_panel", "x": 380, "y": 116, "w": 210, "h": 104, "look": "pixel", "background": True,
        "content": "### Patch Notes\n- Bigger starter islands\n- 12 quests to discover\n- Faster minions"}
STICKER = {"id": "season_sticker", "x": 476, "y": 96, "w": 130, "h": 24, "look": "pixel", "background": True,
           "content": "^^^\n**Season 3 is live!**\n^^^"}

editor = {
    "script": "rotate_and_tilt",
    "background_image": BG,
    "elements": [TITLE, PLAY, SERVERS, NEWS, STICKER],
    "steps": [
        {"target": "season_sticker", "rotate_to": 12, "ticks": 32, "rest_ticks": 22},
        {"target": "news_panel", "vertical_tilt": 25, "ticks": 32},
        {"target": "news_panel", "horizontal_tilt": -30, "ticks": 32, "rest_ticks": 24},
    ],
    "free_spot": [300, 320],
    "hold_ticks": 70,
}

# The straight box of the button would reach its top-right corner at (416, 132), but rotated by 15 degrees, its right end sits lower.
# The pointer tries that corner first (no hover), then the right end of the button as it shows (hover), and the same at the left end,
# where the straight box would reach down to (220, 150), while the button shows higher up.
BUTTON = {"type": "custom_button", "id": "rotated_button", "x": 220, "y": 130, "w": 200, "h": 20,
          "props": {"label": "Join Server", "rotation_degrees": "15.0"}}
hover = {
    "script": "hover_elements",
    "start": "screen",
    "background_image": BG,
    "elements": [BUTTON],
    "pointer": [470, 90],
    "hovers": [
        {"point": [416, 133], "ticks": 44},
        {"point": [402, 161], "ticks": 44},
        {"point": [225, 147], "move_ticks": 24, "ticks": 44},
        {"point": [238, 118], "ticks": 44},
    ],
    "hold_ticks": 10,
}

for name, config in (("axes.json", axes), ("editor.json", editor), ("hover.json", hover)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
