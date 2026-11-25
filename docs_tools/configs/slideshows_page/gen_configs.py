r"""Configs of the Slideshows page images. The slideshow shows real screenshots of a Minecraft world:

1. world.json (world_harness): makes a normal world from SEED and takes the three screenshots of the slideshow, each above the closest
   spot of a biome: a river valley in a meadow, a cherry grove with a snowy mountain behind it, and a coast with islands.
2. make_slideshow.py turns them into the slideshow world_tour. Copy it to run_client_docs/config/fancymenu/slideshows/ before the
   game starts, because FancyMenu loads slideshows at its start.
3. The GIFs (ingame_harness, editor mode):
   - title.json: the Title screen with world_tour as menu background, fading through its images. One loop of it is the GIF.
   - element.json: the Title screen with its buttons on the left and a Slideshow element of world_tour on the right.
   - background.json: Menu Backgrounds -> Slideshow in the editor, Show Background, and picking world_tour in Choose Slideshow, whose
     preview stays open long enough to show a fade.

Most of the screen changes while a slideshow fades, so encode the GIFs with configs/panoramas_page/encode_small.sh (800 px). The
fades of title.json and element.json run at frames 10, 60, 110 and 160 (14 frames each), so frames 24 to 173 are one loop that starts
and ends on the same image:
  sh encode_small.sh <title composed> slideshows_title.gif 200 \
    'between(n\,24\,173)*(not(between(n\,60\,73)+between(n\,110\,123)+between(n\,160\,173))+not(mod(n\,5)))' 128
  sh encode_small.sh <element composed> slideshows_element.gif 80 \
    'between(n\,24\,173)*(not(between(n\,60\,73)+between(n\,110\,123)+between(n\,160\,173))+not(mod(n\,2)))'
background.json: compose only the frames before the full-screen fade after Done (0 to 175), encode them with encode_small.sh (lossy
80) and give the last frame 2.5 seconds: gifsicle -b <gif> "#0-<count - 2>" -d250 "#<count - 1>".
The Title screen without the Fancy Entity Renderer mod: park its jar outside of mods while recording, or its FER Test button shows
in the top right corner.

Usage: python3 gen_configs.py <out dir>
"""
import json
import os
import sys

DOCS_TOOLS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SEED = 20261125

# The spots of the scouting run that looked best: 10 blocks above the ground of the closest spot of each biome, looking down a bit
SHOTS = [
    {'name': '01_river_valley', 'biome': 'minecraft:meadow', 'yaw': 0},
    {'name': '02_cherry_grove', 'biome': 'minecraft:cherry_grove', 'yaw': 0},
    {'name': '03_coast', 'biome': 'minecraft:mangrove_swamp', 'yaw': 90},
]

WORLD = {
    'commands_file': os.path.join(DOCS_TOOLS, 'configs', 'slideshows_page', 'world_setup.txt'),
    'preset': 'normal', 'seed': SEED, 'mode': 'screenshot', 'screenshot_size': [1280, 720], 'fov': 70, 'render_distance': 12,
    'settle_ticks': 60, 'downscale': 2,
    'shots': [dict(shot, above_ground=10, pitch=12, time='6000', settle_ticks=300) for shot in SHOTS],
}

BUTTONS_LEFT = [
    {'id': 'mc_titlescreen_singleplayer_button', 'x': 40, 'y': 124, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_multiplayer_button', 'x': 40, 'y': 148, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_realms_button', 'x': 40, 'y': 172, 'w': 200, 'h': 20},
    {'id': 'mc_titlescreen_options_button', 'x': 40, 'y': 208, 'w': 98, 'h': 20},
    {'id': 'mc_titlescreen_quit_button', 'x': 142, 'y': 208, 'w': 98, 'h': 20},
]
HIDDEN = ['mc_titlescreen_language_button', 'mc_titlescreen_accessibility_button', 'titlescreen_mods_button', '454370',
          'minecraft_realms_notification_icons_widget']

CONFIGS = {
    'world': WORLD,
    # world_tour: duration 5, so the first image shows for 2.5 seconds and every other one for 5. 400 ticks hold a whole loop after
    # the first fade.
    'title': {
        'script': 'screen_tour', 'start': 'screen', 'screen': 'title', 'background_slideshow': 'world_tour', 'elements': [],
        'pointer': [-40, -40], 'tour': [{'wait': 400}], 'hold_ticks': 4,
    },
    'element': {
        'script': 'screen_tour', 'start': 'screen', 'screen': 'title', 'background_image': '/config/fancymenu/assets/docs_bg.png',
        'vanilla': BUTTONS_LEFT + [{'id': i, 'hidden': True} for i in HIDDEN],
        'elements': [{'type': 'slideshow', 'x': 288, 'y': 112, 'w': 320, 'h': 180, 'props': {'slideshow_name': 'world_tour'}}],
        'pointer': [-40, -40], 'tour': [{'wait': 400}], 'hold_ticks': 4,
    },
    'background': {
        'script': 'slideshow_background', 'screen': 'title', 'elements': [],
        # The first image shows for 2.5 seconds, then it fades, so the preview needs about 4 seconds. Done comes right after the fade,
        # so the background holds for about 3 seconds before its next fade, where the GIF ends
        'right_click': [96, 292], 'slideshow': 'world_tour', 'preview_ticks': 80, 'free_spot': [560, 300], 'hold_ticks': 50,
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
