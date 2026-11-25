"""Pixel art of the Dragger page: a Patch Notes banner for the Wildwood title screen (see configs/getting_started_page).

Usage: python3 make_panel.py <out dir> [preview png]
Out dir is usually run_client_docs/config/fancymenu/assets/dragger.

- panel.png: a plum cloth banner with gold stitching and fringe, hanging from a carved wooden rod. The rod is the handle
  players grab, so it has grip notches in its middle. 92x70 art pixels at 4x: as a 184x140 Image element, one art pixel
  is 2 GUI pixels, like the Wildwood scene. The rod is the top 20 GUI pixels, the Text element sits inside the stitching.
"""
import os
import random
import sys
import numpy as np
from PIL import Image

W, H = 92, 70
ROD_H = 10
BANNER_X0, BANNER_X1 = 4, 87
BANNER_BOTTOM = 62
BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 + 1 / 32.0


def hexc(h, a=255):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


OUTLINE = hexc('#1a0d1c')
# Wood of the Wildwood buttons
WOOD_TOP = hexc('#b97c4b')
WOOD = hexc('#83502f')
WOOD_GRAIN = hexc('#6e4127')
WOOD_BOTTOM = hexc('#4f2d1b')
CAP_TOP = hexc('#9b6239')
CAP = hexc('#6e4127')
CAP_BOTTOM = hexc('#4f2d1b')
NOTCH = hexc('#2e1a12')
NOTCH_LIT = hexc('#c88550')
# Brass rings and gold stitching
GOLD_HI = hexc('#f0c97a')
GOLD = hexc('#c9974f')
GOLD_DARK = hexc('#8a5a2a')
# Plum cloth, lit by the sunset from the right
CLOTH = [hexc('#241330'), hexc('#2e1a3c'), hexc('#382046'), hexc('#432753')]
CLOTH_RIM = hexc('#6a3a5e')
CLOTH_SHADOW = hexc('#1c0f25')


# The round rod, from its lit top row to its shaded bottom row
ROD_ROWS = [hexc('#d39a62'), hexc('#b97c4b'), hexc('#9b6239'), hexc('#83502f'), hexc('#6e4127'), hexc('#4f2d1b')]
LEATHER_ROWS = [hexc('#c9785a'), hexc('#ad6049'), hexc('#934d3b'), hexc('#7b3f31'), hexc('#633228'), hexc('#4b251e')]
LEATHER_SEAM = hexc('#2e1712')
KNOB_ROWS = [hexc('#f0c97a'), hexc('#e2b066'), hexc('#c9974f'), hexc('#b08240'), hexc('#97692f'), hexc('#8a5a2a'), hexc('#6e4520'), hexc('#5a3818')]
GRIP_X0, GRIP_X1 = 35, 56
KNOB_W = 5


def draw_rod(img):
    # The rod itself: one outline row on top and below, six shaded rows between
    for x in range(KNOB_W, W - KNOB_W):
        img[1, x] = OUTLINE
        img[8, x] = OUTLINE
        for i, c in enumerate(ROD_ROWS):
            img[2 + i, x] = c
    # Short grain streaks, far apart, on the shaded half only
    for y, x0, x1 in [(5, 9, 14), (6, 19, 27), (5, 63, 69), (6, 74, 80), (4, 29, 31), (4, 60, 61)]:
        for x in range(x0, x1 + 1):
            img[y, x] = WOOD_GRAIN if y > 4 else ROD_ROWS[1]
    # A leather wrap in the middle shows where to grab it: diagonal windings with dark seams, a bit taller than the rod
    for x in range(GRIP_X0, GRIP_X1 + 1):
        if x in (GRIP_X0, GRIP_X1):
            for y in range(1, 9):
                img[y, x] = OUTLINE
            continue
        img[0, x] = OUTLINE
        img[9, x] = OUTLINE
        for y in range(1, 9):
            row = min(len(LEATHER_ROWS) - 1, max(0, round((y - 1) * (len(LEATHER_ROWS) - 1) / 7)))
            c = LEATHER_ROWS[row]
            if (x + y) % 5 == 0:
                c = LEATHER_SEAM
            elif (x + y) % 5 == 1 and y <= 4:
                c = LEATHER_ROWS[max(0, row - 1)]
            img[y, x] = c
    # Brass knobs at both ends, round, lit from the top left
    knob = ['.XXX.', 'XooXX', 'XoooX', 'XoooX', 'XoooX', 'XoooX', 'XoooX', 'XoooX', 'XXooX', '.XXX.']
    for side in (0, 1):
        for y, line in enumerate(knob):
            for i, ch in enumerate(line):
                x = i if side == 0 else W - 1 - i
                if ch == 'X':
                    img[y, x] = OUTLINE
                elif ch == 'o':
                    c = KNOB_ROWS[min(len(KNOB_ROWS) - 1, y - 1)]
                    # The side facing the rod is a bit darker
                    if (side == 0 and i == 3) or (side == 1 and i == 1):
                        c = KNOB_ROWS[min(len(KNOB_ROWS) - 1, y)]
                    img[y, x] = c
        # Specular dot
        img[2, 1 if side == 0 else W - 3] = hexc('#fff0b0')
    # Brass collars where the knobs meet the rod
    for x in (KNOB_W, W - KNOB_W - 1):
        for y in range(1, 9):
            img[y, x] = OUTLINE if y in (1, 8) else KNOB_ROWS[min(len(KNOB_ROWS) - 1, y - 1)]


def cloth_level(x, y):
    """Soft vertical folds of the hanging cloth, a bit darker near the top where the rod shades it."""
    u = (x - BANNER_X0) / (BANNER_X1 - BANNER_X0)
    fold = 0.5 + 0.5 * np.sin(u * np.pi * 3.0 + 0.6)
    v = 1.35 + fold * 1.25 - max(0.0, (16 - y) / 16.0) * 0.6
    return v


def draw_banner(img, rng):
    for y in range(ROD_H - 1, BANNER_BOTTOM + 1):
        for x in range(BANNER_X0, BANNER_X1 + 1):
            if x in (BANNER_X0, BANNER_X1) or y == BANNER_BOTTOM:
                img[y, x] = OUTLINE
                continue
            v = cloth_level(x, y)
            base = int(np.floor(v))
            f = v - base
            level = min(len(CLOTH) - 1, base + (1 if f > BAYER4[y % 4, x % 4] else 0))
            img[y, x] = CLOTH[level]
    # The rod casts a shadow onto the top of the cloth
    for x in range(BANNER_X0 + 1, BANNER_X1):
        img[ROD_H - 1, x] = CLOTH_SHADOW
        img[ROD_H, x] = CLOTH_SHADOW
        if x % 2 == 0:
            img[ROD_H + 1, x] = CLOTH_SHADOW
    # Rim light on the right edge, shadow on the left edge
    for y in range(ROD_H + 1, BANNER_BOTTOM):
        img[y, BANNER_X1 - 1] = CLOTH_RIM
        img[y, BANNER_X0 + 1] = CLOTH[0]
    # Gold hem along the bottom
    for x in range(BANNER_X0 + 1, BANNER_X1):
        img[BANNER_BOTTOM - 2, x] = GOLD
        img[BANNER_BOTTOM - 1, x] = GOLD_DARK
    for x in range(BANNER_X0 + 2, BANNER_X1 - 1, 4):
        img[BANNER_BOTTOM - 2, x] = GOLD_HI
    draw_stitching(img)
    draw_fringe(img, rng)


def draw_stitching(img):
    """A stitched gold frame inside the cloth (two stitches, one gap), with a small diamond at each corner."""
    x0, x1, y0, y1 = BANNER_X0 + 3, BANNER_X1 - 3, ROD_H + 3, BANNER_BOTTOM - 5
    for x in range(x0 + 2, x1 - 1):
        if (x - x0) % 3 != 2:
            img[y0, x] = GOLD
            img[y1, x] = GOLD_DARK
    for y in range(y0 + 2, y1 - 1):
        if (y - y0) % 3 != 2:
            img[y, x0] = GOLD
            img[y, x1] = GOLD_DARK
    for cx, cy in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        img[cy, cx] = GOLD_HI
        img[cy - 1, cx] = GOLD
        img[cy + 1, cx] = GOLD_DARK
        img[cy, cx - 1] = GOLD
        img[cy, cx + 1] = GOLD_DARK


def draw_fringe(img, rng):
    """Gold threads hanging from the hem, every other pixel, with uneven lengths like real fringe."""
    lengths = []
    for x in range(BANNER_X0 + 1, BANNER_X1):
        if (x - BANNER_X0) % 2 == 0:
            continue
        lengths.append((x, rng.choice((4, 5, 5, 6, 6, 7))))
    for x, length in lengths:
        for i in range(length):
            y = BANNER_BOTTOM + 1 + i
            if y >= H:
                break
            img[y, x] = GOLD_HI if i == 0 else (GOLD_DARK if i == length - 1 else GOLD)


def make_panel(seed=4):
    rng = random.Random(seed)
    img = np.zeros((H, W, 4), np.uint8)
    draw_banner(img, rng)
    draw_rod(img)
    return img


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    panel = Image.fromarray(make_panel(), 'RGBA')
    big = panel.resize((W * 4, H * 4), Image.NEAREST)
    big.save(os.path.join(out, 'panel.png'), optimize=True)
    print(f'panel.png: {big.width}x{big.height}')
    if len(sys.argv) > 2:
        # Preview at 8x over the darkest and the brightest part of the Wildwood sky
        prev = Image.new('RGBA', (W * 8 * 2 + 24, H * 8 + 16), (18, 11, 38, 255))
        prev.paste(Image.new('RGBA', (W * 8 + 8, H * 8 + 16), (222, 111, 103, 255)), (W * 8 + 16, 0))
        p8 = panel.resize((W * 8, H * 8), Image.NEAREST)
        prev.paste(p8, (8, 8), p8)
        prev.paste(p8, (W * 8 + 20, 8), p8)
        prev.save(sys.argv[2])


if __name__ == '__main__':
    main()
