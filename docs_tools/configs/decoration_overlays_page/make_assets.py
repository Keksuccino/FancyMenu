"""Turns the picked overlay shots into the page's assets: optional crop, pngquant, oxipng.

Usage: python3 make_assets.py <shots dir> [more shots dirs] <docs assets dir>
The shots dirs hold the screenshots of scenes.json (<scene>_<seconds>s.png). Every pick names its asset, its shot and an optional crop box.
"""
import io
import os
import subprocess
import sys
from PIL import Image

# asset name (decoration_overlay_<name>.png): shot, crop box (left, top, right, bottom) or None
PICKS = {
    "hero": ("hero_winter_45s", None),
    "snowfall": ("snowfall_night_45s", None),
    "rainfall": ("rainfall_night_30s", None),
    "fog": ("fog_15s", None),
    "frost": ("frost_45s", None),
    "leaves": ("leaves_5s", None),
    "cherry_blossoms": ("cherry_blossoms_15s", None),
    "overgrowth": ("overgrowth_45s", None),
    "clouds": ("clouds_5s", None),
    "sunbeams": ("sunbeams_30s", None),
    "bubbles": ("bubbles_15s", None),
    "birds": ("birds_15s", None),
    "butterflies": ("butterflies_15s", None),
    "bats": ("bats_day_2x_15s", None),
    "fireflies": ("fireflies_detailed_30s", None),
    "aurora": ("aurora_30s", None),
    "shooting_stars": ("shooting_stars_10s", None),
    "sky_lanterns": ("sky_lanterns_15s", None),
    "confetti": ("confetti_13s", None),
    "fireworks": ("fireworks_7s", None),
    "string_lights": ("string_lights_15s", None),
    "cobwebs": ("cobwebs_20s", None),
    "embers": ("embers_45s", None),
    # Buddy is small, so its image shows the bottom of the menu around it
    "buddy": ("buddy_15s", (260, 270, 740, 540)),
}

*shot_dirs, out_dir = sys.argv[1:]
for name, (shot, box) in PICKS.items():
    source = next((os.path.join(d, shot + ".png") for d in shot_dirs if os.path.exists(os.path.join(d, shot + ".png"))), None)
    if source is None:
        print("MISSING", shot)
        continue
    image = Image.open(source).convert("RGB")
    if box:
        image = image.crop(box)
    raw = io.BytesIO()
    image.save(raw, "PNG")
    target = os.path.join(out_dir, f"decoration_overlay_{name}.png")
    # pngquant reads the image from stdin, so there are no temporary files
    subprocess.run(["pngquant", "--quality", "80-95", "--speed", "1", "--strip", "--force", "--output", target, "-"], input=raw.getvalue(), check=True)
    subprocess.run(["oxipng", "-o", "4", "--strip", "safe", "-q", target], check=True)
    print(f"{os.path.basename(target)} {image.width}x{image.height} {os.path.getsize(target) // 1024} KB")
