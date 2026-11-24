"""Makes the pixel art of the Wildwood title screen of the Getting Started page, the example menu of its GIFs.

Usage: python3 make_assets.py <out dir>, usually run_client_docs/config/fancymenu/assets/wildwood

- background.png: the whole scene in one image, for the Image menu background of the tutorial GIFs (320x180 art pixels at 4x).
- sky.png, hills.png, forest.png: the scene in three layers for the parallax of the finished menu. hills.png and forest.png
  are 12 art pixels wider on both sides (see wildwood/scene.py MARGIN), so they can move without showing their edges.
- logo.png: the WILDWOOD logo, 130x50 art pixels at 4x. At its default height of 100 GUI pixels, Restore Aspect Ratio makes
  an Image element 260x100, so one art pixel is 2 GUI pixels, like in the scene.
- button.png, button_hover.png: wooden button backgrounds, 40x10 art pixels at 2x, for nine-slicing with 6 pixel borders.
"""
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'wildwood'))

from scene import make_sky_layer  # noqa: E402
from layers import make_hills_layer, make_foreground_layer, flatten  # noqa: E402
from logo import make_logo  # noqa: E402
from buttons import make_button  # noqa: E402


def save(array, path, scale):
    image = Image.fromarray(array, 'RGBA')
    image = image.resize((image.width * scale, image.height * scale), Image.NEAREST)
    image.save(path, optimize=True)
    print(f'{path}: {image.width}x{image.height}')


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    sky = make_sky_layer()
    hills = make_hills_layer()
    forest = make_foreground_layer()
    save(flatten(sky, hills, forest), os.path.join(out, 'background.png'), 4)
    save(sky, os.path.join(out, 'sky.png'), 4)
    save(hills, os.path.join(out, 'hills.png'), 4)
    save(forest, os.path.join(out, 'forest.png'), 4)
    save(make_logo(), os.path.join(out, 'logo.png'), 4)
    save(make_button(False), os.path.join(out, 'button.png'), 2)
    save(make_button(True), os.path.join(out, 'button_hover.png'), 2)


if __name__ == '__main__':
    main()
