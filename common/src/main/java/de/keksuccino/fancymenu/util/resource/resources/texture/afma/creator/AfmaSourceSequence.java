package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class AfmaSourceSequence {

    @NotNull
    protected final List<File> frames;

    public AfmaSourceSequence(@NotNull List<File> frames) {
        Objects.requireNonNull(frames);
        this.frames = List.copyOf(frames);
    }

    @NotNull
    public static AfmaSourceSequence ofFiles(@NotNull List<File> frames) {
        return new AfmaSourceSequence(frames);
    }

    @NotNull
    public static AfmaSourceSequence empty() {
        return new AfmaSourceSequence(List.of());
    }

    public boolean isEmpty() {
        return this.frames.isEmpty();
    }

    public int size() {
        return this.frames.size();
    }

    @Nullable
    public File getFrame(int index) {
        if (index < 0 || index >= this.frames.size()) return null;
        return this.frames.get(index);
    }

    @NotNull
    public List<File> getFrames() {
        return this.frames;
    }

}
