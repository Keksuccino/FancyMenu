package de.keksuccino.fancymenu.rendering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AspectRatio {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final int width;
    protected final int height;

    public AspectRatio(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getInputWidth() {
        return this.width;
    }

    public int getInputHeight() {
        return this.height;
    }

    public int getAspectRatioWidth(int givenHeight) {
        double ratio = (double)this.getInputWidth() / (double)this.getInputHeight();
        return (int)((double)givenHeight * ratio);
    }

    public int getAspectRatioHeight(int givenWidth) {
        double ratio = (double)this.getInputWidth() / (double)this.getInputHeight();
        return (int)((double)givenWidth / ratio);
    }

    /**
     * Will calculate the aspect ratio and never gets smaller than the given width and height.
     *
     * @return The nearest possible width (index 0) and height (index 1).
     **/
    public int[] getAspectRatioSize(int givenWidth, int givenHeight) {
        int aw = this.getAspectRatioWidth(givenHeight);
        int ah = givenHeight;
        if (aw < givenWidth) {
            ah = this.getAspectRatioHeight(givenWidth);
            aw = givenWidth;
        }
        return new int[]{aw,ah};
    }

}
