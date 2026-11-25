"""Writes the configs of the Menu Backgrounds page images. All of them use the Frostvale art of make_art.py:

- scenes.json: stills at 1280x720 (GUI 640x360, scale 2):
  - hero: the title screen with Frostvale as Image background.
  - stack_image / stack_color: the see-through version of Frostvale alone (its sky shows black) and over a Color background.
  - effect_none / effect_overlay / effect_blur: Frostvale without effects, with Show Dark Overlay On Custom Background and with
    Apply Vanilla Background Blur.
- image.json: the GIF of setting Frostvale as Image background of the title screen in the layout editor (menu_background).
- menu_still.json: the still of the open Menu Backgrounds menu (menu_path_still).

Copy frostvale/ of make_art.py to run_client_docs/config/fancymenu/assets/ first.

Usage: python3 gen_configs.py <out dir>
"""

import json
import sys
from pathlib import Path

ASSETS = '/config/fancymenu/assets/frostvale/'
BACKGROUND = ASSETS + 'background.png'
LAND = ASSETS + 'land.png'
SKY_COLOR = '#4A5A9A'
HIDDEN_POINTER = [-40, -40]


def scene(name, **keys):
    data = {'name': name, 'wait': 3, 'pointer': HIDDEN_POINTER, 'elements': []}
    data.update(keys)
    return data


def main():
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)

    scenes = {'gui_width': 640, 'gui_height': 360, 'gui_scale': 2, 'scenes': [
        scene('hero', screen='title', background_image=BACKGROUND),
        scene('stack_image', background_image=LAND),
        scene('stack_color', background_image=LAND, background_color=SKY_COLOR),
        scene('effect_none', background_image=BACKGROUND),
        scene('effect_overlay', background_image=BACKGROUND, background_overlay=True),
        scene('effect_blur', background_image=BACKGROUND, background_blur=True),
    ]}

    image = {
        'script': 'menu_background', 'screen': 'title', 'elements': [],
        'right_click': [96, 292], 'folder': 'frostvale', 'file': 'background.png', 'free_spot': [560, 300], 'hold_ticks': 70,
    }

    menu_still = {
        'script': 'menu_path_still', 'screen': 'title', 'elements': [], 'background_image': BACKGROUND,
        'pointer': [520, 300], 'right_click': [96, 292],
        'path': ['menu_backgrounds'], 'rest_on': 'keep_background_aspect_ratio',
    }

    for name, config in [('scenes', scenes), ('image', image), ('menu_still', menu_still)]:
        (out / f'{name}.json').write_text(json.dumps(config, indent=1))
    print('configs in', out)


if __name__ == '__main__':
    main()
