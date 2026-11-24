"""Writes the configs of the Requirements page GIFs: giving a Text element an Is Element Hovered requirement in the editor, hovering the
button on the title screen, and combining two requirements in an Or group."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"
MULTIPLAYER = "mc_titlescreen_multiplayer_button"
HOVERED = "fancymenu_visibility_requirement_is_element_hovered"

# An info box right of the Multiplayer button of the title screen, which only shows while the button is hovered
INFO = {
    "id": "server_info", "x": 432, "y": 157, "w": 180, "h": 30, "look": "pixel", "background": True,
    "content": "**play.example.net**\nSurvival with friends, open 24/7",
}

# A greeting above the title screen buttons, for the group example
XMAS = {
    "id": "xmas_greeting", "x": 220, "y": 104, "w": 200, "h": 26, "look": "pixel", "background": True,
    "content": "^^^\n**Merry Christmas!**\n^^^",
}

add = {
    "script": "manage_requirements",
    "screen": "title",
    "background_image": BG,
    "elements": [INFO],
    "copy_id": MULTIPLAYER,
    "target": "server_info",
    "requirement_steps": [
        {"add": HOVERED, "search": "hovered", "via": "menu", "paste": True, "read_ticks": 24},
        {"rest": 26},
    ],
    "free_spot": [110, 250],
    "hold_ticks": 50,
}

hover = {
    "script": "hover_elements",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "elements": [dict(INFO, requirements=[{"requirement": HOVERED, "value": MULTIPLAYER}])],
    "pointer": [110, 250],
    "hovers": [
        {"target": "mc_titlescreen_singleplayer_button", "ticks": 26},
        {"target": MULTIPLAYER, "ticks": 50},
        {"point": [110, 250], "ticks": 30},
        {"target": MULTIPLAYER, "ticks": 50},
    ],
    "hold_ticks": 10,
}

group = {
    "script": "manage_requirements",
    "screen": "title",
    "background_image": BG,
    "elements": [XMAS],
    "variables": {},
    "target": "xmas_greeting",
    "open_directly": True,
    "close": False,
    "requirement_steps": [
        {"add_group": "key"},
        {"group_mode": "group_1"},
        {"open_group": "group_1", "from_open_menu": True},
        {"add": "fancymenu_visibility_requirement_is_realtime_month", "search": "month", "value": "12", "read_ticks": 8},
        {"add": "fancymenu_visibility_requirement_is_variable_value", "search": "variable value", "read_ticks": 8, "fields": [
            {"label": "fancymenu.requirements.is_variable_value.var_name", "text": "xmas_lights"},
            {"label": "fancymenu.requirements.is_variable_value.var_value", "text": "on"},
        ]},
        {"rest": 6},
        {"done": True},
        {"rest": 4},
    ],
    "hold_ticks": 36,
}

for name, config in (("add.json", add), ("hover.json", hover), ("group.json", group)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
