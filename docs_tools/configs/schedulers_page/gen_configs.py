"""Writes the configs of the Schedulers page images: a scheduler that shows a random tip every 4 seconds, making that scheduler in
Manage Schedulers (adding it with its action, then its settings and starting it), and a still of Manage Schedulers."""
import json

BG = "/config/fancymenu/assets/docs_bg.png"
# The same tips as the Variables page, whose Next Tip button shuffles them by hand
TIPS = [
    "Craft a bed before your first night.",
    "Torches keep most mobs away.",
    "Sneak to stop falling off edges.",
]
TIP_TEXT = '^^^\n### Survival Tip\n{"placeholder":"list_get","values":{"name":"tips","index":"0"}}\n^^^'
TEXT = {"id": "tip_text", "x": 110, "y": 140, "w": 420, "h": 58, "look": "pixel", "background": True, "scale": 1.5, "content": TIP_TEXT}
SHUFFLE = {"action": "arrange_list", "value": "tips|||shuffle"}
# With this seed, every shuffle puts another tip first, 8 times in a row (found with a Java Random + Collections.shuffle search)
SEED = 112
TICK_DELAY = 4000


def toast(title, message, duration=5000):
    return json.dumps({"width": 160, "durationMs": duration, "title": title, "message": message, "iconSource": "", "backgroundSource": ""})


tips = {
    "script": "press_keys",
    "start": "screen",
    "background_image": BG,
    "variables": {"tips": TIPS},
    "shuffle_seed": SEED,
    "elements": [TEXT],
    "schedulers": [{"id": "random_tip", "tick_delay": TICK_DELAY, "start_on_launch": True, "script": [SHUFFLE], "running": True}],
    "pointer": [-40, -40],
    "keys": [],
    "hold_ticks": 270,
}

add = {
    "script": "manage_schedulers",
    "part": "add",
    "start": "screen",
    "menu_bar": True,
    "background_image": BG,
    "variables": {"tips": TIPS},
    "elements": [TEXT],
    "schedulers": [],
    "search": "arrange",
    "action": "arrange_list",
    "fields": [
        {"label": "fancymenu.actions.variables.list.edit.list_name", "text": "tips"},
        # Sort A-Z -> Sort Z-A -> Numbers, Smallest First -> Numbers, Biggest First -> Reverse Order -> Shuffle
        {"cycle": "fancymenu.actions.variables.arrange_list.edit.arrangement", "clicks": 5},
    ],
    "hold_ticks": 40,
}

# Goes on where the add GIF stops: the new scheduler with its random ID
NEW_ID = "scheduler_5f3d71f5-3bd7-4718-8e67-8055b676e9fa"
start = {
    "script": "manage_schedulers",
    "part": "settings",
    "start": "screen",
    "background_image": BG,
    "variables": {"tips": TIPS},
    "shuffle_seed": SEED,
    "elements": [TEXT],
    "schedulers": [{"id": NEW_ID, "script": [SHUFFLE]}],
    "select": NEW_ID,
    "settings_fields": [
        {"label": "fancymenu.schedulers.settings.id", "text": "random_tip"},
        {"label": "fancymenu.schedulers.settings.tick_delay", "text": str(TICK_DELAY)},
        {"cycle": "fancymenu.schedulers.settings.start_on_launch", "clicks": 1},
    ],
    "rest_point": [560, 300],
    "hold_ticks": 130,
}

COUNTDOWN = 'seconds_left:{"placeholder":"calc","values":{"decimal":"false","expression":"{"placeholder":"getvariable","values":{"name":"seconds_left"}} - 1"}}'
still = {
    "script": "schedulers_still",
    "start": "screen",
    "background_image": BG,
    "variables": {"tips": TIPS},
    "elements": [],
    "schedulers": [
        {"id": "random_tip", "tick_delay": TICK_DELAY, "start_on_launch": True, "script": [SHUFFLE], "running": True},
        {"id": "break_reminder", "start_delay": 1800000, "tick_delay": 1800000, "start_on_launch": True, "running": True,
         "script": [{"action": "show_toast", "value": toast("Time for a Break", "Stand up, stretch and drink some water.")}]},
        {"id": "event_countdown", "tick_delay": 1000, "ticks_to_run": 10, "script": [{"action": "set_variable", "value": COUNTDOWN}]},
    ],
    "select": "random_tip",
    # At 720, the description of Manage Schedulers is one line tall at the colossal UI scale
    "window_height": 1080,
}

for name, config in (("tips.json", tips), ("add.json", add), ("start.json", start), ("still.json", still)):
    with open(name, "w") as f:
        json.dump(config, f, indent=1)
