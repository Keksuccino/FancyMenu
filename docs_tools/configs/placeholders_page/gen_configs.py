"""Writes the configs of the Placeholders page images.

- add.json: the GIF of adding the Player Name placeholder to a Text element in the text editor (script add_placeholder).
- scenes.json: the welcome panel screenshot, and a check scene that is no docs image. The check scene shows the real output of
  placeholders whose format the page states (GUI scale, mouse position, number formats and so on), to compare with the page.

Usage: python3 gen_configs.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
BG = "/config/fancymenu/assets/docs_bg.png"


def ph(identifier, **values):
    if not values:
        return '{"placeholder":"%s"}' % identifier
    inner = ",".join('"%s":"%s"' % (k, v) for k, v in values.items())
    return '{"placeholder":"%s","values":{%s}}' % (identifier, inner)


# The code block of the Examples section shows exactly this text
WELCOME = "\n".join([
    "### Welcome back, " + ph("playername") + "!",
    "Minecraft " + ph("mcversion") + " with " + ph("loadedmods") + " mods",
    "It's " + ph("realtimehour") + ":" + ph("realtimeminute"),
])

CHECKS = [
    ("guiscale", ph("guiscale")),
    ("guiwidth x guiheight", ph("guiwidth") + " x " + ph("guiheight")),
    ("mouse (pointer 300, 200)", ph("mouseposx") + ", " + ph("mouseposy")),
    ("playername", ph("playername")),
    ("mcversion / loader", ph("mcversion") + " / " + ph("loadername") + " " + ph("loaderver")),
    ("mods loaded / total", ph("loadedmods") + " / " + ph("totalmods")),
    ("modversion fancymenu / missing", ph("modversion", modid="fancymenu") + " / " + ph("modversion", modid="no_such_mod")),
    ("option renderDistance / lang", ph("minecraft_option_value", name="renderDistance") + " / " + ph("minecraft_option_value", name="lang")),
    ("keybind jump / inventory / missing", ph("keybind_key", keybind="key.jump") + " / " + ph("keybind_key", keybind="key.inventory") + " / [" + ph("keybind_key", keybind="key.nothing") + "]"),
    ("screenid", ph("screenid")),
    ("calc 2+3*4 decimal / whole", ph("calc", decimal="true", expression="2 + 3 * 4") + " / " + ph("calc", decimal="false", expression="7 / 2")),
    ("calc 7 % 3, log(e), pow(2,3)", ph("calc", decimal="true", expression="7 % 3") + ", " + ph("calc", decimal="true", expression="log(e)") + ", " + ph("calc", decimal="true", expression="pow(2, 3)")),
    ("negnum 10 / 10.5, absnum -10", ph("negnum", num="10") + " / " + ph("negnum", num="10.5") + ", " + ph("absnum", num="-10")),
    ("round 2.5 / 3.14159@2 / 3.10@2", ph("math_round", num="2.5") + " / " + ph("math_round", num="3.14159", decimals="2") + " / " + ph("math_round", num="3.10", decimals="2")),
    ("floor 3.7 / -3.2, ceil 3.2 / -3.7", ph("math_floor", num="3.7") + " / " + ph("math_floor", num="-3.2") + ", " + ph("math_ceil", num="3.2") + " / " + ph("math_ceil", num="-3.7")),
    ("base 255->16, 67->2", ph("number_base_convert", input="255", from_base="10", to_base="16") + ", " + ph("number_base_convert", input="67", from_base="10", to_base="2")),
    ("maxnum 10 15 / random 1-6", ph("maxnum", first="10", second="15") + " / " + ph("random_number", min="1", max="6")),
    ("split Steve,Alex,Notch @1", ph("split_text", input="Steve,Alex,Notch", regex=",", split_index="1")),
    ("switch 2 / no match no default", ph("switch_case", value="2", cases="1:Easy Mode,2:Hard Mode", default="Normal Mode") + " / [" + ph("switch_case", value="9", cases="1:Easy Mode,2:Hard Mode") + "]"),
    ("crop / trim", ph("crop_text", text="hello world", remove_from_start="1", remove_from_end="1") + " / [" + ph("trim_text", text="  Welcome!  ") + "]"),
    ("cases", ph("title_case_text", text="hello world") + ", " + ph("sentence_case_text", text="hello world. this is fancymenu!") + ", " + ph("kebab_case_text", text="Hello World")),
    ("text width / height", ph("text_width", text="Hello World!") + " / " + ph("text_height", text="First line\\nSecond line")),
    ("local menu.singleplayer", ph("local", key="menu.singleplayer")),
    ("base64", ph("base64_encode", text="Hello World") + " / " + ph("base64_decode", text="SGVsbG8gV29ybGQ=")),
    ("json direct", ph("json", source='{"news":{"title":"Big Update"}}', json_path="$.news.title")),
    ("world placeholders without world", ph("current_player_health") + ", " + ph("current_player_level") + ", [" + ph("player_gamemode") + "], " + ph("slot_item", slot="0")),
    ("saves count / names", ph("level_save_count") + " / [" + ph("level_save_names") + "]"),
    ("last world", "[" + ph("last_world_server", type="both", full_world_path="false") + "]"),
    ("os / java / jvm", ph("osname") + " / " + ph("javaver") + " / " + ph("jvmname")),
    ("gpu / gl", ph("gpuinfo") + " / " + ph("glver")),
    ("cpu / usage jvm os", ph("cpuinfo") + " / " + ph("jvmcpu") + " " + ph("oscpu")),
    ("ram used / max / %", ph("usedram") + " / " + ph("maxram") + " / " + ph("percentram")),
    ("uptime / unix", ph("uptime_duration") + " / " + ph("unix_time")),
    ("realtime", ph("realtimeday") + "." + ph("realtimemonth") + "." + ph("realtimeyear") + " " + ph("realtimehour") + ":" + ph("realtimeminute") + ":" + ph("realtimesecond") + " 12h " + ph("realtimehour", twelve_hour_format="true") + " UTC " + ph("realtimehour", timezone="UTC")),
    ("getvariable missing / list size missing", ph("getvariable", name="no_such_variable") + " / " + ph("list_size", name="no_such_list")),
    ("unknown placeholder", ph("no_such_placeholder")),
]
CHECK_TEXT = "\n".join(f"{label}: {value}" for label, value in CHECKS)

scenes = {
    "gui_width": 960, "gui_height": 540, "gui_scale": 2,
    "scenes": [
        {"name": "welcome", "wait": 3, "background_image": BG, "pointer": [-40, -40], "elements": [
            {"id": "welcome_text", "x": 12, "y": 12, "w": 172, "h": 44, "look": "pixel", "background": True, "content": WELCOME},
        ]},
        {"name": "check", "wait": 4, "background_color": "#202020", "pointer": [300, 200], "elements": [
            {"id": "check_text", "x": 8, "y": 6, "w": 944, "h": 528, "look": "pixel", "background": False, "content": CHECK_TEXT},
        ]},
    ],
}

add = {
    "script": "add_placeholder",
    "initial_text": "### Welcome back\nHave fun, ",
    "category": "Player",
    "placeholder": "Player Name",
    "typed_after": "!",
    "background_image": BG,
}

for name, data in (("scenes.json", scenes), ("add.json", add)):
    with open(os.path.join(HERE, name), "w") as f:
        json.dump(data, f, indent=1)
print("wrote scenes.json and add.json")
