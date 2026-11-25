"""Configs of the Dragger page GIFs, on the Wildwood title screen (configs/getting_started_page) with the Patch Notes banner (make_panel.py).

The Dragger is as big as the banner and one layer below it, so players grab the banner itself (images don't take clicks), while
Stay on Screen of the Dragger keeps the whole banner on the screen.

- hero.json: a player drags the banner by its rod to the other side, opens Options and comes back, and the banner stays there.
- setup.json: in the editor, adding a Dragger through New Element, making it as big as the banner, dragging it onto the banner,
  moving it two layers down (below the banner and its text), and anchoring the banner to it: dragging the banner out of the Dragger
  and back, and holding it there. The text is anchored to the banner already, so it moves along.
- check.json: no docs image. Checks what the page says: a Dragger on top of a button takes the click, a button on top of a Dragger
  takes it, a Text element on top of a Dragger (on its text and on a blank part), a Dragger without Save User Drag Offset going
  back after Options -> Done, and the banner dragged by its rod against the bottom edge (Stay on Screen).

Dragger offsets get saved in run_client_docs/fancymenu_data/dragger_metas.json. Move that file to the trash before every recording,
so each one starts with the layout positions.

Usage: python3 gen_configs.py <out dir>
"""
import json
import os
import sys

WILDWOOD = '/config/fancymenu/assets/wildwood/'
PANEL = '/config/fancymenu/assets/dragger/panel.png'
TITLE_BUTTONS = ['mc_titlescreen_singleplayer_button', 'mc_titlescreen_multiplayer_button', 'mc_titlescreen_realms_button',
                 'mc_titlescreen_options_button', 'mc_titlescreen_quit_button',
                 '454370', 'mc_titlescreen_language_button', 'mc_titlescreen_accessibility_button', 'titlescreen_mods_button']
HIDDEN_VANILLA = [{'id': 'minecraft_logo_widget', 'hidden': True}, {'id': 'minecraft_splash_widget', 'hidden': True}]
WOODEN_BUTTONS = [{'id': button, 'props': {
    'backgroundnormal': WILDWOOD + 'button.png',
    'backgroundhovered': WILDWOOD + 'button_hover.png',
    'nine_slice_custom_background': 'true',
    'nine_slice_border_x': '6',
    'nine_slice_border_y': '6',
}} for button in TITLE_BUTTONS]
LOGO = {'type': 'image', 'id': 'wildwood_logo', 'x': 190, 'y': 14, 'w': 260, 'h': 100, 'props': {'source': WILDWOOD + 'logo.png'}}
MENU = {'screen': 'title', 'background_image': WILDWOOD + 'background.png', 'vanilla': HIDDEN_VANILLA + WOODEN_BUTTONS}

# The banner is 92x70 art pixels, 2 GUI pixels each. Its rod is the top 20 GUI pixels.
PANEL_X, PANEL_Y, PANEL_W, PANEL_H = 16, 96, 184, 140
ROD_H = 20
NOTES = '### Patch Notes 1.4\n- New boss: Frost Warden\n- Boats are 20% faster\n- Lanterns light up paths\n- Fixed the map crash'


def anchored_to(parent):
    return {'anchor_point': 'element', 'anchor_point_element': parent}


def banner(parent=None):
    """The banner image and its text. The text is anchored to the banner, the banner to the parent (or the top-left corner)."""
    image = {'type': 'image', 'id': 'notes_banner', 'w': PANEL_W, 'h': PANEL_H, 'props': {'source': PANEL}}
    if parent:
        image.update({'x': 0, 'y': 0})
        image['props'].update(anchored_to(parent))
    else:
        image.update({'x': PANEL_X, 'y': PANEL_Y})
    text = {'id': 'notes_text', 'x': 16, 'y': 27, 'w': 152, 'h': 88, 'look': 'pixel', 'background': False,
            'content': NOTES, 'props': anchored_to('notes_banner')}
    return [image, text]


DRAGGER = {'type': 'dragger', 'id': 'notes_dragger', 'x': PANEL_X, 'y': PANEL_Y, 'w': PANEL_W, 'h': PANEL_H}
# The leather grip in the middle of the rod, as parts of the Dragger's width and height
GRIP = [0.5, ROD_H / 2 / PANEL_H]


def toast(title, message):
    return json.dumps({"width": 160, "durationMs": 3000, "title": title, "message": message, "iconSource": "", "backgroundSource": ""})


def check_elements():
    """Test pairs in the right column, each with a shape anchored to its Dragger that shows if the Dragger moved."""
    elements = []
    x = 470

    def shape(name, color):
        return {'type': 'shape', 'id': f'chk_shape_{name}', 'x': 130, 'y': 0, 'w': 20, 'h': 20,
                'props': dict(anchored_to(f'chk_drag_{name}'), color=color)}

    def dragger(name, y, save=True):
        return {'type': 'dragger', 'id': f'chk_drag_{name}', 'x': x, 'y': y, 'w': 120, 'h': 20,
                'props': {'save_drag_offset': 'true' if save else 'false'}}

    def button(name, y):
        return {'type': 'custom_button', 'id': f'chk_button_{name}', 'x': x, 'y': y, 'w': 120, 'h': 20,
                'props': {'label': f'Button {name}'}, 'actions': [{'action': 'show_toast', 'value': toast(f'Button {name}', 'got the click')}]}

    # a: Dragger on top of a button
    elements += [button('a', 20), dragger('a', 20), shape('a', '#E04040')]
    # b: button on top of a Dragger
    elements += [dragger('b', 70), button('b', 70), shape('b', '#40A0E0')]
    # c: Text element on top of a Dragger
    elements += [dragger('c', 120), {'id': 'chk_text_c', 'x': x, 'y': 120, 'w': 120, 'h': 20, 'look': 'pixel', 'background': True,
                                     'content': 'Text on top'}, shape('c', '#40E070')]
    # d: Dragger without Save User Drag Offset
    elements += [dragger('d', 170, save=False), shape('d', '#E0C040')]
    return elements


CONFIGS = {
    'hero': dict(MENU, **{
        'script': 'screen_tour', 'start': 'screen',
        'elements': [LOGO, DRAGGER] + banner('notes_dragger'),
        'pointer': [330, 320],
        'tour': [
            {'wait': 10},
            # Grab the leather grip in the middle of the rod and pull the banner to the right side
            {'grab': 'notes_dragger', 'at': GRIP, 'by': [424, 22], 'move_ticks': 34, 'drag_ticks': 58, 'ticks': 26},
            {'click': 'menu.options', 'opens': True, 'move_ticks': 28, 'ticks': 30},
            {'click': 'gui.done', 'opens': True, 'move_ticks': 30, 'ticks': 16},
            {'point': [330, 320], 'move_ticks': 34, 'ticks': 10},
        ],
        'hold_ticks': 50,
    }),
    'setup': dict(MENU, **{
        'script': 'add_element', 'element_type': 'dragger',
        'elements': [LOGO] + banner(),
        'right_click': [448, 150],
        'resize_first': True, 'resize_corner': True, 'widen_by': PANEL_W - 100, 'heighten_by': PANEL_H - 100,
        # The center of the Dragger onto the center of the banner, then below the banner and its text
        'drag_to': [PANEL_X + PANEL_W / 2, PANEL_Y + PANEL_H / 2],
        'layer_down': 2, 'exact_drag': True,
        # Grab the banner at its rod, left of the grip, pull it up out of the Dragger and back, and hold it there
        'anchor': {'target': 'notes_banner', 'grab': [PANEL_X + 50, PANEL_Y + ROD_H / 2], 'out': [0, -30], 'hover_ticks': 48},
        'free_spot': [560, 300], 'hold_ticks': 50,
    }),
    'check': dict(MENU, **{
        'script': 'screen_tour', 'start': 'screen',
        'elements': [DRAGGER] + banner('notes_dragger') + check_elements(),
        'pointer': [330, 330],
        'tour': [
            {'wait': 10},
            {'grab': 'chk_drag_a', 'by': [-30, 0], 'ticks': 30},
            {'grab': 'chk_drag_b', 'by': [-30, 0], 'ticks': 30},
            {'grab': 'chk_drag_c', 'by': [-30, 0], 'ticks': 30},
            {'grab': 'chk_drag_c', 'at': [0.92, 0.5], 'by': [-30, 0], 'ticks': 30},
            {'grab': 'chk_drag_d', 'by': [-30, 0], 'ticks': 30},
            {'click': 'menu.options', 'opens': True, 'ticks': 20},
            {'click': 'gui.done', 'opens': True, 'ticks': 30},
            {'grab': 'notes_dragger', 'at': GRIP, 'by': [0, 240], 'ticks': 40},
        ],
        'hold_ticks': 30,
    }),
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
