"""Lays out the six images of a panorama as an unfolded cube, each labeled with its file name and side, for the Panoramas page
(panoramas_cube.png). The images are the real in-game screenshots of the panorama, only scaled down.

Usage: python3 make_cube_net.py <panorama folder with panorama_0.png to panorama_5.png> <out png>
"""
import os
import shutil
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

FACE = 192
GAP = 4
MARGIN = 8
# Column and row of each image in the unfolded cube: the four sides in a row, top above and bottom below the front
CELLS = {4: (1, 0), 3: (0, 1), 0: (1, 1), 1: (2, 1), 2: (3, 1), 5: (1, 2)}
SIDES = {0: 'Front', 1: 'Right', 2: 'Back', 3: 'Left', 4: 'Top', 5: 'Bottom'}


def main():
    folder, out = sys.argv[1], sys.argv[2]
    w = MARGIN * 2 + FACE * 4 + GAP * 3
    h = MARGIN * 2 + FACE * 3 + GAP * 2
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    font = ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc', 13)
    bold = ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc', 13, index=1)
    for i, (c, r) in CELLS.items():
        face = Image.open(os.path.join(folder, 'panorama_%d.png' % i)).convert('RGBA').resize((FACE, FACE), Image.LANCZOS)
        x, y = MARGIN + c * (FACE + GAP), MARGIN + r * (FACE + GAP)
        img.alpha_composite(face, (x, y))
        # Label chip in the top-left corner of the image
        name, side = 'panorama_%d.png' % i, SIDES[i]
        d = ImageDraw.Draw(img)
        name_w = d.textlength(name + '  ', font=font)
        side_w = d.textlength(side, font=bold)
        d.rounded_rectangle([x + 6, y + 6, x + 6 + name_w + side_w + 14, y + 6 + 22], radius=5, fill=(16, 16, 20, 205))
        d.text((x + 13, y + 10), name, font=font, fill=(235, 235, 240, 255))
        d.text((x + 13 + name_w, y + 10), side, font=bold, fill=(255, 214, 102, 255))
    img.save(out)
    if shutil.which('pngquant'):
        subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', out, out], check=True)
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', out], check=True, capture_output=True)
    print(out, img.size, os.path.getsize(out), 'bytes')


if __name__ == '__main__':
    main()
