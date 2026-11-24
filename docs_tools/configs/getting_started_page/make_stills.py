"""Picks the menu bar still of the Getting Started page from the frames of new_layout.json: the frame with the "menu" mark, where
the Customization menu of the menu bar is open over the title screen. Crops it to the menu bar and the menu and optimizes it.

Usage: python3 make_stills.py <frames dir> <out png>
"""
import json
import os
import subprocess
import sys
from PIL import Image

# The menu bar and the open Customization menu, with some of the title screen around them (in 1280x720 frame pixels)
CROP = (0, 0, 1280, 640)


def main():
    frames, out = sys.argv[1], sys.argv[2]
    index = None
    with open(os.path.join(frames, 'frames.jsonl')) as f:
        for line in f:
            data = json.loads(line)
            if any(event.get('type') == 'mark' and event.get('name') == 'menu' for event in data['events']):
                index = data['frame']
    if index is None:
        raise SystemExit('No frame with the mark "menu"')
    # The mark lands on the frame after the step, so the menu has been open for a while there
    image = Image.open(os.path.join(frames, f'f_{index:05d}.png')).convert('RGB').crop(CROP)
    image.save(out)
    subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', out, out], check=True)
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', out], check=True)
    print(out, image.size, os.path.getsize(out))


if __name__ == '__main__':
    main()
