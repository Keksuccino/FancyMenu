"""Wildwood dusk scene: pixel art layers at 320x180 art pixels. The parallax layers are wider by MARGIN on both sides."""
import random
import numpy as np
from PIL import Image
from mountains import draw_mountain

W, H = 320, 180
MARGIN = 12
LW = W + 2 * MARGIN

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 + 1 / 32.0


def hexc(h, a=255):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


SKY = [hexc(c) for c in [
    '#120b26', '#170e2f', '#1e1238', '#271644', '#331a50', '#43205c', '#562666',
    '#6c2d6d', '#843571', '#9c3f72', '#b44c71', '#cb5c6d', '#de6f67', '#ec8562',
    '#f69d61', '#fbb567', '#fdcb76', '#ffdc8c']]
SUN = (244, 112)


def sharpen(f, k=2.6):
    return np.clip((f - 0.5) * k + 0.5, 0.0, 1.0)


def dither_index(v, x, y, k=2.6):
    base = np.floor(v)
    f = sharpen(v - base, k)
    return (base + (f > BAYER4[y % 4, x % 4])).astype(int)


def new_layer(w=W, h=H):
    return np.zeros((h, w, 4), np.uint8)


def paint(img, x, y, c):
    if 0 <= x < img.shape[1] and 0 <= y < img.shape[0]:
        img[y, x] = c


# ---------------------------------------------------------------- sky

def sky_values():
    yy, xx = np.mgrid[0:H, 0:W]
    t = np.clip(yy / 146.0, 0, 1)
    v = (t ** 1.3) * (len(SKY) - 3)
    sx, sy = SUN
    d = np.sqrt(((xx - sx) * 0.75) ** 2 + ((yy - sy) * 1.3) ** 2)
    v = v + np.clip(1.0 - d / 125.0, 0, 1) ** 2 * 3.4
    return np.clip(v, 0, len(SKY) - 1.001), xx, yy


def draw_sky(img):
    v, xx, yy = sky_values()
    pal = np.array(SKY, np.uint8)
    img[:] = pal[np.clip(dither_index(v, xx, yy), 0, len(SKY) - 1)]


def draw_sun(img):
    sx, sy = SUN
    r = 19
    for rad, c in [(r + 3, '#f9b26c'), (r, '#ffcf82'), (r - 3, '#ffe09a'), (r - 7, '#ffecb8'), (r - 11, '#fff6d8')]:
        for y in range(sy - rad, sy + rad + 1):
            for x in range(sx - rad, sx + rad + 1):
                if (x - sx) ** 2 + (y - sy) ** 2 <= rad * rad + rad * 0.7:
                    # The outer halo ring is dithered
                    if rad == r + 3 and (x + y) % 2:
                        continue
                    paint(img, x, y, hexc(c))
    # Haze lines across the lower half of the sun
    for ly, c, gap in [(sy + 5, '#ffc77e', 0), (sy + 9, '#f8ad6c', 0), (sy + 12, '#f29a66', 0), (sy + 15, '#ec8a62', 1)]:
        for x in range(sx - r - 3, sx + r + 4):
            if (x - sx) ** 2 + (ly - sy) ** 2 <= (r + 3) ** 2:
                paint(img, x, ly, hexc(c))
                if gap:
                    paint(img, x, ly + 1, hexc(c))


def draw_stars(img, rng):
    sky_dark = {tuple(c) for c in SKY[:9]}
    dims = [hexc('#5d4a8a'), hexc('#7d68aa'), hexc('#a895d0')]
    for _ in range(170):
        x = rng.randrange(0, W)
        y = int(rng.random() ** 1.5 * 80)
        if x > 180 and y > 34 and rng.random() < 0.85:
            continue
        if tuple(img[y, x]) not in sky_dark:
            continue
        img[y, x] = dims[min(2, int(rng.random() * (1.2 - y / 90) * 3))]
    bright = [(22, 14, 2), (61, 31, 1), (103, 9, 1), (142, 21, 2), (37, 54, 1), (186, 11, 1),
              (8, 40, 1), (126, 46, 1), (214, 26, 2), (80, 64, 1), (262, 14, 1), (300, 30, 1), (166, 58, 1)]
    for x, y, size in bright:
        img[y, x] = hexc('#fff6e0')
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            paint(img, x + dx, y + dy, hexc('#c9b8ec') if size == 2 else hexc('#8a76b8'))
        if size == 2:
            for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
                paint(img, x + dx, y + dy, hexc('#6a5698'))


def blob_mask(shape, ellipses):
    h, w = shape
    yy, xx = np.mgrid[0:h, 0:w]
    m = np.zeros((h, w), bool)
    for cx, cy, rx, ry in ellipses:
        m |= ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= 1.0
    return m


def draw_clouds(img):
    clouds = [
        [(58, 84, 30, 3.0), (78, 81, 15, 3.4), (42, 86, 18, 2.2), (90, 85, 12, 1.8)],
        [(148, 70, 22, 2.4), (162, 68, 10, 2.6)],
        [(206, 92, 28, 2.4), (224, 89, 14, 3.0), (190, 94, 14, 1.8)],
        [(288, 76, 22, 2.4), (300, 74, 11, 2.4)],
        [(110, 98, 20, 1.8), (121, 96, 9, 1.9)],
        [(268, 98, 16, 1.6)],
    ]
    for ells in clouds:
        m = blob_mask((H, W), ells)
        ys, xs = np.nonzero(m)
        for y, x in zip(ys, xs):
            above = not m[y - 1, x]
            above2 = not m[y - 2, x]
            below = not m[y + 1, x]
            near = max(0.0, 1.0 - abs(x - SUN[0]) / 90.0)
            if above:
                c = '#ffd49a' if near > 0.35 else '#f4a08a'
            elif above2 and near > 0.35:
                c = '#f2a37c'
            elif below:
                c = '#8a3a70' if near < 0.35 else '#b2506e'
            else:
                c = '#c8667a' if near > 0.35 else '#a54d79'
            img[y, x] = hexc(c)


FAR = dict(lit=hexc('#a5527a'), shadow=hexc('#7d3b72'), edge=hexc('#d47b82'), snow=hexc('#f7d6cc'), snow_shadow=hexc('#c697b2'))
MID = dict(lit=hexc('#78376a'), shadow=hexc('#5b2a61'), edge=hexc('#a14a6f'), snow=hexc('#e6b2b8'), snow_shadow=hexc('#a77a9c'))


def draw_ranges(img):
    for px, py, lp, rp, snow in [
        (34, 98, (2, 1, 2, 3, 1, 2), (1, 2, 1, 1, 2, 1), 4),
        (94, 108, (2, 2, 1, 3), (2, 1, 2, 2), 3),
        (152, 112, (3, 2, 2, 3), (2, 2, 3, 2), 0),
        (220, 100, (2, 1, 3, 2, 1), (1, 2, 1, 2, 1), 4),
        (302, 96, (2, 1, 2, 2, 1), (2, 1, 2, 3), 4),
    ]:
        draw_mountain(img, px, py, lp, rp, FAR, H, snow, lit_right=px < SUN[0])
    for px, py, lp, rp in [
        (2, 118, (2, 3, 2), (2, 2, 3, 2)),
        (66, 123, (3, 2, 3, 2), (2, 3, 2, 3)),
        (128, 127, (3, 3, 2, 3), (3, 2, 3, 3)),
        (188, 123, (2, 3, 3, 2), (3, 2, 3)),
        (262, 117, (2, 2, 3, 2), (2, 3, 2, 2)),
        (322, 123, (3, 2, 3), (2, 3, 3)),
    ]:
        draw_mountain(img, px, py, lp, rp, MID, H, 0, lit_right=px < SUN[0], edge_len=12)


def draw_mist(img):
    """A band of dithered haze over the valley floor, so the ranges fade into the distance."""
    mist = np.array(hexc('#b35d79'), np.float32)
    for y in range(128, 150):
        strength = max(0.0, 1.0 - abs(y - 146) / 18.0) * 0.75
        for x in range(W):
            if BAYER4[y % 4, x % 4] < strength * 0.6:
                c = img[y, x].astype(np.float32)
                img[y, x] = (c * 0.45 + mist * 0.55).astype(np.uint8)


def make_sky_layer(seed=3):
    rng = random.Random(seed)
    img = new_layer()
    draw_sky(img)
    draw_stars(img, rng)
    draw_sun(img)
    draw_clouds(img)
    draw_ranges(img)
    draw_mist(img)
    return img


def save(img, path, scale=4):
    im = Image.fromarray(img, 'RGBA')
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    im.save(path)
    return im


if __name__ == '__main__':
    import sys
    save(make_sky_layer(), f'{sys.argv[1]}/bg_preview.png', 4)
