package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Tells users that they can drop profile files onto the profile manager.<br>
 * Shows a drop zone while a file gets dragged over the window, and an error when a dropped file is no profile file.
 */
final class ProfileDropIndicator {

    private static final long FADE_MS = 120L;
    private static final long REJECTED_SHOW_MS = 2400L;
    private static final float ZONE_INSET = 10.0F;
    private static final float ICON_SIZE = 32.0F;
    /** The backdrop hides the window content almost fully, so the drop zone reads clean and nothing below competes with it. */
    private static final float BACKDROP_OPACITY = 0.96F;
    /** The icon of the drop zone bobs gently while a file hovers over the window, so the zone feels alive. */
    private static final float ICON_BOB_HEIGHT = 2.0F;
    private static final long ICON_BOB_PERIOD_MS = 1200L;

    private float dragAlpha = 0.0F;
    private long lastFrameMs = -1L;
    private long rejectedSinceMs = -1L;
    private long rejectedUntilMs = -1L;

    /**
     * Renders the drop zone while a file gets dragged over the window, and the error after a file that is no profile file got dropped.
     */
    void renderOverlay(@NotNull GuiGraphics graphics, float width, float height, boolean fileDraggedOver) {

        long now = System.currentTimeMillis();
        long frameMs = (this.lastFrameMs < 0L) ? 0L : (now - this.lastFrameMs);
        this.lastFrameMs = now;
        // A new drag replaces the error right away
        if (fileDraggedOver) this.rejectedUntilMs = -1L;
        float step = UIBase.shouldPlayAnimations() ? (frameMs / (float) FADE_MS) : 1.0F;
        this.dragAlpha = fileDraggedOver ? Math.min(1.0F, this.dragAlpha + step) : Math.max(0.0F, this.dragAlpha - step);
        float rejectedAlpha = this.getRejectedAlpha(now);
        float backdropAlpha = Math.max(this.dragAlpha, rejectedAlpha);
        if (backdropAlpha <= 0.0F) return;

        UIBase.fillF(graphics, 0, 0, width, height, ProfileUiRendering.withAlpha(UIBase.getUITheme().ui_interface_background_color.getColorInt(), BACKDROP_OPACITY * backdropAlpha));
        if (this.dragAlpha > 0.0F) {
            float bob = UIBase.shouldPlayAnimations() ? (float) Math.sin((now % ICON_BOB_PERIOD_MS) / (double) ICON_BOB_PERIOD_MS * Math.PI * 2.0) * ICON_BOB_HEIGHT : 0.0F;
            this.renderZone(graphics, width, height, this.dragAlpha, ProfileUiRendering.getAccentColor(), MaterialIcons.DOWNLOAD, "fancymenu.profiles.drop.title", "fancymenu.profiles.drop.desc", bob);
        }
        if (rejectedAlpha > 0.0F) {
            this.renderZone(graphics, width, height, rejectedAlpha, ProfileUiRendering.getErrorColor(), MaterialIcons.BLOCK, "fancymenu.profiles.drop.rejected", "fancymenu.profiles.drop.rejected.desc", 0.0F);
        }

    }

    private void renderZone(@NotNull GuiGraphics graphics, float width, float height, float alpha, int color, @NotNull MaterialIcon icon, @NotNull String titleKey, @NotNull String descriptionKey, float iconOffsetY) {

        float zoneWidth = width - (ZONE_INSET * 2.0F);
        float zoneHeight = height - (ZONE_INSET * 2.0F);
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        UIBase.renderRoundedRect(graphics, ZONE_INSET, ZONE_INSET, zoneWidth, zoneHeight, radius, radius, radius, radius, ProfileUiRendering.withAlpha(color, 0.1F * alpha));
        UIBase.renderRoundedBorder(graphics, ZONE_INSET, ZONE_INSET, ZONE_INSET + zoneWidth, ZONE_INSET + zoneHeight, 2, radius, radius, radius, radius, ProfileUiRendering.withAlpha(color, alpha));

        float titleSize = UIBase.getUITextSizeNormal();
        float descriptionSize = UIBase.getUITextSizeSmall();
        float maxTextWidth = zoneWidth - 24.0F;
        Component title = Component.literal(ProfileUiRendering.trimToWidth(I18n.get(titleKey), maxTextWidth, titleSize)).withStyle(Style.EMPTY.withBold(true));
        Component description = Component.literal(ProfileUiRendering.trimToWidth(I18n.get(descriptionKey), maxTextWidth, descriptionSize));
        float titleHeight = UIBase.getUITextHeight(titleSize);
        float contentHeight = ICON_SIZE + 10.0F + titleHeight + 4.0F + UIBase.getUITextHeight(descriptionSize);
        float top = (height - contentHeight) / 2.0F;

        icon.render(graphics, (width - ICON_SIZE) / 2.0F, top + iconOffsetY, ICON_SIZE, ICON_SIZE, ProfileUiRendering.withAlpha(color, alpha));
        float titleY = top + ICON_SIZE + 10.0F;
        UIBase.renderText(graphics, title, (width - UIBase.getUITextWidth(title, titleSize)) / 2.0F, titleY, ProfileUiRendering.withAlpha(ProfileUiRendering.getTextColor(), alpha), titleSize);
        UIBase.renderText(graphics, description, (width - UIBase.getUITextWidth(description, descriptionSize)) / 2.0F, titleY + titleHeight + 4.0F, ProfileUiRendering.withAlpha(ProfileUiRendering.getDimTextColor(), alpha), descriptionSize);

    }

    /**
     * Shows that the dropped files contained no profile file.
     */
    void showRejected() {
        long now = System.currentTimeMillis();
        this.rejectedSinceMs = now;
        this.rejectedUntilMs = now + REJECTED_SHOW_MS;
    }

    /**
     * Starts fading out the error right away, because it must never be in the way of the next action.
     */
    void dismissRejected() {
        long fadeOutEnd = System.currentTimeMillis() + FADE_MS;
        if (this.rejectedUntilMs > fadeOutEnd) this.rejectedUntilMs = fadeOutEnd;
    }

    private float getRejectedAlpha(long now) {
        if ((this.rejectedUntilMs < 0L) || (now >= this.rejectedUntilMs)) return 0.0F;
        if (!UIBase.shouldPlayAnimations()) return 1.0F;
        float fadeIn = (now - this.rejectedSinceMs) / (float) FADE_MS;
        float fadeOut = (this.rejectedUntilMs - now) / (float) FADE_MS;
        return Math.max(0.0F, Math.min(1.0F, Math.min(fadeIn, fadeOut)));
    }

}
