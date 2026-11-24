"""Writes the configs of the Player Entities page images (and the Player Entity image of the Elements page): five players that show
the main settings, a GIF of heads and bodies following the mouse, and a still of the Entity Pose window.

Player Entities need the Fancy Entity Renderer mod (FER) in run_client_docs/mods. The skins are Minecraft's built-in ones, except the
Auto Skin example, which loads the skin of a real player name and needs an internet connection.

Usage: python3 gen_configs.py [output folder, default: this folder]
"""
import json, os, sys

BG = "/config/fancymenu/assets/docs_bg.png"
OFF = [-40, -40]
# Name tags get cut off above standing players in FER 0.6.1 on 26.3, so the images hide them
STILL = {"head_follows_mouse": "false", "body_follows_mouse": "false", "showname": "false"}


def skin(name, model="wide"):
    return "[source:location]minecraft:textures/entity/player/" + model + "/" + name + ".png"


def player(id, x, y, w, h, props):
    return {"type": "player_entity_v2", "id": id, "x": x, "y": y, "w": w, "h": h, "props": props}


def caption(x, y, w, label):
    return {"x": x, "y": y, "w": w, "h": 22, "look": "pixel", "background": False, "content": "^^^\n**" + label + "**\n^^^"}


def builtin(name, model="wide", **extra):
    props = {"playername": name.capitalize(), "skin_source": skin(name, model), "slim": "true" if model == "slim" else "false"}
    props.update(STILL)
    props.update(extra)
    return props


ARMOR = {
    "head_wearable": "minecraft:diamond_helmet:::true",
    "chest_wearable": "minecraft:diamond_chestplate:::true",
    "legs_wearable": "minecraft:diamond_leggings:::true",
    "feet_wearable": "minecraft:diamond_boots:::true",
    "right_hand_wearable": "minecraft:diamond_sword:::true",
    "left_hand_wearable": "minecraft:shield:::false",
}
# Waving with the right arm, head a bit to the side
WAVE = {"right_arm_z_rot": "140", "right_arm_x_rot": "-10", "head_z_rot": "-8", "headrotationy": "15"}

PW, PH, GAP = 120, 230, 170
showcase = [
    # jeb_ is a real player with a cape; turned around and looking over the shoulder, so the cape shows
    ("player_auto", "Auto Skin and Cape", dict({"playername": "jeb_", "auto_skin": "true", "auto_cape": "true", "bodyrotationy": "150", "headrotationy": "-70"}, **STILL)),
    ("player_armor", "Armor and Tools", builtin("steve", bodyrotationy="25", **ARMOR)),
    ("player_crouch", "Crouching", builtin("alex", "slim", player_pose="crouching", bodyrotationy="35")),
    ("player_pose", "Entity Pose", builtin("sunny", **WAVE)),
    ("player_baby", "Is Baby", builtin("kai", is_baby="true")),
]
showcase_elements = []
for i, (id, label, props) in enumerate(showcase):
    x = 60 + i * GAP
    showcase_elements.append(player(id, x, 70, PW, PH, props))
    showcase_elements.append(caption(x - 25, 310, PW + 50, label))

scenes = {"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
    {"name": "player_entities_showcase", "wait": 8, "pointer": OFF, "background_image": BG, "elements": showcase_elements},
    # The Elements page shows a new Player Entity, with the defaults of new elements. Its head and body follow the pointer, so the pointer
    # stays in front of it, a bit to the right.
    {"name": "elements_player_entity", "wait": 5, "pointer": [260, 110], "elements": [{"type": "player_entity_v2", "id": "new_player", "x": 60, "y": 30, "w": 100, "h": 300}]},
]}

follow = {
    "script": "hover_elements",
    "start": "screen",
    "background_image": BG,
    "elements": [
        player("head_only", 150, 60, 120, 210, builtin("steve", body_follows_mouse="false", head_follows_mouse="true")),
        caption(110, 282, 200, "Head Follows Mouse"),
        player("head_and_body", 370, 60, 120, 210, builtin("alex", "slim", body_follows_mouse="true", head_follows_mouse="true")),
        caption(330, 282, 200, "Body Follows Mouse"),
    ],
    "pointer": [320, 330],
    "hovers": [
        {"point": [60, 40], "move_ticks": 22, "ticks": 8},
        {"point": [580, 40], "move_ticks": 30, "ticks": 8},
        {"point": [600, 320], "move_ticks": 22, "ticks": 8},
        {"point": [40, 320], "move_ticks": 30, "ticks": 8},
        {"point": [320, 170], "move_ticks": 20, "ticks": 20},
    ],
    "hold_ticks": 20,
}

pose_window = {
    "script": "context_entry_still",
    "background_image": BG,
    "elements": [player("waving", 250, 50, 140, 260, builtin("sunny", **WAVE))],
    "target": "waving",
    "entry": "entity_pose",
    "rest_point": [600, 340],
}

out = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
for name, config in (("scenes.json", scenes), ("follow.json", follow), ("pose_window.json", pose_window)):
    with open(os.path.join(out, name), "w") as f:
        json.dump(config, f, indent=1)
