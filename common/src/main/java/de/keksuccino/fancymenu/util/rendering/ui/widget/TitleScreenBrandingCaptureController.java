package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.mixin.interfaces.TitleScreenBrandingController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Owns the transformed branding captured for one title-screen instance.
 *
 * <p>The target is replaceable because vanilla can initialize the same screen more than once. A replacement target is
 * initialized with the existing capture before it becomes active, preventing reinitialization from temporarily
 * restoring the renderer's default branding or bounds.</p>
 */
public final class TitleScreenBrandingCaptureController {

    @Nullable private Target target;
    @Nullable private String capturedBrandingText;

    /**
     * Replaces the widget receiving branding values. Passing {@code null} detaches the previous widget.
     *
     * <p>If branding was already captured, the replacement receives it immediately. The target is committed only after
     * the complete update succeeds, so a failed replacement can be retried without losing the previous target.</p>
     */
    public void setTarget(@Nullable Target target) {
        if ((target != null) && (this.capturedBrandingText != null)) this.applyToTarget(target, this.capturedBrandingText);
        this.target = target;
    }

    /**
     * Applies a newly observed branding value to the current widget.
     *
     * <p>The value is committed only after the complete target update succeeds. This keeps a transient target failure
     * retryable on the next frame instead of treating a partially applied value as complete.</p>
     *
     * @return whether the current target was updated successfully
     */
    public boolean capture(@NotNull String brandingText) {
        Objects.requireNonNull(brandingText, "brandingText");
        if (brandingText.equals(this.capturedBrandingText)) return false;
        Target target = this.target;
        if (target == null) return false;
        this.applyToTarget(target, brandingText);
        this.capturedBrandingText = brandingText;
        return true;
    }

    private void applyToTarget(@NotNull Target target, @NotNull String brandingText) {
        target.setBrandingText(brandingText);
        if (!target.hasCustomWidth()) target.resizeWidthToContent();
        if (!target.hasCustomHeight()) target.resizeHeightToContent();
    }

    @Nullable
    public String getCapturedBrandingText() {
        return this.capturedBrandingText;
    }

    /**
     * Captures the invocation argument and then tells Mixin Extras to suppress the original draw call.
     */
    public static boolean captureAndSuppress(@NotNull TitleScreenBrandingController controller, @NotNull String brandingText) {
        Objects.requireNonNull(controller, "controller").fancymenu$setBrandingText(Objects.requireNonNull(brandingText, "brandingText"));
        return false;
    }

    /**
     * Minimal widget contract kept independent of Minecraft types so capture and sizing behavior can be tested in isolation.
     */
    public interface Target {

        void setBrandingText(@NotNull String brandingText);

        boolean hasCustomWidth();

        void resizeWidthToContent();

        boolean hasCustomHeight();

        void resizeHeightToContent();
    }
}
