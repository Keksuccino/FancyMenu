"""Makes the stills of the Menu Backgrounds page from the renders of scenes.json and the frames of menu_still.json:

- menu_backgrounds_hero.png: the hero scene as it is.
- menu_backgrounds_stacking.png: the left half of stack_image (the sky shows black) next to the right half of stack_color (a Color
  background fills it), split by a thin line.
- menu_backgrounds_effects.png: thirds of effect_none, effect_overlay and effect_blur, split by thin lines.
- menu_backgrounds_menu.png: the frame with the "still" mark of menu_still.json, cropped to the open menus.

Usage: python3 make_stills.py <scenes dir> <menu still frames dir> <out dir>
"""
import json
import os
import subprocess
import sys
from PIL import Image, ImageDraw

LINE = (231, 238, 251)
# The right-click menu with Menu Backgrounds open, and some of the title screen around it (in 1280x720 frame pixels)
MENU_CROP = (176, 186, 1280, 720)


def optimize(path):
    subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', path, path], check=True)
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', path], check=True, capture_output=True)
    print(path, Image.open(path).size, os.path.getsize(path))


def split(images, out):
    """Puts equal vertical strips of the images next to each other, with a thin line between them."""
    first = images[0]
    width, height = first.size
    result = first.copy()
    draw = ImageDraw.Draw(result)
    for i, image in enumerate(images):
        left, right = width * i // len(images), width * (i + 1) // len(images)
        result.paste(image.crop((left, 0, right, height)), (left, 0))
    for i in range(1, len(images)):
        x = width * i // len(images)
        draw.rectangle((x - 1, 0, x, height), fill=LINE)
    result.save(out)
    optimize(out)


def still_frame(frames):
    index = None
    with open(os.path.join(frames, 'frames.jsonl')) as f:
        for line in f:
            data = json.loads(line)
            if any(event.get('type') == 'mark' and event.get('name') == 'still' for event in data['events']):
                index = data['frame']
    if index is None:
        raise SystemExit('No frame with the mark "still"')
    return Image.open(os.path.join(frames, f'f_{index:05d}.png')).convert('RGB')


def main():
    scenes, frames, out = sys.argv[1], sys.argv[2], sys.argv[3]

    def scene(name):
        return Image.open(os.path.join(scenes, name + '.png')).convert('RGB')

    hero = os.path.join(out, 'menu_backgrounds_hero.png')
    scene('hero').save(hero)
    optimize(hero)
    split([scene('stack_image'), scene('stack_color')], os.path.join(out, 'menu_backgrounds_stacking.png'))
    split([scene('effect_none'), scene('effect_overlay'), scene('effect_blur')], os.path.join(out, 'menu_backgrounds_effects.png'))
    menu = os.path.join(out, 'menu_backgrounds_menu.png')
    still_frame(frames).crop(MENU_CROP).save(menu)
    optimize(menu)


if __name__ == '__main__':
    main()
