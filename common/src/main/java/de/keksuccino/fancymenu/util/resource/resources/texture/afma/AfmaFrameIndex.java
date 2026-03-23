package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AfmaFrameIndex {

    @Nullable
    protected List<AfmaFrameDescriptor> frames;
    @Nullable
    protected List<AfmaFrameDescriptor> intro_frames;

    public AfmaFrameIndex() {
    }

    public AfmaFrameIndex(@NotNull List<AfmaFrameDescriptor> frames, @Nullable List<AfmaFrameDescriptor> introFrames) {
        this.frames = new ArrayList<>(frames);
        this.intro_frames = (introFrames != null) ? new ArrayList<>(introFrames) : new ArrayList<>();
    }

    @NotNull
    public List<AfmaFrameDescriptor> getFrames() {
        return (this.frames != null) ? this.frames : List.of();
    }

    @NotNull
    public List<AfmaFrameDescriptor> getIntroFrames() {
        return (this.intro_frames != null) ? this.intro_frames : List.of();
    }

}
