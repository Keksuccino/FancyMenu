"""Writes the config of the content handle still (script "select_and_scroll" of DocsShowcaseEditorGif).

Usage: python3 gen_content_handle.py [output json, default: content_handle.json next to this script]

A news panel in Anchors mode. The script selects the area and scrolls to the end of its content, where the handle is.
The still is the last recorded frame, cropped to the area.
"""
import json, os, sys

news = "## Server News\nThe **Winter Update** is live!\n\n+ Added an ice castle at spawn\n+ Added snowball fights\n~ Changed the shop prices"
elements = [
    {"type": "scroll_area", "id": "docs_news", "x": 170, "y": 70, "w": 300, "h": 150, "props": {"content_height": "190"}},
    {"parent": "docs_news", "x": 8, "y": 44, "w": 220, "h": 110, "background": False, "content": news},
    {"type": "item", "parent": "docs_news", "x": 244, "y": 50, "w": 40, "h": 40, "props": {"item_key": "minecraft:snowball"}},
    {"type": "custom_button", "parent": "docs_news", "x": 8, "y": 160, "w": 100, "h": 20, "props": {"label": "Join Now"}},
]
config = {"script": "select_and_scroll", "background_image": "/config/fancymenu/assets/docs_bg.png", "target": "docs_news",
          "click": [260, 20], "wheel_turns": -4, "elements": elements}
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "content_handle.json")
json.dump(config, open(out, "w"), indent=1)
print("wrote", out)
