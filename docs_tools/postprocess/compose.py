"""Draws the cursor, click ripples and key badges onto recorded showcase frames and writes the composed frames.

Usage: python3 compose.py <frames dir> <out dir>

<frames dir> holds f_00000.png, f_00001.png, ... (1280x720) and frames.jsonl with one line per frame, written by the recorder:
{"frame":0,"x":560.0,"y":300.0,"cursor":"arrow|text|hand","left":false,"right":false,"events":[...]}
x/y are the pointer position in frame pixels. Events: {"type":"press","button":"left|right","x":..,"y":..} draws a click ripple,
{"type":"badge","text":"Ctrl + B"} shows a key badge at the bottom for BADGE_FRAMES frames.
Pixels that barely change between frames get frozen (FREEZE / FREEZE_DRIFT), so the GIF stays small without ghosting.
Writes <out dir>/c_00000.png, ... for encode.sh."""
import json, sys, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sprites import ARROW, IBEAM

FRAMES, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
FREEZE = 4
FREEZE_DRIFT = 2
BADGE_FRAMES = 17
BADGE_FADE = 4
RIPPLE_FRAMES = 5

def font(size):
    return ImageFont.truetype("/System/Library/Fonts/HelveticaNeue.ttc", size, index=1)

FONT = font(22)

def ring(radius, alpha, color):
    ss = 4
    size = int((radius + 3) * 2)
    big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    c = size * ss / 2
    r = radius * ss
    ImageDraw.Draw(big).ellipse((c - r, c - r, c + r, c + r), outline=color + (alpha,), width=int(2.2 * ss))
    return big.resize((size, size), Image.LANCZOS)

def badge(text, alpha):
    keys = text.split(" + ")
    pad_x, key_h, gap = 14, 40, 10
    widths = [max(key_h, int(FONT.getlength(k)) + 2 * pad_x) for k in keys]
    plus_w = int(FONT.getlength("+"))
    inner_w = sum(widths) + (len(keys) - 1) * (plus_w + 2 * gap)
    w, h = inner_w + 28, key_h + 24
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=14, fill=(12, 14, 20, 225), outline=(255, 255, 255, 40))
    x = 14
    for i, (k, kw) in enumerate(zip(keys, widths)):
        d.rounded_rectangle((x, 12, x + kw, 12 + key_h - 4), radius=8, fill=(46, 52, 64, 255))
        d.rounded_rectangle((x, 12, x + kw, 12 + key_h - 8), radius=8, fill=(70, 78, 94, 255))
        tw = FONT.getlength(k)
        d.text((x + (kw - tw) / 2, 12 + (key_h - 8) / 2), k, font=FONT, fill=(245, 247, 250, 255), anchor="lm")
        x += kw
        if i < len(keys) - 1:
            d.text((x + gap, 12 + (key_h - 8) / 2), "+", font=FONT, fill=(200, 205, 215, 255), anchor="lm")
            x += plus_w + 2 * gap
    if alpha < 255:
        a = np.array(img)
        a[..., 3] = (a[..., 3].astype(np.float32) * alpha / 255).astype(np.uint8)
        img = Image.fromarray(a)
    return img

frames = [json.loads(l) for l in open(os.path.join(FRAMES, "frames.jsonl"))]
presses, badges = [], []
for f in frames:
    for e in f["events"]:
        if e["type"] == "press":
            presses.append((f["frame"], e))
        elif e["type"] == "badge":
            badges.append((f["frame"], e["text"]))

prev_raw = None
prev_out = None
for f in frames:
    n = f["frame"]
    raw = np.array(Image.open(os.path.join(FRAMES, "f_%05d.png" % n)).convert("RGB")).astype(np.int16)
    out = raw.copy()
    if prev_raw is not None:
        # Keep the shown pixel only while the frame barely changed AND the shown pixel is still close to the real one,
        # so slow fades still reach their end instead of leaving ghosts behind
        still = (np.abs(raw - prev_raw).max(axis=2) < FREEZE) & (np.abs(raw - prev_out).max(axis=2) < FREEZE_DRIFT)
        out[still] = prev_out[still]
    prev_raw = raw
    prev_out = out
    img = Image.fromarray(out.astype(np.uint8)).convert("RGBA")
    for start, e in presses:
        k = n - start
        if 0 <= k < RIPPLE_FRAMES:
            color = (255, 255, 255) if e["button"] == "left" else (140, 200, 255)
            r = ring(6 + 4 * k, int(220 * (1 - k / RIPPLE_FRAMES)), color)
            img.alpha_composite(r, (int(round(e["x"] - r.width / 2)), int(round(e["y"] - r.height / 2))))
    for start, text in badges:
        k = n - start
        if 0 <= k < BADGE_FRAMES:
            fade = min(1.0, (BADGE_FRAMES - k) / BADGE_FADE)
            b = badge(text, int(255 * fade))
            # Frames smaller than 1280x720 (like halved ones) get smaller badges, but less small than the frame, so they stay readable
            scale = min(1.0, (img.width / 1280) ** 0.5)
            if scale < 1.0:
                b = b.resize((round(b.width * scale), round(b.height * scale)), Image.LANCZOS)
            img.alpha_composite(b, ((img.width - b.width) // 2, img.height - b.height - round(26 * scale)))
    sprite, hot = IBEAM if f["cursor"] == "text" else ARROW
    img.alpha_composite(sprite, (int(round(f["x"])) - hot[0], int(round(f["y"])) - hot[1]))
    img.convert("RGB").save(os.path.join(OUT, "c_%05d.png" % n))
print("composed", len(frames), "frames,", len(presses), "presses,", len(badges), "badges")
