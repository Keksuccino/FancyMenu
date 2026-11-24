"""Pixel-art button and slider textures of the Global Customizations page: a walnut wood style with brass studs.

Usage: python3 make_textures.py <out dir>
Writes button_{normal,hover,inactive}.png (64x20, nine-slice border 4), slider_background.png (64x20, nine-slice border 4)
and slider_handle_{normal,hover,inactive}.png (8x20, no nine-slice). One texture pixel is one GUI pixel.
The middle of the nine-slice textures gets tiled, so everything between x 4 and 59 repeats every 56 pixels without a seam.
"""
import random
import sys
from pathlib import Path
from PIL import Image

W, H = 64, 20
BORDER = 4
PERIOD = W - 2 * BORDER  # 56, the width of the tiled middle


def hexc(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, amount):
    return tuple(max(0, min(255, round(c + amount))) for c in color[:3]) + (color[3],)


def grain_rows(seed):
    """Per body row, a list of PERIOD brightness offsets: soft horizontal wood grain that wraps around every PERIOD pixels."""
    rng = random.Random(seed)
    rows = []
    for _ in range(H):
        offsets = [0] * PERIOD
        # A few long streaks per row, darker or lighter, which wrap around the tile edge
        for _ in range(rng.randint(2, 4)):
            start = rng.randrange(PERIOD)
            length = rng.randint(5, 26)
            amount = rng.choice([-11, -8, -6, -5, 5, 7])
            for i in range(length):
                offsets[(start + i) % PERIOD] += amount
        rows.append(offsets)
    return rows


def grain_at(rows, x, y):
    # The middle tiles from x = BORDER, so the grain gets indexed from there to wrap exactly at the tile edge
    return rows[y][(x - BORDER) % PERIOD]


def button(palette, seed=7):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    rows = grain_rows(seed)
    body_top, body_bottom = palette["body_top"], palette["body_bottom"]
    for y in range(H):
        for x in range(W):
            corner = (x in (0, W - 1)) and (y in (0, H - 1))
            if corner:
                continue
            on_outline = x in (0, W - 1) or y in (0, H - 1)
            inner_corner = (x in (1, W - 2)) and (y in (1, H - 2))
            if on_outline or inner_corner:
                px[x, y] = palette["outline"]
                continue
            if y == 1:
                px[x, y] = palette["highlight"]
            elif x == 1:
                px[x, y] = palette["highlight_side"]
            elif y == H - 2:
                px[x, y] = palette["shadow"]
            elif x == W - 2:
                px[x, y] = palette["shadow_side"]
            else:
                t = (y - 2) / (H - 5)
                base = tuple(round(body_top[i] + (body_bottom[i] - body_top[i]) * t) for i in range(3)) + (255,)
                color = shade(base, grain_at(rows, x, y))
                if y == H - 3:
                    color = shade(color, -9)
                px[x, y] = color
    # Brass studs at both ends, in the edge pieces, so they never repeat
    for sx in (2, W - 4):
        px[sx, 9] = palette["stud_light"]
        px[sx + 1, 9] = palette["stud_mid"]
        px[sx, 10] = palette["stud_mid"]
        px[sx + 1, 10] = palette["stud_dark"]
    return img


def slider_background(palette, seed=11):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    rows = grain_rows(seed)
    for y in range(H):
        for x in range(W):
            corner = (x in (0, W - 1)) and (y in (0, H - 1))
            if corner:
                continue
            on_outline = x in (0, W - 1) or y in (0, H - 1)
            inner_corner = (x in (1, W - 2)) and (y in (1, H - 2))
            if on_outline or inner_corner:
                px[x, y] = palette["outline"]
            elif y == 1:
                px[x, y] = palette["inset_top"]
            elif y == 2:
                px[x, y] = shade(palette["inset_body"], -10)
            elif x == 1:
                px[x, y] = palette["inset_side"]
            elif y == H - 2:
                px[x, y] = palette["lip"]
            elif x == W - 2:
                px[x, y] = palette["lip_side"]
            else:
                px[x, y] = shade(palette["inset_body"], grain_at(rows, x, y) * 0.6)
    return img


def slider_handle(palette):
    img = Image.new("RGBA", (8, H), (0, 0, 0, 0))
    px = img.load()
    columns = palette["columns"]  # x 1 to 6, from the lit left side to the dark right side
    for y in range(H):
        for x in range(8):
            corner = (x in (0, 7)) and (y in (0, H - 1))
            if corner:
                continue
            if x in (0, 7) or y in (0, H - 1):
                px[x, y] = palette["outline"]
                continue
            color = columns[x - 1]
            if y == 1:
                color = shade(color, 22)
            elif y == H - 2:
                color = shade(color, -34)
            elif y == H - 3:
                color = shade(color, -14)
            px[x, y] = color
    # Engraved grip lines in the middle
    for gy in (7, 9, 11):
        for gx in range(2, 6):
            px[gx, gy] = palette["groove"]
            px[gx, gy + 1] = palette["groove_light"]
    return img


BUTTON_NORMAL = {
    "outline": hexc("#24130a"),
    "highlight": hexc("#b07743"),
    "highlight_side": hexc("#9a6537"),
    "shadow": hexc("#3e2414"),
    "shadow_side": hexc("#4a2c18"),
    "body_top": hexc("#7f4f2b"),
    "body_bottom": hexc("#683e21"),
    "stud_light": hexc("#ffe08f"),
    "stud_mid": hexc("#d39a3e"),
    "stud_dark": hexc("#7c5019"),
}

BUTTON_HOVER = {
    "outline": hexc("#f4bf4f"),
    "highlight": hexc("#d8a160"),
    "highlight_side": hexc("#c58b4c"),
    "shadow": hexc("#5a3519"),
    "shadow_side": hexc("#66401f"),
    "body_top": hexc("#9a6235"),
    "body_bottom": hexc("#7f4d28"),
    "stud_light": hexc("#fff4c8"),
    "stud_mid": hexc("#f2bc55"),
    "stud_dark": hexc("#9c6a22"),
}

BUTTON_INACTIVE = {
    "outline": hexc("#1d1815"),
    "highlight": hexc("#6f655c"),
    "highlight_side": hexc("#665d55"),
    "shadow": hexc("#2b2521"),
    "shadow_side": hexc("#312a25"),
    "body_top": hexc("#4f4740"),
    "body_bottom": hexc("#433c36"),
    "stud_light": hexc("#a49c91"),
    "stud_mid": hexc("#7d756c"),
    "stud_dark": hexc("#4a443e"),
}

SLIDER_BACKGROUND = {
    "outline": hexc("#24130a"),
    "inset_top": hexc("#140b05"),
    "inset_side": hexc("#1c0f07"),
    "inset_body": hexc("#38200f"),
    "lip": hexc("#7a4b27"),
    "lip_side": hexc("#5c3820"),
}

HANDLE_NORMAL = {
    "outline": hexc("#24130a"),
    "columns": [hexc(c) for c in ("#ffe39b", "#f2c25c", "#e4a942", "#d69a37", "#c2832b", "#93601f")],
    "groove": hexc("#7a4c16"),
    "groove_light": hexc("#ffe7a8"),
}

HANDLE_HOVER = {
    "outline": hexc("#3a2108"),
    "columns": [hexc(c) for c in ("#fff6d2", "#ffdc86", "#ffc95e", "#f6b64b", "#e29d39", "#b2772a")],
    "groove": hexc("#94601d"),
    "groove_light": hexc("#fff7d8"),
}

HANDLE_INACTIVE = {
    "outline": hexc("#1d1815"),
    "columns": [hexc(c) for c in ("#b9b2a8", "#a29a90", "#928a80", "#868076", "#77706a", "#5c5650")],
    "groove": hexc("#4f4943"),
    "groove_light": hexc("#c4beb5"),
}


def main():
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    button(BUTTON_NORMAL).save(out / "button_normal.png")
    button(BUTTON_HOVER).save(out / "button_hover.png")
    button(BUTTON_INACTIVE).save(out / "button_inactive.png")
    slider_background(SLIDER_BACKGROUND).save(out / "slider_background.png")
    slider_handle(HANDLE_NORMAL).save(out / "slider_handle_normal.png")
    slider_handle(HANDLE_HOVER).save(out / "slider_handle_hover.png")
    slider_handle(HANDLE_INACTIVE).save(out / "slider_handle_inactive.png")


if __name__ == "__main__":
    main()
