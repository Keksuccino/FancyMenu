"""Writes the configs of the Action Scripts page GIFs: making a button's action script in the editor, and clicking the button on the title screen."""
import json

BUTTON = {"type": "custom_button", "id": "whats_new_button", "x": 220, "y": 254, "w": 200, "h": 20, "props": {"label": "What's New?"}}
TITLE = "What's New in 1.2"
MESSAGE = "New quests and a boss!"
BG = "/config/fancymenu/assets/docs_bg.png"

make = {
    "script": "action_script",
    "screen": "title",
    "background_image": BG,
    "elements": [BUTTON],
    "target": "whats_new_button",
    "search": "toast",
    "action": "show_toast",
    "fields": [
        {"label": "fancymenu.actions.show_toast.edit.title_text", "text": TITLE},
        {"label": "fancymenu.actions.show_toast.edit.message", "text": MESSAGE},
    ],
}

toast = {"width": 160, "durationMs": 5000, "title": TITLE, "message": MESSAGE, "iconSource": "", "backgroundSource": ""}
use = {
    "script": "click_element",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "elements": [dict(BUTTON, actions=[{"action": "show_toast", "value": json.dumps(toast)}])],
    "target": "whats_new_button",
    "hold_ticks": 110,
}

for name, config in (("make_script.json", make), ("use_script.json", use)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
