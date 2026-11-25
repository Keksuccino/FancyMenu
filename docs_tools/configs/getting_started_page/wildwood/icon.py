"""The 32x32 art pixel icon of the Wildwood profile: the valley at dusk in small, with the sun behind two peaks, the lake and two pines."""
import numpy as np
from scene import SKY, hexc, dither_index, paint
from layers import pine_mask

S = 32


def peak(x, px, py, slope_left, slope_right):
    """Height of a straight-sloped peak at column x."""
    return py + ((px - x) * slope_left if x < px else (x - px) * slope_right)


def make_icon():
    img = np.zeros((S, S, 4), np.uint8)
    pal = np.array(SKY, np.uint8)
    yy, xx = np.mgrid[0:S, 0:S]
    sun_x, sun_y = 19, 18
    # Sky: dark at the top, glowing toward the horizon and around the sun
    v = (np.clip(yy / 21.0, 0, 1) ** 1.25) * (len(SKY) - 4)
    d = np.sqrt((xx - sun_x) ** 2 + ((yy - sun_y) * 1.3) ** 2)
    v = v + np.clip(1.0 - d / 13.0, 0, 1) ** 2 * 3.2
    img[:] = pal[np.clip(dither_index(np.clip(v, 0, len(SKY) - 1.001), xx, yy), 0, len(SKY) - 1)]
    for x, y in ((3, 3), (10, 6), (25, 3), (14, 2), (30, 8)):
        paint(img, x, y, hexc('#e8dcff'))
    # Sun, behind the peaks
    for y in range(sun_y - 4, sun_y + 5):
        for x in range(sun_x - 4, sun_x + 5):
            r2 = (x - sun_x) ** 2 + (y - sun_y) ** 2
            if r2 <= 18:
                paint(img, x, y, hexc('#fff3cc') if r2 <= 5 else hexc('#ffd88c'))
    # Far peak right of the sun (darker), then the big snowy peak on the left in front
    for x in range(S):
        top = int(round(peak(x, 27, 15, 1.0, 1.1)))
        for y in range(max(0, top), 23):
            img[y, x] = hexc('#8a4274') if x <= 27 else hexc('#6b3268')
    for x in range(S):
        top = int(round(peak(x, 10, 10, 0.9, 1.0)))
        for y in range(max(0, top), 23):
            lit = (x - 10) >= -(y - 10) * 0.35
            img[y, x] = hexc('#a5527a') if lit else hexc('#7d3b72')
        # Rim light on the slope that faces the sun
        if x > 10 and 0 <= top < 23:
            img[top, x] = hexc('#d47b82')
    for x, y, lit in ((10, 10, True), (9, 11, False), (10, 11, True), (11, 11, True), (8, 12, False), (9, 12, False), (11, 12, True), (12, 12, True)):
        paint(img, x, y, hexc('#f7d6cc') if lit else hexc('#c697b2'))
    # Lake, with a bright shore line and the sun's reflection
    for y in range(23, 27):
        for x in range(S):
            img[y, x] = hexc('#8f3f6c') if y > 23 else hexc('#c45a6c')
    for i, y in enumerate(range(23, 27)):
        half = 4 - i
        for x in range(sun_x - half, sun_x + half + 1):
            if (x + i) % 3 != 2 or y == 23:
                paint(img, x, y, hexc('#ffe7a8') if i < 2 else hexc('#f6a866'))
    # Ground
    for y in range(27, S):
        for x in range(S):
            img[y, x] = hexc('#150a20')
    for x in (1, 7, 13, 22, 26):
        paint(img, x, 26, hexc('#150a20'))
    # Two pines framing the icon, lit from the sun on their inner side
    for cx, h, seed, lit_right in ((4, 23, 3, True), (28, 19, 6, False)):
        m, mcx = pine_mask(h, seed)
        mh, mw = m.shape
        for y in range(mh):
            for x in range(mw):
                if not m[y, x]:
                    continue
                nx = x + (1 if lit_right else -1)
                rim = (nx < 0) or (nx >= mw) or not m[y, nx]
                paint(img, cx - mcx + x, 28 - h + y, hexc('#4a2458') if (rim and y > 1) else hexc('#150a20'))
    # The warm window of the cabin on the far shore
    paint(img, 9, 22, hexc('#ffe39a'))
    return img
