"""Writes the config of the Scrollable Areas showcase GIF (script "scroll_area" of DocsShowcaseEditorGif).

Usage: python3 gen_editor.py [output json, default: editor.json next to this script]

An area that only holds a title, and six buttons next to it in two columns, which the script drags into the area.
The title keeps the editor's guide of empty areas away, which shows as a big tooltip at the colossal UI scale.
"""
import json, os, sys

labels = ["Survival", "Creative", "Skyblock", "Minigames", "Parkour", "Hardcore"]
elements = [
    {"type": "scroll_area", "id": "docs_area", "x": 110, "y": 90, "w": 200, "h": 110, "props": {"child_alignment": "center"}},
    {"parent": "docs_area", "x": 10, "y": 4, "w": 180, "h": 20, "background": False, "content": "^^^\n**Choose a World**\n^^^"},
]
ids = []
for i, label in enumerate(labels):
    ids.append(f"docs_b{i + 1}")
    elements.append({"type": "custom_button", "id": ids[-1], "x": 400 + (i % 2) * 96, "y": 110 + (i // 2) * 26, "w": 90, "h": 20, "props": {"label": label}})
config = {"script": "scroll_area", "background_image": "/config/fancymenu/assets/docs_bg.png", "area": "docs_area", "buttons": ids, "elements": elements}
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "editor.json")
json.dump(config, open(out, "w"), indent=1)
print("wrote", out)
