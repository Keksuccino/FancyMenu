"""Writes the configs of the Listeners page images: making a listener in Manage Listeners, the listener running on the title screen, and stills of the listener windows."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"
TOAST_TITLE = "Key Pressed"
TOAST_MESSAGE = "You pressed $$key_name!"


def toast(title, message, duration=5000):
    return json.dumps({"width": 160, "durationMs": duration, "title": title, "message": message, "iconSource": "", "backgroundSource": ""})


make = {
    "script": "manage_listeners",
    "start": "screen",
    "screen": "title",
    "menu_bar": True,
    "background_image": BG,
    "elements": [],
    "listeners": [],
    "listener": "keyboard_key_pressed",
    "listener_search": "key pressed",
    "search": "toast",
    "action": "show_toast",
    "fields": [
        {"label": "fancymenu.actions.show_toast.edit.title_text", "text": TOAST_TITLE},
        {"label": "fancymenu.actions.show_toast.edit.message", "text": TOAST_MESSAGE},
    ],
    "keys": [{"key": "h", "badge": "H"}],
    "hold_ticks": 60,
}

trigger = {
    "script": "press_keys",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "elements": [],
    "listeners": [
        {"type": "keyboard_key_pressed", "name": "Key Toast", "script": [{"action": "show_toast", "value": toast(TOAST_TITLE, TOAST_MESSAGE, 4000)}]},
    ],
    "pointer": [-40, -40],
    "keys": [{"key": "h", "badge": "H"}, {"key": "f", "badge": "F"}, {"key": "m", "badge": "M"}],
    "key_gap_ticks": 30,
    "hold_ticks": 70,
}

IS_H = '["mode":"equals","text":"$$key_name","compare_with":"H"]$'
still = {
    "script": "listeners_still",
    "start": "screen",
    "screen": "title",
    "background_image": BG,
    "elements": [],
    "listeners": [
        {"type": "keyboard_key_pressed", "name": "Help Key", "script": [
            {"if": [{"requirement": "fancymenu_visibility_requirement_is_text", "value": IS_H}], "actions": [{"action": "opengui", "value": "help_screen"}]},
        ]},
        {"type": "world_entered", "name": "Welcome Back", "script": [{"action": "show_toast", "value": toast("Welcome back!", "Have fun in $$world_name!")}]},
        {"type": "start_joining_server", "name": "Remember Last Server", "script": [{"action": "set_variable", "value": "last_server:$$server_ip"}]},
        {"type": "player_death", "name": "Log Deaths", "script": [{"action": "print_to_log", "value": "Died on day $$days_survived: $$death_reason_string"}]},
        {"type": "screenshot_taken", "script": [{"action": "show_toast", "value": toast("Screenshot saved", "$$screenshot_path")}]},
    ],
    "select": "Help Key",
    "description_wheel_turns": 8,
    "listener_search": "achievement",
    "listener": "achievement_unlocked",
}

for name, config in (("make.json", make), ("trigger.json", trigger), ("still.json", still)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
