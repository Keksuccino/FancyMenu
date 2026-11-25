"""Makes the slideshow world_tour of the Slideshows page from the screenshots of world.json (see gen_configs.py).

Usage: python3 make_slideshow.py <world out dir> <slideshows dir>
  Writes <slideshows dir>/world_tour/properties.txt and images/ with the screenshots in the order of their names.
"""
import os
import shutil
import sys

from gen_configs import SHOTS

PROPERTIES = """type = slideshow

slideshow-meta {
  name = world_tour
  duration = 5.0
  fadespeed = 5.0
  randomize = false
}
"""


def main():
    shots, out = os.path.join(sys.argv[1], 'screenshots'), os.path.join(sys.argv[2], 'world_tour')
    images = os.path.join(out, 'images')
    os.makedirs(images, exist_ok=True)
    for shot in SHOTS:
        shutil.copy(os.path.join(shots, shot['name'] + '.png'), os.path.join(images, shot['name'] + '.png'))
    with open(os.path.join(out, 'properties.txt'), 'w') as f:
        f.write(PROPERTIES)
    print(out, sorted(os.listdir(images)))


if __name__ == '__main__':
    main()
