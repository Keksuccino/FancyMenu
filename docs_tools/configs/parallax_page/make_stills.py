"""Picks the still of the background's parallax settings from the frames of background_still.json (the frame with the "still" mark),
crops it to the open menus and optimizes it.

Usage: python3 make_stills.py <frames dir> <out png>
"""
import json
import os
import subprocess
import sys
from PIL import Image

# The right-click menu with Menu Backgrounds and Image open, and some of the title screen around them (in 1280x1080 frame pixels, the still records in a taller window)
CROP = (104, 150, 1280, 1008)


def main():
    frames, out = sys.argv[1], sys.argv[2]
    index = None
    with open(os.path.join(frames, 'frames.jsonl')) as f:
        for line in f:
            data = json.loads(line)
            if any(event.get('type') == 'mark' and event.get('name') == 'still' for event in data['events']):
                index = data['frame']
    if index is None:
        raise SystemExit('No frame with the mark "still"')
    image = Image.open(os.path.join(frames, f'f_{index:05d}.png')).convert('RGB').crop(CROP)
    image.save(out)
    subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', out, out], check=True)
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', out], check=True)
    print(out, image.size, os.path.getsize(out))


if __name__ == '__main__':
    main()
