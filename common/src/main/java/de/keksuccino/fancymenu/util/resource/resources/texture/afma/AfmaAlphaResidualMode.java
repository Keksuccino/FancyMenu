package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public enum AfmaAlphaResidualMode {

    NONE(0),
    FULL(1),
    SPARSE(2);

    private final int id;

    AfmaAlphaResidualMode(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    @NotNull
    public static AfmaAlphaResidualMode byId(int id) throws IOException {
        for (AfmaAlphaResidualMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        throw new IOException("Unknown AFMA alpha residual mode id: " + id);
    }

}
