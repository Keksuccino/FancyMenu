"""Turns the six screenshots of the world harness into the FancyMenu panorama forest_homestead: the images (losslessly optimized),
its properties.txt, a soft vignette as overlay.png and a README.txt. Also zips it as forest_homestead.zip, the free download of the
Panoramas page (assets/forest_homestead.zip of the docs repo).

Usage: python3 package_panorama.py <folder with panorama_0.png to panorama_5.png> <out dir>
       # writes <out dir>/forest_homestead/ and <out dir>/forest_homestead.zip
"""
import os
import shutil
import subprocess
import sys
import zipfile

import numpy as np
from PIL import Image

PROPERTIES = """type = panorama

panorama-meta {
  name = forest_homestead
  speed = 2.0
  fov = 85.0
  angle = 25.0
  start_rotation = 0
}
"""


README = """Forest Homestead, a panorama for FancyMenu: a log cabin in a forest clearing at sunset, built and captured in Minecraft.

Free to use, for testing and in your own modpacks and other projects.

To use it, put this forest_homestead folder into config/fancymenu/panoramas/ of your game directory and reload FancyMenu
(Ctrl + Alt + R, or Cmd + Alt + R on macOS) or restart the game. The Panoramas page of the FancyMenu docs shows how to use it.
"""


def vignette(w=640, h=360):
    ys, xs = np.indices((h, w))
    dx = (xs + 0.5 - w / 2) / (w / 2)
    dy = (ys + 0.5 - h / 2) / (h / 2)
    d = np.sqrt(dx * dx * 0.75 + dy * dy)
    alpha = np.clip((d - 0.55) / 0.75, 0, 1) ** 1.6 * 120
    img = np.zeros((h, w, 4), dtype=np.uint8)
    img[..., :3] = (18, 12, 8)
    img[..., 3] = np.round(alpha).astype(np.uint8)
    return Image.fromarray(img, 'RGBA')


def main():
    faces, out = sys.argv[1], sys.argv[2]
    folder = os.path.join(out, 'forest_homestead')
    os.makedirs(os.path.join(folder, 'panorama'), exist_ok=True)
    for i in range(6):
        shutil.copyfile(os.path.join(faces, 'panorama_%d.png' % i), os.path.join(folder, 'panorama', 'panorama_%d.png' % i))
    with open(os.path.join(folder, 'properties.txt'), 'w') as f:
        f.write(PROPERTIES)
    vignette().save(os.path.join(folder, 'overlay.png'))
    with open(os.path.join(folder, 'README.txt'), 'w') as f:
        f.write(README)
    pngs = [os.path.join(folder, 'overlay.png')] + [os.path.join(folder, 'panorama', 'panorama_%d.png' % i) for i in range(6)]
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe'] + pngs, check=True, capture_output=True)
    # The zip holds the folder, so it unzips right into config/fancymenu/panoramas/. Nothing else goes next to the folder:
    # FancyMenu reports every file in panoramas/ that isn't a panorama folder as an invalid panorama.
    zip_path = os.path.join(out, 'forest_homestead.zip')
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in ['README.txt', 'properties.txt', 'overlay.png'] + ['panorama/panorama_%d.png' % i for i in range(6)]:
            z.write(os.path.join(folder, name), 'forest_homestead/' + name)
    print(folder)
    print(zip_path, os.path.getsize(zip_path), 'bytes')


if __name__ == '__main__':
    main()
