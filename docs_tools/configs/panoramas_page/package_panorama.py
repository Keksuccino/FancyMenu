"""Turns the six screenshots of the world harness into the FancyMenu panorama forest_homestead: the images, its properties.txt and
a soft vignette as overlay.png.

Usage: python3 package_panorama.py <folder with panorama_0.png to panorama_5.png> <out dir>   # writes <out dir>/forest_homestead/
"""
import os
import shutil
import sys

import numpy as np
from PIL import Image

PROPERTIES = """type = panorama

panorama-meta {
  name = forest_homestead
  speed = 1.0
  fov = 85.0
  angle = 25.0
  start_rotation = 0
}
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
    print(folder)


if __name__ == '__main__':
    main()
