"""Writes the configs of the Layout Editor page images: the basics GIF (moving, widening and nudging a title, selecting buttons with a
rectangle, copying, pasting, dragging the copies and undoing) and a still of the editor with its Layers widget."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"
TITLE = {"id": "pack_title", "x": 160, "y": 70, "w": 320, "h": 44, "look": "pixel", "background": False, "scale": 2,
         "content": "^^^\n**Skyblock Adventures**\n^^^"}
PLAY = {"type": "custom_button", "id": "play_button", "x": 245, "y": 160, "w": 150, "h": 20, "props": {"label": "Play"}}
SERVERS = {"type": "custom_button", "id": "servers_button", "x": 245, "y": 186, "w": 150, "h": 20, "props": {"label": "Join Server"}}
ELEMENTS = [TITLE, PLAY, SERVERS]

basics = {
    "script": "editor_basics",
    "background_image": BG,
    "elements": ELEMENTS,
    "target": "pack_title",
    "move": [0, -30],
    "widen_by": 40,
    "nudge": 3,
    "buttons": ["play_button", "servers_button"],
    "copies_move": [0, 70],
    "free_spot": [560, 300],
}

# The Layers widget covers the right part of the screen at the colossal UI scale, so the overview moves the elements to the left.
# It selects the lower button, because the rotation control of a selected element reaches far above it, and keeps the upper
# button out of the way of the position labels of the selected one.
OVERVIEW_ELEMENTS = [dict(TITLE, x=TITLE["x"] - 110, y=50), dict(PLAY, x=PLAY["x"] - 110, y=134), dict(SERVERS, x=SERVERS["x"] - 110)]
overview = {
    "script": "select_and_scroll",
    "background_image": BG,
    "elements": OVERVIEW_ELEMENTS,
    "show_widgets": True,
    "target": "servers_button",
    "click": [20, 10],
    "wheel_turns": 0,
}

for name, config in (("basics.json", basics), ("overview.json", overview)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
