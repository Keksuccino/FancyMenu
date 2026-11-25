"""Draws the Skyhaven intro of the Game Intro page: a floating island at dawn, the modpack logo drops in letter by letter.

Usage: python3 make_intro.py <out dir> [--preview]

Writes <out dir>/skyhaven_intro.apng (1280x720, 20 fps, plays once) and skyhaven_menu.png, the last frame of the intro without its logo,
as the menu background of the Title screen the intro fades into. Everything gets drawn at 320x180 and scaled up 4x.
--preview also writes every 6th frame as PNG and a contact sheet (preview_sheet.png), to look at the art."""
import os, sys, math
import numpy as np
from PIL import Image

W, H = 320, 180
SCALE = 4
FRAMES = 108
FRAME_MS = 50
RNG = np.random.default_rng(7)

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]])
BAYER_SCREEN = np.tile(BAYER4, (H // 4 + 1, W // 4 + 1))[:H, :W]


def c(s):
    return np.array([int(s[i:i + 2], 16) for i in (1, 3, 5)], dtype=float)


def mix(a, b, t):
    return a + (b - a) * t


def smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


# ---------------------------------------------------------------- sky

NIGHT_SKY = [c(s) for s in ("#0a0920", "#0f0e2c", "#15143a", "#1c1b49", "#24245a", "#2d2e6b", "#37397c", "#43478c")]
DAWN_SKY = [c(s) for s in ("#1f1a52", "#30226a", "#4a2b7a", "#6c3382", "#954084", "#c0527c", "#e4706e", "#f7985f")]
SKY_EDGES = [0, 20, 37, 53, 67, 81, 95, 110, 180]
SUN_GLOW = c("#ffcf86")


DAWN_STEPS = 6


def band_dawn(i, f):
    """How far dawn is in one band of the sky, in a few steps like a palette change. The horizon brightens first, the top last."""
    lead = (len(NIGHT_SKY) - 1 - i) * 3
    return round(smooth((f - 2 - lead) / 52) * DAWN_STEPS) / DAWN_STEPS


def sky_colors(f):
    return [mix(NIGHT_SKY[i], DAWN_SKY[i], band_dawn(i, f)) for i in range(len(NIGHT_SKY))]


def draw_sky(img, f, sun):
    cols = sky_colors(f)
    ys = np.arange(H)[:, None]
    xs = np.arange(W)[None, :]
    for i in range(len(cols)):
        img[SKY_EDGES[i]:SKY_EDGES[i + 1], :] = cols[i]
    # Two dithered rows above each band edge, so the bands melt into each other like hand-dithered pixel art
    for i in range(1, len(cols)):
        e = SKY_EDGES[i]
        row2 = ((xs[0] + e) % 4 == 0)
        row1 = ((xs[0] + e - 1) % 2 == 0)
        img[e - 2, row2] = cols[i]
        img[e - 1, row1] = cols[i]
        img[e, ((xs[0] + e) % 4 == 2)] = cols[i - 1]
    # The glow around the rising sun, in three dithered rings
    sx, sy, strength = sun
    if strength > 0:
        d = np.sqrt((xs - sx) ** 2 + ((ys - sy) * 1.15) ** 2)
        for radius, amount, pattern in ((46, 0.12, 2), (32, 0.22, 1), (21, 0.34, 0)):
            ring = d < radius
            edge = (d >= radius - 3) & ring
            # The outer 3 pixels of each ring are a checkerboard, so the rings have no hard edges
            checker = ((xs + ys) % 2 == 0)
            mask = ring & (~edge | checker)
            img[mask] = mix(img[mask], SUN_GLOW, amount * strength)


def draw_sun(img, sun):
    sx, sy, strength = sun
    if strength <= 0:
        return
    ys, xs = np.mgrid[0:H, 0:W]
    d = np.sqrt((xs - sx) ** 2 + (ys - sy) ** 2)
    img[d < 11.5] = c("#ffd57e")
    img[d < 10.2] = c("#ffe9a8")
    img[d < 7.5] = c("#fff6d6")
    # A soft highlight at its top-left, like on a glowing ball
    hl = ((xs - (sx - 3)) ** 2 + (ys - (sy - 3)) ** 2) < 5
    img[hl & (d < 10)] = c("#fffdf0")


def draw_moon(img, alpha):
    if alpha <= 0:
        return
    mx, my = 44, 30
    ys, xs = np.mgrid[0:H, 0:W]
    outer = ((xs - mx) ** 2 + (ys - my) ** 2) < 7.6 ** 2
    cut = ((xs - (mx + 3.4)) ** 2 + (ys - (my - 2.2)) ** 2) < 6.6 ** 2
    crescent = outer & ~cut
    shade = crescent & (((xs - (mx - 1)) ** 2 + (ys - (my + 1)) ** 2) < 4.2 ** 2) & ~(((xs - (mx + 1.5)) ** 2 + (ys - (my - 1)) ** 2) < 6.2 ** 2)
    visible = BAYER_SCREEN < alpha * 16
    img[crescent & visible] = c("#f4f0d6")
    img[shade & visible] = c("#d8d2b0")
    for px, py in ((mx - 5, my + 1), (mx - 3, my + 4)):
        if visible[py, px] and crescent[py, px]:
            img[py, px] = c("#c8c09c")


# Stars: position, brightness, twinkle phase and speed. The brightest ones are little crosses.
STARS = []
while len(STARS) < 86:
    x, y = int(RNG.integers(2, W - 2)), int(RNG.integers(2, 118))
    if 36 <= x <= 52 and 20 <= y <= 40:
        continue
    STARS.append((x, y, float(RNG.uniform(0.35, 1.0)), float(RNG.uniform(0, 6.28)), float(RNG.uniform(0.15, 0.4))))
STAR_WHITE = c("#ffffff")
STAR_BLUE = c("#c9d8ff")


def draw_stars(img, f, dawn):
    for x, y, b, phase, speed in STARS:
        tw = b * (0.7 + 0.3 * math.sin(phase + f * speed))
        # Dimmer stars fade out first while the sky gets brighter
        visibility = tw - dawn * 1.25
        if visibility <= 0.05:
            continue
        strength = min(1.0, visibility * 1.6)
        base = img[y, x].copy()
        col = STAR_WHITE if b > 0.7 else STAR_BLUE
        img[y, x] = mix(base, col, strength)
        if b > 0.93 and visibility > 0.35:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                img[y + dy, x + dx] = mix(img[y + dy, x + dx], col, strength * 0.45)


# ---------------------------------------------------------------- clouds

def make_cloud_layer(seed, base_y, radii, spacing):
    """Puffs along a 320 px period, each with a lit top-right and a shaded outline where it overlaps the one before it."""
    rng = np.random.default_rng(seed)
    puffs = []
    x = 0.0
    while x < W:
        r = float(rng.uniform(*radii))
        puffs.append((x, base_y - r * float(rng.uniform(0.25, 0.6)), r))
        x += spacing * float(rng.uniform(0.75, 1.25))
    return puffs, base_y


BACK_CLOUDS = make_cloud_layer(3, 152, (7, 14), 17)
FRONT_CLOUDS = make_cloud_layer(11, 166, (9, 18), 21)


def draw_clouds(img, layer, offset, shade, mid, light, top):
    puffs, base_y = layer
    ys, xs = np.mgrid[0:H, 0:W]
    img[base_y:, :] = mid
    for px, py, r in puffs:
        for wrap in (-W, 0, W):
            cx = (px + offset) % W + wrap
            if cx < -r - 2 or cx > W + r + 2:
                continue
            d = np.sqrt((xs - cx) ** 2 + (ys - py) ** 2)
            body = d < r
            img[body & (d >= r - 1.3) & (xs < cx + r * 0.2) & (ys > py - r * 0.6)] = shade
            img[body & (d < r - 1.3)] = mid
            # Lit from the sun on the right: a light crescent at the top-right of every puff
            lit = body & (d < r - 1.3) & ((xs - cx) * 0.6 - (ys - py) > r * 0.35)
            img[lit] = light
            rim = body & (d >= r - 2.3) & (d < r - 1.0) & ((ys - py) < -r * 0.45) & ((xs - cx) > -r * 0.2)
            img[rim] = top


# ---------------------------------------------------------------- the floating island

def build_island():
    """The island as RGBA at 1x: grass, dirt, a stone underside with strata, ores, roots and vines."""
    iw, ih = 128, 78
    rng = np.random.default_rng(21)
    col = np.zeros((ih, iw, 3))
    alpha = np.zeros((ih, iw), dtype=bool)
    emissive = np.zeros((ih, iw), dtype=bool)
    top = np.zeros(iw, dtype=int)
    bottom = np.zeros(iw, dtype=int)
    jag = 0
    for x in range(iw):
        u = (x - (iw - 1) / 2) / ((iw - 1) / 2)
        corner = max(0, int(round((abs(u) - 0.88) * 26)))
        top[x] = 5 + corner + (1 if (x * 7 + 3) % 11 == 0 else 0)
        if x % 3 == 0:
            jag = int(rng.integers(-2, 3))
        depth = (1 - abs(u) ** 1.5) * 44
        bottom[x] = min(62, int(14 + depth + jag + (2 if 40 < x < 88 and x % 9 < 3 else 0)))
    grass_hi, grass, grass_dk = c("#9be05a"), c("#62b23c"), c("#3f7f2e")
    dirt_hi, dirt, dirt_dk = c("#a8724a"), c("#86573a"), c("#62402b")
    stone = [c("#9a97b0"), c("#7c7894"), c("#615d7a"), c("#47435f"), c("#322f47")]
    for x in range(iw):
        u = (x - (iw - 1) / 2) / ((iw - 1) / 2)
        for y in range(top[x], bottom[x] + 1):
            alpha[y, x] = True
            d = y - top[x]
            if d == 0:
                col[y, x] = grass_hi
            elif d < 3:
                col[y, x] = grass if (x + y) % 5 else grass_hi
            elif d < 4:
                col[y, x] = grass_dk
            elif d < 9 + (x % 4 == 0):
                speck = ((x * 13 + y * 7) % 17 == 0)
                col[y, x] = dirt_hi if speck else (dirt_dk if d == 8 else dirt)
            else:
                # Stone: lit from the right, darker towards the tip and the left, with wobbly strata
                depth_t = (y - 9 - top[x]) / max(1, bottom[x] - top[x] - 9)
                light = 0.5 + 0.62 * u - depth_t * 0.5
                strata = ((y + int(2 * math.sin(x * 0.21))) % 7 == 0)
                level = 2.5 - light * 2.5 + (0.9 if strata else 0.0)
                if bottom[x] - y < 2:
                    level += 1.0
                col[y, x] = stone[int(min(4, max(0, round(level))))]
                # Moss creeps a bit down from the dirt
                if d < 12 and ((x * 31 + y * 17) % 7) < (12 - d) * 1.3:
                    col[y, x] = c("#5f7f4c") if (x + y) % 3 else c("#4a6640")
    # Pebble highlights in the stone
    for _ in range(60):
        x, y = int(rng.integers(8, iw - 8)), int(rng.integers(14, ih - 4))
        if alpha[y, x] and alpha[y, min(iw - 1, x + 1)] and y > top[x] + 10 and y < bottom[x] - 2:
            col[y, x] = stone[0] if x > iw * 0.45 else stone[1]
            col[y + 1, x] = stone[3]
    # Ores, like in Minecraft: diamonds and gold
    for ox, oy, kind in ((34, 22, "diamond"), (78, 30, "gold"), (58, 40, "diamond"), (96, 20, "gold"), (48, 27, "gold")):
        if not alpha[oy, ox]:
            continue
        light_c, dark_c = (c("#7ff3ea"), c("#26a39c")) if kind == "diamond" else (c("#ffe066"), c("#c48f17"))
        for dx, dy, which in ((0, 0, 0), (1, 0, 1), (0, 1, 1), (2, 1, 0), (1, 2, 1)):
            if alpha[oy + dy, ox + dx]:
                col[oy + dy, ox + dx] = light_c if which == 0 else dark_c
    # Grass that hangs over the edge
    for x in range(iw):
        if (x * 5 + 1) % 7 == 0 and top[x] + 4 < ih:
            for k in range(1 + (x % 3)):
                y = top[x] + 4 + k
                if y <= bottom[x]:
                    col[y, x] = grass_dk if k else grass
    # Cracks in the stone, each with a lit edge below it
    for cx0, cy0, steps in ((40, 19, ((1, 0), (1, 1), (1, 0), (1, 1), (0, 1))), (86, 24, ((1, 0), (1, -1), (1, 0), (1, 1), (1, 0))), (66, 33, ((1, 1), (0, 1), (1, 1)))):
        x, y = cx0, cy0
        for dx, dy in steps:
            if alpha[y, x] and alpha[y + 1, x]:
                col[y, x] = stone[4]
                col[y + 1, x] = stone[0]
            x, y = x + dx, y + dy
    # Vines that hang down from the grass at the sides, with a leaf here and there
    for rx, length in ((4, 7), (9, 11), (16, 6), (23, 4), (104, 5), (111, 9), (118, 12), (123, 6)):
        x = rx
        for k in range(length):
            y = top[rx] + 3 + k
            if y >= ih:
                break
            if k > 3 and k % 5 == 0:
                x += 1 if (rx % 2) else -1
            if not (0 <= x < iw):
                break
            alpha[y, x] = True
            col[y, x] = c("#3f8a3c") if k % 3 else c("#62b23c")
            if k % 4 == 2 and 0 <= x + (1 if k % 8 == 2 else -1) < iw:
                lx = x + (1 if k % 8 == 2 else -1)
                alpha[y, lx] = True
                col[y, lx] = c("#62b23c")
        if 0 < x < iw - 1 and top[rx] + 2 + length < ih:
            y = top[rx] + 2 + length
            alpha[y, x] = True
            col[y, x] = c("#9be05a")
    # A dark outline around the whole island, so it reads well against the sky
    outline = np.zeros_like(alpha)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        shifted = np.zeros_like(alpha)
        ys0, ye0 = max(0, dy), ih + min(0, dy)
        xs0, xe0 = max(0, dx), iw + min(0, dx)
        shifted[ys0:ye0, xs0:xe0] = alpha[ys0 - dy:ye0 - dy, xs0 - dx:xe0 - dx]
        outline |= shifted & ~alpha
    # Only below the grass line: on top, the grass itself is the edge
    for x in range(iw):
        outline[:top[x] + 2, x] = False
    col[outline] = c("#1d1a2e")
    alpha |= outline
    # Pixels facing right, which get a warm rim light from the sun
    rim = alpha & ~np.roll(alpha, -1, axis=1)
    rim[:, -1] = alpha[:, -1]
    rim &= ~outline
    return col, alpha, emissive, top, rim


ISLAND_COL, ISLAND_ALPHA, ISLAND_EMISSIVE, ISLAND_TOP, ISLAND_RIM = build_island()
ISLAND_X, ISLAND_Y = 96, 104


def build_props():
    """The cottage and the tree on top of the island, at 1x, positioned relative to the island."""
    pw, ph = 128, 40
    col = np.zeros((ph, pw, 3))
    alpha = np.zeros((ph, pw), dtype=bool)
    emissive = np.zeros((ph, pw), dtype=bool)
    base = ph  # the props stand on the island's grass, which is at island y 5, so prop y ph-1 is just above it

    def px(x, y, color, glow=False):
        if 0 <= x < pw and 0 <= y < ph:
            col[y, x] = c(color) if isinstance(color, str) else color
            alpha[y, x] = True
            emissive[y, x] = glow

    # ---- the cottage
    hx, wall_h, hw = 30, 9, 20
    ground = base - 1
    for y in range(ground - wall_h + 1, ground + 1):
        for x in range(hx, hx + hw):
            row = ground - y
            colr = "#b47e49" if (row // 2) % 2 == 0 else "#9c6a3d"
            if (x + row * 3) % 9 == 0 and row % 2 == 1:
                colr = "#86592f"
            px(x, y, colr)
        px(hx, y, "#5e3c22")
        px(hx + hw - 1, y, "#5e3c22")
    # The bottom beam
    for x in range(hx, hx + hw):
        px(x, ground, "#5e3c22")
    # Door with a knob
    for y in range(ground - 6, ground):
        for x in range(hx + 4, hx + 8):
            px(x, y, "#5a371f" if x > hx + 4 else "#4a2c18")
    px(hx + 7, ground - 3, "#e2b04a")
    # The lit window
    for y in range(ground - 6, ground - 2):
        for x in range(hx + 12, hx + 17):
            if y == ground - 6 or y == ground - 3 or x == hx + 12 or x == hx + 16:
                px(x, y, "#5e3c22")
            else:
                px(x, y, "#ffd56b" if x != hx + 14 else "#5e3c22", glow=(x != hx + 14))
    px(hx + 13, ground - 5, "#fff2b8", glow=True)
    # The roof: stepped, with tiles, lit on its right slope
    roof_base = ground - wall_h
    for step in range(10):
        y = roof_base - step
        x0, x1 = hx - 2 + step, hx + hw + 1 - step
        if x0 > x1:
            break
        for x in range(x0, x1 + 1):
            right = x > (hx + hw / 2 - 0.5)
            tile = (x + step) % 3 == 0
            if step == 0:
                colr = "#5c2020"
            elif x == x0 or x == x1:
                colr = "#5c2020"
            elif right:
                colr = "#c95a4b" if not tile else "#a8443b"
            else:
                colr = "#8a3232" if not tile else "#742828"
            px(x, y, colr)
    # The chimney, and its top
    for y in range(roof_base - 9, roof_base - 2):
        for x in range(hx + 3, hx + 6):
            px(x, y, "#7c7894" if x < hx + 5 else "#9a97b0")
        px(hx + 2, y, "#322f47")
        px(hx + 6, y, "#322f47")
    for x in range(hx + 2, hx + 7):
        px(x, roof_base - 10, "#322f47")

    # ---- the tree: a trunk and a canopy of round clusters, lit from the top-right
    tx = 92
    for y in range(base - 15, base):
        px(tx - 1, y, "#4a2e1c")
        px(tx, y, "#6b4428")
        px(tx + 1, y, "#8f5c35")
    px(tx - 2, base - 1, "#4a2e1c")
    px(tx + 2, base - 1, "#6b4428")
    px(tx + 2, base - 9, "#6b4428")
    px(tx + 3, base - 10, "#6b4428")
    clusters = [(0, -22, 9.5), (-9, -18, 7.5), (9, -18, 7.5), (-5, -27, 6.5), (5, -27.5, 6.5), (0, -14, 6.0)]
    leaf = {"dark": c("#2c6532"), "mid": c("#3e8a3b"), "light": c("#5daf45"), "hi": c("#93d75b"), "edge": c("#1f4a28")}
    rng = np.random.default_rng(5)
    canopy = np.zeros((ph, pw), dtype=bool)
    tone = np.zeros((ph, pw), dtype=int)
    for y in range(ph):
        for x in range(pw):
            best = None
            for cx, cy, r in clusters:
                dx, dy = x - (tx + cx), y - (base + cy)
                d = math.hypot(dx, dy)
                if d < r:
                    # The cluster in front is the one whose center is lowest
                    if best is None or (base + cy) > best[1]:
                        best = (cx, base + cy, r, dx, dy, d)
            if best is None:
                continue
            cx, cy, r, dx, dy, d = best
            canopy[y, x] = True
            lightv = (dx * 0.55 - dy * 0.85) / r
            t = 1 if lightv < -0.35 else (2 if lightv < 0.25 else (3 if lightv < 0.62 else 4))
            if d > r - 1.2 and lightv < 0.0:
                t = 0
            tone[y, x] = t
    for y in range(ph):
        for x in range(pw):
            if not canopy[y, x]:
                continue
            t = tone[y, x]
            # Leaf texture: little darker clusters, so it doesn't look like flat circles
            if t >= 2 and rng.random() < 0.16:
                t -= 1
            colr = [leaf["edge"], leaf["dark"], leaf["mid"], leaf["light"], leaf["hi"]][t]
            px(x, y, colr)
    # A few apples
    for ax, ay in ((tx - 6, base - 17), (tx + 5, base - 21), (tx - 1, base - 26)):
        px(ax, ay, "#d8413a")
        px(ax, ay - 1, "#f07a5e")
    return col, alpha, emissive, (hx + 4, roof_base - 11)


PROPS_COL, PROPS_ALPHA, PROPS_EMISSIVE, CHIMNEY = build_props()
PROPS_H = PROPS_COL.shape[0]
TREE_LEAVES = [(x, y) for y in range(PROPS_H) for x in range(PROPS_COL.shape[1]) if PROPS_ALPHA[y, x] and tuple(PROPS_COL[y, x].astype(int)) == (147, 215, 91)]

NIGHT_TINT = np.array([0.42, 0.46, 0.78])
DAWN_TINT = np.array([1.0, 0.93, 0.88])
RIM_LIGHT = c("#ffb46e")


def paste(img, col, alpha, x0, y0, tint=None, emissive=None, rim=None, rim_amount=0.0):
    # Clip to the screen: the bottom of the island can reach below it
    h = min(alpha.shape[0], H - y0)
    w = min(alpha.shape[1], W - x0)
    col, alpha = col[:h, :w], alpha[:h, :w]
    if emissive is not None:
        emissive = emissive[:h, :w]
    if rim is not None:
        rim = rim[:h, :w]
    region = img[y0:y0 + h, x0:x0 + w]
    src = col.copy()
    if tint is not None:
        src = src * tint
        if emissive is not None:
            src[emissive] = col[emissive]
    if rim is not None and rim_amount > 0:
        src[rim] = mix(src[rim], RIM_LIGHT, rim_amount)
    region[alpha] = src[alpha]


def draw_waterfall(img, f, x0, y_top, y_end):
    deep, mid, light, foam = c("#3f86d6"), c("#6fb3f5"), c("#bfe6ff"), c("#ffffff")
    # The lip where the water leaves the grass
    for dx, colr in ((0, mid), (1, light), (2, mid)):
        img[y_top, x0 - dx] = colr
    for y in range(y_top + 1, y_end):
        for k in range(4):
            x = x0 - 2 - k + (1 if y > y_top + 3 else 0)
            v = (y - f * 2 + k * 5) % 9
            colr = light if v < 2 else (mid if v < 6 else deep)
            if k == 0 or k == 3:
                colr = deep if v > 1 else mid
            img[y, x] = colr


YS, XS = np.mgrid[0:H, 0:W]


def draw_smoke(img, f, chimney_x, chimney_y, dawn):
    # A puff leaves the chimney every 12 frames, rises, drifts right with the wind, grows and dissolves at the end
    light = mix(c("#b8b5d8"), c("#ffe4d4"), dawn)
    shade = mix(c("#8683ad"), c("#dca3a4"), dawn)
    for birth in range(-120, FRAMES, 12):
        age = f - birth
        if age < 0 or age > 66:
            continue
        t = age / 66
        x = chimney_x + 0.5 + age * 0.21 + math.sin(age * 0.15 + birth * 0.7) * 0.7
        y = chimney_y - age * 0.36
        r = 1.3 + t * 2.7
        d = np.sqrt((XS - x) ** 2 + (YS - y) ** 2)
        body = d < r
        if t > 0.5:
            body &= BAYER_SCREEN >= (t - 0.5) / 0.5 * 16
        img[body] = shade
        # Lit at its top-right, like the clouds
        img[body & (((XS - x) - (YS - y)) > r * 0.15)] = light


BIRD_UP = ["X...X", ".X.X.", "..X.."]
BIRD_DOWN = [".....", "XX.XX", "..X.."]


def draw_birds(img, f, dawn):
    col = mix(c("#181230"), c("#3a2140"), dawn)
    for i, (start, y0, speed) in enumerate(((26, 72, 1.5), (31, 66, 1.55), (35, 77, 1.45))):
        if f < start:
            continue
        x = int(round(W + 8 - (f - start) * speed * 1.6))
        y = int(round(y0 + 2.2 * math.sin((f + i * 7) * 0.2)))
        sprite = BIRD_UP if ((f + i * 2) // 3) % 2 == 0 else BIRD_DOWN
        for sy, row in enumerate(sprite):
            for sx, ch in enumerate(row):
                if ch == "X" and 0 <= x + sx < W and 0 <= y + sy < H:
                    img[y + sy, x + sx] = col


def draw_distant_island(img, f, x0, y0, w, dawn, seed, trees):
    """A small island far away: the same materials as the big one, but faded into the sky color."""
    rng = np.random.default_rng(seed)
    sky = sky_colors(f)
    band = next(i for i in range(len(sky)) if SKY_EDGES[i + 1] > y0)
    tint = mix(NIGHT_TINT, DAWN_TINT, dawn)

    def hazy(color, amount=0.5):
        return mix(c(color) * tint, sky[band], amount)

    grass_l, grass_d, dirt = hazy("#8fd35a"), hazy("#4f9a38"), hazy("#86573a")
    stone_l, stone_m, stone_d = hazy("#8c88a3"), hazy("#5f5b78"), hazy("#3e3a56")
    edge = hazy("#1d1a2e", 0.4)
    for x in range(w):
        u = (x - (w - 1) / 2) / ((w - 1) / 2)
        top = y0 + (1 if abs(u) > 0.84 else 0)
        bottom = y0 + 4 + int((1 - abs(u) ** 1.4) * w * 0.4) + int(rng.integers(0, 2))
        for y in range(top, bottom + 1):
            d = y - top
            if y == bottom:
                colr = edge
            elif d == 0:
                colr = grass_l
            elif d == 1:
                colr = grass_d
            elif d == 2:
                colr = dirt
            else:
                colr = stone_l if (u > 0.2 + (d - 3) * 0.12) else (stone_m if (u > -0.4 and (y - top) < (bottom - top) * 0.7) else stone_d)
            img[y, x0 + x] = colr
    # Tiny round trees: a trunk and a lit canopy
    canopy = [" XX ", "XXXX", "XXXX", " XX "]
    for tx in trees:
        x = x0 + tx
        img[y0 - 1, x + 1] = hazy("#6b4428")
        img[y0 - 2, x + 1] = hazy("#6b4428")
        for ry, row in enumerate(canopy):
            for rx, ch in enumerate(row):
                if ch == "X":
                    lit = (rx >= 2 and ry <= 1)
                    img[y0 - 6 + ry, x + rx] = grass_l if lit else grass_d


# ---------------------------------------------------------------- logo and subtitle

LOGO_GLYPHS = {
    "S": [".XXXXX.", "XXXXXXX", "XX...XX", "XX.....", "XXXXXX.", ".XXXXXX", ".....XX", "XX...XX", "XXXXXXX", ".XXXXX."],
    "K": ["XX...XX", "XX..XXX", "XX.XXX.", "XXXXX..", "XXXX...", "XXXX...", "XXXXX..", "XX.XXX.", "XX..XXX", "XX...XX"],
    "Y": ["XX...XX", "XX...XX", "XX...XX", "XXX.XXX", ".XXXXX.", "..XXX..", "..XXX..", "..XXX..", "..XXX..", "..XXX.."],
    "H": ["XX...XX", "XX...XX", "XX...XX", "XX...XX", "XXXXXXX", "XXXXXXX", "XX...XX", "XX...XX", "XX...XX", "XX...XX"],
    "A": ["..XXX..", ".XXXXX.", "XXX.XXX", "XX...XX", "XX...XX", "XXXXXXX", "XXXXXXX", "XX...XX", "XX...XX", "XX...XX"],
    "V": ["XX...XX", "XX...XX", "XX...XX", "XX...XX", "XX...XX", "XXX.XXX", ".XX.XX.", ".XXXXX.", "..XXX..", "...X..."],
    "E": ["XXXXXXX", "XXXXXXX", "XX.....", "XX.....", "XXXXXX.", "XXXXXX.", "XX.....", "XX.....", "XXXXXXX", "XXXXXXX"],
    "N": ["XX...XX", "XXX..XX", "XXXX.XX", "XXXXXXX", "XX.XXXX", "XX..XXX", "XX...XX", "XX...XX", "XX...XX", "XX...XX"],
}
LOGO_TEXT = "SKYHAVEN"
LOGO_Y = 25
LOGO_GAP = 3
FACE = [c("#fff4bd")] * 2 + [c("#ffdc62")] * 6 + [c("#ffc13f")] * 6 + [c("#f29a2c")] * 6
EXTRUDE = [c("#c46c1f"), c("#a3561a"), c("#844316")]
LOGO_OUTLINE = c("#2b1208")


def glyph_mask(ch):
    rows = LOGO_GLYPHS[ch]
    m = np.array([[cc == "X" for cc in row] for row in rows])
    return np.kron(m, np.ones((2, 2), dtype=bool))


LOGO_MASKS = [glyph_mask(ch) for ch in LOGO_TEXT]
LOGO_W = sum(m.shape[1] for m in LOGO_MASKS) + LOGO_GAP * (len(LOGO_MASKS) - 1)
LOGO_X = (W - LOGO_W) // 2
LETTER_X = []
_x = LOGO_X
for _m in LOGO_MASKS:
    LETTER_X.append(_x)
    _x += _m.shape[1] + LOGO_GAP


def letter_offset(i, f):
    """The letters fall in one after another, land and bounce a little. None while a letter is still above the screen."""
    land = 46 + i * 3
    if f < land - 8:
        return None
    if f <= land:
        t = (land - f) / 8
        return -int(round(72 * t * t))
    return {1: -3, 2: -4, 3: -3, 4: -1}.get(f - land, 0)


def draw_logo(img, f):
    shine = None
    if 82 <= f <= 94:
        shine = -24 + (f - 82) * 15
    # The whole logo goes into one layer first, so the outline wraps around the extrusion of every letter
    layer = np.zeros((H, W, 3))
    mask = np.zeros((H, W), dtype=bool)
    for i, m in enumerate(LOGO_MASKS):
        off = letter_offset(i, f)
        if off is None:
            continue
        x0, y0 = LETTER_X[i], LOGO_Y + off
        gh, gw = m.shape
        for depth in (3, 2, 1):
            for gy in range(gh):
                y = y0 + gy + depth
                if not (0 <= y < H):
                    continue
                for gx in range(gw):
                    if m[gy, gx]:
                        layer[y, x0 + gx] = EXTRUDE[depth - 1]
                        mask[y, x0 + gx] = True
        for gy in range(gh):
            y = y0 + gy
            if not (0 <= y < H):
                continue
            for gx in range(gw):
                if not m[gy, gx]:
                    continue
                colr = FACE[gy]
                # A bright inner edge at the top and left of every stroke
                if (gy == 0 or not m[gy - 1, gx]) or (gx == 0 or not m[gy, gx - 1]):
                    colr = mix(colr, c("#fffbe8"), 0.55)
                if shine is not None:
                    band = (x0 + gx) - LOGO_X + gy * 0.6 - shine
                    if 0 <= band < 3:
                        colr = c("#fffdf2")
                    elif 3 <= band < 5:
                        colr = mix(colr, c("#fffdf2"), 0.55)
                layer[y, x0 + gx] = colr
                mask[y, x0 + gx] = True
    outline = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            outline |= np.roll(np.roll(mask, dy, axis=0), dx, axis=1)
    outline &= ~mask
    img[outline] = LOGO_OUTLINE
    img[mask] = layer[mask]


SMALL_FONT = {
    "A": [".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
    "S": [".XXXX", "X....", "X....", ".XXX.", "....X", "....X", "XXXX."],
    "K": ["X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X"],
    "Y": ["X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X.."],
    "B": ["XXXX.", "X...X", "X...X", "XXXX.", "X...X", "X...X", "XXXX."],
    "L": ["X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"],
    "O": [".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."],
    "C": [".XXXX", "X....", "X....", "X....", "X....", "X....", ".XXXX"],
    "D": ["XXXX.", "X...X", "X...X", "X...X", "X...X", "X...X", "XXXX."],
    "V": ["X...X", "X...X", "X...X", "X...X", ".X.X.", ".X.X.", "..X.."],
    "E": ["XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX"],
    "N": ["X...X", "XX..X", "X.X.X", "X.X.X", "X..XX", "X...X", "X...X"],
    "T": ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
    "U": ["X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."],
    "R": ["XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X"],
}
SUBTITLE = "A SKYBLOCK ADVENTURE"
SUBTITLE_Y = 57


def draw_subtitle(img, f):
    if f < 74:
        return
    level = min(16, (f - 74) * 2 + 1)
    width = sum(4 if ch == " " else 6 for ch in SUBTITLE) - 1
    x = (W - width) // 2
    mask = np.zeros((H, W), dtype=bool)
    for ch in SUBTITLE:
        if ch == " ":
            x += 4
            continue
        for gy, row in enumerate(SMALL_FONT[ch]):
            for gx, cc in enumerate(row):
                if cc == "X":
                    mask[SUBTITLE_Y + gy, x + gx] = True
        x += 6
    outline = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            outline |= np.roll(np.roll(mask, dy, axis=0), dx, axis=1)
    # The shadow goes one more pixel down, like the logo's extrusion
    outline |= np.roll(mask, 2, axis=0)
    outline &= ~mask
    visible = BAYER_SCREEN < level
    img[outline & visible] = c("#2a1236")
    img[mask & visible] = c("#fff1d6")


SPARKLES = [(LOGO_X - 6, LOGO_Y + 2, 70), (LOGO_X + LOGO_W + 4, LOGO_Y + 15, 76), (LOGO_X + 40, LOGO_Y - 4, 82),
            (LOGO_X + LOGO_W - 30, LOGO_Y + 25, 88), (LOGO_X + 10, LOGO_Y + 24, 93), (LOGO_X + LOGO_W - 6, LOGO_Y - 3, 98)]
SPARKLE_SIZES = [0, 1, 2, 3, 2, 1, 0]


def draw_sparkles(img, f):
    for sx, sy, start in SPARKLES:
        age = f - start
        if age < 0 or age >= len(SPARKLE_SIZES):
            continue
        s = SPARKLE_SIZES[age]
        img[sy, sx] = c("#ffffff")
        for k in range(1, s + 1):
            colr = c("#fff3b0") if k < s else c("#ffd36b")
            for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
                img[sy + dy, sx + dx] = colr
        if s >= 2:
            for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
                img[sy + dy, sx + dx] = c("#fff3b0")


# ---------------------------------------------------------------- frames

def render(f, title=True):
    img = np.zeros((H, W, 3))
    # Night turns into dawn in a few steps, like a palette change, so whole-screen changes stay rare
    dawn = round(smooth((f - 6) / 64) * DAWN_STEPS) / DAWN_STEPS
    sun = (252, 166 - 44 * smooth((f - 10) / 80), smooth((f - 8) / 40))
    draw_sky(img, f, sun)
    draw_stars(img, f, dawn)
    draw_moon(img, 1 - smooth((dawn - 0.25) / 0.45))
    draw_sun(img, sun)
    bob_far = int(round(1.0 * math.sin(f * 0.09 + 1.3)))
    draw_distant_island(img, f, 30, 92 + bob_far, 28, dawn, 2, (6, 17))
    draw_distant_island(img, f, 268, 79 - bob_far, 18, dawn, 4, (9,))
    draw_clouds(img, BACK_CLOUDS, -f * 0.11,
                mix(c("#202052"), c("#93496f"), dawn), mix(c("#2a2c60"), c("#c86677"), dawn),
                mix(c("#363b74"), c("#ee9580"), dawn), mix(c("#40467f"), c("#ffbe98"), dawn))
    draw_birds(img, f, dawn)
    bob = int(round(1.2 * math.sin(f * 0.075)))
    iy = ISLAND_Y + bob
    tint = mix(NIGHT_TINT, DAWN_TINT, dawn)
    draw_waterfall(img, f, ISLAND_X + 9, iy + ISLAND_TOP[9] + 1, 179)
    paste(img, ISLAND_COL, ISLAND_ALPHA, ISLAND_X, iy, tint=tint, rim=ISLAND_RIM, rim_amount=0.55 * dawn)
    paste(img, PROPS_COL, PROPS_ALPHA, ISLAND_X, iy + 5 - PROPS_H, tint=tint, emissive=PROPS_EMISSIVE)
    # Leaves catch the light one after another
    for k, (lx, ly) in enumerate(TREE_LEAVES):
        if (k * 7 + f // 2) % 23 == 0:
            img[iy + 5 - PROPS_H + ly, ISLAND_X + lx] = mix(c("#d4f59a"), c("#93d75b"), 1 - dawn) * mix(NIGHT_TINT, np.ones(3), dawn)
    draw_smoke(img, f, ISLAND_X + CHIMNEY[0], iy + 5 - PROPS_H + CHIMNEY[1], dawn)
    draw_clouds(img, FRONT_CLOUDS, -f * 0.22,
                mix(c("#2a2b5c"), c("#b35f7a"), dawn), mix(c("#383d75"), c("#ec998a"), dawn),
                mix(c("#4b528d"), c("#ffcfab"), dawn), mix(c("#5c64a0"), c("#ffe6c8"), dawn))
    if title:
        draw_logo(img, f)
        draw_subtitle(img, f)
        draw_sparkles(img, f)
    # Fade in from black in four steps
    if f < 8:
        img *= (f // 2 + 1) / 5
    return np.clip(np.round(img), 0, 255).astype(np.uint8)


def main():
    out = sys.argv[1]
    preview = "--preview" in sys.argv
    os.makedirs(out, exist_ok=True)
    frames = []
    for f in range(FRAMES):
        small = Image.fromarray(render(f), "RGB")
        frames.append(small.resize((W * SCALE, H * SCALE), Image.NEAREST))
        if preview and f % 6 == 0:
            small.resize((W * 2, H * 2), Image.NEAREST).save(os.path.join(out, f"preview_{f:03d}.png"))
    menu = Image.fromarray(render(FRAMES - 1, title=False), "RGB").resize((W * SCALE, H * SCALE), Image.NEAREST)
    menu.save(os.path.join(out, "skyhaven_menu.png"), optimize=True)
    path = os.path.join(out, "skyhaven_intro.apng")
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=FRAME_MS, loop=1, optimize=True)
    print(path, os.path.getsize(path))
    if preview:
        cols, picks = 4, list(range(0, FRAMES, 6))
        tw, th = W * 2, H * 2
        rows = (len(picks) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * tw, rows * th))
        for i, f in enumerate(picks):
            sheet.paste(Image.open(os.path.join(out, f"preview_{f:03d}.png")), ((i % cols) * tw, (i // cols) * th))
        sheet.save(os.path.join(out, "preview_sheet.png"))


if __name__ == "__main__":
    main()
