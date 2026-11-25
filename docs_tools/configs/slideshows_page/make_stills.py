"""Picks the stills of the Slideshows page from the screenshots of scenes.json and optimizes them.

Usage: python3 make_stills.py <scenes out dir> <assets dir> [title shot, default 2] [gallery shot, default 1]
  The shots are the seconds of the scene's shots, like 9 for title_9s.png.
"""
import os
import shutil
import subprocess
import sys

from PIL import Image


def main():
    shots, assets = sys.argv[1], sys.argv[2]
    title = sys.argv[3] if len(sys.argv) > 3 else '2'
    gallery = sys.argv[4] if len(sys.argv) > 4 else '1'
    for source, name in [(f'title_{title}s.png', 'slideshows_title.png'), (f'gallery_{gallery}s.png', 'slideshows_gallery.png')]:
        out = os.path.join(assets, name)
        Image.open(os.path.join(shots, source)).convert('RGB').save(out)
        if shutil.which('pngquant'):
            subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', out, out], check=True)
        subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', out], check=True, capture_output=True)
        print(out, os.path.getsize(out), 'bytes, from', source)


if __name__ == '__main__':
    main()
