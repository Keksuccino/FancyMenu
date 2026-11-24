"""Renders the frames of the mockup chat GIF of the Export for AI page: a player asks Alice for help in a forum post and sends an export.

This is the one docs image that is no in-game render, because it shows a chat outside of the game. It copies the look of Alice's
support posts on the Discord server: a purple forum post with a big title, a tag, the OP message, Alice's replies that start with
one of her emotes, and her buttons under her latest reply.
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

W, H = 800, 620
COMPOSER_H = 78
CHAT_TOP, CHAT_BOTTOM = 0, H - COMPOSER_H
FPS = 10

BG = (55, 21, 64)
DARK = (38, 13, 45)
RAISED = (70, 33, 80)
RAISED_BORDER = (88, 50, 98)
DIVIDER = (78, 42, 88)
TEXT = (236, 231, 241)
BRIGHT = (250, 247, 252)
MUTED = (178, 160, 188)
ALICE_COLOR = (243, 132, 232)
MIA_COLOR = (142, 226, 92)
APP_BG = (88, 101, 242)
OP_BG = (104, 110, 214)
OP_TEXT = (214, 218, 255)
CODE_TEXT = (240, 226, 240)
LINK = (96, 186, 255)

SF = "/System/Library/Fonts/SFNS.ttf"
MONO = "/System/Library/Fonts/SFNSMono.ttf"


def font(size, weight="Regular", path=SF):
    f = ImageFont.truetype(path, size)
    f.set_variation_by_name(weight)
    return f


F_TEXT = font(16)
F_BOLD = font(16, "Bold")
F_NAME = font(16, "Semibold")
F_TIME = font(12)
F_TITLE = font(28, "Bold")
F_CHIP = font(13, "Semibold")
F_DATE = font(12.5, "Semibold")
F_CODE = font(14, "Regular", MONO)
F_MONO_SMALL = font(12.5, "Regular", MONO)
F_TAG = font(11, "Bold")
F_BUTTON = font(14, "Semibold")
F_BADGE = font(17, "Semibold")

LINE_H = 24
INLINE_EMOTE = 34
EMOTE_LINE_H = 36
TEXT_X = 74
TEXT_W = W - TEXT_X - 30


def emote(name, size):
    image = Image.open(os.path.join(EMOTES_DIR, f"alice_{name}.png")).convert("RGBA")
    return image.resize((size, size), Image.LANCZOS)


def circle(image, size):
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
    d.text((size * 2, size * 2), letter, font=font(size * 2, "Semibold"), fill=(255, 255, 255), anchor="mm")
    return big.resize((size, size), Image.LANCZOS)


ALICE_AVATAR = circle(Image.open(os.path.join(EMOTES_DIR, "alice_working_maniac.png")).convert("RGBA"), 40)
MIA_AVATAR = letter_avatar("M", (232, 132, 54), 40)


# ---- Rich text: (kind, value) parts with kind text/bold/code/emote, and ("break", "") for an empty line ----

def tokenize(rich):
    tokens = []
    for kind, value in rich:
        if kind in ("emote", "break"):
            tokens.append((kind, value))
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
        return INLINE_EMOTE + 4
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
        if kind == "break":
            lines.append(line)
            lines.append([])
            line, x = [], 0
            continue
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
    y = y0
    for line in layout_rich(rich, width):
        if not line:
            y += 12
            continue
        # Lines with an emote get taller, so the emote is big enough to see what Alice does
        line_h = EMOTE_LINE_H if any(kind == "emote" for kind, _, _ in line) else LINE_H
        ty = y + (line_h - LINE_H) // 2
        for kind, value, x in line:
            if kind == "emote":
                image.alpha_composite(emote(value, INLINE_EMOTE), (int(x0 + x), int(y + (line_h - INLINE_EMOTE) // 2)))
            elif kind == "bold":
                d.text((x0 + x, ty + 2), value, font=F_BOLD, fill=BRIGHT)
            elif kind == "code":
                cw = F_CODE.getlength(value)
                d.rounded_rectangle((x0 + x, ty + 1, x0 + x + cw + 8, ty + 22), radius=4, fill=DARK, outline=RAISED_BORDER)
                d.text((x0 + x + 4, ty + 5), value, font=F_CODE, fill=CODE_TEXT)
            elif kind == "text":
                d.text((x0 + x, ty + 2), value, font=F_TEXT, fill=TEXT)
        y += line_h
    return y


# ---- Icons, drawn with shapes so they need no emoji ----

def file_icon(d, x, y):
    d.polygon([(x, y), (x + 14, y), (x + 20, y + 6), (x + 20, y + 26), (x, y + 26)], fill=(140, 128, 230))
    d.polygon([(x + 14, y), (x + 20, y + 6), (x + 14, y + 6)], fill=(200, 192, 255))
    for i in range(3):
        d.line((x + 4, y + 12 + i * 4, x + 16, y + 12 + i * 4), fill=(235, 232, 255), width=1)


def icon(d, name, x, cy, color):
    if name == "play":
        d.polygon([(x, cy - 6), (x + 10, cy), (x, cy + 6)], fill=color)
    elif name == "check":
        d.line((x, cy, x + 4, cy + 5, x + 13, cy - 6), fill=color, width=3)
    elif name == "lines":
        for i, w in enumerate((14, 10, 14)):
            d.line((x, cy - 5 + i * 5, x + w, cy - 5 + i * 5), fill=color, width=2)
        d.line((x + 12, cy - 1, x + 18, cy - 1), fill=color, width=2)
        d.line((x + 15, cy - 4, x + 15, cy + 2), fill=color, width=2)
    elif name == "gear":
        cx = x + 8
        for i in range(8):
            a = i * math.pi / 4
            d.ellipse((cx + math.cos(a) * 6.5 - 2.2, cy + math.sin(a) * 6.5 - 2.2, cx + math.cos(a) * 6.5 + 2.2, cy + math.sin(a) * 6.5 + 2.2), fill=color)
        d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=color)
        d.ellipse((cx - 2.5, cy - 2.5, cx + 2.5, cy + 2.5), fill=RAISED)
    elif name == "megaphone":
        d.polygon([(x, cy - 3), (x + 5, cy - 3), (x + 13, cy - 8), (x + 13, cy + 8), (x + 5, cy + 3), (x, cy + 3)], fill=color)
        d.line((x + 3, cy + 3, x + 5, cy + 8), fill=color, width=2)
    elif name == "bubble":
        d.rounded_rectangle((x, cy - 7, x + 16, cy + 5), radius=5, outline=color, width=2)
        d.polygon([(x + 3, cy + 4), (x + 3, cy + 9), (x + 8, cy + 4)], fill=color)
    elif name == "pencil":
        d.line((x, cy + 6, x + 11, cy - 5), fill=color, width=4)
        d.polygon([(x - 2, cy + 8), (x - 1, cy + 3), (x + 3, cy + 7)], fill=color)
    elif name == "plus":
        d.line((x - 7, cy, x + 7, cy), fill=color, width=2)
        d.line((x, cy - 7, x, cy + 7), fill=color, width=2)
    elif name == "smiley":
        d.ellipse((x, cy - 10, x + 20, cy + 10), fill=color)
        d.ellipse((x + 6, cy - 4, x + 8, cy - 1), fill=RAISED)
        d.ellipse((x + 12, cy - 4, x + 14, cy - 1), fill=RAISED)
        d.arc((x + 5, cy - 3, x + 15, cy + 6), 20, 160, fill=RAISED, width=2)
    elif name == "apps":
        for dx, dy in ((0, -9), (9, -9), (0, 0), (9, 0)):
            d.rounded_rectangle((x + dx, cy + dy, x + dx + 7, cy + dy + 7), radius=2, fill=color)
    elif name == "send":
        d.polygon([(x, cy - 9), (x + 18, cy), (x, cy + 9), (x + 4, cy)], fill=color)


# ---- Blocks of the post ----

def header_block(title):
    block = Image.new("RGBA", (W, 230), (0, 0, 0, 0))
    d = ImageDraw.Draw(block)
    d.ellipse((24, 22, 88, 86), fill=(26, 9, 31))
    d.ellipse((40, 38, 72, 66), fill=(225, 222, 228))
    d.polygon([(43, 58), (41, 72), (54, 63)], fill=(225, 222, 228))
    d.text((24, 102), title, font=F_TITLE, fill=BRIGHT)
    cw = F_CHIP.getlength("FancyMenu") + 42
    d.rounded_rectangle((24, 148, 24 + cw, 176), radius=14, fill=RAISED, outline=RAISED_BORDER)
    d.rounded_rectangle((36, 155, 50, 169), radius=4, fill=(236, 84, 196))
    d.text((58, 162), "FancyMenu", font=F_CHIP, fill=BRIGHT, anchor="lm")
    date = "November 24, 2026"
    dw = F_DATE.getlength(date)
    d.line((24, 206, W / 2 - dw / 2 - 10, 206), fill=DIVIDER, width=1)
    d.line((W / 2 + dw / 2 + 10, 206, W - 24, 206), fill=DIVIDER, width=1)
    d.text((W / 2, 206), date, font=F_DATE, fill=MUTED, anchor="mm")
    return block.crop((0, 0, W, 216))


def name_line(d, block, author, top, time):
    if author == "alice":
        block.alpha_composite(ALICE_AVATAR, (20, top + 2))
        d.text((TEXT_X, top), "Alice", font=F_NAME, fill=ALICE_COLOR)
        x = TEXT_X + F_NAME.getlength("Alice") + 6
        d.rounded_rectangle((x, top + 2, x + 34, top + 18), radius=4, fill=APP_BG)
        d.text((x + 17, top + 10), "APP", font=F_TAG, fill=(255, 255, 255), anchor="mm")
        x += 42
    else:
        block.alpha_composite(MIA_AVATAR, (20, top + 2))
        d.text((TEXT_X, top), "Mia", font=F_NAME, fill=MIA_COLOR)
        x = TEXT_X + F_NAME.getlength("Mia") + 6
        if author == "mia_op":
            d.rounded_rectangle((x, top + 2, x + 26, top + 18), radius=4, fill=OP_BG)
            d.text((x + 13, top + 10), "OP", font=F_TAG, fill=OP_TEXT, anchor="mm")
            x += 34
    d.text((x, top + 3), time, font=F_TIME, fill=MUTED)


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
    d.rounded_rectangle((0, 0, width - 1, card_h - 1), radius=8, fill=DARK, outline=RAISED_BORDER)
    file_icon(d, 14, 12)
    d.text((44, 12), "message.txt", font=F_BOLD, fill=LINK)
    d.text((44, 32), "3.4 KB", font=F_TIME, fill=MUTED)
    d.line((12, 52, width - 12, 52), fill=RAISED_BORDER, width=1)
    y = 60
    for line in EXPORT_PREVIEW:
        d.text((14, y), line, font=F_MONO_SMALL, fill=(222, 210, 228))
        y += 15
    # The preview fades out at the bottom, like the rest of the file is still there
    fade = Image.new("RGBA", (width - 2, 36), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fade)
    for i in range(36):
        fd.line((0, i, width, i), fill=DARK + (int(255 * min(1, i / 30)),))
    card.alpha_composite(fade, (1, card_h - 37))
    return card


ALICE_BUTTONS = [
    [("play", "Resume", True), ("megaphone", "Request Human", False), ("bubble", "Disable AI", False)],
    [("check", "Mark Solved", False), ("lines", "Summarize", False), ("gear", "Settings", False)],
]


def alice_buttons(block, y):
    d = ImageDraw.Draw(block)
    d.line((TEXT_X, y + 8, W - 60, y + 8), fill=DIVIDER, width=1)
    y += 20
    for row in ALICE_BUTTONS:
        x = TEXT_X
        for name, label, disabled in row:
            bw = F_BUTTON.getlength(label) + 50
            d.rounded_rectangle((x, y, x + bw, y + 32), radius=6, fill=RAISED if not disabled else (64, 30, 73))
            color = MUTED if disabled else BRIGHT
            icon(d, name, x + 13, y + 16, color)
            d.text((x + 38, y + 16), label, font=F_BUTTON, fill=color, anchor="lm")
            x += bw + 8
        y += 40
    return y


def message_block(author, rich, time="14:32", card=False, buttons=False):
    block = Image.new("RGBA", (W, 700), (0, 0, 0, 0))
    d = ImageDraw.Draw(block)
    top = 14
    name_line(d, block, author, top, time)
    y = draw_rich(block, rich, TEXT_X, top + 22, TEXT_W)
    if card:
        c = export_card(560)
        block.alpha_composite(c, (TEXT_X, y + 6))
        y += c.height + 8
    if buttons:
        y = alice_buttons(block, y)
    if author == "mia_op":
        # The first message of a post ends with a thick line, like the post's own reaction bar
        d.rectangle((0, y + 14, W, y + 20), fill=(68, 32, 77))
        y += 20
    return block.crop((0, 0, W, y + 6))


def rename_block(title):
    block = Image.new("RGBA", (W, 60), (0, 0, 0, 0))
    d = ImageDraw.Draw(block)
    icon(d, "pencil", 34, 30, MUTED)
    x = TEXT_X
    for text, f, color in (("Alice", F_TEXT, ALICE_COLOR), (" changed the post title to ", F_TEXT, MUTED), (title, F_BOLD, MUTED), ("  14:32", F_TIME, MUTED)):
        d.text((x, 30), text, font=f, fill=color, anchor="lm")
        x += f.getlength(text)
    return block.crop((0, 0, W, 50))


OLD_TITLE = "discord button not showing??"
NEW_TITLE = "Discord Button Only Shows in the Editor"
MIA_1 = "My Discord button shows up in the layout editor, but not in the actual menu. What am I doing wrong?"
MIA_2 = "Here you go!"
MIA_3 = "That was it! Thank you so much!"

ALICE_1 = [
    ("emote", "waving"), ("text", " Hi Mia! To see what's going on, I need to know how your button is set up. In the layout editor, right-click the button, open "),
    ("bold", "Export for AI"), ("text", " and click "), ("bold", "Copy to Clipboard"), ("text", ". Then paste it here!"),
]
ALICE_2 = [
    ("emote", "working"), ("text", " Found it! Your button has the "), ("bold", "Is Window Width Bigger Than"),
    ("text", " requirement set to "), ("code", "1920"),
    ("text", ", so it only shows when the game window is wider than 1920 pixels. The layout editor always shows all elements, that's why you still see it there."),
    ("break", ""),
    ("text", "Lower the value or remove the requirement, and your button is back!"),
]
ALICE_3 = [("emote", "victory"), ("text", " Yay! Happy building!")]

BLOCKS = {
    "header_old": header_block(OLD_TITLE),
    "header_new": header_block(NEW_TITLE),
    "mia_1": message_block("mia_op", [("text", MIA_1)], time="14:31"),
    "rename": rename_block(NEW_TITLE),
    "alice_1": message_block("alice", ALICE_1),
    "alice_1+": message_block("alice", ALICE_1, buttons=True),
    "mia_2": message_block("mia", [("text", MIA_2)], card=True, time="14:33"),
    "alice_2": message_block("alice", ALICE_2, time="14:33"),
    "alice_2+": message_block("alice", ALICE_2, time="14:33", buttons=True),
    "mia_3": message_block("mia", [("text", MIA_3)], time="14:34"),
    "alice_3+": message_block("alice", ALICE_3, time="14:34", buttons=True),
}


# ---- Timeline ----

frames = []
state = {"shown": [], "title": "old", "composer": "", "chip": False, "typing": None, "badge": None}


def hold(n):
    for _ in range(n):
        frames.append(dict(state, shown=list(state["shown"])))


def type_into_composer(text, cps=36):
    count = 0.0
    while int(count) < len(text):
        count += cps / FPS
        state["composer"] = text[:int(count)]
        hold(1)


def send(message):
    state["composer"] = ""
    state["chip"] = False
    state["shown"].append(message)


def typing(who, n):
    state["typing"] = who
    hold(n)
    state["typing"] = None


# Mia just made the post
state["shown"] = ["mia_1"]
hold(18)
# Alice gives the post a clear title, then answers
state["title"] = "new"
send("rename")
hold(8)
typing("Alice", 16)
send("alice_1")
hold(44)
type_into_composer(MIA_2)
hold(3)
state["badge"] = "Ctrl + V"
hold(4)
state["chip"] = True
hold(8)
state["badge"] = None
send("mia_2")
hold(16)
typing("Alice", 22)
send("alice_2")
hold(58)
type_into_composer(MIA_3)
hold(4)
send("mia_3")
hold(6)
typing("Alice", 14)
send("alice_3")
hold(34)


# ---- Rendering ----

def draw_composer(image, st, frame_index, title):
    d = ImageDraw.Draw(image)
    d.rectangle((0, CHAT_BOTTOM, W, H), fill=BG)
    box = (16, H - COMPOSER_H + 8, W - 16, H - 26)
    d.rounded_rectangle(box, radius=10, fill=(66, 30, 77), outline=(84, 46, 95))
    cy = (box[1] + box[3]) // 2
    icon(d, "plus", box[0] + 24, cy, (200, 186, 210))
    for name, x in (("smiley", box[2] - 112), ("apps", box[2] - 72)):
        icon(d, name, x, cy, (200, 186, 210))
    d.line((box[2] - 50, cy - 12, box[2] - 50, cy + 12), fill=(96, 58, 106), width=1)
    icon(d, "send", box[2] - 34, cy, (150, 128, 162))
    tx = box[0] + 50
    if st["chip"]:
        cw = 116
        d.rounded_rectangle((tx, cy - 13, tx + cw, cy + 13), radius=6, fill=DARK)
        file_icon(d, tx + 8, cy - 13)
        d.text((tx + 34, cy), "message.txt", font=F_TIME, fill=BRIGHT, anchor="lm")
        tx += cw + 10
    text = st["composer"]
    max_w = box[2] - 130 - tx
    if text:
        # Long messages scroll sideways in the one-line composer, so its end stays visible
        shown = text
        while F_TEXT.getlength(shown) > max_w:
            shown = shown[1:]
        d.text((tx, cy), shown, font=F_TEXT, fill=TEXT, anchor="lm")
        caret_x = tx + F_TEXT.getlength(shown) + 2
        if (frame_index // 5) % 2 == 0:
            d.line((caret_x, cy - 10, caret_x, cy + 10), fill=TEXT, width=2)
    elif not st["chip"]:
        d.text((tx, cy), f"Message \"{title}\"", font=F_TEXT, fill=(150, 128, 162), anchor="lm")
    if st["typing"]:
        y = H - 13
        for i in range(3):
            phase = frame_index * 0.9 - i * 0.9
            lift = max(0.0, math.sin(phase)) * 2.5
            v = 150 + int(105 * max(0.0, math.sin(phase)))
            d.ellipse((20 + i * 9, y - 3 - lift, 26 + i * 9, y + 3 - lift), fill=(v, v, v))
        bold = font(12, "Bold")
        d.text((54, y), st["typing"], font=bold, fill=BRIGHT, anchor="lm")
        d.text((54 + bold.getlength(st["typing"]) + 4, y), "is typing…", font=F_TIME, fill=TEXT, anchor="lm")


def draw_badge(image, label):
    d = ImageDraw.Draw(image)
    bw, bh = F_BADGE.getlength(label) + 32, 38
    x, y = (W - bw) / 2, H - COMPOSER_H - bh - 14
    d.rounded_rectangle((x + 2, y + 3, x + bw + 2, y + bh + 3), radius=9, fill=(0, 0, 0, 90))
    d.rounded_rectangle((x, y, x + bw, y + bh), radius=9, fill=(32, 16, 38, 235), outline=(140, 110, 150), width=2)
    d.text((x + bw / 2, y + bh / 2), label, font=F_BADGE, fill=(255, 255, 255), anchor="mm")


def block_names(st):
    names = ["header_" + st["title"]] + st["shown"]
    # Alice's buttons are only under her latest reply
    last_alice = max((i for i, n in enumerate(names) if n.startswith("alice_")), default=None)
    if last_alice is not None and not names[last_alice].endswith("+"):
        names[last_alice] += "+"
    return [n if (not n.endswith("+") or i == last_alice) else n[:-1] for i, n in enumerate(names)]


offset = 0.0
appear = {}
area = CHAT_BOTTOM - CHAT_TOP - 6
for index, st in enumerate(frames):
    image = Image.new("RGBA", (W, H), BG + (255,))
    names = block_names(st)
    blocks = [BLOCKS[n] for n in names]
    total = sum(b.height for b in blocks)
    # The post starts at the top, and once it's longer than the window, it sticks to the bottom and glides up for new messages
    target = max(0, total - area)
    offset += (target - offset) * 0.45
    if abs(target - offset) < 0.5:
        offset = target
    y = -offset
    for name, block in zip(names, blocks):
        # The header only changes its title, so it doesn't fade in again
        key = "header" if name.startswith("header") else name.rstrip("+")
        first = appear.setdefault(key, index)
        alpha = min(1.0, (index - first + 1) / 3)
        if alpha < 1.0:
            faded = block.copy()
            faded.putalpha(faded.getchannel("A").point(lambda a: int(a * alpha)))
            image.alpha_composite(faded, (0, int(y)))
        else:
            image.alpha_composite(block, (0, int(y)))
        y += block.height
    draw_composer(image, st, index, NEW_TITLE if st["title"] == "new" else OLD_TITLE)
    if st["badge"]:
        draw_badge(image, st["badge"])
    image.convert("RGB").save(os.path.join(OUT_DIR, f"c_{index:05d}.png"))

print(len(frames), "frames,", len(frames) / FPS, "seconds")
