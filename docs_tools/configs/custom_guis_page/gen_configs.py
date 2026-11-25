"""Writes the configs of the Custom GUIs page GIFs: a Modpack Info button on the title screen that opens a Custom GUI as a popup, which
its Back button closes again (popup.json), and making that Custom GUI in Manage Custom GUIs and opening it (new.json).

Needs frostfall_emblem.png (make_emblem.py draws it) in run_client_docs/config/fancymenu/assets/.

Usage: python3 gen_configs.py [output folder, default: this folder]
"""
import json, os, sys

BG = "/config/fancymenu/assets/docs_bg.png"
EMBLEM = "/config/fancymenu/assets/frostfall_emblem.png"
GUI = "modpack_info"

HEADER = "# Frostfall\nVersion 2.4 for Minecraft 26.3"
INFO = ("### Getting Started\n"
        "- Press **K** to open your quest book\n"
        "- Light a campfire before your first night\n"
        "- Ask on our Discord if you get stuck")

open_button = {"type": "custom_button", "id": "modpack_info_button", "x": 220, "y": 254, "w": 200, "h": 20,
               "props": {"label": "Modpack Info"}, "actions": [{"action": "opengui", "value": GUI}]}

info_gui = {
    "identifier": GUI,
    "popup_mode": True,
    "elements": [
        # A Text element with a background and no text is the panel, below the logo of the title screen, so it stays visible
        {"id": "info_panel", "x": 165, "y": 92, "w": 310, "h": 172, "look": "pixel", "background": True, "content": " "},
        {"type": "image", "id": "frostfall_emblem", "x": 178, "y": 102, "w": 64, "h": 64, "props": {"source": EMBLEM}},
        {"id": "info_header", "x": 250, "y": 112, "w": 215, "h": 48, "look": "pixel", "background": False, "content": HEADER},
        {"id": "info_text", "x": 178, "y": 174, "w": 290, "h": 96, "look": "pixel", "background": False, "content": INFO},
        {"type": "custom_button", "id": "back_button", "x": 371, "y": 236, "w": 92, "h": 20,
         "props": {"label": "Back"}, "actions": [{"action": "back_to_last_screen"}]},
    ],
}

popup = {
    "script": "click_through",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "elements": [open_button],
    "custom_guis": [info_gui],
    "clicks": [
        {"target": "modpack_info_button", "wait_ticks": 120},
        {"target": "back_button", "wait_ticks": 20},
    ],
    "rest_point": [520, 320],
    "hold_ticks": 50,
}

new = {
    "script": "manage_custom_guis",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "menu_bar": True,
    "elements": [],
    "custom_guis": [],
    "identifier": GUI,
    "cycles": ["fancymenu.custom_guis.build.popup_mode"],
    "rest_point": [520, 320],
    "hold_ticks": 70,
}

out = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
for name, config in (("popup.json", popup), ("new.json", new)):
    with open(os.path.join(out, name), "w") as f:
        json.dump(config, f, indent=1)
