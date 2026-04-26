package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public enum AfmaResidualCodec {

    INTERLEAVED(0, 0),
    PLANAR(1, 1),
    PLANAR_ZIGZAG(2, 2),
    PLANAR_ZIGZAG_DELTA(3, 3);

    private final int id;
    private final int complexityScore;

    AfmaResidualCodec(int id, int complexityScore) {
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
    public static AfmaResidualCodec byId(int id) throws IOException {
        for (AfmaResidualCodec codec : values()) {
            if (codec.id == id) {
                return codec;
            }
        }
        throw new IOException("Unknown AFMA residual codec id: " + id);
    }

}
