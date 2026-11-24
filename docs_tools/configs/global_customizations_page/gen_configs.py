"""Configs of the Global Customizations page: a tour through vanilla screens (Title screen, Options, Music & Sounds) without layouts,
with global wood button and brass slider textures (make_textures.py), cream labels that turn gold on hover and a custom menu background.

Usage: python3 gen_configs.py <out dir>
Before recording, copy the textures of make_textures.py to run_client_docs/config/fancymenu/assets/global_style/ and docs_bg_night.png
of docs_tools/assets to run_client_docs/config/fancymenu/assets/.
"""
import json
import sys
from pathlib import Path

STYLE = "[source:local]/config/fancymenu/assets/global_style/"
LABEL = "#FFF1D6"
LABEL_HOVER = "#FFD35C"

OPTIONS = {
    "global_button_background_normal": STYLE + "button_normal.png",
    "global_button_background_hover": STYLE + "button_hover.png",
    "global_button_background_inactive": STYLE + "button_inactive.png",
    "global_button_background_nine_slice": True,
    "global_button_background_nine_slice_border_top": 4,
    "global_button_background_nine_slice_border_right": 4,
    "global_button_background_nine_slice_border_bottom": 4,
    "global_button_background_nine_slice_border_left": 4,
    "global_button_label_base_color": LABEL,
    "global_button_label_hover_color": LABEL_HOVER,
    "global_slider_background": STYLE + "slider_background.png",
    "global_slider_background_nine_slice": True,
    "global_slider_background_nine_slice_border_top": 4,
    "global_slider_background_nine_slice_border_right": 4,
    "global_slider_background_nine_slice_border_bottom": 4,
    "global_slider_background_nine_slice_border_left": 4,
    "global_slider_handle_normal": STYLE + "slider_handle_normal.png",
    "global_slider_handle_hover": STYLE + "slider_handle_hover.png",
    "global_slider_handle_inactive": STYLE + "slider_handle_inactive.png",
    "global_slider_label_base_color": LABEL,
    "global_slider_label_hover_color": LABEL_HOVER,
    "global_menu_background_texture": "[source:local]/config/fancymenu/assets/docs_bg_night.png",
}

TOUR = {
    "script": "screen_tour",
    "start": "screen",
    "screen": "title",
    "no_layout": True,
    "elements": [],
    "fancymenu_options": OPTIONS,
    "pointer": [560, 330],
    "tour": [
        {"hover": "menu.singleplayer", "ticks": 26},
        {"hover": "menu.multiplayer", "ticks": 22, "move_ticks": 12},
        {"click": "menu.options", "opens": True, "ticks": 24},
        {"drag": "options.fov", "from": 0.36, "to": 0.72, "ticks": 20},
        {"hover": "options.video", "ticks": 18},
        {"click": "options.sounds", "opens": True, "ticks": 24},
        {"drag": "soundCategory.weather", "from": 0.97, "to": 0.45, "ticks": 20},
        {"hover": "soundCategory.ambient", "ticks": 26},
    ],
    "hold_ticks": 70,
}


def main():
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    (out / "tour.json").write_text(json.dumps(TOUR, indent=1))


if __name__ == "__main__":
    main()
