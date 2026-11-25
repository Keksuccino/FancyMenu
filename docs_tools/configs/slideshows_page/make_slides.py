"""Draws the pixel art of the Slideshows page: four places of a world tour, 320x180 art pixels each.

- world_tour/: the slideshow of the menu background images, four scenes and a properties.txt.
- gallery/: the same scenes with a wooden picture frame as overlay.png, for the Slideshow element image.

Every art pixel becomes 4x4 image pixels (1280x720), which is one image pixel per screen pixel for a full-screen background at GUI
scale 2 in a 1280x720 window, so the game never has to smooth or skip pixels. A Slideshow element of 320x180 GUI pixels shows every
art pixel as 2x2 screen pixels.

Usage: python3 make_slides.py <out dir> [--preview]
  --preview also writes preview.png, all scenes and the frame at 2x art size, to look at them.
"""

import math
import random
import sys
from pathlib import Path

from PIL import Image

W, H = 320, 180
SCALE = 4

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def c(hex_color):
    h = hex_color.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255


def dither(x, y, amount):
    """True for `amount` (0..1) of the pixels, in an ordered pattern."""
    return (BAYER[int(y) % 4][int(x) % 4] + 0.5) / 16.0 < amount


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def interp(points, x, smooth=False):
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        if x0 <= x <= x1:
            t = (x - x0) / (x1 - x0) if x1 != x0 else 0
            if smooth:
                t = (1 - math.cos(t * math.pi)) / 2
            return y0 + (y1 - y0) * t
    return points[0][1] if x < points[0][0] else points[-1][1]


def walk(seed, length, spread=1):
    """A random walk of small steps, for rough edges of rocks, grass and snow."""
    rng = random.Random(seed)
    value, values = 0, []
    for _ in range(length):
        value += rng.choice([-1, 0, 0, 1])
        value = max(-spread, min(spread, value))
        values.append(value)
    return values


class Canvas:

    def __init__(self, transparent=False):
        self.img = Image.new('RGBA', (W, H), (0, 0, 0, 0 if transparent else 255))
        self.px = self.img.load()

    def put(self, x, y, color):
        x, y = int(x), int(y)
        if 0 <= x < W and 0 <= y < H:
            self.px[x, y] = color

    def get(self, x, y):
        x, y = int(x), int(y)
        if 0 <= x < W and 0 <= y < H:
            return self.px[x, y]
        return (0, 0, 0, 0)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, color)

    def scaled(self):
        return self.img.resize((W * SCALE, H * SCALE), Image.NEAREST)


def banded(x, y, p, colors):
    """The color of position p (0..len-1) in a list of colors: clean bands with a dithered step between two neighbors."""
    p = max(0.0, min(len(colors) - 1.0, p))
    i = int(p)
    if i >= len(colors) - 1:
        return colors[-1]
    t = (p - i - 0.3) / 0.4
    if t <= 0:
        return colors[i]
    if t >= 1:
        return colors[i + 1]
    return colors[i + 1] if dither(x, y, t) else colors[i]


def gradient(canvas, colors, top, bottom, shift=None):
    """Fills rows top..bottom with a banded vertical gradient. shift(x, y) moves the band position, like for a glow."""
    for y in range(top, bottom):
        base = (y - top) / max(1, bottom - top - 1) * (len(colors) - 1)
        for x in range(W):
            p = base + (shift(x, y) if shift else 0)
            canvas.put(x, y, banded(x, y, p, colors))


def cloud(canvas, bottom, puffs, top_lit, body, shade, low):
    """A cumulus cloud with a flat bottom and round puffs, lit from above. puffs: (center x, half width, height)."""
    inside = set()
    for cx, half, height in puffs:
        for x in range(int(cx - half), int(cx + half) + 1):
            t = abs(x - cx) / half
            top = bottom - int(round(height * math.sqrt(max(0.0, 1 - t * t))))
            for y in range(top, bottom + 1):
                inside.add((x, y))
    for x, y in inside:
        if (x, y - 1) not in inside:
            color = top_lit
        elif (x, y + 1) not in inside:
            color = low
        elif (x, y + 2) not in inside or (x, y + 3) not in inside and dither(x, y, 0.5):
            color = shade
        else:
            color = body
        canvas.put(x, y, color)


def disc(canvas, cx, cy, r, color, check=None):
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r and (check is None or check(x, y)):
                canvas.put(x, y, color)


def glow(canvas, cx, cy, rings):
    """Rings of dithered glow around a light: rings are (radius, color, amount), drawn from the outside in."""
    for r, color, amount in rings:
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r and dither(x, y, amount):
                    canvas.put(x, y, color)


def peaks(canvas, mountains, seed, base_y, lit, shade, snow_lit, snow_shade, rim, light_from_left=True, snow_depth=0.0, behind=None):
    """Mountains from peaks (x, y, left slope, right slope, crease lean). Every mountain has a lit and a shaded side split by a
    crease, and snow down to snow_depth of its height along the crease. Later mountains are in front. The rim only shows against
    the sky, not in front of the mountains in behind (what an earlier call returned)."""
    rng = random.Random(seed)
    owner = {}
    for k, (px, py, sl, sr, lean) in enumerate(mountains):
        rough = walk(rng.randint(0, 9999), W + 40, 1)
        for x in range(-20, W + 20):
            dx = x - px
            top = py + (sl * -dx if dx < 0 else sr * dx)
            top = int(round(top + rough[x + 20] * min(1.0, abs(dx) / 8.0)))
            for y in range(max(0, top), base_y):
                owner[(x, y)] = (k, top)
    for (x, y), (k, top) in owner.items():
        px, py, sl, sr, lean = mountains[k]
        crease = px + lean * (y - py) + (1 if (y // 3) % 3 == 0 else 0)
        lit_side = (x <= crease) if light_from_left else (x >= crease)
        color = lit if lit_side else shade
        depth = (base_y - py) * snow_depth - abs(x - crease) * 0.45 + ((x * 7 + y) % 4 == 0) * 1.5
        if y < py + depth:
            color = snow_lit if lit_side else snow_shade
        canvas.put(x, y, color)
    for (x, y), (k, top) in owner.items():
        if y == top and (x, y - 1) not in owner and rim is not None and (behind is None or (x, y - 1) not in behind):
            canvas.put(x, y, rim)
    return owner


def scatter(canvas, seed, count, area, colors, check=None):
    rng = random.Random(seed)
    x0, y0, x1, y1 = area
    for _ in range(count):
        x, y = rng.randint(x0, x1), rng.randint(y0, y1)
        if check is None or check(x, y):
            canvas.put(x, y, rng.choice(colors))


# ---------------------------------------------------------------- 1: cherry grove in the morning

def blossom_crown(canvas, blobs, seed, deep, shade, body, light, highlight):
    """A cherry crown from clumps (x, y, r), lit from the top left. Lower clumps are in front, and the clumps behind them get dark
    where they meet, so every clump reads on its own. Single blossoms break up the smooth shading."""
    rng = random.Random(seed)
    owner = {}
    for k, (bx, by, r) in sorted(enumerate(blobs), key=lambda e: e[1][1]):
        for y in range(int(by - r) - 2, int(by + r) + 3):
            for x in range(int(bx - r * 1.3) - 2, int(bx + r * 1.3) + 3):
                dx, dy = x + 0.5 - bx, y + 0.5 - by
                # Ragged outline: blossoms stick out or leave gaps here and there
                ragged = 0.9 if (x * 13 + y * 7 + k) % 6 == 0 else (-0.9 if (x * 5 + y * 11) % 9 == 0 else 0)
                # Clumps are wider than tall, like the flat crowns of cherry trees
                if math.hypot(dx / 1.25, dy / 0.85) <= r + ragged:
                    owner[(x, y)] = (by, -(dx * 0.6 + dy * 0.8) / r)
    for (x, y), (depth, lit) in owner.items():
        below = [owner.get((x + ox, y + oy)) for ox, oy in [(0, 1), (1, 1), (0, 2)]]
        if (x, y + 1) not in owner:
            color = deep
        elif any(b is not None and b[0] > depth + 0.5 for b in below[:2]):
            # A clump in front starts right below: a line of shadow
            color = shade
        elif lit > 0.62:
            color = highlight if dither(x, y, min(1.0, (lit - 0.62) / 0.25)) else light
        elif lit > 0.28:
            color = light if dither(x, y, min(1.0, (lit - 0.28) / 0.12)) else body
        elif lit > -0.42:
            color = body
        else:
            color = shade if dither(x, y, min(1.0, (-0.42 - lit) / 0.12)) else body
        canvas.put(x, y, color)
    # Single blossoms: light dots in the shade and dark dots in the light, a few of them as small crosses
    cells = list(owner.keys())
    for _ in range(len(cells) // 22):
        x, y = rng.choice(cells)
        depth, lit = owner[(x, y)]
        if (x, y + 1) not in owner:
            continue
        color = light if lit < 0.0 else (shade if lit < 0.5 else body)
        canvas.put(x, y, color)
        if rng.random() < 0.25 and lit > 0.3:
            for ox, oy in [(-1, 0), (1, 0), (0, -1)]:
                if (x + ox, y + oy) in owner:
                    canvas.put(x + ox, y + oy, highlight)
    return owner


def branch(canvas, points, light, color, dark):
    """A trunk or branch along points (x, y, half width): lit on its left edge, dark on its right edge."""
    for (x0, y0, w0), (x1, y1, w1) in zip(points, points[1:]):
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for s in range(steps + 1):
            t = s / steps
            x, y, w = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w0 + (w1 - w0) * t
            left, right = int(round(x - w)), int(round(x + w))
            for px in range(left, right + 1):
                color_here = color
                if right - left >= 2 and px == left:
                    color_here = light
                elif right - left >= 1 and px == right:
                    color_here = dark
                canvas.put(px, y, color_here)


def cherry_tree(canvas, base_x, base_y, s, seed, colors):
    """A cherry tree of size s (1 is about 46 art pixels tall): a bent trunk with three branches under clumps of blossoms."""
    bark_light, bark, bark_dark, deep, shade, body, light, highlight = colors
    bx, by = base_x, base_y
    branch(canvas, [(bx, by, 2.4 * s), (bx + 2 * s, by - 10 * s, 1.8 * s), (bx + 1 * s, by - 20 * s, 1.4 * s), (bx - 2 * s, by - 28 * s, 1.0 * s)],
           bark_light, bark, bark_dark)
    branch(canvas, [(bx + 2 * s, by - 13 * s, 1.2 * s), (bx + 9 * s, by - 20 * s, 0.8 * s), (bx + 15 * s, by - 26 * s, 0.5 * s)],
           bark_light, bark, bark_dark)
    branch(canvas, [(bx + 1 * s, by - 17 * s, 1.0 * s), (bx - 7 * s, by - 23 * s, 0.7 * s), (bx - 14 * s, by - 26 * s, 0.4 * s)],
           bark_light, bark, bark_dark)
    # Roots spread on the ground
    for side in (-1, 1):
        for i in range(int(3 * s)):
            canvas.put(bx + side * (2.4 * s + i), by - (1 if i < s else 0), bark_dark if side > 0 else bark)
    clumps = [(-2, -37, 7.5), (8, -35, 7), (-12, -32, 6.5), (16, -29, 6), (2, -30, 7.5), (-6, -27, 6.5), (11, -25, 6),
              (-17, -25, 5), (21, -23, 4.5), (-1, -23, 5.5), (5, -42, 5.5), (-9, -40, 5)]
    blossom_crown(canvas, [(bx + dx * s, by + dy * s, r * s) for dx, dy, r in clumps], seed, deep, shade, body, light, highlight)
    # Branches that show between the clumps
    for dx, dy in [(10, -21), (-8, -22), (14, -24)]:
        for i in range(max(1, int(s * 1.5))):
            canvas.put(bx + dx * s + i, by + dy * s + i // 2, bark_dark)


def draw_cherry_grove():
    cv = Canvas()
    sky = [c('#4f9fe3'), c('#5eabe8'), c('#70b7ec'), c('#86c3ef'), c('#9dcff1'), c('#b5daf2'), c('#cbe3f2'), c('#dfeaf0'), c('#f0ecee')]
    sun_x, sun_y = 64, 30
    gradient(cv, sky, 0, 112, lambda x, y: 1.6 * math.exp(-(((x - sun_x) / 60.0) ** 2 + ((y - sun_y) / 40.0) ** 2)))
    glow(cv, sun_x, sun_y, [(20, c('#d3e8f2'), 0.25), (15, c('#e2eff2'), 0.5), (11, c('#f2f6ee'), 0.75)])
    disc(cv, sun_x, sun_y, 8, c('#fff4d0'))
    disc(cv, sun_x, sun_y, 6.5, c('#fffbea'))

    white, body, shade, low = c('#ffffff'), c('#f3f5fb'), c('#d9e0f2'), c('#c3cbe8')
    cloud(cv, 34, [(150, 12, 6), (166, 14, 10), (184, 12, 7), (199, 9, 4), (138, 7, 3)], white, body, shade, low)
    cloud(cv, 52, [(250, 10, 5), (264, 12, 8), (280, 10, 6), (293, 7, 3)], white, body, shade, low)
    cloud(cv, 20, [(292, 8, 3), (304, 9, 5), (316, 7, 3)], white, body, shade, low)
    cloud(cv, 60, [(106, 7, 2), (116, 8, 4), (126, 6, 2)], white, body, shade, low)

    # Far mountains in the morning haze, the sun on their left sides
    peaks(cv, [(30, 62, 0.75, 0.6, 0.2), (112, 70, 0.62, 0.66, -0.1), (206, 58, 0.7, 0.62, 0.25), (292, 66, 0.66, 0.72, -0.15)],
          31, 112, c('#b4bfe6'), c('#9aa6d8'), c('#f4f6fc'), c('#d0d8f0'), c('#dfe5f6'), True, 0.38)
    peaks(cv, [(-6, 88, 0.5, 0.45, 0.2), (156, 86, 0.48, 0.5, 0.1), (250, 90, 0.5, 0.46, -0.1), (330, 84, 0.5, 0.5, 0)],
          32, 112, c('#a0acd9'), c('#8e98cc'), c('#e6ebf8'), c('#c0c9e8'), None, True, 0.0)

    # Rolling hills, lighter at their tops where the sun hits them
    hills = [([(-10, 104), (40, 98), (90, 103), (140, 99), (200, 104), (250, 97), (330, 103)], c('#9fd486'), c('#8cc876'), 41),
             ([(-10, 116), (30, 110), (80, 114), (130, 108), (180, 113), (240, 110), (290, 115), (330, 111)], c('#86c46c'), c('#72b55e'), 42),
             ([(-10, 128), (50, 122), (110, 127), (170, 120), (230, 125), (280, 121), (330, 126)], c('#76b95e'), c('#60a652'), 43)]
    tops = []
    for points, top_color, color, seed in hills:
        rough = walk(seed, W, 1)
        top = [int(round(interp(points, x, True))) + rough[x] for x in range(W)]
        tops.append(top)
        for x in range(W):
            for y in range(top[x], H):
                near_top = y - top[x]
                cv.put(x, y, top_color if near_top < 2 or (near_top < 4 and dither(x, y, 0.5)) else color)
    # Small cherry trees on the far hills: a pink dab with a trunk pixel
    rng = random.Random(44)
    for k, (top, size) in enumerate([(tops[0], 2), (tops[1], 3)]):
        for x in range(4 + k * 3, W - 4, 9 + k * 5):
            if rng.random() < 0.55:
                continue
            x += rng.randint(-2, 2)
            y = top[x] - 1
            cv.put(x, y + 1, c('#7a5a6a'))
            for dy in range(size):
                for dx in range(-size + dy // 2, size - dy // 2):
                    cv.put(x + dx, y - dy, c('#f2b3c9') if dy == size - 1 or dx < 0 else c('#e297b6'))

    # The river: wide and bright in front, narrow far away, with light lines on the water
    river = [(206, 120), (196, 126), (178, 132), (170, 140), (186, 150), (214, 160), (232, 170), (240, 180)]
    for y in range(120, H):
        cx = interp([(p[1], p[0]) for p in river], y, True)
        half = 1 + (y - 120) * 0.42
        if y <= tops[2][int(cx)] + 1:
            # The river comes out from behind the crest of the front hill
            continue
        for x in range(int(cx - half), int(cx + half) + 1):
            edge = abs(x - cx) > half - 1.2
            line = (y % 5 == 1) and ((x * 3 + y) % 13 < 5) and not edge
            cv.put(x, y, c('#5fb2de') if edge else (c('#d6f0fa') if line else (c('#8fd0ee') if (x + y) % 9 < 7 else c('#a6dcf2'))))
        # Bank: a row of light earth on both sides
        cv.put(int(cx - half) - 1, y, c('#a88c6a'))
        cv.put(int(cx + half) + 1, y, c('#8a6e54'))

    # A small wooden bridge over the river
    bridge_y = 141
    for x in range(150, 196):
        t = (x - 150) / 45.0
        y = bridge_y - int(round(math.sin(t * math.pi) * 4))
        cv.put(x, y, c('#a8794f'))
        cv.put(x, y + 1, c('#8a5e3c'))
        cv.put(x, y + 2, c('#6a4430'))
        if x % 5 == 0:
            for dy in range(-5, 0):
                cv.put(x, y + dy, c('#7a5238'))
        cv.put(x, y - 5, c('#a8794f'))
    for x in (151, 194):
        for dy in range(0, 6):
            cv.put(x, bridge_y + 2 + dy, c('#5a3a2a'))

    # Front meadow with flowers and petals
    rough = walk(45, W, 1)
    meadow = [int(round(interp([(-10, 150), (60, 146), (130, 152), (200, 158), (260, 152), (330, 148)], x, True))) + rough[x] for x in range(W)]
    for x in range(W):
        for y in range(meadow[x], H):
            d = y - meadow[x]
            if cv.get(x, y)[:3] in [(0x5f, 0xb2, 0xde), (0x8f, 0xd0, 0xee), (0xa6, 0xdc, 0xf2), (0xd6, 0xf0, 0xfa)] and d > 1:
                continue
            color = c('#6cbc56') if d < 2 else (c('#58a84a') if d < 14 or dither(x, y, 0.5) else c('#4a9442'))
            cv.put(x, y, color)
        # Grass blades along the edge of the meadow
        if x % 3 == 0:
            cv.put(x, meadow[x] - 1, c('#6cbc56'))
    rng = random.Random(46)
    for _ in range(140):
        x, y = rng.randint(0, W - 1), rng.randint(152, H - 1)
        if cv.get(x, y)[:3] in [(0x58, 0xa8, 0x4a), (0x4a, 0x94, 0x42)]:
            kind = rng.random()
            if kind < 0.45:
                cv.put(x, y, c('#ffc8dc'))
            elif kind < 0.7:
                cv.put(x, y, c('#ffffff'))
                cv.put(x, y + 1, c('#3e8a3c'))
            elif kind < 0.85:
                cv.put(x, y, c('#ffe36e'))
                cv.put(x, y + 1, c('#3e8a3c'))
            else:
                cv.put(x, y, c('#ff8fb8'))
                cv.put(x + 1, y, c('#ffc8dc'))

    colors = (c('#8e6866'), c('#6e4a4a'), c('#4a3036'), c('#b65a88'), c('#d8739f'), c('#ee98bb'), c('#f8bad2'), c('#ffe4ef'))
    cherry_tree(cv, 120, 125, 0.6, 49, colors)
    cherry_tree(cv, 272, 152, 1.2, 47, colors)
    cherry_tree(cv, 30, 182, 1.9, 48, colors)

    # Petals in the air, drifting to the right
    rng = random.Random(50)
    for _ in range(46):
        x, y = rng.randint(0, W - 1), rng.randint(20, H - 10)
        cv.put(x, y, c('#ffd2e2') if rng.random() < 0.6 else c('#f4a6c4'))
        if rng.random() < 0.3:
            cv.put(x + 1, y + 1, c('#f4a6c4'))
    return cv


# ---------------------------------------------------------------- 2: badlands at noon

# Terracotta bands, with how often they show up: mostly orange, sometimes a light or a dark one
STRIPE_COLORS = [('#c8643f', 3), ('#d9804c', 3), ('#e49a5e', 2), ('#b4503a', 2), ('#eebb80', 1.5), ('#9c4636', 1.5), ('#f4d6a6', 0.7),
                 ('#8e4a3e', 1), ('#e9ad74', 1.5)]


def stripes_for(seed, height):
    """An irregular stack of terracotta bands, one color per row, so no cliff repeats a pattern."""
    rng = random.Random(seed)
    rows, last = [], None
    while len(rows) < height:
        options = [(color, weight) for color, weight in STRIPE_COLORS if color != last]
        color = rng.choices([o[0] for o in options], [o[1] for o in options])[0]
        rows += [c(color)] * rng.choice([2, 3, 3, 4, 4, 5, 6])
        last = color
    return rows


def mesa(canvas, x0, x1, top, base_y, seed, haze, cap, slope_left=3.0, slope_right=3.0, shade_to=0.3):
    """A flat-topped mesa: a striped cliff that gets a bit wider downwards. The sun is in the top right, so the left part of the
    cliff is in shade. Its edges get a rim: light on the right, dark on the left. cap: the colors of the dry grass on top."""
    rng = random.Random(seed)
    rough_l = walk(rng.randint(0, 999), 200, 2)
    rough_r = walk(rng.randint(0, 999), 200, 2)
    top_rough = walk(rng.randint(0, 999), W + 1, 1)
    rows = stripes_for(seed, base_y - top + 8)
    shadow, haze_color = c('#5a2a3a'), c('#ead6c6')
    for y in range(top, base_y):
        d = y - top
        left = x0 - d / slope_left + rough_l[d % 200]
        right = x1 + d / slope_right + rough_r[d % 200]
        for x in range(int(left), int(right) + 1):
            t = (x - left) / max(1, right - left)
            # The bands bend a little along the cliff
            wave = int(round(math.sin(x * 0.07 + seed) * 1.6 + math.sin(x * 0.23 + seed * 2) * 0.6))
            color = rows[max(0, min(len(rows) - 1, d + wave + 2))]
            shaded = t < shade_to + 0.05 * math.sin(y * 0.6 + seed)
            if shaded:
                color = mix(color, shadow, 0.3)
            elif (x * 7 + seed) % 13 == 0 and (y + x) % 7 < 3:
                # Cracks in the lit part of the rock
                color = mix(color, c('#7a3a34'), 0.22)
            if x == int(left):
                color = mix(color, shadow, 0.45)
            elif x == int(right):
                color = mix(color, c('#ffe2b8'), 0.4)
            if haze > 0:
                color = mix(color, haze_color, haze)
            canvas.put(x, y, color)
    for x in range(int(x0), int(x1) + 1):
        y = top + top_rough[x % (W + 1)]
        canvas.put(x, y - 1, cap[0])
        canvas.put(x, y, cap[1])
        if (x * 5) % 7 < 3:
            canvas.put(x, y - 2, cap[0])


def cactus(canvas, x, base_y, height, arms):
    light, body, dark, spine = c('#86b858'), c('#5c9440'), c('#3f6e30'), c('#d8e8a8')
    for y in range(base_y - height, base_y):
        for dx in range(-2, 3):
            color = light if dx == -1 else (dark if dx == 2 else body)
            canvas.put(x + dx, y, color)
        if y % 3 == 0:
            canvas.put(x + (-2 if y % 6 == 0 else 2), y, spine)
    for dx in range(-1, 2):
        canvas.put(x + dx, base_y - height - 1, body if dx else light)
    for side, at, up in arms:
        ay = base_y - at
        for i in range(1, 6):
            for dy in range(-1, 2):
                canvas.put(x + side * (2 + i), ay + dy, dark if dy == 1 else body)
        ax = x + side * 6
        for y in range(ay - up, ay + 1):
            for dx in range(-1, 2):
                canvas.put(ax + dx, y, light if dx == -1 else (dark if dx == 1 else body))
        canvas.put(ax, ay - up - 1, body)


DEAD_BUSH = ['#   #  #',
             ' # #  # ',
             '  #  ## ',
             ' ## #   ',
             '   ##   ',
             '   #    ']


def dead_bush(canvas, x, y, color):
    """A dry bush of bare twigs, standing on (x, y)."""
    for row, line in enumerate(DEAD_BUSH):
        for col, ch in enumerate(line):
            if ch == '#':
                canvas.put(x - 3 + col, y - len(DEAD_BUSH) + 1 + row, color)


def pyramid(canvas, cx, base_y, half, haze):
    """A small desert temple far away: a stepped sandstone pyramid, lit from the right."""
    lit, shade, line = c('#f2deac'), c('#d2b684'), c('#c4a474')
    for y in range(base_y - half, base_y):
        w = (y - (base_y - half)) + 1
        for x in range(cx - w, cx + w + 1):
            color = lit if x >= cx else shade
            if (y - base_y) % 3 == 0:
                color = line
            canvas.put(x, y, mix(color, c('#ead6c6'), haze))
    canvas.rect(cx - 2, base_y - 4, cx + 2, base_y, mix(c('#8a6a4a'), c('#ead6c6'), haze))


def draw_badlands():
    cv = Canvas()
    sky = [c('#2a64c8'), c('#3271d2'), c('#3f80da'), c('#4f8fe1'), c('#62a0e7'), c('#7ab1ec'), c('#95c2ef'), c('#b3d3f0'), c('#d2e1ec'),
           c('#ece7da')]
    sun_x, sun_y = 270, 22
    gradient(cv, sky, 0, 118, lambda x, y: 1.2 * math.exp(-(((x - sun_x) / 34.0) ** 2 + ((y - sun_y) / 30.0) ** 2)))
    glow(cv, sun_x, sun_y, [(14, c('#d4e6f8'), 0.25), (10.5, c('#eaf2fb'), 0.5)])
    disc(cv, sun_x, sun_y, 7, c('#fffcef'))
    disc(cv, sun_x, sun_y, 5.5, c('#ffffff'))
    # Thin, high clouds
    rng = random.Random(60)
    for cx, cy, length in [(70, 18, 46), (128, 30, 30), (196, 14, 38), (36, 34, 24), (160, 46, 22)]:
        for i in range(length):
            x = cx - length // 2 + i
            y = cy + int(round(math.sin(i * 0.12) * 1.2))
            t = i / length
            if 0.08 < t < 0.92 or dither(x, y, 0.5):
                cv.put(x, y, c('#eef5fc') if 0.2 < t < 0.8 else c('#bcd6f2'))
                if 0.3 < t < 0.7 and rng.random() < 0.6:
                    cv.put(x, y + 1, c('#a8c8ee'))

    # The plain far away, in the haze
    gradient(cv, [c('#ecd2b4'), c('#e6c09a'), c('#e0ae82')], 114, 150)
    for x in range(W):
        cv.put(x, 114, c('#f2e0c8'))
    pyramid(cv, 146, 120, 9, 0.35)
    # Far mesas in the haze, then near ones, from the back to the front
    cap_far, cap_near = (c('#dcc8a8'), c('#d4b896')), (c('#c2b85c'), c('#9a8a44'))
    mesa(cv, 8, 70, 80, 118, 61, 0.55, cap_far)
    mesa(cv, 176, 214, 88, 118, 62, 0.6, cap_far, 4, 4)
    mesa(cv, 236, 318, 76, 118, 63, 0.5, cap_far)
    mesa(cv, 86, 124, 96, 124, 64, 0.42, cap_far, 2.5, 2.5)
    mesa(cv, -24, 60, 40, 150, 65, 0.0, cap_near, 6, 2.2, 0.0)
    mesa(cv, 220, 344, 56, 150, 66, 0.0, cap_near, 2.4, 6, 0.24)
    # A mineshaft in the right cliff, with an oak frame
    for y in range(116, 140):
        for x in range(250, 266):
            cv.put(x, y, c('#2a1414') if y > 118 else c('#3a1c18'))
    for y in range(114, 141):
        cv.put(248, y, c('#a07a50'))
        cv.put(249, y, c('#6a4a30'))
        cv.put(266, y, c('#a07a50'))
        cv.put(267, y, c('#6a4a30'))
    for x in range(246, 270):
        cv.put(x, 112, c('#b08a5c'))
        cv.put(x, 113, c('#8a6440'))
        cv.put(x, 114, c('#5a3a24'))

    # The ground: red sand with ripples and darker patches
    rough = walk(68, W, 1)
    ground = [int(round(interp([(-10, 146), (80, 142), (160, 140), (240, 143), (330, 141)], x, True))) + rough[x] for x in range(W)]
    for x in range(W):
        for y in range(ground[x], H):
            d = y - ground[x]
            color = c('#e6a066') if d < 2 else c('#d6844c')
            ripple = (y * 3 + int(math.sin(x * 0.15 + y * 0.5) * 3)) % 9
            if ripple == 0:
                color = c('#e9a46a')
            elif ripple == 1:
                color = c('#c47040')
            if d > 22 and dither(x, y, min(1.0, (d - 22) / 14.0) * 0.5):
                color = c('#c47040')
            cv.put(x, y, color)
    # Rails from the mineshaft to the front, with oak ties
    for y in range(140, H):
        t = (y - 140) / 40.0
        cx = 258 - t * 26
        half = 4 + t * 9
        if int(t * 40) % (3 + int(t * 4)) == 0:
            for x in range(int(cx - half) - 1, int(cx + half) + 3):
                cv.put(x, y, c('#7a5634'))
        for side in (-1, 1):
            cv.put(cx + side * half, y, c('#a4a4ae'))
            cv.put(cx + side * half + 1, y, c('#5c5c66'))
    # A minecart on the rails
    mx, my = 245, 160
    for y in range(my - 8, my):
        for x in range(mx - 9, mx + 10):
            if y == my - 8 or x in (mx - 9, mx + 9):
                color = c('#c8c8d2')
            elif y == my - 7:
                color = c('#3c3c46')
            else:
                color = c('#8a8a96') if y < my - 3 else c('#6c6c78')
            cv.put(x, y, color)
    for x in range(mx - 8, mx + 9, 4):
        cv.put(x, my - 4, c('#5c5c66'))
    for wx in (mx - 6, mx + 6):
        disc(cv, wx + 0.5, my + 0.5, 1.8, c('#2e2e38'))
        cv.put(wx, my, c('#8a8a96'))
    cactus(cv, 46, 176, 30, [(-1, 18, 9), (1, 12, 7)])
    cactus(cv, 132, 154, 15, [(1, 8, 5)])
    cactus(cv, 304, 172, 20, [(-1, 10, 6)])
    for x, y in [(92, 162), (178, 152), (206, 174), (16, 152), (286, 154), (150, 172)]:
        dead_bush(cv, x, y, c('#7a5232'))
    # Small stones
    scatter(cv, 69, 70, (0, 146, W - 1, H - 1), [c('#a85a3a'), c('#f0b880')])
    return cv


# ---------------------------------------------------------------- 3: frozen peaks at night

def aurora(canvas, base, height, streak_seed, colors, strength):
    """A curtain of light: brightest at its lower edge base(x), fading upwards over height(x), with vertical streaks.
    strength(x) (0..1) lets the curtain fade out at its ends."""
    rng = random.Random(streak_seed)
    streaks = [0.55 + 0.45 * rng.random() for _ in range(W)]
    for x in range(1, W - 1):
        streaks[x] = (streaks[x - 1] + 2 * streaks[x] + streaks[x + 1]) / 4
    for x in range(W):
        y0, h = base(x), height(x)
        for y in range(int(y0 - h), int(y0) + 1):
            t = min(1.0, max(0.0, (y0 - y) / h))
            amount = (1 - t) ** 1.6 * streaks[x]
            if y0 - y < 1.5:
                amount = max(amount, 0.85 * streaks[x])
            amount *= strength(x)
            for threshold, color in colors:
                if amount > threshold + 0.06 or (amount > threshold - 0.06 and dither(x, y, (amount - threshold + 0.06) / 0.12)):
                    canvas.put(x, y, color)
                    break


def spruce(canvas, x, base_y, height, body, dark, snow, snow_shade):
    """A snowy spruce: tiers that get wider downwards. Snow lies on the top row of every tier and drips down a bit, lit from the
    left by the moon."""
    tier_length = 5 if height < 24 else 8
    for i in range(height):
        y = base_y - height + i
        half = int(i * 0.36) + 1
        tier = i % tier_length
        if tier == 0 and i > 2:
            half = max(1, half - (2 if tier_length == 5 else 4))
        for dx in range(-half, half + 1):
            color = dark if dx > half // 3 else body
            # Snow lies on the left and middle of a tier's top row and drips down at a few spots
            if i > 1 and tier == 0 and -half < dx <= half // 2:
                color = snow if dx <= half // 4 else snow_shade
            elif i > 1 and tier == 1 and -half + 1 < dx < half // 3 and (x * 3 + dx * 7 + i) % 5 < 2:
                color = snow if dx <= 0 else snow_shade
            canvas.put(x + dx, y, color)
    canvas.put(x, base_y - height - 1, snow)
    canvas.put(x, base_y - height, snow)
    for y in range(base_y, base_y + 2):
        canvas.put(x, y, dark)


def draw_frozen_peaks():
    cv = Canvas()
    sky = [c('#060a1e'), c('#09102c'), c('#0d163a'), c('#111d48'), c('#162556'), c('#1c2f66'), c('#233a74'), c('#2b4682')]
    gradient(cv, sky, 0, 118)
    rng = random.Random(70)
    for _ in range(120):
        x, y = rng.randint(0, W - 1), rng.randint(0, 100)
        cv.put(x, y, c('#ffffff') if rng.random() < 0.3 else (c('#9fb4e8') if rng.random() < 0.6 else c('#6a80c0')))
    for x, y in [(22, 12), (96, 8), (152, 40), (300, 58), (210, 10), (58, 52)]:
        cv.put(x, y, c('#ffffff'))
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            cv.put(x + dx, y + dy, c('#7f94d0'))

    # Two curtains of aurora
    greens = [(0.78, c('#c8ffe6')), (0.6, c('#6cf5be')), (0.42, c('#34d2a0')), (0.27, c('#22a28c')), (0.15, c('#1b6f78'))]
    aurora(cv, lambda x: 70 + 10 * math.sin(x * 0.021 + 0.6) + 6 * math.sin(x * 0.057 + 2.0) - 0.06 * x,
           lambda x: 34 + 10 * math.sin(x * 0.033), 71, greens, lambda x: 1.0)
    teal = [(0.6, c('#8bf0c8')), (0.42, c('#3fc4a6')), (0.27, c('#2a8c96')), (0.15, c('#2c4c8e'))]
    aurora(cv, lambda x: 44 + 8 * math.sin(x * 0.026 + 3.1) + 0.04 * x, lambda x: 24 + 6 * math.sin(x * 0.05 + 1.0), 72, teal,
           lambda x: max(0.0, min(1.0, (x - 110) / 70.0)) * 0.9)

    # The moon, a crescent with a soft glow, in front of the aurora
    moon_x, moon_y = 40, 20
    glow(cv, moon_x, moon_y, [(14, c('#18285e'), 0.25), (11, c('#22346f'), 0.5)])
    disc(cv, moon_x, moon_y, 8, c('#f2f5ff'))
    disc(cv, moon_x + 4, moon_y - 3, 7.2, c('#0c1436'), lambda x, y: math.hypot(x + 0.5 - moon_x, y + 0.5 - moon_y) <= 8.5)
    for x, y in [(36, 23), (34, 19), (38, 26)]:
        cv.put(x, y, c('#cfd8f4'))

    # Snowy mountains lit by the moon from the left
    back = peaks(cv, [(70, 50, 0.85, 0.7, 0.15), (168, 60, 0.75, 0.8, -0.12), (262, 54, 0.72, 0.9, 0.1)], 73, 124,
                 c('#3c4c80'), c('#28345e'), c('#dbe5fa'), c('#7f96c8'), c('#eef3ff'), True, 0.62)
    peaks(cv, [(-4, 84, 0.6, 0.55, 0.2), (120, 90, 0.55, 0.6, -0.1), (216, 92, 0.6, 0.55, 0.15), (330, 88, 0.6, 0.6, 0)], 74, 124,
          c('#34426f'), c('#232d52'), c('#c4d2f0'), c('#6c84b8'), c('#d8e2f8'), True, 0.45, back)

    # Spruce forest at the foot of the mountains
    rng = random.Random(75)
    x = -2
    while x < W + 2:
        h = rng.randint(9, 16)
        spruce(cv, x, 128 + rng.randint(-1, 1), h, c('#14223e'), c('#0e182e'), c('#a8bce4'), c('#7088bc'))
        x += rng.choice([4, 5, 5, 6, 7])
    for x in range(W):
        for y in range(126, 131):
            if cv.get(x, y)[:3] in [(0x34, 0x42, 0x6f), (0x23, 0x2d, 0x52), (0x2b, 0x46, 0x82)]:
                cv.put(x, y, c('#14223e'))

    # The frozen lake: dark ice that mirrors the aurora in soft streaks, with a few light lines
    for y in range(130, 162):
        for x in range(W):
            color = c('#1a2a52')
            depth = y - 130
            curtain = 70 + 10 * math.sin(x * 0.021 + 0.6) + 6 * math.sin(x * 0.057 + 2.0) - 0.06 * x
            glow_amount = max(0.0, 1 - depth / 12.0) * max(0.0, min(1.0, (84 - curtain) / 14.0))
            if glow_amount > 0 and dither(x, y, glow_amount * 0.7):
                color = c('#1f5a70') if glow_amount < 0.55 else c('#2a8a84')
            elif y % 4 == 2 and (x * 3 + y * 5) % 29 < 6:
                color = c('#2a3e70')
            cv.put(x, y, color)
    for x in range(W):
        cv.put(x, 130, c('#2e4478'))

    # Snow in front, with soft drifts and sparkles
    rough = walk(76, W, 1)
    snow = [int(round(interp([(-10, 150), (60, 146), (140, 152), (210, 148), (330, 144)], x, True))) + rough[x] for x in range(W)]
    for x in range(W):
        for y in range(snow[x], H):
            d = y - snow[x]
            wave = math.sin(x * 0.05 + y * 0.12) * 4
            if d < 2:
                color = c('#e6eeff')
            elif (d + wave) % 16 < 3:
                color = c('#8ea4d6') if dither(x, y, 0.5) else c('#b4c6ec')
            else:
                color = c('#b4c6ec') if d < 12 or dither(x, y, 0.6) else c('#9cb0e0')
            cv.put(x, y, color)
    scatter(cv, 77, 60, (0, 150, W - 1, H - 1), [c('#ffffff')])

    # An igloo with warm light in its entrance, the light falling on the snow
    ix, iy, ir = 236, 160, 20
    for y in range(iy - ir, iy + 1):
        for x in range(ix - ir, ix + ir + 1):
            d = math.hypot(x + 0.5 - ix, (y + 0.5 - iy) * 1.15)
            if d <= ir:
                row = (iy - y) // 4
                joint = (y - iy) % 4 == 0 or (x + row * 3) % 7 == 0
                lit = x < ix + 4 - (iy - y) * 0.2
                color = (c('#c6d4f2') if lit else c('#93a8d6')) if joint else (c('#f2f6ff') if lit else c('#b8c8ea'))
                if d > ir - 1:
                    color = c('#7a90c4')
                cv.put(x, y, color)
    for y in range(iy - 9, iy + 1):
        for x in range(ix - 17, ix - 5):
            d = math.hypot((x + 0.5 - (ix - 11)) * 0.9, (y + 0.5 - iy) * 1.0)
            if d <= 9:
                cv.put(x, y, c('#c6d4f2') if d > 8 else (c('#eaf0ff') if y < iy - 5 else c('#d4def6')))
    for y in range(iy - 6, iy + 1):
        for x in range(ix - 14, ix - 8):
            d = math.hypot((x + 0.5 - (ix - 11)) * 1.0, (y + 0.5 - iy) * 0.9)
            if d <= 5:
                cv.put(x, y, c('#ffd88a') if d < 2.8 else c('#f0a050'))
    for y in range(iy + 1, iy + 12):
        spread = (y - iy) * 1.6 + 3
        for x in range(int(ix - 11 - spread), int(ix - 11 + spread) + 1):
            amount = 0.65 - (y - iy) / 14.0 - abs(x - (ix - 11)) / (spread * 2.2)
            if amount > 0 and dither(x, y, amount):
                cv.put(x, y, c('#e8c49a') if amount > 0.3 else c('#c8b4b8'))
    # A lantern on a post next to the igloo
    for y in range(146, 162):
        cv.put(206, y, c('#2a2a3a'))
    glow(cv, 206.5, 145.5, [(7, c('#4a5a8a'), 0.35), (4.5, c('#c9a070'), 0.5)])
    cv.rect(205, 143, 209, 148, c('#ffcf70'))
    cv.put(206, 144, c('#fff2c4'))
    for x in range(204, 210):
        cv.put(x, 142, c('#2a2a3a'))
        cv.put(x, 148, c('#2a2a3a'))

    # Tall spruces framing the left side
    spruce(cv, 18, 178, 64, c('#101a30'), c('#0a1222'), c('#c4d4f2'), c('#7d94c8'))
    spruce(cv, 44, 170, 42, c('#121e36'), c('#0b1426'), c('#b8caee'), c('#7890c4'))
    spruce(cv, 304, 176, 50, c('#101a30'), c('#0a1222'), c('#c4d4f2'), c('#7d94c8'))
    return cv


# ---------------------------------------------------------------- 4: coral reef

def coral_fan(canvas, x, base_y, height, color, dark, seed):
    """A sea fan: thin branches that split as they grow, like a lattice. Branches lean apart, so the fan gets wide."""
    rng = random.Random(seed)
    tips = [(x, base_y, -math.pi / 2)]
    for step in range(height):
        new = []
        for tx, ty, angle in tips:
            angle += rng.uniform(-0.22, 0.22)
            nx, ny = tx + math.cos(angle), ty + math.sin(angle)
            canvas.put(nx, ny, dark if step < 3 or rng.random() < 0.2 else color)
            if rng.random() < 0.12 and len(tips) < 16:
                new.append((nx, ny, angle - 0.55))
                angle += 0.35
            new.append((nx, ny, angle))
        tips = new
    for tx, ty, angle in tips:
        canvas.put(tx, ty, mix(color, c('#ffffff'), 0.35))


def tube_coral(canvas, x, base_y, tubes, light, body, dark, rim):
    """A cluster of tubes (x offset, height), each with a lit left side and an open, lighter rim."""
    for dx, height in tubes:
        for y in range(base_y - height, base_y + 1):
            canvas.put(x + dx, y, light)
            canvas.put(x + dx + 1, y, body)
            canvas.put(x + dx + 2, y, dark)
        canvas.put(x + dx, base_y - height - 1, rim)
        canvas.put(x + dx + 1, base_y - height - 1, dark)
        canvas.put(x + dx + 2, base_y - height - 1, rim)


def brain_coral(canvas, cx, base_y, r, light, body, groove):
    """A dome with winding grooves, lit from the top left."""
    for y in range(int(base_y - r), base_y + 1):
        for x in range(int(cx - r * 1.3), int(cx + r * 1.3) + 1):
            d = math.hypot((x + 0.5 - cx) / 1.3, y + 0.5 - base_y)
            if d <= r:
                maze = math.sin(x * 0.9 + math.sin(y * 0.8) * 2.0) + math.sin(y * 1.1 + x * 0.3)
                lit = (x < cx + 1) and (y < base_y - r * 0.4)
                color = groove if abs(maze) < 0.35 else (light if lit else body)
                if d > r - 1 and x > cx:
                    color = groove
                canvas.put(x, y, color)


def kelp(canvas, x, base_y, height, phase, light, body, dark):
    """A kelp strand that sways more towards its top, with leaves on alternating sides and small floats."""
    for i in range(height):
        y = base_y - i
        sway = math.sin(i * 0.09 + phase) * (i / height) * 7
        cx = x + sway
        canvas.put(cx - 1, y, light)
        canvas.put(cx, y, body)
        canvas.put(cx + 1, y, dark)
        if i % 8 == 4 and 4 < i < height - 3:
            side = 1 if (i // 8) % 2 else -1
            for k in range(1, 7):
                lx, ly = cx + side * (1 + k), y - k // 2 - (1 if k > 4 else 0)
                canvas.put(lx, ly, light if k < 3 else body)
                canvas.put(lx, ly + 1, dark if k < 6 else body)
            canvas.put(cx + side * 2, y + 1, c('#c8d84a'))


def seagrass(canvas, x, base_y, seed, light, dark):
    rng = random.Random(seed)
    for blade in range(rng.randint(3, 5)):
        bx = x + rng.randint(-3, 3)
        height = rng.randint(4, 9)
        lean = rng.choice([-1, 1])
        for i in range(height):
            canvas.put(bx + (lean * i) // 4, base_y - i, light if blade % 2 else dark)


def fish(canvas, x, y, body, stripe, eye, facing=1, size=1):
    """A small tropical fish facing left (-1) or right (1)."""
    shape = [' bbb   ', 'bbsbb t', 'bbsbbtt', 'bbsbb t', ' bbb   ']
    if size == 0:
        shape = ['bbs t', 'bbstt', 'bbs t']
    for row, line in enumerate(shape):
        for col, ch in enumerate(line):
            px = x + (col if facing < 0 else -col)
            py = y + row
            if ch == 'b':
                canvas.put(px, py, body if row < len(shape) - 1 else mix(body, c('#000000'), 0.15))
            elif ch == 's':
                canvas.put(px, py, stripe)
            elif ch == 't':
                canvas.put(px, py, mix(body, c('#000000'), 0.2))
    canvas.put(x + (1 if facing < 0 else -1), y + (1 if size else 0), eye)


TURTLE = ['      ss          ',
          '    sshhss   ff   ',
          '  sshssshhss ffe f',
          ' ssshhsssshssffff ',
          ' sssssshhssssff   ',
          '  ddssssssdd      ',
          ' ff  dddd  ff     ',
          'ff          ff    ']


def turtle(canvas, x, y):
    """A sea turtle swimming to the right."""
    colors = {'s': c('#5e8f4a'), 'h': c('#9cc46a'), 'd': c('#3e6a38'), 'f': c('#a8c88a'), 'e': c('#1c2a20')}
    for row, line in enumerate(TURTLE):
        for col, ch in enumerate(line):
            if ch in colors:
                canvas.put(x + col, y + row, colors[ch])


def reef_rock(canvas, cx, base_y, rx, ry, seed):
    """A rounded rock of the reef, lit from above, with darker pores. Returns its top per x, to put corals on it."""
    rough = walk(seed, int(rx * 2) + 4, 1)
    tops = {}
    for x in range(int(cx - rx), int(cx + rx) + 1):
        t = (x - cx) / rx
        top = int(round(base_y - ry * math.sqrt(max(0.0, 1 - t * t)) ** 0.7)) + rough[x - int(cx - rx)]
        tops[x] = top
        for y in range(top, base_y + 1):
            d = y - top
            color = c('#5c4f8c')
            if d < 2:
                color = c('#7a6cae') if t < 0.5 else c('#6a5c9c')
            elif t > 0.5 or d > ry * 0.75:
                color = c('#463b72') if dither(x, y, 0.5 if t <= 0.5 else 1.0) else color
            if (x * 7 + y * 13) % 31 == 0 and d > 2:
                color = c('#3a305e')
            canvas.put(x, y, color)
    return tops


def draw_coral_reef():
    cv = Canvas()
    water = [c('#6ad8e6'), c('#4ec6de'), c('#3cb0d4'), c('#3098c6'), c('#2780b4'), c('#1f6aa2'), c('#18578f'), c('#12477c')]
    gradient(cv, water, 0, 150)
    # Beams of sunlight from the top left, a lighter version of the water, fading with depth
    for y in range(6, 150):
        for x in range(W):
            amount = 0.0
            for start, width in [(26, 7), (78, 5), (136, 9), (198, 6), (262, 8)]:
                along = x - (start + y * 0.4)
                amount += max(0.0, 1 - abs(along) / width)
            amount *= max(0.0, 1 - y / 140.0)
            if amount > 0 and dither(x, y, min(1.0, amount) * 0.75):
                cv.put(x, y, mix(cv.get(x, y), c('#d8fbff'), 0.2))
    # The surface: wavy bright lines at the top
    for x in range(W):
        y = 3 + int(round(math.sin(x * 0.18) * 1.2 + math.sin(x * 0.05) * 1.5))
        for dy in range(0, y + 1):
            cv.put(x, dy, c('#a8ecf2') if dy < y else c('#e2fafc'))
        if (x // 6) % 3 == 0:
            cv.put(x, y + 3, c('#9ae4ee'))

    # Far away: reef mounds and a sunken ship, pale in the blue
    far = c('#1e6a9c')
    rough = walk(81, W, 2)
    for x in range(W):
        top = int(round(interp([(-10, 124), (50, 116), (110, 122), (170, 112), (230, 120), (290, 110), (330, 118)], x, True))) + rough[x]
        for y in range(top, 150):
            cv.put(x, y, far)
    # The ship lies tilted on the ground: a hull with a raised bow, a broken mast and a yard
    ship, ship_dark = c('#1a5c8e'), c('#175282')
    for x in range(56, 150):
        t = (x - 56) / 94.0
        deck = 112 - int(round((1 - t) ** 3 * 18)) + int(round(t * 6))
        bottom = 136 - int(round(math.sin(min(1.0, t * 1.4) * math.pi / 2) * 6)) + int(round(t * 2))
        for y in range(deck, bottom):
            cv.put(x, y, ship_dark if (y - deck) % 6 == 5 else ship)
    for x in range(132, 150):
        # The broken stern: a jagged edge
        for y in range(116, 118 + ((x * 5) % 7)):
            cv.put(x, y, far if x > 140 else cv.get(x, y))
    for i in range(48):
        cv.put(98 + i // 8, 112 - i, ship)
        cv.put(99 + i // 8, 112 - i, ship)
    for x in range(86, 118):
        cv.put(x, 78 + (x - 86) // 6, ship)
    for x, y in [(76, 114), (92, 116), (108, 118), (124, 120)]:
        cv.rect(x, y, x + 3, y + 3, c('#13466e'))

    # The sand floor with light ripples
    rough = walk(82, W, 1)
    floor = [int(round(interp([(-10, 140), (70, 136), (150, 142), (230, 138), (330, 136)], x, True))) + rough[x] for x in range(W)]
    for x in range(W):
        for y in range(floor[x], H):
            d = y - floor[x]
            ripple = math.sin(x * 0.2 + math.sin(y * 0.3) * 2 + y * 0.6)
            color = c('#e8d49a') if ripple > 0.75 else (c('#d4bc80') if ripple > -0.4 else c('#bea36c'))
            if d < 1:
                color = c('#f0e0aa')
            if d > 20 and dither(x, y, 0.4) and ripple < 0:
                color = c('#a88f5c')
            cv.put(x, y, color)

    # Fish far away and a turtle
    for x, y in [(150, 46), (158, 42), (164, 48), (172, 44), (178, 50), (186, 46), (168, 54), (154, 52), (180, 38), (192, 42), (198, 48)]:
        for dx, dy in [(0, 0), (1, 0), (2, 0), (1, -1), (-1, -1), (-1, 1)]:
            cv.put(x + dx, y + dy, c('#2b8cbc'))
    turtle(cv, 214, 74)

    # The reef in front: rocks with corals on them, seagrass and kelp
    kelp_colors = (c('#6cd890'), c('#3cb878'), c('#24865a'))
    for x, height, phase in [(190, 58, 1.8), (204, 44, 2.6), (106, 40, 0.4)]:
        kelp(cv, x, floor[x] + 2, height, phase, *kelp_colors)
    coral_fan(cv, 22, 152, 30, c('#ff7ab6'), c('#c8407e'), 85)
    left = reef_rock(cv, 46, 182, 52, 28, 86)
    coral_fan(cv, 240, 146, 36, c('#ff7ab6'), c('#c8407e'), 83)
    right = reef_rock(cv, 278, 182, 54, 32, 87)
    middle = reef_rock(cv, 150, 182, 24, 16, 88)
    brain_coral(cv, 30, left[30] + 3, 10, c('#ffe27a'), c('#f2c64a'), c('#b8862a'))
    tube_coral(cv, 54, left[56] + 2, [(0, 14), (4, 20), (8, 11), (12, 17)], c('#ff9a7a'), c('#f06a4a'), c('#b8423a'), c('#ffd0b8'))
    tube_coral(cv, 76, left[78] + 2, [(0, 8), (4, 12)], c('#d88cff'), c('#b05ad8'), c('#7e3aa8'), c('#f2d4ff'))
    brain_coral(cv, 300, right[300] + 3, 9, c('#b6f07a'), c('#8ad656'), c('#4e9a34'))
    tube_coral(cv, 262, right[264] + 2, [(0, 18), (4, 12), (8, 22), (12, 9)], c('#7aa8ff'), c('#4a78ec'), c('#3052b0'), c('#c8dcff'))
    coral_fan(cv, 150, middle[150] + 1, 20, c('#ffb06a'), c('#d4702e'), 84)
    tube_coral(cv, 8, left[10] + 2, [(0, 10), (4, 15), (8, 7)], c('#ffe48a'), c('#f2c040'), c('#b8862a'), c('#fff4c8'))
    brain_coral(cv, 92, left[92] + 4, 6, c('#7ae8e0'), c('#4ac8c0'), c('#2a8a8a'))
    tube_coral(cv, 238, right[240] + 2, [(0, 7), (4, 11), (8, 6)], c('#ff9a7a'), c('#f06a4a'), c('#b8423a'), c('#ffd0b8'))
    brain_coral(cv, 316, right[316] + 3, 6, c('#ffe27a'), c('#f2c64a'), c('#b8862a'))
    brain_coral(cv, 140, middle[140] + 2, 6, c('#ff9ac8'), c('#f070a8'), c('#b84a80'))
    for x in (100, 118, 178, 196, 214, 224):
        seagrass(cv, x, floor[x] + 6, x, c('#5cc884'), c('#2f9a64'))
    # Sea pickles: small glowing lights on the sand
    for x, y in [(112, 158), (116, 160), (186, 162), (204, 156), (208, 158)]:
        cv.put(x, y, c('#7ad84a'))
        cv.put(x, y - 1, c('#f0ff9a'))
    for x, height, phase in [(6, 128, 0.0), (15, 100, 1.2), (312, 132, 2.1), (300, 96, 0.6)]:
        kelp(cv, x, H - 1, height, phase, *kelp_colors)

    # Tropical fish near the reef
    fish(cv, 98, 104, c('#ff8a3c'), c('#ffffff'), c('#1a1a2a'), 1)
    fish(cv, 110, 112, c('#ff8a3c'), c('#ffffff'), c('#1a1a2a'), 1, 0)
    fish(cv, 252, 96, c('#ffd23c'), c('#4a7af0'), c('#1a1a2a'), -1)
    fish(cv, 268, 106, c('#5a8cff'), c('#ffe46a'), c('#1a1a2a'), -1, 0)
    fish(cv, 176, 126, c('#ff6aa8'), c('#ffe0ee'), c('#1a1a2a'), 1, 0)
    fish(cv, 60, 70, c('#ffd23c'), c('#4a7af0'), c('#1a1a2a'), 1)
    # Bubbles rising from the reef
    for x, y in [(58, 122), (60, 114), (57, 104), (59, 92), (270, 112), (272, 100), (269, 86), (271, 72), (152, 128), (153, 119)]:
        cv.put(x, y, c('#d8f6fa'))
        cv.put(x + 1, y - 1, c('#a8e6f0'))
    return cv


# ---------------------------------------------------------------- the frame of the gallery

def draw_frame():
    """A wooden picture frame with a gold inner edge and small gold corners. Its middle stays transparent."""
    cv = Canvas(transparent=True)
    t = 8
    for y in range(H):
        for x in range(W):
            d = min(x, y, W - 1 - x, H - 1 - y)
            if d >= t:
                continue
            # Wood grain that runs along the frame
            along = x if min(y, H - 1 - y) <= min(x, W - 1 - x) else y
            grain = (along * 3 + d * 11) % 17
            if d == 0:
                color = c('#2e1a12')
            elif d == 1:
                color = c('#8a5a34')
            elif d >= t - 2:
                color = c('#ffd86a') if d == t - 2 else c('#a8741e')
            elif d == t - 3:
                color = c('#3e2416')
            else:
                color = c('#6a4026') if grain < 12 else c('#5a3420')
            # Mitered corners: the light side is at the top and left
            top_left = (y <= x and y <= H - 1 - y and y < W - 1 - x) or (x < y and x <= W - 1 - x and x < H - 1 - y)
            if 1 < d < t - 3 and not top_left:
                color = mix(color, c('#1a0e0a'), 0.25)
            cv.put(x, y, color)
    # Gold corner brackets: an L along both edges and a brace across the corner, with a rivet
    for cx, cy in [(0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)]:
        for dy in range(12):
            for dx in range(12):
                arm = (dy < 3 and dx < 12) or (dx < 3 and dy < 12)
                brace = 5 <= dx + dy <= 6 and dx < 6 and dy < 6
                if not (arm or brace):
                    continue
                if brace and not arm:
                    color = c('#e8b848')
                elif min(dx, dy) == 0:
                    color = c('#ffe08a')
                elif min(dx, dy) == 2 or max(dx, dy) == 11:
                    color = c('#a8741e')
                else:
                    color = c('#e8b848')
                x = cx + dx if cx == 0 else cx - dx
                y = cy + dy if cy == 0 else cy - dy
                cv.put(x, y, color)
        cv.put(cx + 1 if cx == 0 else cx - 1, cy + 1 if cy == 0 else cy - 1, c('#fff4c8'))
    return cv


SCENES = [('01_cherry_grove', draw_cherry_grove), ('02_badlands', draw_badlands), ('03_frozen_peaks', draw_frozen_peaks),
          ('04_coral_reef', draw_coral_reef)]

PROPERTIES = """type = slideshow

slideshow-meta {{
  name = {name}
  duration = {duration}
  fadespeed = {fadespeed}
  randomize = false
}}
"""


def main():
    out = Path(sys.argv[1])
    scenes = [(name, draw()) for name, draw in SCENES]
    frame = draw_frame()
    for folder, duration, fadespeed in [('world_tour', 8.0, 3.0), ('gallery', 6.0, 4.0)]:
        images = out / folder / 'images'
        images.mkdir(parents=True, exist_ok=True)
        for name, canvas in scenes:
            canvas.scaled().convert('RGB').save(images / f'{name}.png')
        (out / folder / 'properties.txt').write_text(PROPERTIES.format(name=folder, duration=duration, fadespeed=fadespeed))
    frame.scaled().save(out / 'gallery' / 'overlay.png')
    if '--preview' in sys.argv:
        sheet = Image.new('RGBA', (W * 2 * 2 + 12, H * 2 * 3 + 24), (40, 40, 40, 255))
        for i, (name, canvas) in enumerate(scenes + [('frame', None)]):
            img = canvas.img if canvas else Image.alpha_composite(scenes[0][1].img, frame.img)
            sheet.paste(img.resize((W * 2, H * 2), Image.NEAREST), ((i % 2) * (W * 2 + 12), (i // 2) * (H * 2 + 12)))
        sheet.save(out / 'preview.png')
    print('done:', out)


if __name__ == '__main__':
    main()
