package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public enum AfmaSparseLayoutCodec {

    BITMASK(0, 0),
    ROW_SPANS(1, 1),
    TILE_MASK(2, 2),
    COORD_LIST(3, 2);

    private final int id;
    private final int complexityScore;

    AfmaSparseLayoutCodec(int id, int complexityScore) {
        this.id = id;
        this.complexityScore = complexityScore;
    }

    public int getId() {
        return this.id;
    }

    public int getComplexityScore() {
        return this.complexityScore;
    }

    @NotNull
    public static AfmaSparseLayoutCodec byId(int id) throws IOException {
        for (AfmaSparseLayoutCodec codec : values()) {
            if (codec.id == id) {
                return codec;
            }
        }
        throw new IOException("Unknown AFMA sparse layout codec id: " + id);
    }

}
