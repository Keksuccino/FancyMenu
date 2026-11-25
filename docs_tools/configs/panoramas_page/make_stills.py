"""Picks the still of the Panoramas page from the frames of hero.json and optimizes it.

Usage: python3 make_stills.py <hero frames dir> <out png> [frame index, default: the third-last frame]
"""
import os
import shutil
import subprocess
import sys

from PIL import Image


def main():
    frames, out = sys.argv[1], sys.argv[2]
    names = sorted(n for n in os.listdir(frames) if n.startswith('f_') and n.endswith('.png'))
    index = int(sys.argv[3]) if len(sys.argv) > 3 else len(names) - 3
    Image.open(os.path.join(frames, names[index])).convert('RGB').save(out)
    if shutil.which('pngquant'):
        subprocess.run(['pngquant', '--quality', '80-95', '--force', '--output', out, out], check=True)
    subprocess.run(['oxipng', '-o', '4', '--strip', 'safe', out], check=True, capture_output=True)
    print(out, os.path.getsize(out), 'bytes, frame', names[index])


if __name__ == '__main__':
    main()
