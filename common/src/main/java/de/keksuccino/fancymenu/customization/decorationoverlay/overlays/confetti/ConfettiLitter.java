package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The settled confetti: pieces that landed lie flat as thin litter on the surfaces, each on top of the highest litter under it, up to the
 * surface's {@link ConfettiSurface#maxLevels layer limit}. When there are more pieces than the cap allows, the oldest ones blink away,
 * and the pieces lying on top of a removed one sink into its place. Removed pieces get reused, so settling allocates nothing.
 */
final class ConfettiLitter {

    static final float DISSOLVE_TIME = 0.5F;

    /** Settled pieces, oldest first */
    final List<Piece> pieces = new ArrayList<>();
    private final List<Piece> spare = new ArrayList<>();
    private int dissolvingCount = 0;
    /** How many pieces can lie around before the oldest start to dissolve */
    int cap;

    ConfettiLitter(int cap) {
        this.cap = cap;
    }

    /**
     * Lays a piece flat on a surface, on top of the litter below it, unless the litter there is already as high as it gets
     * (see {@link ConfettiSurface#levelLimit}).
     *
     * @param widthInArtPixels how wide the piece lies, see {@link ConfettiArt#litterWidth}
     * @param x where the piece's left edge came down, in GUI pixels; the piece gets moved inside the surface
     * @param scale size of an art pixel in GUI pixels
     * @return true if the piece settled
     */
    boolean settle(int widthInArtPixels, int pair, @NotNull ConfettiSurface surface, float x, int scale) {
        int width = widthInArtPixels * scale;
        if (surface.width < width) return false;
        int left = Math.max(surface.x, Math.min(surface.x + surface.width - width, (int)Math.floor(x)));
        int level = surface.highestLevel(left, width);
        if (level >= surface.levelLimit(left, width, scale)) return false;
        for (int i = 0; i < width; i++) surface.levels[left - surface.x + i] = level + 1;
        Piece piece = this.spare.isEmpty() ? new Piece() : this.spare.remove(this.spare.size() - 1);
        piece.surface = surface;
        piece.x = left;
        piece.y = surface.y - (level + 1) * scale;
        piece.width = width;
        piece.level = level + 1;
        piece.pair = pair;
        piece.dissolving = -1.0F;
        this.pieces.add(piece);
        if (this.pieces.size() > this.cap) this.dissolveOldest();
        return true;
    }

    private void dissolveOldest() {
        for (int i = 0; i < this.pieces.size(); i++) {
            Piece oldest = this.pieces.get(i);
            if (oldest.dissolving < 0.0F) {
                oldest.dissolving = 0.0F;
                this.dissolvingCount++;
                return;
            }
        }
    }

    /**
     * Lets dissolving pieces blink away and removes them after {@value #DISSOLVE_TIME} seconds. A lowered cap dissolves one more piece per call.
     */
    void update(float deltaSeconds, int scale) {
        if (this.pieces.size() - this.dissolvingCount > this.cap) this.dissolveOldest();
        if (this.dissolvingCount == 0) return;
        for (int i = this.pieces.size() - 1; i >= 0; i--) {
            Piece gone = this.pieces.get(i);
            if ((gone.dissolving < 0.0F) || ((gone.dissolving += deltaSeconds) <= DISSOLVE_TIME)) continue;
            ConfettiSurface surface = gone.surface;
            for (int c = 0; c < gone.width; c++) {
                int column = gone.x - surface.x + c;
                if (surface.levels[column] >= gone.level) surface.levels[column]--;
            }
            for (int j = 0; j < this.pieces.size(); j++) {
                Piece other = this.pieces.get(j);
                if ((other != gone) && (other.surface == surface) && (other.level > gone.level) && (other.x < gone.x + gone.width) && (other.x + other.width > gone.x)) {
                    other.level--;
                    other.y += scale;
                }
            }
            this.pieces.remove(i);
            this.spare.add(gone);
            this.dissolvingCount--;
        }
    }

    /**
     * Removes the pieces lying on surfaces that were not seen in the given surfaces version (removed or moved elements).
     */
    void dropUnseen(int version) {
        for (int i = this.pieces.size() - 1; i >= 0; i--) {
            Piece piece = this.pieces.get(i);
            if (piece.surface.seenInVersion == version) continue;
            if (piece.dissolving >= 0.0F) this.dissolvingCount--;
            this.pieces.remove(i);
            this.spare.add(piece);
        }
    }

    /**
     * Removes all pieces. The layer heights of the surfaces have to be cleared too.
     */
    void clear() {
        this.spare.addAll(this.pieces);
        this.pieces.clear();
        this.dissolvingCount = 0;
    }

    int size() {
        return this.pieces.size();
    }

    static final class Piece {

        ConfettiSurface surface;
        int x;
        int y;
        /** Width in GUI pixels */
        int width;
        /** Layer the piece lies in, 1 for the lowest */
        int level;
        int pair;
        /** Seconds since the piece started to dissolve, below 0 while it doesn't */
        float dissolving;

        /**
         * The blink step of the dissolve: 0 while the piece stays, then 1 and 2 by turns, twelve times a second.
         */
        int blinkStep() {
            if (this.dissolving < 0.0F) return 0;
            return ((((int)(this.dissolving * 12.0F)) & 1) == 0) ? 1 : 2;
        }

    }

}
