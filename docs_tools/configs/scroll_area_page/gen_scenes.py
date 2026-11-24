"""Writes the scene config of the Scrollable Areas screenshots.

Usage: python3 gen_scenes.py [output json, default: scenes.json next to this script]

Scene "modes": three Scrollable Areas in Stack, Grid and Flow mode, with a title above each.
Children have a "parent", so their x/y are relative to the area's content (and ignored by the arranging modes).
"""
import json, os, sys

AREA_W, AREA_H, AREA_Y, GAP, LEFT = 140, 130, 36, 44, 24
elements = []


def area(index, area_id, title, props):
    x = LEFT + index * (AREA_W + GAP)
    elements.append({"x": x, "y": 10, "w": AREA_W, "h": 20, "background": False, "content": f"^^^\n**{title}**\n^^^"})
    elements.append({"type": "scroll_area", "id": area_id, "x": x, "y": AREA_Y, "w": AREA_W, "h": AREA_H, "props": props})


def child(parent, type_, w, h, props):
    elements.append({"type": type_, "parent": parent, "x": 0, "y": 0, "w": w, "h": h, "props": props})


area(0, "docs_stack", "Stack", {"layout_mode": "stack", "child_alignment": "fill"})
for label in ["Survival", "Creative", "Skyblock", "Minigames", "Parkour", "Hardcore", "Factions"]:
    child("docs_stack", "custom_button", 100, 20, {"label": label})

area(1, "docs_grid", "Grid", {"layout_mode": "grid", "grid_lines": "4", "child_alignment": "center"})
items = ["diamond", "emerald", "gold_ingot", "iron_ingot", "redstone", "lapis_lazuli", "amethyst_shard", "netherite_ingot",
         "apple", "golden_apple", "bread", "cake", "diamond_sword", "diamond_pickaxe", "bow", "golden_carrot",
         "ender_pearl", "blaze_rod", "totem_of_undying", "experience_bottle", "elytra", "compass", "clock", "name_tag"]
for item in items:
    child("docs_grid", "item", 24, 24, {"item_key": "minecraft:" + item})

area(2, "docs_flow", "Flow", {"layout_mode": "flow"})
for tag in ["Adventure", "Magic", "Tech", "Building", "PvP", "Exploration", "Farming", "Redstone", "Quests", "Multiplayer", "Mini Games", "Cozy"]:
    child("docs_flow", "custom_button", len(tag) * 6 + 10, 20, {"label": tag})

config = {"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [{"name": "scroll_area_modes", "wait": 4, "elements": elements}]}
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "scenes.json")
json.dump(config, open(out, "w"), indent=1)
print("wrote", out)
