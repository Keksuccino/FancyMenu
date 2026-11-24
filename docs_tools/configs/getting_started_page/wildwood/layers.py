"""Hills (with cabin and lake) and foreground (big pines, ground, lantern) layers, LW x H art pixels with transparency."""
import math
import random
import numpy as np
from scene import W, H, MARGIN, LW, SKY, SUN, hexc, new_layer, paint, dither_index, BAYER4


def pine_mask(height, seed=0):
    """Mask of a pine (crown and trunk) in a local box, with its tip at (cx, 0). Returns mask, cx."""
    rng = random.Random(seed)
    trunk_h = max(1, height // 9)
    crown_h = height - trunk_h
    tiers = max(2, min(8, round(height / 9)))
    maxw = max(2.0, height * 0.22)
    cx = int(maxw) + 3
    m = np.zeros((height + 1, cx * 2 + 1), bool)
    bounds = [round(k * crown_h / tiers) for k in range(tiers + 1)]
    for k in range(tiers):
        r0, r1 = bounds[k], bounds[k + 1]
        wk = maxw * ((k + 1) / tiers) ** 0.85
        start = 0.0 if k == 0 else wk * 0.5
        for row in range(r0, r1):
            t = (row - r0 + 1) / max(1, r1 - r0)
            half = start + (wk - start) * t
            hw = int(round(half))
            for x in range(cx - hw, cx + hw + 1):
                m[row, x] = True
            # Needle tips droop below the bottom row of each tier
            if row == r1 - 1 and hw >= 2:
                for x in (cx - hw, cx + hw):
                    if row + 1 < m.shape[0]:
                        m[row + 1, x] = True
                for x in (cx - hw - 1, cx + hw + 1):
                    if rng.random() < 0.8:
                        m[row, x] = True
    m[0, cx] = True
    tw = 0 if height < 34 else 1
    for row in range(crown_h, height + 1):
        for x in range(cx - tw, cx + tw + 1):
            m[row, x] = True
    return m, cx


def draw_pine(img, cx, base_y, height, body, rim, rim2=None, trunk=None, seed=0, lit_right=True):
    """A pixel pine silhouette with a rim light on the side facing the sun."""
    m, mcx = pine_mask(height, seed)
    side = 1 if lit_right else -1
    shifted = np.zeros_like(m)
    if side == 1:
        shifted[:, :-1] = m[:, 1:]
    else:
        shifted[:, 1:] = m[:, :-1]
    up = np.zeros_like(m)
    up[1:, :] = m[:-1, :]
    rim_mask = m & ~shifted
    # Tier tops on the sun side catch light too
    rim_top = m & ~up
    ox, oy = cx - mcx, base_y - height
    hh, ww = m.shape
    for y in range(hh):
        for x in range(ww):
            if not m[y, x]:
                continue
            paint(img, ox + x, oy + y, rim if (rim_mask[y, x] and y > 1) else body)
    if rim2 is not None:
        paint(img, cx, base_y - height, rim2)


# ---------------------------------------------------------------- hills layer

HILL = hexc('#3d2050')
HILL_RIM = hexc('#5f2d63')
HILL_RIM2 = hexc('#8a4068')
HILL_PINE = hexc('#2c1740')
HILL_PINE_RIM = hexc('#53285c')


def hill_top(x):
    """Height of the hills at layer x: a left hill, a valley with the lake and a right hill."""
    sx = x - MARGIN
    left = 127 + 23 * (((sx - 64) / 124.0) ** 2) if sx < 262 else 999
    right = 141 + 9 * (((sx - 336) / 36.0) ** 2) if sx > 296 else 999
    return int(min(left, right, 170))


LAKE_TOP = 149
LAKE_BOTTOM = 172


def draw_lake(img):
    """The lake mirrors the glowing horizon, a few shades darker, with ripples and the sun's reflection."""
    from scene import sky_values
    v_sky, _, _ = sky_values()
    pal = np.array(SKY, np.uint8)
    sx = SUN[0] + MARGIN
    for y in range(LAKE_TOP, LAKE_BOTTOM):
        depth = y - LAKE_TOP
        mirror_row = max(0, 146 - int(depth * 3.0))
        for x in range(LW):
            col = min(W - 1, max(0, x - MARGIN))
            v = v_sky[mirror_row, col] - 3.6 - depth * 0.12
            # Ripples: short streaks a shade lighter, more of them near the sun
            near = max(0.0, 1.0 - abs(x - sx) / 60.0)
            if depth % 3 == 2 and (x * 7 + depth * 11) % 19 < 5 + int(near * 8):
                v += 1.2
            img[y, x] = pal[int(np.clip(dither_index(np.array(max(v, 0.0)), x, y), 0, len(SKY) - 1))]
    # Sun reflection: broken dashes that get narrower and dimmer with depth
    for i, y in enumerate(range(LAKE_TOP + 1, LAKE_BOTTOM, 2)):
        half = 15 - i * 1.25
        if half < 1:
            break
        c = hexc('#fff3c8') if i < 2 else (hexc('#ffd690') if i < 5 else hexc('#f6a866'))
        x = int(sx - half)
        while x <= sx + half:
            run = 2 + (x * 5 + i * 3) % 4
            for xx in range(x, min(int(sx + half) + 1, x + run)):
                paint(img, xx, y, c)
            x += run + 1 + (x + i) % 3
    # Bright line where the water meets the far shore
    for x in range(LW):
        paint(img, x, LAKE_TOP, hexc('#ffd28e') if abs(x - sx) < 22 else hexc('#c45a6c'))


def draw_hills(img):
    for x in range(LW):
        top = hill_top(x)
        for y in range(top, H):
            img[y, x] = HILL
        img[top, x] = HILL_RIM
        # Sunlit slopes: hills face the sun on their right side
        if hill_top(x + 1) > top or hill_top(x - 1) > top:
            paint(img, x, top + 1, HILL_RIM)
        if abs(x - (SUN[0] + MARGIN)) < 70:
            img[top, x] = HILL_RIM2
    # Dithered shadow toward the bottom of the hills
    for y in range(140, H):
        for x in range(LW):
            if img[y, x][3] and tuple(img[y, x]) == HILL and BAYER4[y % 4, x % 4] < (y - 150) / 40.0:
                img[y, x] = hexc('#321a45')


def draw_cabin(img, cx, base_y):
    """A small log cabin with a warm window and chimney smoke."""
    wall, wall_dark, roof, roof_lit, roof_dark = hexc('#4a2a45'), hexc('#3a2038'), hexc('#2a1430'), hexc('#7a3a5a'), hexc('#1e0e24')
    glow, glow2, glow3 = hexc('#ffe39a'), hexc('#ffbf63'), hexc('#d9844c')
    # Walls 14x7
    for y in range(base_y - 7, base_y):
        for x in range(cx - 7, cx + 7):
            img[y, x] = wall if (y % 2 == 0) else wall_dark
    # Roof: 18 wide triangle-ish with overhang
    for i in range(6):
        y = base_y - 8 - i
        half = 9 - i * 1.5
        for x in range(int(cx - half - 1), int(cx + half) + 1):
            paint(img, x, y, roof)
        paint(img, int(cx + half), y, roof_lit)
        paint(img, int(cx + half) - 1, y, roof_lit) if i < 2 else None
    for x in range(cx - 10, cx + 10):
        paint(img, x, base_y - 8, roof_dark)
    paint(img, cx + 9, base_y - 8, roof_lit)
    # Chimney
    for y in range(base_y - 15, base_y - 10):
        paint(img, cx + 4, y, hexc('#3a2038'))
        paint(img, cx + 5, y, hexc('#5a3048'))
    # Window with cross and glow
    for y in range(base_y - 6, base_y - 2):
        for x in range(cx - 4, cx):
            img[y, x] = glow if (x + y) % 3 else glow2
    paint(img, cx - 3, base_y - 4, glow3)
    paint(img, cx - 2, base_y - 4, glow3)
    for y in range(base_y - 6, base_y - 2):
        paint(img, cx - 2, y, glow3) if y == base_y - 5 else None
    # Door
    for y in range(base_y - 5, base_y):
        paint(img, cx + 2, y, hexc('#24112a'))
        paint(img, cx + 3, y, hexc('#24112a'))
    # Light falling on the ground in front of the window
    for x in range(cx - 6, cx + 1):
        if x % 2 == 0:
            paint(img, x, base_y, glow3)
    paint(img, cx - 3, base_y + 1, hexc('#9a5a4a'))
    # Smoke: soft puffs drifting right, fading out
    puffs = [(cx + 5, base_y - 17, 1, 200), (cx + 7, base_y - 20, 1, 170), (cx + 10, base_y - 23, 2, 140), (cx + 14, base_y - 26, 2, 105), (cx + 19, base_y - 28, 2, 70)]
    for px, py, r, a in puffs:
        for y in range(py - r, py + r + 1):
            for x in range(px - r - 1, px + r + 2):
                if (x - px) ** 2 / (r + 1.2) ** 2 + (y - py) ** 2 / (r + 0.4) ** 2 <= 1.0:
                    if img[y, x][3] == 0:
                        img[y, x] = hexc('#b07a98', a)


def draw_path(img, x0, y0):
    """A dirt path winding from the cabin door down toward the lake."""
    path, path_lit = hexc('#5a3150'), hexc('#74405e')
    pts = []
    for i in range(0, 160):
        t = i / 159.0
        x = x0 + t * 110 + math.sin(t * 5.2) * 9
        y = y0 + 1 + t * 30
        pts.append((int(round(x)), int(round(y))))
    for i, (x, y) in enumerate(pts):
        if img[y, x][3] == 0:
            continue
        w = 1 + int(i / 70)
        for dx in range(w):
            paint(img, x + dx, y, path)
        if i % 7 < 3:
            paint(img, x + w, y, path_lit)


def make_hills_layer(seed=11):
    rng = random.Random(seed)
    img = new_layer(LW, H)
    draw_lake(img)
    draw_hills(img)
    cabin_x = 92
    # Little pines along both hills, leaving room around the cabin
    for x in range(2, LW - 2):
        sx = x - MARGIN
        if 186 < sx < 300 or abs(sx - cabin_x) < 14:
            continue
        if rng.random() < 0.36:
            h = rng.choice([7, 8, 9, 10, 11, 12, 14])
            draw_pine(img, x, hill_top(x) + 3, h, HILL_PINE, HILL_PINE_RIM, seed=x)
    draw_path(img, cabin_x + 3 + MARGIN, hill_top(cabin_x + MARGIN) + 1)
    draw_cabin(img, cabin_x + MARGIN, hill_top(cabin_x + MARGIN) + 1)
    # Two pines right beside the cabin
    for dx, h in ((-11, 15), (12, 12)):
        x = cabin_x + MARGIN + dx
        draw_pine(img, x, hill_top(x) + 3, h, HILL_PINE, HILL_PINE_RIM, seed=dx + 99)
    return img


# ---------------------------------------------------------------- foreground layer

FG = hexc('#150a20')
FG_RIM = hexc('#3b1c4a')
FG_RIM_HOT = hexc('#6d2f5e')


def ground_top(x):
    sx = x - MARGIN
    return int(170 + 2.5 * math.sin(sx / 17.0) + 1.5 * math.sin(sx / 7.3 + 1.0))


def draw_lantern(img, x, base_y):
    post, post_lit = hexc('#24112a'), hexc('#4a2440')
    for y in range(base_y - 26, base_y + 1):
        paint(img, x, y, post)
        paint(img, x + 1, y, post_lit if y % 5 else post)
    # Arm
    for xx in range(x - 6, x + 1):
        paint(img, xx, base_y - 26, post)
    paint(img, x - 6, base_y - 25, post)
    # Lantern body
    ly = base_y - 24
    lx = x - 6
    for y in range(ly, ly + 7):
        for xx in range(lx - 2, lx + 3):
            paint(img, xx, y, hexc('#1e0e24'))
    for y in range(ly + 2, ly + 6):
        for xx in range(lx - 1, lx + 2):
            paint(img, xx, y, hexc('#ffe7a6') if xx == lx and y in (ly + 3, ly + 4) else hexc('#ffbf63'))
    paint(img, lx, ly - 1, hexc('#1e0e24'))
    # Dithered warm halo around the lantern
    for y in range(ly - 6, ly + 12):
        for xx in range(lx - 8, lx + 9):
            d = math.hypot(xx - lx, (y - ly - 3) * 1.1)
            if 3.5 < d < 8.5 and img[y, xx][3] == 0 and BAYER4[y % 4, xx % 4] < (8.5 - d) / 9.0:
                img[y, xx] = hexc('#ffb860', 110)


def make_foreground_layer(seed=5):
    rng = random.Random(seed)
    img = new_layer(LW, H)
    # Big pines framing the scene, back ones first
    for sx, h, s in [(-4, 118, 1), (44, 70, 2), (16, 138, 3), (66, 30, 4)]:
        draw_pine(img, sx + MARGIN, ground_top(sx + MARGIN) + 2, h, FG, FG_RIM, seed=s)
    for sx, h, s in [(332, 132, 6), (292, 58, 7), (312, 108, 8), (278, 24, 9)]:
        draw_pine(img, sx + MARGIN, ground_top(sx + MARGIN) + 2, h, FG, FG_RIM_HOT, seed=s, lit_right=False)
    # Ground with grass tufts
    for x in range(LW):
        top = ground_top(x)
        for y in range(top, H):
            img[y, x] = FG
        r = rng.random()
        if r < 0.22:
            for dy in range(1, rng.choice([2, 3, 3, 4])):
                paint(img, x, top - dy, FG)
        elif r < 0.30:
            paint(img, x, top - 1, FG)
            paint(img, x + 1, top - 2, FG)
        if abs(x - (SUN[0] + MARGIN)) < 70 and img[top - 1, x][3] == 0:
            img[top, x] = FG_RIM_HOT
    draw_lantern(img, 214 + MARGIN, ground_top(214 + MARGIN))
    return img


def flatten(sky, hills, fg, shift_hills=0, shift_fg=0):
    """The scene as one image, like the parallax layers at rest."""
    from PIL import Image
    base = Image.fromarray(sky, 'RGBA')
    for layer, shift in ((hills, shift_hills), (fg, shift_fg)):
        im = Image.fromarray(layer, 'RGBA').crop((MARGIN + shift, 0, MARGIN + shift + W, H))
        base.alpha_composite(im)
    return np.array(base)


if __name__ == '__main__':
    import sys
    from scene import make_sky_layer, save
    out = sys.argv[1]
    sky = make_sky_layer()
    hills = make_hills_layer()
    fg = make_foreground_layer()
    save(flatten(sky, hills, fg), f'{out}/scene_preview.png', 4)
    save(hills, f'{out}/hills_preview.png', 4)
    save(fg, f'{out}/fg_preview.png', 4)
