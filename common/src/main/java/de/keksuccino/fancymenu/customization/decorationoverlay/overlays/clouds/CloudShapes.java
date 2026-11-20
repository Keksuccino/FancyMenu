package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.clouds;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * Builds the cloud sprites from a seeded random generator, so the same seed always gives the same clouds.<br>
 * Cumulus sprites use the codes {@code 'a'} (sunlit top) to {@code 'e'} (shaded base), wisps use {@code 'a'} (stroke) and {@code 'b'} (its soft underside).
 */
final class CloudShapes {

    /** Shading thresholds of the five tones, from the sunlit top to the shaded base. */
    private static final float[] TONE_THRESHOLDS = {0.83F, 0.66F, 0.5F, 0.37F};
    static final int WISP_HEIGHT = 6;

    private CloudShapes() {
    }

    /**
     * Builds one cumulus cloud with {@code tones} tones (1 to 5). Three rows of puffs follow a humped outline, and every pixel is
     * shaded as the front-most puff's dome under light from the upper left, darkened toward the flat base. Two passes of a 3 x 3
     * majority vote then merge thin streaks and stray pixels into a few clean shading shapes, and the top edge catches the light.
     */
    @NotNull
    static PixelSprite cumulus(@NotNull PixelRandom random, int width, int height, int tones) {
        int base = height - 1;
        float peak = 0.3F + random.next() * 0.4F;
        boolean hasSecond = random.next() < 0.65F;
        float secondPeak = 0.0F;
        float secondHeight = 0.0F;
        if (hasSecond) {
            secondPeak = (peak < 0.5F) ? (peak + 0.28F + random.next() * 0.14F) : (peak - 0.28F - random.next() * 0.14F);
            secondHeight = 0.5F + random.next() * 0.3F;
        }
        float noiseOffset = random.next() * 50.0F;
        Outline outline = new Outline(peak, hasSecond, secondPeak, secondHeight);

        // Puffs: x, y, radius, depth
        float[] puffs = new float[64 * 4];
        int puffCount = 0;
        float[][] rows = {{0.22F, 0.78F, 0.56F, 0.33F, 0.0F}, {0.12F, 0.88F, 0.3F, 0.31F, 2.0F}, {0.06F, 0.94F, 0.08F, 0.2F, 4.0F}};
        for (float[] row : rows) {
            for (float u = row[0]; u <= row[1]; ) {
                float e = outline.at(u);
                float radius = Math.max(2.4F, Math.max(height * 0.17F, height * row[3] * (0.35F + 0.65F * e) * (0.85F + random.next() * 0.3F)));
                if (puffCount * 4 + 4 > puffs.length) puffs = Arrays.copyOf(puffs, puffs.length * 2);
                puffs[puffCount * 4] = PixelMath.clamp(u * width, radius + 1.0F, width - radius - 1.0F);
                puffs[puffCount * 4 + 1] = base - height * row[2] * e;
                puffs[puffCount * 4 + 2] = radius;
                puffs[puffCount * 4 + 3] = row[4];
                puffCount++;
                u += radius * (1.0F + random.next() * 0.45F) / width;
            }
        }

        byte[] codes = new byte[width * height];
        float[] light = new float[width * height];
        int[] columnPuffs = new int[puffCount];
        int topY = height;
        for (int x = 0; x < width; x++) {
            int floor = base - Math.round(PixelMath.valueNoise(x * 0.13F + noiseOffset, 1.7F) * 1.7F);
            // Only the puffs that reach over this column can cover its pixels
            int active = 0;
            for (int p = 0; p < puffCount; p++) {
                if (Math.abs(x + 0.5F - puffs[p * 4]) < puffs[p * 4 + 2]) columnPuffs[active++] = p;
            }
            for (int y = 0; y <= floor; y++) {
                float best = -1.0F;
                float lit = 0.0F;
                for (int n = 0; n < active; n++) {
                    int p = columnPuffs[n];
                    float radius = puffs[p * 4 + 2];
                    float dx = x + 0.5F - puffs[p * 4];
                    float dy = y + 0.5F - puffs[p * 4 + 1];
                    float q = radius * radius - dx * dx - dy * dy;
                    if (q <= 0.0F) continue;
                    float z = (float)Math.sqrt(q);
                    if (z + puffs[p * 4 + 3] > best) {
                        best = z + puffs[p * 4 + 3];
                        lit = (dx * -0.4F + dy * -0.74F + z * 0.54F) / radius;
                    }
                }
                if (best >= 0.0F) {
                    codes[y * width + x] = 1;
                    light[y * width + x] = lit;
                    if (y < topY) topY = y;
                }
            }
        }

        float span = Math.max(1, base - topY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (codes[i] == 0) continue;
                float v = (light[i] * 0.5F + 0.5F) * 0.6F + (1.0F - (y - topY) / span) * 0.44F;
                int tone = 4;
                for (int t = 0; t < TONE_THRESHOLDS.length; t++) {
                    if (v > TONE_THRESHOLDS[t]) {
                        tone = t;
                        break;
                    }
                }
                codes[i] = (byte)(1 + Math.min(tones - 1, Math.round(tone * (tones - 1) / 4.0F)));
            }
        }

        int[] votes = new int[tones + 1];
        for (int pass = 0; pass < 2; pass++) {
            byte[] previous = codes.clone();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = y * width + x;
                    if (previous[i] == 0) continue;
                    Arrays.fill(votes, 0);
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int xx = x + dx;
                            int yy = y + dy;
                            if ((xx >= 0) && (yy >= 0) && (xx < width) && (yy < height) && (previous[yy * width + xx] != 0)) votes[previous[yy * width + xx]]++;
                        }
                    }
                    int winner = previous[i];
                    for (int k = 1; k <= tones; k++) {
                        if (votes[k] > votes[winner]) winner = k;
                    }
                    codes[i] = (byte)winner;
                }
            }
        }

        // The top edge catches the light
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = y * width + x;
                if (codes[i] != 0) {
                    if (codes[i] > 2) codes[i] = 2;
                    break;
                }
            }
        }
        return toSprite(width, height, codes);
    }

    /**
     * Builds a thin high-altitude streak: two tapering strokes that curl up toward one end.
     */
    @NotNull
    static PixelSprite wisp(@NotNull PixelRandom random, int width) {
        int height = WISP_HEIGHT;
        byte[] codes = new byte[width * height];
        for (int stroke = 0; stroke < 2; stroke++) {
            int x0 = (int)(random.next() * width * 0.3F);
            int x1 = width - (int)(random.next() * width * 0.3F);
            int y0 = 2 + stroke * 2 + (int)(random.next() * 2.0F);
            for (int x = x0; x < x1; x++) {
                float u = (x - x0) / (float)(x1 - x0);
                int y = Math.round(y0 - u * u * 2.0F);
                if ((y < 0) || (y >= height)) continue;
                codes[y * width + x] = 1;
                if ((u > 0.2F) && (u < 0.75F) && (y + 1 < height) && (codes[(y + 1) * width + x] == 0)) codes[(y + 1) * width + x] = 2;
            }
        }
        return toSprite(width, height, codes);
    }

    @NotNull
    private static PixelSprite toSprite(int width, int height, @NotNull byte[] codes) {
        char[] chars = new char[codes.length];
        for (int i = 0; i < codes.length; i++) chars[i] = (codes[i] == 0) ? '.' : (char)('a' + codes[i] - 1);
        return PixelSprite.fromCodes(width, height, chars);
    }

    /**
     * The humped outline of a cloud over its width (0 to 1): a main hump, maybe a second smaller one, and a low floor between them.
     */
    private record Outline(float peak, boolean hasSecond, float secondPeak, float secondHeight) {

        float at(float u) {
            float main = (u - this.peak) / 0.24F;
            float e = (float)Math.exp(-(main * main));
            if (this.hasSecond) {
                float second = (u - this.secondPeak) / 0.17F;
                e = Math.max(e, this.secondHeight * (float)Math.exp(-(second * second)));
            }
            return Math.max(e, 0.16F) * (float)Math.pow(Math.sin(Math.PI * PixelMath.clamp(u, 0.0F, 1.0F)), 0.4);
        }

    }

}
