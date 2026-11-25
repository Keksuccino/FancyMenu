"""Makes the downloadable Wildwood profile of the Getting Started page: the finished title screen from the top of the page, as a
FancyMenu profile (.fmp) that's free to use for testing, learning and modpacks.

Usage: python3 make_profile.py <work dir>

It writes into <work dir>:
- profile/: the profile folder without its layout: the Wildwood images in assets/wildwood/, the title screen in
  customizablemenus.txt, an options.txt that only turns off the welcome screen, profile.json and profile_icon.png.
- write_profile.json: the harness config (script write_profile) that builds the layout, saves it into profile/customization/
  and packs profile/ into wildwood_title_screen.fmp with FancyMenu's own archive writer. Record it with
  FANCYMENU_DOCS_SHOWCASE=editor and FANCYMENU_DOCS_SHOWCASE_CONFIG=<work dir>/write_profile.json.
- check_<size>.json: configs that show the title screen of a game instance with the extracted profile in several window sizes
  (record them with FANCYMENU_TEST_RUN_DIR set to that instance), to check that the layout fits every screen.

Unlike the hero GIF, the layout fits every window size: the parallax layers and the logo get their size from the screen through
Advanced Positioning and Advanced Sizing, and the logo gets smaller on short screens, so it never covers the Singleplayer button.
"""
import json
import os
import shutil
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, 'wildwood'))

from icon import make_icon  # noqa: E402

FANCYMENU_VERSION = '3.11.0'
ASSETS = '/config/fancymenu/assets/wildwood/'
# How FancyMenu saves local resources in layouts. Element properties go into the layout as written, so they need it too.
LOCAL = '[source:local]' + ASSETS
MAIN_BUTTONS = ['mc_titlescreen_singleplayer_button', 'mc_titlescreen_multiplayer_button', 'mc_titlescreen_realms_button',
                'mc_titlescreen_options_button', 'mc_titlescreen_quit_button']

DESCRIPTION = (
    "The Wildwood title screen of the Getting Started page of the FancyMenu docs: a pixel art valley at dusk whose layers move "
    "with the mouse, a logo, wooden buttons that glow when hovered, and fireflies.\n\n"
    "Free to use for anything: to try FancyMenu, to learn how it works, or in your modpack, with or without changes. "
    "No credit needed."
)


def calc(expression):
    return '{"placeholder":"calc","values":{"expression":"' + expression + '","decimal":"false"}}'


GW = '{"placeholder":"guiwidth"}'
GH = '{"placeholder":"guiheight"}'
# The Singleplayer button starts at guiheight / 4 + 48. The logo is 100 high, but never reaches closer than 8 pixels to it:
# min(100, floor(guiheight / 4) + 28), written as (a + b - abs(a - b)) / 2 because the calculator has no min().
LOGO_H = '((128 + floor(' + GH + ' / 4) - abs(72 - floor(' + GH + ' / 4))) / 2)'


def layer(name, intensity):
    # 12 art pixels wider on both sides (3.75% of the screen width), so parallax never shows their edges
    return {'type': 'image', 'id': f'wildwood_{name}', 'x': -24, 'y': 0, 'w': 688, 'h': 360, 'props': {
        'source': LOCAL + f'{name}.png',
        'stay_on_screen': 'false',
        'advanced_posx': calc(GW + ' * -0.0375'),
        'advanced_posy': '0',
        'advanced_width': calc(GW + ' * 1.075'),
        'advanced_height': GH,
        'enable_parallax': 'true',
        'parallax_intensity_x': str(intensity),
        'parallax_intensity_y': '0',
    }}


LOGO = {'type': 'image', 'id': 'wildwood_logo', 'x': 190, 'y': 12, 'w': 260, 'h': 100, 'props': {
    'source': LOCAL + 'logo.png',
    'stay_on_screen': 'false',
    'advanced_posx': calc('(' + GW + ' - 2.6 * ' + LOGO_H + ') / 2'),
    'advanced_posy': '12',
    'advanced_width': calc('2.6 * ' + LOGO_H),
    'advanced_height': calc(LOGO_H),
}}

LAYOUT = {
    'background_image': ASSETS + 'sky.png', 'background_parallax': [0.01, 0],
    'elements': [layer('hills', 0.2), layer('forest', 0.55), LOGO],
    'elements_behind_vanilla': True,
    'vanilla': [{'id': 'minecraft_logo_widget', 'hidden': True}, {'id': 'minecraft_splash_widget', 'hidden': True}] + [
        {'id': button, 'props': {
            'backgroundnormal': LOCAL + 'button.png',
            'backgroundhovered': LOCAL + 'button_hover.png',
            'nine_slice_custom_background': 'true',
        }} for button in MAIN_BUTTONS],
    'overlays': [{'type': 'fireflies', 'props': {'firefly_scale': '2.0'}}],
}

# Window sizes to check: (name, window width, window height, GUI scale)
CHECK_SIZES = [
    ('720p_scale2', 1280, 720, 2),      # 640x360, like the docs GIFs
    ('720p_scale3', 1280, 720, 3),      # 426x240, the automatic GUI scale of 1280x720 and 2560x1440
    ('1080p_scale4', 1920, 1080, 4),    # 480x270, the automatic GUI scale of 1920x1080
    ('16x10_scale2', 1280, 800, 2),     # 640x400
    ('ultrawide_scale2', 1720, 720, 2), # 860x360
]


def main():
    work = os.path.abspath(sys.argv[1])
    profile = os.path.join(work, 'profile')
    if os.path.exists(profile):
        raise SystemExit(f'{profile} exists already, move it to the trash first')
    os.makedirs(os.path.join(profile, 'assets', 'wildwood'))
    # The images, made like for the GIFs
    import subprocess
    subprocess.run([sys.executable, os.path.join(HERE, 'make_assets.py'), os.path.join(profile, 'assets', 'wildwood')], check=True,
                   stdout=subprocess.DEVNULL, env=dict(os.environ, PYTHONDONTWRITEBYTECODE='1'))
    with open(os.path.join(profile, 'customizablemenus.txt'), 'w') as f:
        f.write('type = customizablemenus\n\nnet.minecraft.client.gui.screens.TitleScreen {\n}\n')
    # Only the welcome screen: FancyMenu gives all other options their defaults when it loads the file
    with open(os.path.join(profile, 'options.txt'), 'w') as f:
        f.write("##[tutorial]\n\nB:show_welcome_screen = 'false';\n")
    metadata = {
        'format_version': 1,
        'id': 'wildwood',
        'title': 'Wildwood Title Screen',
        'version': '1.0',
        'authors': ['Keksuccino'],
        'description': DESCRIPTION,
        'links': [{'type': 'website', 'label': 'FancyMenu Docs', 'url': 'https://docs.fancymenu.net/home'}],
        'fancymenu_version': FANCYMENU_VERSION,
    }
    with open(os.path.join(profile, 'profile.json'), 'w') as f:
        json.dump(metadata, f, indent=2)
    icon = Image.fromarray(make_icon(), 'RGBA').resize((64, 64), Image.NEAREST)
    icon.save(os.path.join(profile, 'profile_icon.png'), optimize=True)
    config = dict(LAYOUT)
    config.update({
        'script': 'write_profile', 'start': 'screen', 'screen': 'title', 'no_layout': True,
        'profile_dir': profile, 'layout_name': 'wildwood_title_screen',
        'archive': os.path.join(work, 'wildwood_title_screen.fmp'),
    })
    with open(os.path.join(work, 'write_profile.json'), 'w') as f:
        json.dump(config, f, indent=1)
    for name, width, height, scale in CHECK_SIZES:
        with open(os.path.join(work, f'check_{name}.json'), 'w') as f:
            json.dump({
                'script': 'screen_tour', 'start': 'screen', 'screen': 'title', 'no_layout': True, 'elements': [],
                'window_width': width, 'window_height': height, 'gui_scale': scale,
                'pointer': [-40, -40], 'tour': [{'wait': 40}], 'hold_ticks': 4,
            }, f, indent=1)
    print('profile folder and configs in', work)


if __name__ == '__main__':
    main()
