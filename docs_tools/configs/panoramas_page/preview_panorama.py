"""Renders views of a panorama (six images panorama_0.png to panorama_5.png) like FancyMenu shows them, to check a panorama
without starting the game, plus the six images laid out as an unfolded cube.

Usage: python3 preview_panorama.py <folder with the six images> <out dir> [angle, default 25] [fov, default 85]
Writes net.png and views.png (four views, turned by 90 degrees each).
"""
import math
import os
import sys

import numpy as np
from PIL import Image


def load_faces(folder):
    return [np.array(Image.open(os.path.join(folder, 'panorama_%d.png' % i)).convert('RGB')) for i in range(6)]


def sample_cube(faces, x, y, z):
    """Looks up the color of every direction. Front is -z, right is +x, up is +y, like Minecraft's panorama images."""
    ax, ay, az = np.abs(x), np.abs(y), np.abs(z)
    n = faces[0].shape[0]
    out = np.zeros(x.shape + (3,), dtype=np.uint8)
    conds = [
        (az >= ax) & (az >= ay) & (z < 0), (ax >= ay) & (ax > az) & (x > 0), (az >= ax) & (az >= ay) & (z > 0),
        (ax >= ay) & (ax > az) & (x < 0), (ay > ax) & (ay > az) & (y > 0), (ay > ax) & (ay > az) & (y < 0),
    ]
    with np.errstate(divide='ignore', invalid='ignore'):
        uv = [(x / az, -y / az), (z / ax, -y / ax), (-x / az, -y / az), (-z / ax, -y / ax), (x / ay, -z / ay), (x / ay, z / ay)]
    taken = np.zeros(x.shape, dtype=bool)
    for i in range(6):
        m = conds[i] & ~taken
        taken |= m
        u, v = uv[i]
        cu = np.clip(((np.nan_to_num(u) + 1) / 2 * n).astype(np.int64), 0, n - 1)
        cv = np.clip(((np.nan_to_num(v) + 1) / 2 * n).astype(np.int64), 0, n - 1)
        out[m] = faces[i][cv[m], cu[m]]
    return out


def view(faces, yaw, angle, fov, w=1280, h=720):
    sx, sy = np.meshgrid(np.arange(w) + 0.5, np.arange(h) + 0.5)
    tan = math.tan(math.radians(fov) / 2)
    px = (sx - w / 2) / (h / 2) * tan
    py = (h / 2 - sy) / (h / 2) * tan
    pz = -np.ones_like(px)
    p = math.radians(-angle)
    y1 = py * math.cos(p) - pz * math.sin(p)
    z1 = py * math.sin(p) + pz * math.cos(p)
    q = math.radians(yaw)
    x2 = px * math.cos(q) - z1 * math.sin(q)
    z2 = px * math.sin(q) + z1 * math.cos(q)
    return Image.fromarray(sample_cube(faces, x2, y1, z2))


def main():
    folder, out = sys.argv[1], sys.argv[2]
    angle = float(sys.argv[3]) if len(sys.argv) > 3 else 25.0
    fov = float(sys.argv[4]) if len(sys.argv) > 4 else 85.0
    os.makedirs(out, exist_ok=True)
    faces = load_faces(folder)
    n = 256
    net = Image.new('RGB', (4 * n, 3 * n), (30, 30, 30))
    for i, (c, r) in {4: (1, 0), 3: (0, 1), 0: (1, 1), 1: (2, 1), 2: (3, 1), 5: (1, 2)}.items():
        net.paste(Image.fromarray(faces[i]).resize((n, n), Image.LANCZOS), (c * n, r * n))
    net.save(os.path.join(out, 'net.png'))
    sheet = Image.new('RGB', (1280, 720))
    for i, yaw in enumerate((0, 90, 180, 270)):
        sheet.paste(view(faces, yaw, angle, fov).resize((640, 360), Image.LANCZOS), ((i % 2) * 640, (i // 2) * 360))
    sheet.save(os.path.join(out, 'views.png'))


if __name__ == '__main__':
    main()
