package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

public class AfmaBlockInter {

    protected int tile_size;

    public AfmaBlockInter() {
    }

    public AfmaBlockInter(int tileSize) {
        this.tile_size = tileSize;
    }

    public int getTileSize() {
        return this.tile_size;
    }

    public void validate(@NotNull String context, int width, int height) {
        if (this.tile_size <= 0) {
            throw new IllegalArgumentException(context + " has an invalid block tile size: " + this.tile_size);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(context + " has an invalid block region size: " + width + "x" + height);
        }
    }

}
