"""Writes the config of the Element Visibility GIF (toggle.json): outside the editor, a welcome text shows up after a Delay Appearance
with a fade-in, then a Server Rules button toggles a rules panel through a variable. The panel has a Loading Requirement for that
variable and fades in and out.

Usage: python3 gen_configs.py [output folder, default: this folder]
"""
import json, os, sys

BG = "/config/fancymenu/assets/docs_bg.png"
IS_VARIABLE_VALUE = "fancymenu_visibility_requirement_is_variable_value"
RULES_SHOWN = [{"requirement": IS_VARIABLE_VALUE, "value": "show_rules:true"}]

title = {"x": 120, "y": 26, "w": 400, "h": 44, "look": "pixel", "background": False, "scale": 2,
         "content": "^^^\n## Skyblock Odyssey\n^^^"}
welcome = {"x": 150, "y": 68, "w": 340, "h": 32, "look": "pixel", "background": False, "scale": 1.5,
           "content": "^^^\nWelcome back, have fun!\n^^^",
           "props": {"appearance_delay": "every_time", "appearance_delay_seconds": "3", "fade_in_v2": "every_time", "fade_in_speed": "1.5"}}
rules = {"id": "rules_panel", "x": 190, "y": 108, "w": 260, "h": 92, "look": "pixel", "background": True, "scale": 1.5,
         "content": "### Server Rules\n1. Be kind to others\n2. No griefing\n3. Have fun!",
         "requirements": RULES_SHOWN,
         "props": {"fade_in_v2": "every_time", "fade_out": "every_time", "fade_in_speed": "2", "fade_out_speed": "2"}}
button = {"type": "custom_button", "id": "rules_button", "x": 245, "y": 300, "w": 150, "h": 20, "props": {"label": "Server Rules"},
          "actions": [{"if": RULES_SHOWN,
                       "actions": [{"action": "set_variable", "value": "show_rules:false"}],
                       "else": [{"action": "set_variable", "value": "show_rules:true"}]}]}

toggle = {
    "script": "click_element",
    "start": "screen",
    "background_image": BG,
    "variables": {"show_rules": "false"},
    "elements": [title, welcome, rules, button],
    "target": "rules_button",
    "start_wait_ticks": 70,
    "clicks": 2,
    "click_gap_ticks": 80,
    "hold_ticks": 60,
}

out = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
json.dump(toggle, open(os.path.join(out, "toggle.json"), "w"), indent=1)
print("wrote", out)
