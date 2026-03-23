package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

public class AfmaResidualPayload {

    protected int channels;

    public AfmaResidualPayload() {
    }

    public AfmaResidualPayload(int channels) {
        this.channels = channels;
    }

    public int getChannels() {
        return this.channels;
    }

    public void validate(@NotNull String context) {
        if (!AfmaResidualPayloadHelper.isValidChannelCount(this.channels)) {
            throw new IllegalArgumentException(context + " has an invalid residual channel count: " + this.channels);
        }
    }

}
