"""Draws the pixel art of the Menu Backgrounds page: Frostvale, a snowy valley at night with an aurora, a moon and a cabin.

- frostvale/background.png: the whole scene, the Image background of the page's images.
- frostvale/land.png: the same without its sky (transparent), to show how a Color background fills transparent parts.

Both are 320x180 art pixels. Every art pixel becomes 4x4 image pixels (1280x720), which is one image pixel per screen pixel
in a 1280x720 window at GUI scale 2.

Usage: python3 make_art.py <out dir> [--preview]
"""

import math
import random
import sys
from pathlib import Path

from PIL import Image

W, H = 320, 180
SCALE = 4
HORIZON = 132
MOON_X, MOON_Y, MOON_R = 262, 32, 11

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def c(hex_color, alpha=255):
    h = hex_color.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha


def dither(x, y, amount):
    """True for `amount` (0..1) of the pixels, in an ordered pattern."""
    return (BAYER[y % 4][x % 4] + 0.5) / 16.0 < amount


def ramp(x, y, value, palette):
    """Picks a color of `palette` for `value` (0..1), dithering between neighbors."""
    v = max(0.0, min(1.0, value)) * (len(palette) - 1)
    i = int(v)
    if i >= len(palette) - 1:
        return palette[-1]
    return palette[i + 1] if dither(x, y, v - i) else palette[i]


SKY = [c('#090e22'), c('#0e1633'), c('#152047'), c('#1d2a5a'), c('#27356c'), c('#33407b'), c('#434b88'), c('#5a5893')]
HALO = [c('#2a3a72'), c('#34468a'), c('#40559c')]
AURORA = [c('#1f4d6e'), c('#23767f'), c('#2da38a'), c('#47d39c'), c('#93f5c6')]
STARS = [c('#7d92c8'), c('#b9cbf2'), c('#ffffff')]
MOON = {'light': c('#f6f2dc'), 'mid': c('#e2dcc0'), 'dark': c('#c6bf9f'), 'crater': c('#b3ab8b'), 'rim': c('#fffbea')}

FAR = {'shadow': c('#2a3567'), 'body': c('#323f76'), 'lit': c('#41518a'), 'snow_shadow': c('#8d9dcc'), 'snow': c('#b9c7ea'), 'snow_lit': c('#e2eafb')}
NEAR = {'shadow': c('#1d2650'), 'body': c('#242e5c'), 'lit': c('#303d72'), 'snow_shadow': c('#7383b6'), 'snow': c('#9fafdc'), 'snow_lit': c('#cfdaf5')}

GROUND = [c('#7686b8'), c('#8d9dcb'), c('#a6b5dd'), c('#bfcbeb'), c('#d5dff5'), c('#e7eefb')]
PINE = {'dark': c('#0d1430'), 'body': c('#131c3d'), 'edge': c('#1c2a52'), 'snow': c('#9eb0dc'), 'snow_lit': c('#d8e3f8')}
FAR_PINE = {'body': c('#1a2650'), 'edge': c('#26346a')}

WOOD = {'dark': c('#3a2420'), 'body': c('#5c3b2c'), 'light': c('#75503a'), 'line': c('#46291f')}
ROOF_SNOW = [c('#a9b8df'), c('#cbd7f2'), c('#eef3fd')]
WINDOW = {'frame': c('#2a1912'), 'glow': c('#ffc25c'), 'bright': c('#ffe3a1'), 'warm': c('#e89a45')}
GLOW_SNOW = [c('#e9d6b2'), c('#f4e2bd')]
SMOKE = [c('#7480ad'), c('#a9b2d2')]


class Canvas:

    def __init__(self):
        self.img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        self.px = self.img.load()

    def put(self, x, y, color):
        x, y = int(x), int(y)
        if 0 <= x < W and 0 <= y < H:
            self.px[x, y] = color

    def get(self, x, y):
        return self.px[int(x), int(y)]

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, color)


def draw_sky(cv, rng):
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - MOON_X, (y - MOON_Y) * 1.15)
            halo = max(0.0, 1.0 - (d - MOON_R) / 34.0) if d > MOON_R else 1.0
            value = y / (HORIZON - 8)
            color = ramp(x, y, value, SKY)
            if halo > 0.0:
                level = halo * halo * len(HALO)
                index = int(level)
                if dither(x, y, level - index):
                    index += 1
                if index > 0:
                    halo_color = HALO[min(index, len(HALO)) - 1]
                    # The halo only lightens the sky
                    if sum(halo_color[:3]) > sum(color[:3]):
                        color = halo_color
            cv.put(x, y, color)


def aurora_base(x):
    return 76 + 9 * math.sin(x / 34.0 + 0.6) + 5 * math.sin(x / 13.0 + 2.1) + 2 * math.sin(x / 5.3)


def draw_aurora(cv):
    for x in range(-4, 236):
        # Fades in at the left and out before the moon
        edge = min(1.0, (x + 4) / 40.0, (236 - x) / 60.0)
        if edge <= 0:
            continue
        rays = 0.55 + 0.25 * math.sin(x * 0.83) + 0.2 * math.sin(x * 0.29 + 1.3)
        height = 16 + 16 * (0.5 + 0.5 * math.sin(x / 21.0 + 1.9)) + 8 * rays
        base = aurora_base(x)
        for y in range(int(base - height) - 1, int(base) + 3):
            if y < 0 or y >= H:
                continue
            if y > base:
                intensity = 1.0 - (y - base) / 2.5
            else:
                intensity = max(0.0, 1.0 - (base - y) / height) ** 1.4
            intensity *= edge * (0.6 + 0.4 * rays)
            if intensity <= 0.04:
                continue
            level = intensity * len(AURORA)
            index = int(level)
            if dither(x, y, level - index):
                index += 1
            if index > 0:
                cv.put(x, y, AURORA[min(index, len(AURORA)) - 1])


def draw_stars(cv, rng):
    placed = 0
    while placed < 120:
        x, y = rng.randrange(W), rng.randrange(0, 112)
        if math.hypot(x - MOON_X, y - MOON_Y) < MOON_R + 9:
            continue
        # Keep the stars out of the bright aurora
        if aurora_base(x) - 30 < y < aurora_base(x) + 3 and x < 230 and rng.random() < 0.8:
            continue
        brightness = rng.choices([0, 1, 2], weights=[6, 3, 1])[0]
        if y > 90:
            brightness = 0
        cv.put(x, y, STARS[brightness])
        placed += 1
    # A few bright stars with a small cross
    for x, y in [(38, 18), (131, 12), (204, 22), (300, 64), (92, 40), (168, 46)]:
        cv.put(x, y, STARS[2])
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            cv.put(x + dx, y + dy, STARS[1])


def draw_moon(cv):
    craters = [(-4, -3, 2.6), (3, 2, 2.0), (-1, 5, 1.6), (5, -5, 1.3), (-6, 3, 1.2)]
    for y in range(MOON_Y - MOON_R - 1, MOON_Y + MOON_R + 2):
        for x in range(MOON_X - MOON_R - 1, MOON_X + MOON_R + 2):
            dx, dy = x - MOON_X + 0.5, y - MOON_Y + 0.5
            d = math.hypot(dx, dy)
            if d > MOON_R:
                continue
            # Light from the upper right, the lower left edge gets darker
            shade = (dx * -0.55 + dy * 0.65) / MOON_R
            color = MOON['light']
            if shade > 0.55:
                color = MOON['dark'] if dither(x, y, min(1.0, (shade - 0.55) * 3)) else MOON['mid']
            elif shade > 0.2:
                color = MOON['mid'] if dither(x, y, (shade - 0.2) * 2.5) else MOON['light']
            for cx, cy, cr in craters:
                cd = math.hypot(dx - cx, dy - cy)
                if cd < cr:
                    color = MOON['crater'] if cd < cr - 0.8 or shade > 0.3 else MOON['dark']
            if d > MOON_R - 1.2 and dx > 0 and dy < 0:
                color = MOON['rim']
            cv.put(x, y, color)


def ridge(peaks, x, jag):
    best, owner = 1e9, None
    for px, py, k in peaks:
        y = py + abs(x - px) * k
        if y < best:
            best, owner = y, (px, py, k)
    return best + jag, owner


def smooth_noise(rng, length, step, amplitude):
    """Values for 0..length that wander smoothly, by interpolating random points `step` apart."""
    points = [rng.uniform(-amplitude, amplitude) for _ in range(length // step + 3)]
    values = []
    for i in range(length + 1):
        a, t = divmod(i, step)
        t /= step
        t = t * t * (3 - 2 * t)
        values.append(points[a] * (1 - t) + points[a + 1] * t)
    return values


def draw_mountains(cv, rng, peaks, palette, bottom, snow_depth):
    jags = {}
    j = 0.0
    for x in range(-2, W + 2):
        j = max(-1.5, min(1.5, j + rng.choice([-1, 0, 0, 1]) * 0.6))
        jags[x] = round(j)
    # Snow runs down in fingers along the gullies, more on the shadow side
    fingers = smooth_noise(rng, W, 3, 1.0)
    gully = smooth_noise(rng, W, 2, 1.0)
    for x in range(W):
        top, (px, py, k) = ridge(peaks, x, jags[x])
        top = int(round(top))
        line = py + snow_depth + 4 * fingers[x] + (5 if gully[x] > 0.55 else 0)
        slant = 0.45 if px % 2 else -0.35
        for y in range(max(0, top), bottom):
            depth = y - top
            # The spine between the lit and the shadow face runs down at a slant
            lit = x > px + (y - py) * slant
            if y < line:
                if lit:
                    color = palette['snow_lit'] if depth < 3 or (depth < 5 and dither(x, y, 0.5)) else palette['snow']
                else:
                    color = palette['snow'] if depth < 1 else palette['snow_shadow']
            else:
                if lit:
                    color = palette['lit'] if depth < 2 else palette['body']
                else:
                    color = palette['shadow']
            cv.put(x, y, color)


def ground_line(x):
    return 140 + 3 * math.sin(x / 41.0 + 0.4) + 2 * math.sin(x / 17.0 + 1.0)


def draw_ground(cv):
    for x in range(W):
        top = int(round(ground_line(x)))
        for y in range(top, H):
            value = (y - top) / 34.0
            # Shadow of the forest edge right below it
            if y - top < 3:
                value -= 0.15
            cv.put(x, y, ramp(x, y, value, GROUND))


def lump(*values):
    """A fixed pseudo random number 0..1 for the given integers, for lumps of snow."""
    h = 0
    for v in values:
        h = (h * 1000003) ^ (v * 73856093 + 19349663)
    return (h % 1000) / 1000.0


def draw_pine(cv, x, base, height, palette):
    """A pine with drooping, ragged tiers. Snow lies on their shoulders and on the tier below each tier, more on the right,
    where the moon shines from. Lower tiers get drawn first, so upper tiers cover them."""
    tiers = max(2, round(height / 8))
    th = height / (tiers + 0.5)
    specs = []
    for i in range(tiers):
        bottom = base - i * th
        top = bottom - th * 1.7 if i < tiers - 1 else base - height
        w = height * 0.36 * (1.0 - i / (tiers + 0.2)) + 1.0
        specs.append((top, bottom, w))

    def curve(spec, dx):
        # The tips of the branches hang lower than the middle
        s = abs(dx) / spec[2]
        return spec[1] - 1 + 2.6 * s * s

    snowy = 'snow' in palette
    for i, spec in enumerate(specs):
        top, bottom, w = spec
        upper = specs[i + 1] if i + 1 < len(specs) else None
        for y in range(int(top), int(bottom) + 3):
            t = (y - top) / max(1.0, bottom - top)
            half = 0.5 + w * min(1.0, t)
            for px in range(int(math.floor(x - half)), int(math.ceil(x + half)) + 1):
                dx = px - x
                if abs(dx) > half:
                    continue
                cy = curve(spec, dx)
                if y > cy:
                    continue
                # Needle tips: the lower edge of the outer branches is ragged
                if y > cy - 1 and abs(dx) / w > 0.45 and (px + i) % 2 == 0:
                    continue
                color = palette['body']
                if dx > 0 and abs(dx) > half - 1 and 'edge' in palette:
                    color = palette['edge']
                if y > cy - 1 and 'dark' in palette:
                    color = palette['dark']
                if snowy and y < cy - 1:
                    lit = dx >= 0
                    edge_dist = half - abs(dx)
                    # Snow on the shoulders of the tier
                    shoulder = t < (0.68 if lit else 0.5) and edge_dist < (2.0 if lit else 1.0)
                    # Snow piled up right below the tier above, in lumps
                    ledge = False
                    if upper is not None and abs(dx) < upper[2] * (0.8 if lit else 0.5):
                        uy = curve(upper, dx)
                        depth = 1 + (1 if lit and lump(px, i, x) > 0.45 else 0)
                        ledge = uy < y <= uy + depth and (lit or lump(px, i, x, 7) > 0.35)
                    if shoulder or ledge:
                        color = palette['snow_lit'] if lit else palette['snow']
                cv.put(px, y, color)
    if 'dark' in palette:
        for y in range(base + 1, base + 3):
            cv.put(x, y, palette['dark'])


def draw_far_forest(cv, rng):
    x = -2
    while x < W + 2:
        in_clearing = 150 < x < 178 or 222 < x < 238
        if not in_clearing:
            base = int(round(ground_line(x))) + 1
            height = rng.randint(5, 11)
            draw_pine(cv, x, base, height, FAR_PINE)
        x += rng.randint(2, 5)


def draw_cabin(cv):
    x0, x1, base = 196, 232, 158
    wall_top = base - 13
    # Warm light of the windows on the snow in front of them
    for y in range(base - 1, base + 10):
        for x in range(x0 - 10, x1 + 12):
            amount = 0.0
            for wx in (x0 + 7, x1 - 6):
                d = math.hypot((x - wx) / 1.9, (y - base - 1) * 1.1)
                amount = max(amount, 0.95 - d / 7.5)
            if amount > 0 and cv.get(x, y)[3] > 0:
                level = amount * len(GLOW_SNOW)
                index = int(level)
                if dither(x, y, level - index):
                    index += 1
                if index > 0:
                    cv.put(x, y, GLOW_SNOW[min(index, len(GLOW_SNOW)) - 1])
    # Walls of logs
    for y in range(wall_top, base + 1):
        for x in range(x0, x1 + 1):
            log = (y - wall_top) % 3
            color = WOOD['light'] if log == 0 else WOOD['body']
            if log == 2:
                color = WOOD['line']
            if x in (x0, x1):
                color = WOOD['dark']
            cv.put(x, y, color)
    # Log ends at the corners
    for y in range(wall_top, base + 1, 3):
        cv.put(x0 - 1, y, WOOD['light'])
        cv.put(x1 + 1, y, WOOD['light'])
    # Chimney
    cv.rect(x0 + 5, wall_top - 13, x0 + 8, wall_top - 4, c('#4b4a5e'))
    for y in range(wall_top - 13, wall_top - 4):
        cv.put(x0 + 8, y, c('#5f5e74'))
        if y % 3 == 0:
            cv.put(x0 + 6, y, c('#3b3a4c'))
    cv.rect(x0 + 4, wall_top - 14, x0 + 9, wall_top - 14, ROOF_SNOW[2])
    # Roof with snow, wider than the walls
    roof_left, roof_right = x0 - 4, x1 + 4
    peak_x, peak_y = (x0 + x1) // 2, wall_top - 12
    for y in range(peak_y, wall_top + 1):
        t = (y - peak_y) / (wall_top - peak_y)
        left = int(round(peak_x - t * (peak_x - roof_left)))
        right = int(round(peak_x + t * (roof_right - peak_x)))
        for x in range(left, right + 1):
            if y >= wall_top - 1:
                color = WOOD['dark']
            elif x > peak_x:
                color = ROOF_SNOW[2] if dither(x, y, 0.85) else ROOF_SNOW[1]
            else:
                color = ROOF_SNOW[0] if dither(x, y, 0.7) else ROOF_SNOW[1]
            cv.put(x, y, color)
        # Snow hangs over the edge of the roof
        if y == wall_top - 2:
            for x in range(left - 1, right + 2):
                cv.put(x, y, ROOF_SNOW[2] if x > peak_x else ROOF_SNOW[1])
    for x in range(roof_left - 1, roof_right + 2, 4):
        cv.put(x, wall_top, ROOF_SNOW[1])
    # Windows with a cross and a warm inside
    for wx in (x0 + 4, x1 - 9):
        wy = wall_top + 3
        cv.rect(wx - 1, wy - 1, wx + 6, wy + 6, WINDOW['frame'])
        for y in range(wy, wy + 6):
            for x in range(wx, wx + 6):
                color = WINDOW['bright'] if (y - wy) < 2 and (x - wx) > 1 else WINDOW['glow']
                if (y - wy) > 3:
                    color = WINDOW['warm'] if dither(x, y, 0.5) else WINDOW['glow']
                cv.put(x, y, color)
        for i in range(6):
            cv.put(wx + 2, wy + i, WINDOW['frame'])
            cv.put(wx + i, wy + 2, WINDOW['frame'])
        # Snow on the window sill
        cv.rect(wx - 1, wy + 6, wx + 6, wy + 6, ROOF_SNOW[2])
    # Door
    dx = (x0 + x1) // 2 - 2
    cv.rect(dx, base - 8, dx + 4, base, WOOD['dark'])
    cv.rect(dx + 1, base - 7, dx + 3, base, c('#4a2d22'))
    cv.put(dx + 3, base - 4, WINDOW['glow'])
    # Snow drift along the wall
    for x in range(x0 - 3, x1 + 4):
        if x < dx - 1 or x > dx + 5:
            cv.put(x, base, GROUND[4])
            if (x * 5) % 7 < 4:
                cv.put(x, base - 1, GROUND[5])
    # Smoke rising from the chimney, widening and drifting left, breaking up at its end
    chimney_x, chimney_y = x0 + 6.5, wall_top - 15
    rise = 30
    for step in range(rise):
        t = step / rise
        y = int(chimney_y - step)
        cx = chimney_x - 11 * t ** 1.4 - 1.6 * math.sin(t * 7.0)
        half = 1.2 + 3.6 * t
        for x in range(int(cx - half) - 1, int(cx + half) + 2):
            d = abs(x + 0.5 - cx)
            if d > half:
                continue
            # The end of the column breaks up into wisps
            if t > 0.62 and lump(x, y, 3) < (t - 0.62) * 2.4:
                continue
            lit = x + 0.5 > cx + half * 0.15
            cv.put(x, y, SMOKE[1] if lit else SMOKE[0])


def draw_footprints(cv):
    # From the door down to the lower left
    x, y = 214.0, 161.0
    step = 0
    while y < H:
        side = -1 if step % 2 else 1
        px, py = int(round(x + side * 1.5)), int(round(y))
        cv.put(px, py, GROUND[1])
        cv.put(px + 1, py, GROUND[2])
        x -= 2.6
        y += 2.2
        step += 1


def draw_fence(cv):
    for i, x in enumerate(range(156, 194, 6)):
        base = 160 + (i % 2) * 0 + int((194 - x) * 0.05)
        cv.rect(x, base - 6, x + 1, base, WOOD['dark'])
        cv.put(x, base - 7, ROOF_SNOW[2])
        cv.put(x + 1, base - 7, ROOF_SNOW[1])
    for x in range(156, 194):
        y = 156 + int((194 - x) * 0.05)
        cv.put(x, y, WOOD['body'])
        cv.put(x, y - 1, ROOF_SNOW[1] if x % 3 else ROOF_SNOW[2])


def draw_edge_pines(cv, rng):
    # Big pines that frame the scene at the left and right
    for x, base, height in [(9, 186, 74), (27, 182, 52), (-4, 178, 58), (309, 186, 70), (292, 184, 44), (322, 176, 60)]:
        draw_pine(cv, x, base, height, PINE)
    # Small bushes of snow at their feet
    for x0, x1 in [(0, 44), (282, 320)]:
        for x in range(x0, x1):
            top = 174 + int(2 * math.sin(x * 0.4)) + (x * 13) % 3
            for y in range(top, H):
                cv.put(x, y, GROUND[5] if y - top < 2 else GROUND[4])


def build(with_sky):
    rng = random.Random(7)
    cv = Canvas()
    if with_sky:
        draw_sky(cv, rng)
        draw_aurora(cv)
        draw_stars(cv, rng)
        draw_moon(cv)
    rng = random.Random(11)
    draw_mountains(cv, rng, [(-10, 96, 0.62), (52, 86, 0.66), (112, 100, 0.7), (176, 80, 0.62), (236, 94, 0.7), (298, 84, 0.6), (350, 98, 0.6)],
                   FAR, HORIZON + 12, 10)
    draw_mountains(cv, rng, [(18, 112, 0.5), (84, 106, 0.55), (140, 116, 0.6), (206, 108, 0.5), (266, 114, 0.6), (330, 104, 0.5)],
                   NEAR, HORIZON + 20, 7)
    draw_ground(cv)
    draw_far_forest(cv, random.Random(5))
    draw_fence(cv)
    draw_cabin(cv)
    draw_footprints(cv)
    draw_edge_pines(cv, rng)
    return cv.img


def main():
    out = Path(sys.argv[1]) / 'frostvale'
    out.mkdir(parents=True, exist_ok=True)
    for name, with_sky in [('background', True), ('land', False)]:
        img = build(with_sky)
        img.resize((W * SCALE, H * SCALE), Image.NEAREST).save(out / f'{name}.png', optimize=True)
        if '--preview' in sys.argv:
            preview = Image.new('RGBA', img.size, c('#ff00ff'))
            preview.alpha_composite(img)
            preview.resize((W * 3, H * 3), Image.NEAREST).save(out / f'{name}_preview.png')
    print('done', out)


if __name__ == '__main__':
    main()
