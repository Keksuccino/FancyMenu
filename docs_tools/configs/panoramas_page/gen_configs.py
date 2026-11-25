"""Configs of the Panoramas page images. They need the forest_homestead panorama in the docs instance: copy
forest_homestead/ to run_client_docs/config/fancymenu/panoramas/ (see build_homestead.py for how it gets made).

- hero.json: the title screen without a layout, with forest_homestead as Custom Menu Background Panorama (Global Customizations).
  One of its last frames is the still of the page (make_stills.py).
- background.json: Menu Backgrounds -> Cubic Panorama in the editor, Show Background, and picking forest_homestead in Choose Panorama.

The title screen without a layout shows the FER Test button of the Fancy Entity Renderer mod in its top-right corner, so park
FER's jar outside of mods while recording hero.json, and put it back after.

Usage: python3 gen_configs.py <out dir>
"""
import json
import os
import sys

CONFIGS = {
    'hero': {
        'script': 'screen_tour', 'start': 'screen', 'screen': 'title', 'no_layout': True,
        'fancymenu_options': {'global_background_panorama': 'forest_homestead'},
        'pointer': [-40, -40], 'tour': [{'wait': 80}], 'hold_ticks': 4,
    },
    'background': {
        'script': 'panorama_background', 'screen': 'title', 'elements': [],
        'right_click': [96, 292], 'panorama': 'forest_homestead', 'preview_ticks': 50, 'free_spot': [560, 300], 'hold_ticks': 60,
    },
}


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, config in CONFIGS.items():
        with open(os.path.join(out, f'{name}.json'), 'w') as f:
            json.dump(config, f, indent=1)
        print(f'{name}.json')


if __name__ == '__main__':
    main()
