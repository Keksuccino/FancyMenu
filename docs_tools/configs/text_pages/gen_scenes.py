"""Writes the scene config of the Text Formatting and Text in Menus screenshots.

Usage: python3 gen_scenes.py [output json, default: scenes.json next to this script]

Every scene is one screenshot. Elements are Text elements anchored top-left, in GUI pixels.
"""
import json, os, sys
looks_text = lambda name: f"""# {name}
Our modpack got a **big update**!

+ Added _shiny_ new menus
- Removed the old logo
~ Changed the `button` colors

> [!TIP]
> Press ++F11++ for fullscreen."""

showcase = """# Welcome to Skyblock!
Thanks for playing! Here are the **most important** things to know.

## What's New
+ Added a _brand new_ quest book
- Removed the old ++spawn island++
~ Changed the ~~iron~~ diamond prices
* Fixed the crash on login

> [!WARNING]
> Always back up your worlds before updating!

Join us on [Discord](https://discord.gg) and type `/help` in-game."""

lists = """### Bullet List
- Diamonds
- Emeralds
  - Rare ones
    - Even rarer

### Numbered List
1. Open the menu bar
2. Click on Customization
3. Have fun!

### Patch Notes
+ Added a new main menu
- Removed the old logo
~ Changed the button colors
* Fixed a crash on startup"""

callouts = """> [!NOTE]
> Good to know, even when skimming.

> [!TIP]
> Hold Shift to scroll sideways.

> [!IMPORTANT]
> Save your layout before closing the editor.

> [!WARNING]
> This button resets all your settings!

> [!CAUTION]
> Only import profiles from people you trust."""

table_code = """### Shop Prices
| Item | Price | Stock |
|:---|:---:|---:|
| Diamond Sword | 10 | 3 |
| Golden Apple | 5 | 12 |
| Ender Pearl | 2 | 64 |

### Code
Type `/spawn` to get back home.
```
/give @p minecraft:diamond 64
```"""

config = {"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": [
  {"name": "looks", "wait": 4, "elements": [
     {"x": 12, "y": 12, "w": 200, "h": 142, "look": "pixel", "content": looks_text("Pixel")},
     {"x": 222, "y": 12, "w": 200, "h": 142, "look": "classic", "content": looks_text("Classic")},
     {"x": 432, "y": 12, "w": 200, "h": 142, "look": "neutral", "content": looks_text("Neutral")}]},
  {"name": "showcase", "wait": 3, "elements": [{"x": 12, "y": 12, "w": 400, "h": 176, "look": "pixel", "content": showcase}]},
  {"name": "lists", "wait": 3, "elements": [{"x": 12, "y": 12, "w": 210, "h": 196, "look": "pixel", "content": lists}]},
  {"name": "callouts", "wait": 3, "elements": [{"x": 12, "y": 12, "w": 300, "h": 186, "look": "pixel", "content": callouts}]},
  {"name": "table_code", "wait": 3, "elements": [{"x": 12, "y": 12, "w": 300, "h": 150, "look": "pixel", "content": table_code}]},
]}
target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "scenes.json")
json.dump(config, open(target, "w"), indent=1)
