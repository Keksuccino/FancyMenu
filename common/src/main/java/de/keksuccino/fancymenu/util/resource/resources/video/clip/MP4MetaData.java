package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class MP4MetaData {

    public int width = 1;
    public int height = 1;
    public boolean pureRef = false;
    public int timescale = 1;
    public double totalVideoDuration = 1.0D;
    public int totalVideoFrames = 1;
    public double videoFps = 1.0D;

    @NotNull
    protected MP4MetaData copy() {
        MP4MetaData clone = new MP4MetaData();
        clone.width = this.width;
        clone.height = this.height;
        clone.pureRef = this.pureRef;
        clone.timescale = this.timescale;
        clone.totalVideoDuration = this.totalVideoDuration;
        clone.totalVideoFrames = this.totalVideoFrames;
        clone.videoFps = this.videoFps;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MP4MetaData that)) return false;
        return width == that.width
                && height == that.height
                && pureRef == that.pureRef
                && timescale == that.timescale
                && Double.compare(that.totalVideoDuration, totalVideoDuration) == 0
                && totalVideoFrames == that.totalVideoFrames
                && Double.compare(that.videoFps, videoFps) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, pureRef, timescale, totalVideoDuration, totalVideoFrames, videoFps);
    }

}
