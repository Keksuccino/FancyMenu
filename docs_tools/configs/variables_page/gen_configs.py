"""Writes the configs of the Variables page GIFs: making a text variable and a list in Manage Variables, adding Arrange List (Shuffle)
to a Next Tip button, and clicking that button outside the editor."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"
TIPS = [
    "Craft a bed before your first night.",
    "Torches keep most mobs away.",
    "Sneak to stop falling off edges.",
]
TIP_TEXT = '^^^\n### Tip of the Day\n{"placeholder":"list_get","values":{"name":"tips","index":"0"}}\n^^^'
TEXT = {"id": "tip_text", "x": 110, "y": 112, "w": 420, "h": 58, "look": "pixel", "background": True, "scale": 1.5, "content": TIP_TEXT}
BUTTON = {"type": "custom_button", "id": "next_tip_button", "x": 245, "y": 186, "w": 150, "h": 20, "props": {"label": "Next Tip"}}

manage = {
    "script": "manage_variables",
    "start": "screen",
    "screen": "title",
    "menu_bar": True,
    "background_image": BG,
    "elements": [],
    "variables": {},
    "text_variable": {"name": "menu_theme", "value": "dark"},
    "list_variable": {"name": "tips", "entries": TIPS},
}

action = {
    "script": "action_script",
    "background_image": BG,
    "variables": {"tips": TIPS},
    "elements": [TEXT, BUTTON],
    "target": "next_tip_button",
    "search": "arrange",
    "action": "arrange_list",
    "fields": [
        {"label": "fancymenu.actions.variables.list.edit.list_name", "text": "tips"},
        # Sort A-Z -> Sort Z-A -> Numbers, Smallest First -> Numbers, Biggest First -> Reverse Order -> Shuffle
        {"cycle": "fancymenu.actions.variables.arrange_list.edit.arrangement", "clicks": 5},
    ],
}

use = {
    "script": "click_element",
    "start": "screen",
    "background_image": BG,
    "variables": {"tips": TIPS},
    # With this seed, every click shows another tip first (found with a quick Collections.shuffle search)
    "shuffle_seed": 11,
    "elements": [TEXT, dict(BUTTON, actions=[{"action": "arrange_list", "value": "tips|||shuffle"}])],
    "target": "next_tip_button",
    "clicks": 3,
    "click_gap_ticks": 36,
    "hold_ticks": 60,
}

for name, config in (("manage.json", manage), ("action_script.json", action), ("use.json", use)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
