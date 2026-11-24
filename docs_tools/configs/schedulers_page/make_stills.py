"""Picks the marked frame of a schedulers_still recording, crops it to its window and writes it as an optimized page image.

Usage: python3 make_stills.py <frames dir> <output dir>
Writes schedulers_manage.png (mark "manage"). Needs Pillow, pngquant and oxipng.
"""
import json, os, subprocess, sys
from PIL import Image

frames, out = sys.argv[1], sys.argv[2]
NAMES = {"manage": "schedulers_manage.png"}
# The window with a bit of background around it, in pixels of the 1280x1080 frames (still.json records with window_height 1080)
CROPS = {"manage": (0, 50, 1280, 1032)}

marks = {}
with open(os.path.join(frames, "frames.jsonl")) as f:
    for line in f:
        frame = json.loads(line)
        for event in frame["events"]:
            if event["type"] == "mark":
                marks[event["name"]] = frame["frame"]

for mark, name in NAMES.items():
    if mark not in marks:
        sys.exit(f"no frame with the mark {mark}")
    source = os.path.join(frames, "f_%05d.png" % marks[mark])
    target = os.path.join(out, name)
    Image.open(source).convert("RGB").crop(CROPS[mark]).save(target)
    subprocess.run(["pngquant", "--quality", "80-95", "--force", "--output", target, target], check=True)
    subprocess.run(["oxipng", "-o", "4", "--strip", "safe", "-q", target], check=True)
    print(name, "from frame", marks[mark], Image.open(target).size, os.path.getsize(target) // 1024, "KB")
