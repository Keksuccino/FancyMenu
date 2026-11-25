"""Configs of the Slideshows page images. They need the slideshows of make_slides.py in the docs instance: copy world_tour/ and
gallery/ to run_client_docs/config/fancymenu/slideshows/ before the game starts, because FancyMenu loads slideshows at its start.

- scenes.json (scene mode):
  - title: the Title screen with world_tour as menu background. Its shots catch every image once, the still of the page is one
    of them (make_stills.py).
  - gallery: the Title screen with the buttons on the left and a Slideshow element of gallery (framed by its overlay) on the right.
- background.json (editor GIF): Menu Backgrounds -> Slideshow in the editor, Show Background, and picking world_tour in Choose
  Slideshow. The preview stays open long enough to show a fade.

Every image of world_tour is 1280x720, so the 640x360 GUI pixels of a 1280x720 window at GUI scale 2 show one image pixel per
screen pixel. The Slideshow element is 320x180 GUI pixels, exactly half of that.

Usage: python3 gen_configs.py <out dir>
"""
import json
import os
import sys

BUTTONS_LEFT = [
    {'id': 'mc_titlescreen_singleplayer_button', 'x': 40, 'y': 124, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_multiplayer_button', 'x': 40, 'y': 148, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_realms_button', 'x': 40, 'y': 172, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_options_button', 'x': 40, 'y': 208, 'w': 98, 'h': 20},
    {'id': 'mc_titlescreen_quit_button', 'x': 142, 'y': 208, 'w': 98, 'h': 20},
]
HIDDEN = ['mc_titlescreen_language_button', 'mc_titlescreen_accessibility_button', 'titlescreen_mods_button', '454370',
          'minecraft_realms_notification_icons_widget']

SCENES = {
    'gui_width': 640, 'gui_height': 360, 'gui_scale': 2,
    'scenes': [
        # world_tour: duration 8, so the first image shows for 4 seconds and every other one for 8, each after a fade of about 2
        {'name': 'title', 'screen': 'title', 'background_slideshow': 'world_tour', 'elements': [], 'pointer': [-40, -40],
         'shots': [2, 9, 17, 25]},
        {'name': 'gallery', 'screen': 'title', 'background_image': '/config/fancymenu/assets/docs_bg.png', 'pointer': [-40, -40],
         'vanilla': BUTTONS_LEFT + [{'id': i, 'hidden': True} for i in HIDDEN],
         'elements': [{'type': 'slideshow', 'x': 288, 'y': 112, 'w': 320, 'h': 180, 'props': {'slideshow_name': 'gallery'}}],
         'shots': [1, 7]},
    ],
}

CONFIGS = {
    'background': {
        'script': 'slideshow_background', 'screen': 'title', 'elements': [],
        # The first image shows for 4 seconds, then it fades for about 2, so the preview needs about 7 seconds
        'right_click': [96, 292], 'slideshow': 'world_tour', 'preview_ticks': 140, 'free_spot': [560, 300], 'hold_ticks': 60,
    },
}


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'scenes.json'), 'w') as f:
        json.dump(SCENES, f, indent=1)
    print('scenes.json')
    for name, config in CONFIGS.items():
        with open(os.path.join(out, f'{name}.json'), 'w') as f:
            json.dump(config, f, indent=1)
        print(f'{name}.json')


if __name__ == '__main__':
    main()
