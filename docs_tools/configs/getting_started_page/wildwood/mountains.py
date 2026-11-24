"""Faceted pixel-art mountains: a lit face toward the sun, a shadow face, a jagged crease and snow caps."""
import numpy as np


def slope_profile(length, pattern):
    """Heights along a slope from a repeating run-length pattern: each entry is how many pixels the slope runs per step down."""
    out = []
    y = 0
    i = 0
    while len(out) < length:
        run = pattern[i % len(pattern)]
        for _ in range(run):
            if len(out) < length:
                out.append(y)
        y += 1
        i += 1
    return out


def draw_mountain(img, peak_x, peak_y, left_pattern, right_pattern, colors, base_y, snow=0, crease_pattern=(1, 2, 1, 1, 2), width_limit=200, lit_right=True, edge_len=999):
    """colors: dict with lit, shadow, edge (rim along the lit ridge), snow, snow_shadow."""
    H, W = img.shape[:2]
    left = slope_profile(width_limit, left_pattern)
    right = slope_profile(width_limit, right_pattern)
    top = {}
    for d in range(width_limit):
        lx, rx = peak_x - d, peak_x + d
        if peak_y + left[d] < base_y:
            top[lx] = min(top.get(lx, 10 ** 6), peak_y + left[d])
        if peak_y + right[d] < base_y:
            top[rx] = min(top.get(rx, 10 ** 6), peak_y + right[d])
    # Crease: runs from the peak down and to the left, so the lit face is wider
    crease = {}
    cx = peak_x
    step = 0
    for y in range(peak_y, base_y):
        crease[y] = cx
        run = crease_pattern[step % len(crease_pattern)]
        step += 1
        d = -1 if lit_right else 1
        cx += d if run == 1 else 0
        if run == 2 and step % 3 == 0:
            cx += d
    for x, ty in top.items():
        if not (0 <= x < W):
            continue
        for y in range(max(0, ty), min(H, base_y)):
            on_lit = x >= crease.get(y, peak_x) if lit_right else x <= crease.get(y, peak_x)
            img[y, x] = colors['lit'] if on_lit else colors['shadow']
        # Rim light along the lit ridge line
        if ((x > peak_x) if lit_right else (x < peak_x)) and 0 <= ty < H and ty - peak_y < edge_len:
            img[ty, x] = colors['edge']
    # Snow cap
    if snow:
        for y in range(peak_y, min(H, peak_y + snow + 3)):
            for x in range(peak_x - 40, peak_x + 41):
                if not (0 <= x < W) or x not in top or y < top[x]:
                    continue
                depth = y - peak_y
                # Jagged lower edge of the snow
                limit = snow - (1 if (x * 7 + 3) % 5 < 2 else 0) - (2 if (x * 13) % 7 == 0 else 0)
                if depth <= limit:
                    on_lit = x >= crease.get(y, peak_x) if lit_right else x <= crease.get(y, peak_x)
                    img[y, x] = colors['snow'] if on_lit else colors['snow_shadow']
    return top
