package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the final branding text captured from a title-screen render call and determines which uncustomized widget bounds need refreshing.
 * Instances must remain scoped to one title screen so branding transformed for one screen cannot leak into another.
 */
final class TitleScreenBrandingCapture {

    @Nullable
    private String capturedText;

    @NotNull
    Update capture(@NotNull String brandingText, @Nullable Integer customWidth, @Nullable Integer customHeight) {
        if (brandingText.equals(this.capturedText)) return Update.NONE;
        this.capturedText = brandingText;
        return new Update(true, customWidth == null, customHeight == null);
    }

    @Nullable
    String getCapturedText() {
        return this.capturedText;
    }

    record Update(boolean textChanged, boolean resizeWidth, boolean resizeHeight) {

        private static final Update NONE = new Update(false, false, false);
    }
}
