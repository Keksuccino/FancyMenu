"""Picks the marked frames of a listeners_still recording and writes them as optimized page images.

Usage: python3 make_stills.py <frames dir> <output dir>
Writes listeners_manage.png (mark "manage") and listeners_choose.png (mark "choose"). Needs pngquant and oxipng.
"""
import json, os, subprocess, sys

frames, out = sys.argv[1], sys.argv[2]
NAMES = {"manage": "listeners_manage.png", "choose": "listeners_choose.png"}

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
    # pngquant reads the frame and writes the quantized image, so no temporary file is needed
    subprocess.run(["pngquant", "--quality", "80-95", "--force", "--output", target, source], check=True)
    subprocess.run(["oxipng", "-o", "4", "--strip", "safe", "-q", target], check=True)
    print(name, "from frame", marks[mark], os.path.getsize(target) // 1024, "KB")
