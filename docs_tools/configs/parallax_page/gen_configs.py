"""Writes the configs of the Parallax Effect page images. Usage: python3 gen_configs.py <out dir>

All of them show the title screen with the layers of make_layers.py behind the vanilla elements:
- scene.json: the GIF of the title screen with the pointer moving around (hover_elements).
- settings.json: the GIF of turning on the Parallax Effect of the foreground layer and setting its X intensity (element_settings).
- background_still.json: the still of Menu Backgrounds -> Image with the parallax settings (menu_path_still).

Copy the layers to run_client_docs/config/fancymenu/assets/parallax/ first.
"""

import json
import sys
from pathlib import Path

ASSETS = '/config/fancymenu/assets/parallax/'

# Every layer reaches 32 GUI pixels past the left and right edge and 20 past the top and bottom, so its edges never show:
# at 1.0, an element moves up to 5% of the screen width (32 of 640) and 5% of the screen height (18 of 360).
LAYER_BOX = {'x': -32, 'y': -20, 'w': 704, 'h': 400}


def layer(name, intensity_x, intensity_y, parallax=True, shift_y=0):
    props = {
        'source': ASSETS + f'parallax_{name}.png',
        'stay_on_screen': 'false',
        'enable_parallax': 'true' if parallax else 'false',
    }
    if parallax:
        props['parallax_intensity_x'] = str(intensity_x)
        props['parallax_intensity_y'] = str(intensity_y)
    box = dict(LAYER_BOX, y=LAYER_BOX['y'] + shift_y)
    return {'type': 'image', 'id': f'parallax_{name}', **box, 'props': props}


def base(foreground_parallax=True, shift_y=0):
    return {
        'screen': 'title',
        'background_image': ASSETS + 'parallax_sky.png',
        'background_parallax': [0.02, 0.01],
        'elements_behind_vanilla': True,
        'elements': [
            layer('mountains', 0.35, 0.2, shift_y=shift_y),
            layer('hills', 0.6, 0.3, shift_y=shift_y),
            layer('foreground', 1.0, 0.5, foreground_parallax, shift_y=shift_y),
        ],
    }


def main():
    out = Path(sys.argv[1] if len(sys.argv) > 1 else '.')
    out.mkdir(parents=True, exist_ok=True)

    scene = base()
    scene.update({
        'script': 'hover_elements',
        'start': 'screen',
        'pointer': [320, 196],
        # A slow tour around the edges, where the layers move the most, and back to the middle
        'hovers': [
            {'point': [36, 196], 'move_ticks': 40, 'ticks': 12},
            {'point': [604, 36], 'move_ticks': 56, 'ticks': 12},
            {'point': [596, 330], 'move_ticks': 36, 'ticks': 10},
            {'point': [40, 330], 'move_ticks': 50, 'ticks': 10},
            {'point': [320, 196], 'move_ticks': 38, 'ticks': 10},
        ],
        'hold_ticks': 20,
    })

    settings = base(foreground_parallax=False)
    settings.update({
        'script': 'element_settings',
        'pointer': [520, 300],
        'right_click': [92, 300],
        'settings': [
            {'toggle': 'enable_parallax'},
            # Y too would right-click and scroll again, which makes the GIF about 1.9 MB
            {'number': 'menu_entry_parallax_intensity_x', 'value': '1.0'},
        ],
        'rest_point': [520, 300],
        'hold_ticks': 50,
    })

    # A taller window, so the whole menu of the Image background fits. The layers start lower, so the right-click above them
    # reaches the editor background instead of the foreground layer, which would open its menu.
    still = base(shift_y=160)
    still.update({
        'script': 'menu_path_still',
        'window_height': 1080,
        'pointer': [520, 300],
        'right_click': [60, 80],
        'path': ['menu_backgrounds', 'menu_background_image'],
        'rest_on': 'menu_entry_parallax',
    })

    for name, config in [('scene', scene), ('settings', settings), ('background_still', still)]:
        (out / f'{name}.json').write_text(json.dumps(config, indent=1))


if __name__ == '__main__':
    main()
