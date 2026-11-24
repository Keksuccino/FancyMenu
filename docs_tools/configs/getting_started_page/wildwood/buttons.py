"""Wooden plank button textures (normal and hover), 40x10 art pixels, saved at 2x for nine-slicing with 6 px borders."""
import numpy as np
from scene import hexc

BW, BH = 40, 10


def make_button(hover=False):
    img = np.zeros((BH, BW, 4), np.uint8)
    outline = hexc('#ffcf6b') if hover else hexc('#1a0d1c')
    face = hexc('#a5653a') if hover else hexc('#83502f')
    face_dark = hexc('#8c5230') if hover else hexc('#6e4127')
    top = hexc('#e9aa6a') if hover else hexc('#b97c4b')
    left = hexc('#c88550') if hover else hexc('#9b6239')
    right = hexc('#7a4428') if hover else hexc('#5f3720')
    bottom = hexc('#6a3a22') if hover else hexc('#4f2d1b')
    nail = hexc('#fff0b0') if hover else hexc('#e2b96e')
    nail_shadow = hexc('#5a321e') if hover else hexc('#3e2416')
    for y in range(BH):
        for x in range(BW):
            edge_x = x in (0, BW - 1)
            edge_y = y in (0, BH - 1)
            if edge_x and edge_y:
                continue  # rounded corners
            if edge_x or edge_y:
                img[y, x] = outline
                continue
            if y == 1:
                c = top
            elif y == BH - 2:
                c = bottom
            elif x == 1:
                c = left
            elif x == BW - 2:
                c = right
            else:
                c = face
                # Two grain lines along the plank, broken up near the ends
                if y == 3 and not (5 <= x <= 7) and not (BW - 9 <= x <= BW - 7):
                    c = face_dark
                if y == 6 and not (9 <= x <= 10) and not (BW - 6 <= x <= BW - 5):
                    c = face_dark
            img[y, x] = c
    # Nails near both ends, inside the 6 pixel (3 art pixel) nine-slice borders, so they never get stretched
    for nx in (2, BW - 3):
        img[4, nx] = nail
        img[5, nx] = nail_shadow
    if hover:
        # Warm glow pixels just inside the outline at the corners
        for x, y in ((1, 1), (BW - 2, 1), (1, BH - 2), (BW - 2, BH - 2)):
            img[y, x] = hexc('#ffe39a')
    return img


if __name__ == '__main__':
    import sys
    from scene import save
    from PIL import Image
    n = Image.fromarray(make_button(False)).resize((BW * 8, BH * 8), Image.NEAREST)
    h = Image.fromarray(make_button(True)).resize((BW * 8, BH * 8), Image.NEAREST)
    out = Image.new('RGBA', (BW * 8, BH * 8 * 2 + 16), (40, 20, 50, 255))
    out.paste(n, (0, 0), n)
    out.paste(h, (0, BH * 8 + 16), h)
    out.save(f'{sys.argv[1]}/buttons_preview.png')
