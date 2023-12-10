package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import org.jcodec.common.model.*;
import org.jcodec.scale.AWTUtil;
import org.jetbrains.annotations.NotNull;
import java.awt.image.BufferedImage;
import java.util.List;

public class MP4Frame extends Frame {

    @NotNull
    protected final BufferedImage bufferedImage;

    public MP4Frame(Picture pic, RationalLarge pts, RationalLarge duration, Rational pixelAspect, int frameNo, TapeTimecode tapeTimecode, List<String> messages) {
        super(pic, pts, duration, pixelAspect, frameNo, tapeTimecode, messages);
        this.bufferedImage = AWTUtil.toBufferedImage(pic);
    }

    @NotNull
    public BufferedImage getBufferedImage() {
        return this.bufferedImage;
    }

}
