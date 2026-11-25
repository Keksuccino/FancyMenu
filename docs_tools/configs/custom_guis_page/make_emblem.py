"""Draws the Frostfall emblem of the Custom GUIs page GIF (32x32 pixel art, saved 4x as frostfall_emblem.png): a shield with a snowy
mountain and pines under a starry night sky. The scene is drawn by hand, row by row, inside the field of the shield.

Usage: python3 make_emblem.py [output png]
"""
import os
import sys
from PIL import Image

PAL = {
    'O': (20, 30, 44),      # outline and inner line
    'L': (214, 230, 242),   # rim highlight (top)
    'B': (156, 184, 208),   # rim light (left)
    'b': (110, 138, 166),   # rim mid (bottom left)
    'd': (70, 92, 120),     # rim dark (right)
    'S': (24, 40, 70),      # sky
    's': (32, 54, 92),      # lower sky
    't': (48, 78, 124),     # glow near the mountains
    '*': (255, 246, 206),   # star
    '+': (150, 172, 210),   # dim star
    'W': (242, 249, 255),   # snow
    'w': (188, 212, 234),   # snow shade
    'v': (140, 170, 204),   # deep snow shade
    'R': (132, 148, 170),   # rock, lit
    'r': (92, 108, 132),    # rock
    'k': (58, 70, 94),      # rock, shadow
    'm': (78, 100, 138),    # far mountains, lit
    'n': (58, 76, 112),     # far mountains, shadow
    'G': (56, 118, 88),     # pine
    'g': (30, 76, 60),      # pine, shadow
    'T': (96, 64, 44),      # trunk
}

N = 32
# Left outline column of the shield per row, the right side mirrors it (row 0 is the top edge)
LEFT = [3] + [2] * 18 + [3, 3, 3, 4, 4, 5, 6, 7, 8, 9, 11, 13, 15]
FIELD_X0 = 5  # column of x = 0 in the scene rows

# The scene, rows 3 to 29, x = 0..21 (columns 5..26). Pixels outside the field of the shield get cut off.
SCENE = {
    3:  "SSSSSSSSSSSSSSSSSSSSSS",
    4:  "SS*SSSSSS+SSSSSS+SSSSS",
    5:  "SSSSSSSSSSSSSSSS*SSS+S",
    6:  "SSSSS+SSSSSSSS+***+SSS",
    7:  "SSSSSSSSSSSSSSSS*SSSSS",
    8:  "SSS*SSSSSSWwSSSS+SSSSS",
    9:  "sssssssssWWwwssssss+ss",
    10: "ss+sssssWWWWwwssssssss",
    11: "ssssssssWWWWwwwsssssss",
    12: "sssWsssRWWWWwwvkssssss",
    13: "ssWwnsRRWRWWwkwkssWsss",
    14: "tmWmnnRRRRWRRkkkkWWwtt",
    15: "mmmnnRRRRrRRRRkkkkmwnt",
    16: "mmnnRWRRRrRRRRkkkrkmnn",
    17: "mnnRRGRRrRRRRRRkWkrknn",
    18: "nnRRWGgrRRRrRRRWGgkrkn",
    19: "nRRRGGgRrRRrRRRGGgkkkk",
    20: "RRRWWGggRrRRrRWWGggkkk",
    21: "wwwGGGggwWWWWwGGGggwww",
    22: "WWWWwTwWWWWWWWWwTwWWWW",
    23: "WWWWwwwWWWWWWWWwwwWWWW",
    24: "WWWWWWWWWWWWWWWWWWWWWW",
    25: "WWWWWWWwwwwWWWWWWWWWWW",
    26: "WWWWWWWWWWwwwWWWWWWWWW",
    27: "WWWWWWWWWWWWWWWWWWWWWW",
    28: "WWWWWWWWWWWWWWWWWWWWWW",
    29: "WWWWWWWWWWWWWWWWWWWWWW",
}

grid = [[None] * N for _ in range(N)]


def shield_inside(c, r):
    return 0 <= r < N and LEFT[r] <= c <= N - 1 - LEFT[r]


def ring(c, r):
    """0 for the outline, 1 for the rim, 2 for the inner line, 3 for the field. Straight distances only, so the
    diagonal edges of the point stay one pixel thick per row."""
    for d in range(1, 4):
        for dr, dc in ((-d, 0), (d, 0), (0, -d), (0, d)):
            if not shield_inside(c + dc, r + dr):
                return d - 1
    return 3


for r in range(N):
    for c in range(N):
        if not shield_inside(c, r):
            continue
        k = ring(c, r)
        if k == 0 or k == 2:
            grid[r][c] = 'O'
        elif k == 1:
            if r <= 1:
                grid[r][c] = 'L'
            elif r <= 19:
                grid[r][c] = 'B' if c < 16 else 'd'
            else:
                grid[r][c] = 'b' if c < 16 else 'd'
        else:
            row = SCENE.get(r)
            x = c - FIELD_X0
            if row is None or not (0 <= x < len(row)):
                raise SystemExit(f"No scene pixel for column {c}, row {r}")
            grid[r][c] = row[x]

img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
for r in range(N):
    for c in range(N):
        if grid[r][c]:
            img.putpixel((c, r), PAL[grid[r][c]] + (255,))

# 4x, so it shows pixel for pixel in a 64x64 Image element at GUI scale 2, without blurry upscaling
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), 'frostfall_emblem.png')
img.resize((N * 4, N * 4), Image.NEAREST).save(out)
print('wrote', out)
