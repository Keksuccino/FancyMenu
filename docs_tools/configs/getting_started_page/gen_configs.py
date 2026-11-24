"""Configs of the Getting Started page GIFs, which build the Wildwood title screen (see make_assets.py) step by step.

- hero.json: the finished menu, like a player sees it: parallax layers, wooden buttons and Fireflies, with the pointer moving around.
- new_layout.json: turning on Current Screen Customization from the menu bar and making a layout for the title screen.
  Its frame with the open Customization menu (mark "menu") is the menu bar still (make_stills.py).
- background.json: Menu Backgrounds -> Image, Show Background and picking background.png.
- logo.json: deleting the Minecraft logo and splash, adding an Image element with logo.png and Restore Aspect Ratio.
- save.json: saving with Ctrl + S, closing the editor and looking at the menu. It saves a real layout file into the docs
  instance: move run_client_docs/config/fancymenu/customization/title_screen_layout.txt to the trash after recording.

Usage: python3 gen_configs.py <out dir>
"""
import json
import os
import sys

ASSETS = '/config/fancymenu/assets/wildwood/'
TITLE_BUTTONS = ['mc_titlescreen_singleplayer_button', 'mc_titlescreen_multiplayer_button', 'mc_titlescreen_realms_button',
                 'mc_titlescreen_options_button', 'mc_titlescreen_quit_button',
                 # The row of icon buttons: skin, language, accessibility and mods
                 '454370', 'mc_titlescreen_language_button', 'mc_titlescreen_accessibility_button', 'titlescreen_mods_button']
HIDDEN_VANILLA = [{'id': 'minecraft_logo_widget', 'hidden': True}, {'id': 'minecraft_splash_widget', 'hidden': True}]
WOODEN_BUTTONS = [{'id': button, 'props': {
    'backgroundnormal': ASSETS + 'button.png',
    'backgroundhovered': ASSETS + 'button_hover.png',
    'nine_slice_custom_background': 'true',
    'nine_slice_border_x': '6',
    'nine_slice_border_y': '6',
}} for button in TITLE_BUTTONS]
# The logo at its default height of 100 after Restore Aspect Ratio, centered above the buttons
LOGO = {'type': 'image', 'id': 'wildwood_logo', 'x': 190, 'y': 14, 'w': 260, 'h': 100, 'props': {'source': ASSETS + 'logo.png'}}


def layer(name, intensity):
    # The layers are 12 art pixels (24 GUI pixels) wider on both sides, so parallax never shows their edges
    return {'type': 'image', 'id': f'wildwood_{name}', 'x': -24, 'y': 0, 'w': 688, 'h': 360, 'props': {
        'source': ASSETS + f'{name}.png',
        'enable_parallax': 'true',
        'parallax_intensity_x': str(intensity),
        'parallax_intensity_y': '0',
    }}


CONFIGS = {
    'hero': {
        'script': 'screen_tour', 'start': 'screen', 'screen': 'title',
        'background_image': ASSETS + 'sky.png', 'background_parallax': [0.01, 0],
        'elements': [layer('hills', 0.2), layer('forest', 0.55), LOGO],
        'elements_behind_vanilla': True,
        'vanilla': HIDDEN_VANILLA + WOODEN_BUTTONS,
        'overlays': [{'type': 'fireflies', 'props': {'firefly_scale': '2.0'}}],
        'seed': 7,
        'pointer': [90, 250],
        'tour': [
            {'wait': 14},
            {'point': [560, 262], 'move_ticks': 90, 'ticks': 6},
            {'hover': 'menu.singleplayer', 'move_ticks': 36, 'ticks': 34},
            {'hover': 'menu.multiplayer', 'move_ticks': 14, 'ticks': 28},
            {'point': [150, 300], 'move_ticks': 60, 'ticks': 6},
        ],
        'hold_ticks': 16,
    },
    'new_layout': {
        'script': 'first_layout', 'start': 'screen', 'screen': 'title', 'menu_bar': True, 'no_layout': True,
        'customization': False, 'elements': [], 'hold_ticks': 50,
    },
    'background': {
        'script': 'menu_background', 'screen': 'title', 'elements': [],
        'right_click': [96, 292], 'folder': 'wildwood', 'file': 'background.png', 'free_spot': [560, 300], 'hold_ticks': 60,
    },
    'logo': {
        'script': 'add_image', 'screen': 'title', 'elements': [], 'background_image': ASSETS + 'background.png',
        'remove': ['minecraft_logo_widget', 'minecraft_splash_widget'],
        'right_click': [300, 36], 'menu_target_y': 275, 'folder': 'wildwood', 'file': 'logo.png', 'drag_to': [320, 64],
        'free_spot': [560, 300], 'hold_ticks': 60,
    },
    'save': {
        'script': 'save_layout', 'screen': 'title', 'customization': True,
        'elements': [LOGO], 'background_image': ASSETS + 'background.png', 'vanilla': HIDDEN_VANILLA,
        'hovers': [{'label': 'menu.singleplayer', 'move_ticks': 24, 'ticks': 30}, {'point': [520, 300], 'move_ticks': 24, 'ticks': 10}],
        'hold_ticks': 40,
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
