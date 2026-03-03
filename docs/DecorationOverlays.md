---
title: Decoration Overlays
description: Add fullscreen visual overlays to menus in the FancyMenu layout editor.
---

# Decoration Overlays

Decoration Overlays are fullscreen effects that render in front of your menu elements.

They are useful when you want to add atmosphere or motion to a menu without building those effects manually.

Common examples:

- Add **Snowfall** for a winter menu with snow buildup.
- Add **Rainfall** for a storm look with puddles and drips.
- Add **Fireflies** for a calm night-style menu.
- Add **String Lights** for festive or decorative menu themes.
- Add **Leaves**, **Fireworks**, or **Confetti** for seasonal/event menus.
- Add the **Browser** overlay to show a fullscreen webpage/video layer.

# Where To Find It

Open a layout in the layout editor, then right-click the editor background and open **Decoration Overlays**.

# Quick Start

1. Open a layout in the layout editor.
2. Right-click the background (empty area).
3. Open **Decoration Overlays**.
4. Select an overlay type.
5. Set **Show Overlay** to **Enabled**.
6. Configure the overlay settings.
7. Save the layout and test the screen.

# How Overlay Types Work

Each overlay type has its own submenu and its own **Show Overlay** toggle.

- You can enable only the types you want.
- You can combine multiple enabled types in one layout.
- Settings are per overlay type (for example color, intensity, speed, density, scale, special behavior).

> [!INFO]
> It is possible to stack multiple instances of the same overlay type by using multiple layouts with the same type enabled.

# Overlay Types

- **Snowfall**: snowfall with optional snow accumulation on surfaces/buttons.
- **Rainfall**: rain with optional puddles, drips, and optional thunder flashes.
- **Fireflies**: moving firefly groups with configurable group amount, density, size, and color.
- **String Lights**: configurable string combinations, light colors, wind/flicker behavior, and holiday color mode.
- **Leaves**: falling leaves with configurable colors, wind, speed, scale, and density.
- **Fireworks**: frequent fireworks with configurable amount, explosion size, and scale.
- **Confetti**: confetti rain with optional mouse-click confetti mode.
- **Browser**: fullscreen browser overlay with URL and media settings.
- **GLSL Shader**: fullscreen custom shader overlay (for animated or static shader-based visuals).

# Browser Overlay: Interactive vs Passive

The Browser overlay can be configured either as an interactive browser or as a passive visual layer.

- **Process Mouse/Keyboard** settings control whether the browser itself handles input.
- **Consume Mouse/Keyboard** settings control whether input is blocked from the menu behind it.

Practical setup examples:

- Interactive browser in front: enable both **Process** and **Consume**.
- Visual-only browser layer: disable **Process** and disable **Consume**.

> [!IMPORTANT]
> The Browser decoration overlay requires the **MCEF** mod.
