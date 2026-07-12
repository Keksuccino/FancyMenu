package de.keksuccino.fancymenu.mixin.support.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the final branding text captured from a title-screen render call and determines which uncustomized widget bounds need refreshing.
 * Instances must remain scoped to one title screen so branding transformed for one screen cannot leak into another.
 */
public final class TitleScreenBrandingCapture {

    @Nullable
    private String capturedText;

    public TitleScreenBrandingCapture() {
    }

    @NotNull
    public Update capture(@NotNull String brandingText, @Nullable Integer customWidth, @Nullable Integer customHeight) {
        if (brandingText.equals(this.capturedText)) return Update.NONE;
        this.capturedText = brandingText;
        return new Update(true, customWidth == null, customHeight == null);
    }

    @Nullable
    public String getCapturedText() {
        return this.capturedText;
    }

    public record Update(boolean textChanged, boolean resizeWidth, boolean resizeHeight) {

        private static final Update NONE = new Update(false, false, false);
    }
}
