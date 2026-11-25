"""Scales recorded GIF frames to half their size, for the GIFs of the title screen with moving layers. Run it before compose.py,
so the cursor keeps its usual size.

Almost every pixel of these GIFs changes while the layers move, which makes them about 2.5 MB at 1280x720. At GUI scale 2, every GUI
pixel is 2x2 frame pixels, so keeping one pixel of each 2x2 block shows the screen like GUI scale 1, without blur, at a quarter
of the pixels (about 1 MB). The pointer positions in frames.jsonl get halved too.

Usage: python3 half_size.py <recorded frames dir> <out dir>
"""
import json
import os
import sys
from PIL import Image


def main():
    source, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    for name in sorted(os.listdir(source)):
        if name.startswith('f_') and name.endswith('.png'):
            image = Image.open(os.path.join(source, name))
            image.resize((image.width // 2, image.height // 2), Image.NEAREST).save(os.path.join(out, name))
    lines = []
    with open(os.path.join(source, 'frames.jsonl')) as f:
        for line in f:
            data = json.loads(line)
            data['x'] /= 2
            data['y'] /= 2
            for event in data['events']:
                if 'x' in event:
                    event['x'] /= 2
                    event['y'] /= 2
            lines.append(json.dumps(data))
    with open(os.path.join(out, 'frames.jsonl'), 'w') as f:
        f.write('\n'.join(lines) + '\n')


if __name__ == '__main__':
    main()
