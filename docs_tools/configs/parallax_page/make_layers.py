"""Draws the pixel art layers of the Parallax Effect page: a landscape at dusk in four layers.

- parallax_sky.png: the menu background (sky, sun, stars and clouds), 320x180 art pixels.
- parallax_mountains.png: far mountains and a lake, moves a little.
- parallax_hills.png: hills with pine forests and a cabin, moves more.
- parallax_foreground.png: a tree, bushes, grass and flowers, moves the most.

The element layers are 352x200 art pixels: the 320x180 of the screen plus 16 and 10 art pixels on every side, so their edges never show
while they move. Every art pixel becomes 4x4 image pixels, which is 2x2 GUI pixels at GUI scale 2 (one image pixel per screen pixel).

Usage: python3 make_layers.py <out dir>
"""

import math
import random
import sys
from pathlib import Path

from PIL import Image

W, H = 320, 180
MX, MY = 16, 10
SCALE = 4
SUN_X, SUN_Y, SUN_R = 250, 110, 15
SHORE_Y = 119

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def c(hex_color, alpha=255):
    h = hex_color.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha


def dither(x, y, amount):
    """True for `amount` (0..1) of the pixels, in an ordered pattern."""
    return (BAYER[y % 4][x % 4] + 0.5) / 16.0 < amount


class Layer:
    """A canvas that gets drawn in screen coordinates (0..320, 0..180), with a margin around them."""

    def __init__(self, margin_x, margin_y):
        self.mx, self.my = margin_x, margin_y
        self.w, self.h = W + 2 * margin_x, H + 2 * margin_y
        self.img = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        self.px = self.img.load()

    def xs(self):
        return range(-self.mx, W + self.mx)

    def ys(self):
        return range(-self.my, H + self.my)

    def put(self, x, y, color):
        x, y = int(x) + self.mx, int(y) + self.my
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = color

    def get(self, x, y):
        x, y = int(x) + self.mx, int(y) + self.my
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[x, y]
        return (0, 0, 0, 0)

    def filled(self, x, y):
        return self.get(x, y)[3] > 0

    def save(self, path):
        self.img.resize((self.w * SCALE, self.h * SCALE), Image.NEAREST).save(path)


def interp(points, x, smooth=False):
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        if x0 <= x <= x1:
            t = (x - x0) / (x1 - x0) if x1 != x0 else 0
            if smooth:
                t = (1 - math.cos(t * math.pi)) / 2
            return y0 + (y1 - y0) * t
    return points[0][1] if x < points[0][0] else points[-1][1]


def walk(seed, length, spread=1):
    """A random walk of small steps, for rough edges of rocks and grass."""
    rng = random.Random(seed)
    value, values = 0, []
    for _ in range(length):
        value += rng.choice([-1, 0, 0, 1])
        value = max(-spread, min(spread, value))
        values.append(value)
    return values


# ---------------------------------------------------------------- sky

SKY = [c('#141233'), c('#1b1744'), c('#271c54'), c('#3a2265'), c('#532971'), c('#723078'), c('#963c7c'),
       c('#bb4e7b'), c('#d96674'), c('#ec866d'), c('#f6a86f'), c('#fbc97f'), c('#fde09a')]
SKY_TOPS = [0, 13, 26, 39, 51, 62, 73, 83, 92, 100, 106, 111, 116]


def sky_band(x, y):
    # The warm bands rise around the sun, like a glow
    glow = 12 * math.exp(-((x - SUN_X) / 70.0) ** 2)
    band = 0
    for i, top in enumerate(SKY_TOPS):
        if i > 3:
            top -= glow * (i - 3) / (len(SKY_TOPS) - 3)
        if y >= top:
            band = i
        elif y >= top - 2:
            # 2 rows of dither above every band
            if dither(x, y, 0.25 if y < top - 1 else 0.5):
                band = i
            break
        else:
            break
    return band


def cloud(layer, puffs, shade, body, low, lit):
    """A cloud with a flat bottom and round puffs on top: the bottom catches the low sun, the top stays in the shade.
    puffs: (x, width, height) on a common bottom line, given as the first entry (bottom_y, None, None)."""
    bottom = puffs[0][0]
    inside = set()
    for cx, half, height in puffs[1:]:
        for x in range(int(cx - half), int(cx + half) + 1):
            # Rounded ends: each puff gets lower towards its sides
            t = abs(x - cx) / half
            top = bottom - int(round(height * math.sqrt(max(0.0, 1 - t * t))))
            for y in range(top, bottom + 1):
                inside.add((x, y))
    for x, y in inside:
        if (x, y + 1) not in inside:
            color = lit
        elif (x, y + 2) not in inside:
            color = low
        elif (x, y - 1) not in inside:
            color = shade
        else:
            color = body
        layer.put(x, y, color)


def draw_sky():
    sky = Layer(0, 0)
    for y in range(H):
        for x in range(W):
            sky.put(x, y, SKY[sky_band(x, y)])

    # Stars, only in the dark part of the sky
    dim, bright = c('#8f7fcf'), c('#fff3d6')
    stars = [(14, 8), (37, 21), (52, 6), (71, 15), (96, 4), (118, 19), (133, 9), (151, 25), (167, 5), (189, 14),
             (204, 30), (221, 8), (236, 19), (259, 5), (277, 13), (296, 23), (309, 7), (8, 30), (62, 33), (108, 36),
             (181, 38), (247, 34), (288, 41), (315, 33), (29, 40), (142, 44), (85, 27), (230, 44)]
    for i, (x, y) in enumerate(stars):
        sky.put(x, y, bright if i % 3 == 0 else dim)
    for x, y in [(44, 12), (202, 9), (283, 26), (123, 28)]:
        sky.put(x, y, bright)
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            sky.put(x + dx, y + dy, c('#a998de'))

    # The sun with two rings of glow
    for y in range(SUN_Y - 30, SUN_Y + 30):
        for x in range(SUN_X - 30, SUN_X + 30):
            d = math.hypot(x - SUN_X + 0.5, y - SUN_Y + 0.5)
            if d <= SUN_R - 2:
                sky.put(x, y, c('#fff6cf'))
            elif d <= SUN_R:
                sky.put(x, y, c('#ffe7a8'))
            elif d <= SUN_R + 3 and dither(x, y, 0.5):
                sky.put(x, y, c('#fee4a4'))
            elif d <= SUN_R + 8 and dither(x, y, 0.25):
                sky.put(x, y, c('#fdd893'))

    # Clouds, lit from below by the setting sun, warmer the closer they are to it
    cloud(sky, [(48, None, None), (30, 15, 4), (49, 12, 7), (65, 14, 5), (83, 10, 3), (15, 7, 2)],
          c('#59296f'), c('#6a3077'), c('#87397a'), c('#b54d7b'))
    cloud(sky, [(67, None, None), (226, 14, 4), (246, 12, 7), (265, 14, 5), (287, 12, 3), (304, 7, 2)],
          c('#8b3b7b'), c('#a5457c'), c('#cf6478'), c('#f69a74'))
    cloud(sky, [(104, None, None), (238, 20, 1), (262, 18, 2), (286, 10, 1), (214, 8, 1)],
          c('#d86c72'), c('#e58070'), c('#f6ae78'), c('#ffd9a0'))
    cloud(sky, [(82, None, None), (13, 11, 3), (29, 9, 5), (43, 7, 2)],
          c('#6a2d75'), c('#7a3278'), c('#a8457b'), c('#e0707a'))
    cloud(sky, [(32, None, None), (300, 12, 2), (286, 6, 1)],
          c('#3e2468'), c('#4a2870'), c('#5e2e74'), c('#7d3778'))
    cloud(sky, [(89, None, None), (151, 13, 3), (168, 8, 2)],
          c('#833a7a'), c('#93407c'), c('#b65079'), c('#f08a70'))
    return sky


# ---------------------------------------------------------------- far mountains and the lake

# Each mountain: peak x, peak y, left slope, right slope, crease lean (x per row downwards). Later ones are in front.
BACK_MOUNTAINS = [(52, 56, 0.78, 0.62, 0.22), (140, 64, 0.7, 0.66, -0.15), (204, 74, 0.6, 0.72, 0.2),
                  (296, 60, 0.82, 0.7, -0.25), (-8, 72, 0.6, 0.6, 0.2), (346, 74, 0.6, 0.6, -0.2)]
FRONT_MOUNTAINS = [(14, 90, 0.5, 0.42, 0.3), (96, 84, 0.46, 0.5, 0.2), (176, 88, 0.48, 0.5, -0.1), (330, 92, 0.5, 0.45, -0.2),
                   (232, 102, 0.42, 0.62, 0.2)]


def mountain_range(layer, mountains, seed, lit, shade, snow_lit, snow_shade, snow_below, rim_far, rim_near, behind=None,
                   rim_inner=None):
    rng = random.Random(seed)
    rough = [walk(rng.randint(0, 9999), W + 2 * MX + 20, 1) for _ in mountains]
    owner = {}
    for k, (px, py, sl, sr, lean) in enumerate(mountains):
        for x in layer.xs():
            dx = x - px
            top = py + (sl * -dx if dx < 0 else sr * dx)
            # Rocks make the slopes rough, more so further down
            top = int(round(top + rough[k][x + MX] * min(1.0, abs(dx) / 8.0)))
            for y in range(top, SHORE_Y):
                owner[(x, y)] = (k, top)
    for (x, y), (k, top) in owner.items():
        px, py, sl, sr, lean = mountains[k]
        crease = px + lean * (y - py) + (1 if (y // 3) % 3 == 0 else 0)
        sun_side = (x >= crease) if px < SUN_X else (x <= crease)
        color = lit if sun_side else shade
        if py < snow_below:
            # Snow caps that reach further down along the crease
            depth = (snow_below - py) * 0.75 - abs(x - crease) * 0.35 + ((x * 7 + y) % 4 == 0)
            if y < py + depth:
                color = snow_lit if sun_side else snow_shade
        layer.put(x, y, color)
    # Rim light on the outline against the sky, brighter close to the sun
    for (x, y), (k, top) in owner.items():
        if y == top and (x, y - 1) not in owner:
            if behind is not None and (x, y - 1) in behind:
                # In front of the mountains behind it, the sun doesn't reach the outline
                layer.put(x, y, rim_inner)
                continue
            near = math.exp(-((x - SUN_X) / 60.0) ** 2)
            layer.put(x, y, rim_near if near > 0.35 else rim_far)
    return owner


def draw_mountains():
    layer = Layer(MX, MY)
    back = mountain_range(layer, BACK_MOUNTAINS, 7, c('#7a5c94'), c('#614a86'), c('#e2c2d4'), c('#a08bc4'), 84,
                          c('#c59ab4'), c('#ffcf9e'))
    front = mountain_range(layer, FRONT_MOUNTAINS, 8, c('#654b84'), c('#523e76'), c('#c8a8cc'), c('#8f7ab4'), 0,
                           c('#a77ba6'), c('#f6b48e'), back, c('#7d6199'))
    # Haze over the foot of the mountains, in two steps
    for (x, y) in list(back.keys()) + list(front.keys()):
        if y >= 113 or (y >= 111 and dither(x, y, 0.5)):
            layer.put(x, y, c('#8c6292'))
        elif y >= 106 or (y >= 104 and dither(x, y, 0.5)):
            if layer.get(x, y)[:3] not in [(0xff, 0xcf, 0x9e), (0xf6, 0xb4, 0x8e)]:
                layer.put(x, y, c('#7a5890'))

    # The lake: a dark mirror line at the shore, calm water with light lines and the sun in broken streaks
    water, water_line, mirror = c('#764a82'), c('#8a5689'), c('#5e4378')
    for y in range(SHORE_Y, H + MY):
        for x in layer.xs():
            depth = y - SHORE_Y
            color = water
            if depth < 3 and (x + depth * 5) % 11 < 8:
                color = mirror
            elif depth >= 4 and (y % 4 == 1) and (x * 3 + y * 7) % 23 < 9:
                color = water_line
            layer.put(x, y, color)
    rng = random.Random(3)
    for depth in range(1, 18, 2):
        y = SHORE_Y + depth
        half = int(SUN_R * (1.05 - depth * 0.035))
        x = SUN_X - half + rng.randint(0, 2)
        while x < SUN_X + half:
            run = rng.randint(2, 7)
            color = c('#fff0b8') if abs(x - SUN_X) < half * 0.4 else c('#f7b875')
            for i in range(run):
                if x + i < SUN_X + half:
                    layer.put(x + i, y, color)
            x += run + rng.randint(1, 3)
    return layer


# ---------------------------------------------------------------- hills with forests and a cabin

BACK_HILL = [(-16, 136), (14, 131), (46, 127), (80, 132), (112, 136), (146, 133), (178, 138), (212, 135), (246, 140),
             (282, 134), (308, 128), (336, 131)]
FRONT_HILL = [(-16, 154), (24, 148), (64, 152), (104, 156), (144, 153), (184, 158), (224, 155), (264, 150), (302, 153),
              (336, 157)]


def pine(layer, base_x, base_y, height, body, rim, sun_side):
    """A pine tree from tiers that get wider downwards, with a notch at every tier and rim light on the side of the sun."""
    for y in range(height):
        row = base_y - height + y
        half = int(y * (0.44 if height < 14 else 0.34))
        if y > 3 and y % 4 == 0:
            half = max(0, half - (1 if height < 14 else 2))
        for dx in range(-half, half + 1):
            layer.put(base_x + dx, row, body)
        if y > 1 and half > 0 and not (y > 3 and y % 4 == 0):
            layer.put(base_x + (half if sun_side > 0 else -half), row, rim)
    layer.put(base_x, base_y - height - 1, body)
    for y in range(max(1, height // 8) + 1):
        layer.put(base_x, base_y + y, body)


def forest(layer, ridge, seed, groups, body, rim_far, rim_near, heights):
    """Pines in groups: tall in the middle of a group, smaller at its edges."""
    rng = random.Random(seed)
    for start, end in groups:
        x = start
        while x <= end:
            t = (x - start) / max(1, end - start)
            middle = math.sin(t * math.pi)
            height = int(heights[0] + (heights[1] - heights[0]) * middle) + rng.randint(-2, 2)
            rim = rim_near if abs(x - SUN_X) < 85 else rim_far
            pine(layer, x, ridge[x] + 2, max(5, height), body, rim, 1 if x < SUN_X else -1)
            x += rng.choice([3, 4, 4, 5, 6])


def draw_hills():
    layer = Layer(MX, MY)
    back, back_rim = c('#3a2b5c'), c('#9b5579')
    front, front_rim = c('#271e42'), c('#6f3a66')
    back_rough = walk(11, W + 2 * MX, 1)
    front_rough = walk(13, W + 2 * MX, 1)
    back_top = {x: int(round(interp(BACK_HILL, x, True))) + back_rough[x + MX] for x in layer.xs()}
    front_top = {x: int(round(interp(FRONT_HILL, x, True))) + front_rough[x + MX] for x in layer.xs()}

    for x in layer.xs():
        for y in range(back_top[x], H + MY):
            layer.put(x, y, back)
        near = math.exp(-((x - SUN_X) / 90.0) ** 2)
        layer.put(x, back_top[x], c('#d57880') if near > 0.4 else back_rim)
    forest(layer, back_top, 21, [(-14, 30), (52, 118), (142, 170), (188, 236), (292, 334)], c('#2d2249'),
           c('#7d4470'), c('#c0667e'), (7, 15))

    # A cabin with a lit window and smoke from the chimney
    cx = 268
    cy = back_top[cx] + 1
    wall, wall_lit, roof, roof_rim = c('#2a2046'), c('#3a2a52'), c('#1f1838'), c('#c4687c')
    for y in range(cy - 6, cy + 2):
        for x in range(cx - 6, cx + 7):
            layer.put(x, y, wall_lit if x == cx - 6 else wall)
    for i in range(7):
        for x in range(cx - 8 + i, cx + 9 - i):
            layer.put(x, cy - 7 - i, roof)
        layer.put(cx - 8 + i, cy - 7 - i, roof_rim)
    for y in range(cy - 16, cy - 9):
        layer.put(cx + 4, y, roof)
        layer.put(cx + 5, y, roof)
    layer.put(cx + 4, cy - 16, roof_rim)
    for x, y in [(cx - 3, cy - 4), (cx - 2, cy - 4), (cx - 3, cy - 3), (cx - 2, cy - 3), (cx + 2, cy - 4), (cx + 3, cy - 4),
                 (cx + 2, cy - 3), (cx + 3, cy - 3)]:
        layer.put(x, y, c('#ffd36e'))
    for x in range(cx - 4, cx + 5):
        layer.put(x, cy - 2, c('#5a3a58') if x in (cx - 4, cx - 1, cx + 1, cx + 4) else wall)
    smoke = [(0, -18), (1, -19), (1, -20), (0, -21), (-1, -22), (-1, -23), (0, -24), (1, -25), (2, -26), (2, -27),
             (1, -29), (0, -30), (0, -32)]
    for i, (dx, dy) in enumerate(smoke):
        alpha = max(50, 190 - i * 12)
        layer.put(cx + 4 + dx, cy + dy, c('#b496bd', alpha))
        if i % 3 == 1:
            layer.put(cx + 5 + dx, cy + dy, c('#b496bd', max(30, alpha - 50)))

    for x in layer.xs():
        for y in range(front_top[x], H + MY):
            layer.put(x, y, front)
        layer.put(x, front_top[x], front_rim)
    forest(layer, front_top, 5, [(-12, 44), (88, 132), (150, 214), (236, 262), (300, 334)], c('#1f1836'),
           c('#5e3460'), c('#8c4672'), (12, 26))
    return layer


# ---------------------------------------------------------------- foreground

def leaf_mass(layer, clusters, base, shade, highlight, seed):
    """A crown or bush from overlapping round clusters with slightly bumpy edges. The lower right of every cluster is a bit lighter,
    where the low sun shines through the leaves."""
    rng = random.Random(seed)
    cluster_area = set()
    for cx, cy, rx, ry in clusters:
        for y in range(int(cy - ry) - 3, int(cy + ry) + 4):
            for x in range(int(cx - rx) - 3, int(cx + rx) + 4):
                cluster_area.add((x, y))
    for cx, cy, rx, ry in clusters:
        phase = rng.random() * math.pi * 2
        lobes = rng.choice([9, 10, 11])
        cells = []
        for y in range(int(cy - ry) - 2, int(cy + ry) + 3):
            for x in range(int(cx - rx) - 2, int(cx + rx) + 3):
                angle = math.atan2((y - cy) / ry, (x - cx) / rx)
                edge = 1.0 + 0.09 * math.sin(angle * lobes + phase)
                d = math.hypot((x - cx) / rx, (y - cy) / ry)
                if d <= edge:
                    cells.append((x, y, d, angle))
        for x, y, d, angle in cells:
            color = base
            # A crescent of light on the lower right, a shade on the upper left
            if d > 0.74 and -0.1 < angle < 1.8:
                color = highlight
            elif d > 0.74 and (angle < -1.7 or angle > 2.7):
                color = shade
            layer.put(x, y, color)
    # Single leaves stick out of the edges
    edge = [(x, y, dx, dy) for y in layer.ys() for x in layer.xs() if layer.filled(x, y)
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)] if not layer.filled(x + dx, y + dy)]
    for x, y, dx, dy in edge:
        if rng.random() < 0.16 and (x, y) in cluster_area:
            layer.put(x + dx, y + dy, layer.get(x, y))
            if rng.random() < 0.3:
                layer.put(x + 2 * dx + dy, y + 2 * dy + dx, layer.get(x, y))


def rim_light(layer, color, direction, area):
    """Lights the outer pixels of shapes on one side: direction (dx, dy) points to the light."""
    dx, dy = direction
    lit = [(x, y) for y in layer.ys() for x in layer.xs()
           if area(x, y) and layer.filled(x, y) and not layer.filled(x + dx, y + dy)]
    for x, y in lit:
        layer.put(x, y, color)


def line(layer, x0, y0, x1, y1, w0, w1, color):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        w = w0 + (w1 - w0) * t
        for dy in range(-int(w) - 1, int(w) + 2):
            for dx in range(-int(w) - 1, int(w) + 2):
                if dx * dx + dy * dy <= w * w:
                    layer.put(round(x + dx), round(y + dy), color)


GROUND = [(-16, 170), (30, 167), (80, 171), (140, 174), (200, 173), (260, 169), (300, 165), (336, 167)]


def draw_foreground():
    layer = Layer(MX, MY)
    dark = c('#140e24')
    bark_lit = c('#2c1a35')

    # The trunk of the tree on the left, it bends a little to the right
    for y in range(24, H + MY):
        t = (y - 24) / (H + MY - 24)
        center = 18 - 6 * (1 - t) ** 2
        half = 4.5 + 4 * t ** 4
        left, right = int(round(center - half)), int(round(center + half))
        for x in range(left, right + 1):
            layer.put(x, y, dark)
        layer.put(right, y, bark_lit)
        if (y * 5) % 17 < 3:
            layer.put(right - 2, y, bark_lit)
    line(layer, 24, 168, 38, 177, 2.2, 0.6, dark)
    line(layer, 11, 168, -4, 179, 2.4, 0.8, dark)
    # Branches
    line(layer, 19, 76, 52, 52, 2.2, 0.7, dark)
    line(layer, 42, 58, 64, 47, 1.2, 0.5, dark)
    line(layer, 14, 58, -10, 38, 2.0, 0.8, dark)
    line(layer, 19, 44, 58, 24, 1.7, 0.6, dark)
    # The crown, over the top-left corner
    leaf_mass(layer, [(-10, 4, 24, 17), (18, -6, 22, 15), (44, 6, 18, 12), (64, 16, 13, 9), (4, 28, 16, 10),
                      (32, 26, 13, 8), (58, 40, 10, 6), (76, 24, 7, 5), (-14, 34, 11, 8)],
              c('#160f27'), c('#110b1f'), c('#21162f'), 40)
    # Bushes on the right
    leaf_mass(layer, [(326, 148, 20, 14), (304, 160, 16, 11), (342, 160, 14, 12), (284, 167, 11, 7), (318, 168, 14, 8)],
              c('#160f27'), c('#110b1f'), c('#21162f'), 70)

    # Ground with grass blades and some tall reeds
    rng = random.Random(9)
    rough = walk(17, W + 2 * MX, 1)
    for x in layer.xs():
        top = int(round(interp(GROUND, x, True))) + rough[x + MX]
        for y in range(top, H + MY):
            layer.put(x, y, dark)
        blade = rng.choice([0, 0, 1, 1, 2, 2, 3, 4, 5, 7])
        for y in range(top - blade, top):
            layer.put(x, y, dark)
    for x, height, lean in [(92, 16, 1), (96, 12, -1), (128, 10, 1), (208, 13, 1), (213, 18, -1), (246, 11, 1), (152, 9, -1)]:
        top = int(round(interp(GROUND, x, True)))
        for i in range(height):
            layer.put(x + (lean * i) // 6, top - i, dark)

    # Rim light where the low sun reaches: right edges left of the sun, left edges right of it
    # Not on the grass, where it would turn every blade into a line
    rim_light(layer, c('#5c2d4d'), (1, 0), lambda x, y: x < 150 and 0 < y < 158)
    rim_light(layer, c('#5c2d4d'), (-1, 0), lambda x, y: x > 230 and y < 166)
    rim_light(layer, c('#2f1c3c'), (0, -1), lambda x, y: y > 150)

    # Flowers in the grass: a stem, petals and a light center
    flowers = [(44, 162, 'pink'), (58, 165, 'white'), (71, 164, 'pink'), (110, 168, 'white'), (122, 170, 'pink'),
               (186, 169, 'pink'), (226, 167, 'white'), (238, 165, 'pink'), (262, 162, 'white'), (276, 160, 'pink'),
               (140, 172, 'white'), (166, 171, 'pink')]
    petals = {'pink': (c('#e8789c'), c('#a8507e')), 'white': (c('#f2dcea'), c('#a994bd'))}
    for x, y, kind in flowers:
        light, shade = petals[kind]
        for i in range(1, 6):
            layer.put(x, y + i, c('#251a36'))
        layer.put(x - 1, y, light)
        layer.put(x + 1, y, light)
        layer.put(x, y - 1, light)
        layer.put(x, y + 1, shade)
        layer.put(x, y, c('#ffd56e'))
        layer.put(x + 1, y + 3, c('#251a36'))

    # Fireflies
    for x, y in [(78, 132), (101, 146), (64, 118), (232, 138), (268, 128), (291, 140), (120, 152), (196, 150), (40, 104)]:
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            layer.put(x + dx, y + dy, c('#d9b45a', 110))
        layer.put(x, y, c('#fff2a8'))
    return layer


def main():
    out = Path(sys.argv[1] if len(sys.argv) > 1 else '.')
    out.mkdir(parents=True, exist_ok=True)
    draw_sky().save(out / 'parallax_sky.png')
    draw_mountains().save(out / 'parallax_mountains.png')
    draw_hills().save(out / 'parallax_hills.png')
    draw_foreground().save(out / 'parallax_foreground.png')


if __name__ == '__main__':
    main()
