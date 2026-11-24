"""Builds the cursor sprites that compose.py draws onto showcase GIF frames (1280x720), each with its hotspot:
- ARROW: a classic arrow, drawn 8x supersampled and downscaled once, so it gets clean anti-aliased edges at its final size.
- IBEAM: a hand-made pixel art text cursor, so its 1 pixel stem stays sharp.
Both have a dark outline, so they read on light and dark UIs."""
from PIL import Image, ImageDraw, ImageFilter

SS = 8

def _arrow():
    # Outline of a classic arrow cursor (tip at 0,0), in final pixels
    pts = [(0, 0), (0, 19.2), (4.6, 14.9), (8.0, 22.6), (11.0, 21.3), (7.7, 13.8), (13.9, 13.8)]
    w, h = 18, 27
    def poly(scale_pts, off):
        return [((x + off[0]) * SS, (y + off[1]) * SS) for x, y in scale_pts]
    pad = (1.5, 1.5)
    big = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    # soft shadow
    shadow = Image.new("L", big.size, 0)
    ImageDraw.Draw(shadow).polygon(poly(pts, (pad[0] + 1.2, pad[1] + 1.6)), fill=110)
    shadow = shadow.filter(ImageFilter.GaussianBlur(1.3 * SS))
    big.paste((0, 0, 0, 255), (0, 0), shadow)
    # dark outline: the shape grown by the outline width, then the white body on top
    d.line(poly(pts + [pts[0]], pad), fill=(18, 18, 22, 255), width=int(1.9 * SS), joint="curve")
    d.polygon(poly(pts, pad), fill=(18, 18, 22, 255))
    inner = [(x, y) for x, y in pts]
    body = Image.new("L", big.size, 0)
    ImageDraw.Draw(body).polygon(poly(inner, pad), fill=255)
    body = body.filter(ImageFilter.MinFilter(int(0.95 * SS) | 1))
    big.paste((255, 255, 255, 255), (0, 0), body)
    img = big.resize((w, h), Image.LANCZOS)
    return img, (int(pad[0]), int(pad[1]))

def _ibeam():
    # Hand-made pixel art, so the 1 pixel stem stays sharp: K = outline, W = white
    rows = [
        ".KKK.KKK.",
        "KWWWKWWWK",
        ".KKKWKKK.",
    ] + ["...KWK..."] * 14 + [
        ".KKKWKKK.",
        "KWWWKWWWK",
        ".KKK.KKK.",
    ]
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == "K":
                img.putpixel((x, y), (18, 18, 22, 255))
            elif c == "W":
                img.putpixel((x, y), (255, 255, 255, 255))
    return img, (4, len(rows) // 2)

ARROW = _arrow()
IBEAM = _ibeam()

if __name__ == "__main__":
    # Usage: python3 sprites.py [out dir]: writes cursor_<name>.png and 10x previews on a dark and a light backdrop
    import os, sys
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for name, (img, hot) in (("arrow", ARROW), ("ibeam", IBEAM)):
        img.save(os.path.join(out, f"cursor_{name}.png"))
        preview = Image.new("RGBA", (img.width * 2 + 8, img.height + 4), (40, 44, 56, 255))
        preview.alpha_composite(img, (2, 2))
        light = Image.new("RGBA", (img.width + 4, img.height + 4), (200, 200, 200, 255))
        preview.paste(light, (img.width + 4, 0))
        preview.alpha_composite(img, (img.width + 6, 2))
        preview.resize((preview.width * 10, preview.height * 10), Image.NEAREST).save(os.path.join(out, f"preview_{name}.png"))
        print(name, img.size, "hotspot", hot)
