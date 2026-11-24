"""Renders the frames of the mockup chat GIF of the Export for AI page: a player asks Alice for help and sends an export.

This is the one docs image that is no in-game render, because it shows a chat outside of the game.
Usage: python3 chat_gif.py <emotes dir> <frames dir>
Then encode with: sh ../../postprocess/encode.sh <frames dir> <output gif> (10 fps, frames named c_00000.png)

Alice's avatar and every emote she uses come from the emotes dir (alice_*.png).
"""
import math
import os
import sys
from PIL import Image, ImageDraw, ImageFont

EMOTES_DIR, OUT_DIR = sys.argv[1], sys.argv[2]
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 800, 600
HEADER_H = 52
COMPOSER_H = 76
CHAT_TOP, CHAT_BOTTOM = HEADER_H, H - COMPOSER_H
FPS = 10

BG = (49, 51, 56)
HEADER_BG = (43, 45, 49)
DIVIDER = (38, 39, 43)
TEXT = (219, 222, 225)
BRIGHT = (242, 243, 245)
MUTED = (148, 155, 164)
CODE_BG = (30, 31, 34)
CARD_BORDER = (35, 36, 40)
COMPOSER_BG = (56, 58, 64)
ALICE_COLOR = (244, 127, 210)
MIA_COLOR = (88, 196, 220)
TAG_BG = (232, 92, 175)
CODE_TEXT = (230, 210, 170)

SF = "/System/Library/Fonts/SFNS.ttf"
MONO = "/System/Library/Fonts/SFNSMono.ttf"


def font(size, weight="Regular", path=SF):
    f = ImageFont.truetype(path, size)
    f.set_variation_by_name(weight)
    return f


F_TEXT = font(16)
F_BOLD = font(16, "Semibold")
F_NAME = font(16, "Semibold")
F_TIME = font(12)
F_HEADER = font(16, "Semibold")
F_CODE = font(14, "Regular", MONO)
F_MONO_SMALL = font(12.5, "Regular", MONO)
F_TAG = font(10, "Bold")
F_BADGE = font(17, "Semibold")

LINE_H = 24
INLINE_EMOTE = 36
EMOTE_LINE_H = 38
JUMBO_EMOTE = 72
TEXT_X = 76
TEXT_W = W - TEXT_X - 28


def emote(name, size):
    image = Image.open(os.path.join(EMOTES_DIR, f"alice_{name}.png")).convert("RGBA")
    return image.resize((size, size), Image.LANCZOS)


def circle_avatar(image, size):
    image = image.resize((size * 4, size * 4), Image.LANCZOS)
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, image.width - 1, image.height - 1), fill=255)
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.paste(image, (0, 0), mask)
    return out.resize((size, size), Image.LANCZOS)


def letter_avatar(letter, color, size):
    big = Image.new("RGBA", (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=color)
    f = font(size * 2, "Semibold")
    d.text((size * 2, size * 2), letter, font=f, fill=(255, 255, 255), anchor="mm")
    return big.resize((size, size), Image.LANCZOS)


# The square emote looks best as a round avatar with a soft background behind it
alice_source = Image.open(os.path.join(EMOTES_DIR, "alice_working_maniac.png")).convert("RGBA")
alice_backed = Image.new("RGBA", alice_source.size, (255, 214, 236, 255))
alice_backed.alpha_composite(alice_source)
ALICE_AVATAR = circle_avatar(alice_backed, 40)
MIA_AVATAR = letter_avatar("M", (232, 132, 54), 40)


def tokenize(rich):
    """rich: list of (kind, value) with kind text/bold/code/emote. Text gets split into words, keeping the spaces."""
    tokens = []
    for kind, value in rich:
        if kind == "emote":
            tokens.append(("emote", value))
            continue
        word = ""
        for ch in value:
            if ch == " ":
                if word:
                    tokens.append((kind, word))
                    word = ""
                tokens.append(("space", " "))
            else:
                word += ch
        if word:
            tokens.append((kind, word))
    return tokens


def token_width(kind, value):
    if kind == "emote":
        return INLINE_EMOTE + 2
    if kind == "space":
        return F_TEXT.getlength(" ")
    if kind == "bold":
        return F_BOLD.getlength(value)
    if kind == "code":
        return F_CODE.getlength(value) + 8
    return F_TEXT.getlength(value)


def layout_rich(rich, width):
    lines, line, x = [], [], 0
    for kind, value in tokenize(rich):
        w = token_width(kind, value)
        if kind == "space" and not line:
            continue
        if x + w > width and line and kind != "space":
            while line and line[-1][0] == "space":
                line.pop()
            lines.append(line)
            line, x = [], 0
        line.append((kind, value, x))
        x += w
    if line:
        lines.append(line)
    return lines


def draw_rich(image, rich, x0, y0, width):
    d = ImageDraw.Draw(image)
    lines = layout_rich(rich, width)
    y = y0
    for line in lines:
        # Lines with an emote get taller, so the emote is big enough to see what Alice does
        line_h = EMOTE_LINE_H if any(kind == "emote" for kind, _, _ in line) else LINE_H
        ty = y + (line_h - LINE_H) // 2
        for kind, value, x in line:
            if kind == "emote":
                e = emote(value, INLINE_EMOTE)
                image.alpha_composite(e, (int(x0 + x + 1), int(y + (line_h - INLINE_EMOTE) // 2)))
            elif kind == "bold":
                d.text((x0 + x, ty + 2), value, font=F_BOLD, fill=BRIGHT)
            elif kind == "code":
                cw = F_CODE.getlength(value)
                d.rounded_rectangle((x0 + x, ty + 2, x0 + x + cw + 8, ty + 22), radius=4, fill=CODE_BG)
                d.text((x0 + x + 4, ty + 5), value, font=F_CODE, fill=CODE_TEXT)
            elif kind == "text":
                d.text((x0 + x, ty + 2), value, font=F_TEXT, fill=TEXT)
        y += line_h
    return y


def file_icon(d, x, y, color):
    d.polygon([(x, y), (x + 14, y), (x + 20, y + 6), (x + 20, y + 26), (x, y + 26)], fill=color)
    d.polygon([(x + 14, y), (x + 20, y + 6), (x + 14, y + 6)], fill=(255, 255, 255, 120))
    for i in range(3):
        d.line((x + 4, y + 12 + i * 4, x + 16, y + 12 + i * 4), fill=(255, 255, 255, 170), width=1)


EXPORT_PREVIEW = [
    "# FancyMenu AI Export: Element",
    "",
    "This file was made with the \"Export for AI\" feature of FancyMenu,",
    "a Minecraft mod for customizing menus and other screens of the game.",
    "",
    "## Environment",
    "",
    "- **FancyMenu version:** `3.11.0`",
    "- **Minecraft version:** `26.3`",
]


def export_card(width):
    card_h = 214
    card = Image.new("RGBA", (width, card_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(card)
    d.rounded_rectangle((0, 0, width - 1, card_h - 1), radius=8, fill=CODE_BG, outline=CARD_BORDER)
    file_icon(d, 14, 12, (120, 130, 220, 255))
    d.text((44, 12), "message.txt", font=F_BOLD, fill=(0, 168, 252))
    d.text((44, 32), "3.4 KB", font=F_TIME, fill=MUTED)
    d.line((12, 52, width - 12, 52), fill=CARD_BORDER, width=1)
    y = 60
    for line in EXPORT_PREVIEW:
        d.text((14, y), line, font=F_MONO_SMALL, fill=(200, 204, 210))
        y += 15
    # The preview fades out at the bottom, like the rest of the file is still there
    fade = Image.new("RGBA", (width - 2, 36), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fade)
    for i in range(36):
        fd.line((0, i, width, i), fill=CODE_BG + (int(255 * min(1, i / 30)),))
    card.alpha_composite(fade, (1, card_h - 37))
    return card


def message_block(author, rich_parts, card=False, jumbo=None):
    """One message with avatar, name and time. rich_parts: list of paragraphs (rich lists)."""
    block = Image.new("RGBA", (W, 600), (0, 0, 0, 0))
    d = ImageDraw.Draw(block)
    top = 14
    if author == "alice":
        block.alpha_composite(ALICE_AVATAR, (18, top + 2))
        d.text((TEXT_X, top), "Alice", font=F_NAME, fill=ALICE_COLOR)
        x = TEXT_X + F_NAME.getlength("Alice") + 6
        d.rounded_rectangle((x, top + 3, x + 20, top + 17), radius=3, fill=TAG_BG)
        d.text((x + 10, top + 10), "AI", font=F_TAG, fill=(255, 255, 255), anchor="mm")
        x += 28
    else:
        block.alpha_composite(MIA_AVATAR, (18, top + 2))
        d.text((TEXT_X, top), "Mia", font=F_NAME, fill=MIA_COLOR)
        x = TEXT_X + F_NAME.getlength("Mia") + 8
    d.text((x, top + 3), "Today at 14:32", font=F_TIME, fill=MUTED)
    y = top + 22
    for rich in rich_parts:
        y = draw_rich(block, rich, TEXT_X, y, TEXT_W)
    if card:
        c = export_card(560)
        block.alpha_composite(c, (TEXT_X, y + 6))
        y += c.height + 8
    if jumbo:
        block.alpha_composite(emote(jumbo, JUMBO_EMOTE), (TEXT_X, y + 4))
        y += JUMBO_EMOTE + 6
    return block.crop((0, 0, W, y + 6))


MIA_1 = "Hey Alice! My Discord button shows up in the layout editor, but not in the actual menu. What am I doing wrong?"
MIA_2 = "Here you go!"
MIA_3 = "That was it! Thank you so much!"

MESSAGES = {
    "mia_1": message_block("mia", [[("text", MIA_1)]]),
    "alice_1": message_block("alice", [[
        ("text", "Let's find out! "), ("emote", "thinking"),
        ("text", " In the layout editor, right-click your button, open "), ("bold", "Export for AI"),
        ("text", " and click "), ("bold", "Copy to Clipboard"),
        ("text", ". Then paste it here, so I can see exactly how it's set up."),
    ]]),
    "mia_2": message_block("mia", [[("text", MIA_2)]], card=True),
    "alice_2": message_block("alice", [[
        ("text", "Found it! "), ("emote", "working"),
        ("text", " Your button has the "), ("bold", "Is Window Width Bigger Than"),
        ("text", " requirement set to "), ("code", "1920"),
        ("text", ", so it only shows in windows wider than 1920 pixels. The layout editor always shows all elements, that's why you still see it there."),
    ], [
        ("text", "Lower the value or remove the requirement, and your button is back! "), ("emote", "happy"),
    ]]),
    "mia_3": message_block("mia", [[("text", MIA_3)]]),
    "alice_3": message_block("alice", [[("text", "Yay! Happy building!")]], jumbo="victory"),
}


# ---- Timeline ----

frames = []  # each: dict(state)
state = {"shown": [], "composer": "", "chip": False, "typing": None, "badge": None}


def hold(n):
    for _ in range(n):
        frames.append(dict(state, shown=list(state["shown"])))


def type_into_composer(text, cps=40):
    per_frame = cps / FPS
    count = 0.0
    while int(count) < len(text):
        count += per_frame
        state["composer"] = text[:int(count)]
        hold(1)
    state["composer"] = text


def send(message):
    state["composer"] = ""
    state["chip"] = False
    state["shown"].append(message)


def typing(who, n):
    state["typing"] = who
    hold(n)
    state["typing"] = None


hold(8)
type_into_composer(MIA_1)
hold(4)
send("mia_1")
hold(8)
typing("Alice", 18)
send("alice_1")
hold(42)
type_into_composer(MIA_2)
hold(3)
state["badge"] = "Ctrl + V"
hold(4)
state["chip"] = True
hold(8)
state["badge"] = None
send("mia_2")
hold(18)
typing("Alice", 22)
send("alice_2")
hold(56)
type_into_composer(MIA_3)
hold(4)
send("mia_3")
hold(6)
typing("Alice", 14)
send("alice_3")
hold(36)


# ---- Rendering ----

def draw_header(image):
    d = ImageDraw.Draw(image)
    d.rectangle((0, 0, W, HEADER_H), fill=HEADER_BG)
    d.line((0, HEADER_H - 1, W, HEADER_H - 1), fill=DIVIDER, width=2)
    d.text((22, HEADER_H // 2), "#", font=font(22, "Medium"), fill=MUTED, anchor="lm")
    d.text((46, HEADER_H // 2), "support", font=F_HEADER, fill=MUTED, anchor="lm")
    x = 46 + F_HEADER.getlength("support") + 10
    d.text((x, HEADER_H // 2), "›", font=F_HEADER, fill=MUTED, anchor="lm")
    d.text((x + 16, HEADER_H // 2), "My Discord button only shows in the editor", font=F_HEADER, fill=BRIGHT, anchor="lm")


def draw_composer(image, st, frame_index):
    d = ImageDraw.Draw(image)
    box = (16, H - COMPOSER_H + 8, W - 16, H - 26)
    d.rounded_rectangle(box, radius=10, fill=COMPOSER_BG)
    cy = (box[1] + box[3]) // 2
    d.ellipse((box[0] + 12, cy - 11, box[0] + 34, cy + 11), fill=(181, 186, 193))
    d.text((box[0] + 23, cy - 1), "+", font=font(20, "Bold"), fill=COMPOSER_BG, anchor="mm")
    tx = box[0] + 48
    if st["chip"]:
        cw = 116
        d.rounded_rectangle((tx, cy - 13, tx + cw, cy + 13), radius=6, fill=CODE_BG)
        file_icon(d, tx + 8, cy - 9, (120, 130, 220, 255))
        d.text((tx + 30, cy), "message.txt", font=F_TIME, fill=BRIGHT, anchor="lm")
        tx += cw + 10
    text = st["composer"]
    if text:
        # Long messages scroll sideways in the one-line composer, so its end stays visible
        max_w = box[2] - tx - 20
        shown = text
        while F_TEXT.getlength(shown) > max_w:
            shown = shown[1:]
        d.text((tx, cy), shown, font=F_TEXT, fill=TEXT, anchor="lm")
        caret_x = tx + F_TEXT.getlength(shown) + 2
        if (frame_index // 5) % 2 == 0:
            d.line((caret_x, cy - 10, caret_x, cy + 10), fill=TEXT, width=2)
    elif not st["chip"]:
        d.text((tx, cy), "Message My Discord button only shows in the editor", font=F_TEXT, fill=(109, 111, 120), anchor="lm")
    if st["typing"]:
        y = H - 13
        for i in range(3):
            phase = (frame_index * 0.9 - i * 0.9)
            lift = max(0.0, math.sin(phase)) * 2.5
            alpha = 150 + int(105 * max(0.0, math.sin(phase)))
            d.ellipse((20 + i * 9, y - 3 - lift, 26 + i * 9, y + 3 - lift), fill=(alpha, alpha, alpha))
        d.text((54, y), st["typing"], font=font(12, "Bold"), fill=BRIGHT, anchor="lm")
        d.text((54 + font(12, "Bold").getlength(st["typing"]) + 4, y), "is typing…", font=F_TIME, fill=TEXT, anchor="lm")


def draw_badge(image, label):
    d = ImageDraw.Draw(image)
    tw = F_BADGE.getlength(label)
    bw, bh = tw + 32, 38
    x, y = (W - bw) / 2, H - COMPOSER_H - bh - 14
    d.rounded_rectangle((x + 2, y + 3, x + bw + 2, y + bh + 3), radius=9, fill=(0, 0, 0, 90))
    d.rounded_rectangle((x, y, x + bw, y + bh), radius=9, fill=(32, 34, 38, 235), outline=(110, 114, 122), width=2)
    d.text((x + bw / 2, y + bh / 2), label, font=F_BADGE, fill=(255, 255, 255), anchor="mm")


offset = 0.0
appear = {}
for index, st in enumerate(frames):
    image = Image.new("RGBA", (W, H), BG + (255,))
    blocks = [MESSAGES[m] for m in st["shown"]]
    total = sum(b.height for b in blocks)
    area = CHAT_BOTTOM - CHAT_TOP - 8
    # The chat sticks to the bottom like a real chat, and glides up when a message comes in
    target = max(0, total - area)
    offset += (target - offset) * 0.45
    if abs(target - offset) < 0.5:
        offset = target
    chat = Image.new("RGBA", (W, CHAT_BOTTOM - CHAT_TOP), (0, 0, 0, 0))
    y = (area - total) if total < area else (area - total + (target - offset))
    for name, block in zip(st["shown"], blocks):
        first = appear.setdefault(name, index)
        alpha = min(1.0, (index - first + 1) / 3)
        if alpha < 1.0:
            faded = block.copy()
            faded.putalpha(faded.getchannel("A").point(lambda a: int(a * alpha)))
            chat.alpha_composite(faded, (0, int(y)))
        else:
            chat.alpha_composite(block, (0, int(y)))
        y += block.height
    image.alpha_composite(chat, (0, CHAT_TOP))
    draw_header(image)
    draw_composer(image, st, index)
    if st["badge"]:
        draw_badge(image, st["badge"])
    image.convert("RGB").save(os.path.join(OUT_DIR, f"c_{index:05d}.png"))

print(len(frames), "frames,", len(frames) / FPS, "seconds")
