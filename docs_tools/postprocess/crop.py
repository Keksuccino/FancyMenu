"""Crops every scene screenshot to the box around its elements, plus a margin that shows a bit of the background.

Usage: python3 crop.py <scenes.json> <screenshots dir> <out dir> [margin in GUI pixels, default 10]

Reads the element boxes and the GUI scale from the same scene config the in-game harness rendered,
so <screenshots dir>/<scene name>.png becomes <out dir>/<scene name>.png.
"""
import json
import os
import sys
from PIL import Image

config = json.load(open(sys.argv[1]))
source, out = sys.argv[2], sys.argv[3]
margin = int(sys.argv[4]) if len(sys.argv) > 4 else 10
scale = config.get("gui_scale", 2)
os.makedirs(out, exist_ok=True)

for scene in config["scenes"]:
    elements = scene["elements"]
    if not elements:
        continue
    left = min(e["x"] for e in elements) - margin
    top = min(e["y"] for e in elements) - margin
    right = max(e["x"] + e["w"] for e in elements) + margin
    bottom = max(e["y"] + e["h"] for e in elements) + margin
    image = Image.open(os.path.join(source, scene["name"] + ".png"))
    box = (max(0, left * scale), max(0, top * scale), min(image.width, right * scale), min(image.height, bottom * scale))
    cropped = image.crop(box)
    cropped.save(os.path.join(out, scene["name"] + ".png"))
    print(scene["name"], cropped.size)
