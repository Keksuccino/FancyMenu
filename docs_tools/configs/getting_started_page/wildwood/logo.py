"""The WILDWOOD logo: golden chunky letters with depth, a pine crown and a ribbon, 130x50 art pixels."""
import numpy as np
from scene import hexc, paint
from layers import pine_mask

LOGO_W, LOGO_H = 130, 50

GLYPHS = {
    'W': [
        "###...........###",
        "###...........###",
        "###...........###",
        "###...........###",
        "###....###....###",
        "###....###....###",
        "###...#####...###",
        "###...#####...###",
        "###..###.###..###",
        "###..###.###..###",
        "###.###...###.###",
        "#######...#######",
        "######.....######",
        "#####.......#####",
        "####.........####",
    ],
    'I': [
        "#######",
        "#######",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "..###..",
        "#######",
        "#######",
    ],
    'L': [
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "###.........",
        "############",
        "############",
        "############",
    ],
    'D': [
        "##########....",
        "###########...",
        "###.....####..",
        "###......####.",
        "###.......###.",
        "###.......####",
        "###........###",
        "###........###",
        "###........###",
        "###.......####",
        "###.......###.",
        "###......####.",
        "###.....####..",
        "###########...",
        "##########....",
    ],
    'O': [
        "....######....",
        "..##########..",
        ".####....####.",
        ".###......###.",
        "####......####",
        "###........###",
        "###........###",
        "###........###",
        "###........###",
        "###........###",
        "####......####",
        ".###......###.",
        ".####....####.",
        "..##########..",
        "....######....",
    ],
}

TINY = {
    'A': [".#.", "#.#", "###", "#.#", "#.#"],
    'C': [".##", "#..", "#..", "#..", ".##"],
    'O': [".#.", "#.#", "#.#", "#.#", ".#."],
    'Z': ["###", "..#", ".#.", "#..", "###"],
    'Y': ["#.#", "#.#", ".#.", ".#.", ".#."],
    'M': ["#...#", "##.##", "#.#.#", "#...#", "#...#"],
    'D': ["##.", "#.#", "#.#", "#.#", "##."],
    'P': ["##.", "#.#", "##.", "#..", "#.."],
    'K': ["#.#", "#.#", "##.", "#.#", "#.#"],
    ' ': ["..", "..", "..", "..", ".."],
}

OUTLINE = hexc('#1a0d1c')
FACE_ROWS = ['#fff1bd', '#ffe6a0', '#ffd77a', '#ffca64', '#fbbb52', '#f6aa46', '#f09b3f', '#e98a39',
             '#e07c34', '#d66e30', '#cb612d', '#bf552b', '#b24a29', '#a54127', '#983a26']
DEPTH = [hexc('#7a2f2a'), hexc('#622423'), hexc('#4d1c1e')]


def text_mask(word, glyphs, gap):
    widths = [len(glyphs[c][0]) for c in word]
    total = sum(widths) + gap * (len(word) - 1)
    h = len(glyphs[word[0]])
    m = np.zeros((h, total), bool)
    x = 0
    for c, w in zip(word, widths):
        for r, row in enumerate(glyphs[c]):
            for i, ch in enumerate(row):
                if ch == '#':
                    m[r, x + i] = True
        x += w + gap
    return m


def outline_of(mask):
    """Pixels next to the mask (4-neighbourhood plus diagonals) that aren't in it."""
    h, w = mask.shape
    out = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            shifted = np.zeros_like(mask)
            ys = slice(max(0, dy), h + min(0, dy))
            yd = slice(max(0, -dy), h + min(0, -dy))
            xs = slice(max(0, dx), w + min(0, dx))
            xd = slice(max(0, -dx), w + min(0, -dx))
            shifted[yd, xd] = mask[ys, xs]
            out |= shifted
    return out & ~mask


def make_logo():
    img = np.zeros((LOGO_H, LOGO_W, 4), np.uint8)

    # ---- letters
    word = text_mask('WILDWOOD', GLYPHS, 2)
    th, tw = word.shape
    ox = (LOGO_W - tw) // 2
    oy = 17
    canvas = np.zeros((LOGO_H, LOGO_W), bool)
    canvas[oy:oy + th, ox:ox + tw] = word
    depth_layers = []
    for d in range(1, 4):
        dm = np.zeros_like(canvas)
        dm[d:, :] = canvas[:-d, :]
        depth_layers.append(dm)
    full = canvas.copy()
    for dm in depth_layers:
        full |= dm
    # Outline around face and depth together
    for y, x in zip(*np.nonzero(outline_of(full))):
        img[y, x] = OUTLINE
    # Depth, darker further down
    for i, dm in enumerate(depth_layers):
        for y, x in zip(*np.nonzero(dm & ~canvas)):
            img[y, x] = DEPTH[min(i, 2)]
    # Face gradient with highlights
    for y, x in zip(*np.nonzero(canvas)):
        r = y - oy
        img[y, x] = hexc(FACE_ROWS[r])
    up = np.zeros_like(canvas)
    up[1:, :] = canvas[:-1, :]
    left = np.zeros_like(canvas)
    left[:, 1:] = canvas[:, :-1]
    down = np.zeros_like(canvas)
    down[:-1, :] = canvas[1:, :]
    for y, x in zip(*np.nonzero(canvas & ~up)):
        img[y, x] = hexc('#fffbe6')
    for y, x in zip(*np.nonzero(canvas & ~left & up)):
        r = y - oy
        img[y, x] = hexc(FACE_ROWS[max(0, r - 3)])
    # A thin dark line on the bottom edge of strokes inside the letters (above a hole)
    for y, x in zip(*np.nonzero(canvas & ~down)):
        if y + 1 < LOGO_H and not full[y + 1, x]:
            continue
        img[y, x] = hexc('#8f3524')
    # Sparkle glints on a few letters
    for gx, gy in ((ox + 1, oy + 1), (ox + 46, oy + 1), (ox + 95, oy + 3)):
        paint(img, gx, gy, hexc('#ffffff'))

    # ---- pine crown
    for dx, h, seed in ((-9, 11, 3), (9, 11, 4), (0, 16, 7)):
        m, mcx = pine_mask(h, seed)
        mh, mw = m.shape
        cx = LOGO_W // 2 + dx
        base = oy - 1
        px, py = cx - mcx, base - h
        crown = np.zeros((LOGO_H, LOGO_W), bool)
        for yy in range(mh):
            for xx in range(mw):
                if m[yy, xx] and 0 <= py + yy < LOGO_H and 0 <= px + xx < LOGO_W:
                    crown[py + yy, px + xx] = True
        for y, x in zip(*np.nonzero(outline_of(crown))):
            if img[y, x][3] == 0 or tuple(img[y, x]) == OUTLINE:
                img[y, x] = OUTLINE
        right = np.zeros_like(crown)
        right[:, :-1] = crown[:, 1:]
        for y, x in zip(*np.nonzero(crown)):
            if not full[y, x]:
                lit = not right[y, x]
                img[y, x] = hexc('#4f8a5e') if lit else (hexc('#2f5e4a') if (x + y) % 5 else hexc('#386a50'))
        paint(img, cx, py, hexc('#ffe9a0'))
    # Fireflies around the crown
    for fx, fy, big in ((LOGO_W // 2 - 22, 6, True), (LOGO_W // 2 + 21, 3, False), (LOGO_W // 2 + 26, 10, True), (LOGO_W // 2 - 28, 12, False)):
        paint(img, fx, fy, hexc('#fff3b0'))
        if big:
            for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                paint(img, fx + ddx, fy + ddy, hexc('#e8b04a'))

    # ---- ribbon with "A COZY MODPACK"
    tiny = text_mask('A COZY MODPACK', TINY, 1)
    rh, rw = tiny.shape
    ry = oy + th + 3 + 3
    pad = 4
    x0 = (LOGO_W - rw) // 2 - pad
    x1 = x0 + rw + pad * 2
    band_top, band_bottom = ry - 2, ry + rh + 1
    # Folded tails behind the band
    for side in (-1, 1):
        tx = x0 - 1 if side == -1 else x1
        for i in range(6):
            x = tx + side * i
            for y in range(band_top + 2, band_bottom + 3):
                notch = (i >= 4) and (y == (band_top + band_bottom + 5) // 2)
                if not notch:
                    paint(img, x, y, hexc('#5e1f2c'))
    band = np.zeros((LOGO_H, LOGO_W), bool)
    band[band_top:band_bottom + 1, x0:x1] = True
    tails = np.zeros_like(band)
    for y in range(LOGO_H):
        for x in range(LOGO_W):
            if tuple(img[y, x]) == hexc('#5e1f2c'):
                tails[y, x] = True
    for y, x in zip(*np.nonzero(outline_of(band | tails))):
        if img[y, x][3] == 0:
            img[y, x] = OUTLINE
    for y in range(band_top, band_bottom + 1):
        for x in range(x0, x1):
            c = '#a33d45' if y == band_top else ('#6e2532' if y == band_bottom else '#8a3140')
            img[y, x] = hexc(c)
    for y, x in zip(*np.nonzero(tiny)):
        img[ry + y, (LOGO_W - rw) // 2 + x] = hexc('#ffe9c4')
    return img


if __name__ == '__main__':
    import sys
    from scene import save
    save(make_logo(), f'{sys.argv[1]}/logo_preview.png', 6)
