package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.profile.ProfileTask;
import de.keksuccino.fancymenu.customization.profile.ProfileTaskProgress;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * Covers the profile manager while a profile operation runs, and shows its progress with percentage, processed size and remaining time.
 */
final class ProfileTaskOverlay {

    private static final float CARD_WIDTH = 300.0F;
    private static final float BAR_HEIGHT = 6.0F;
    /** Operations that finish quickly would only flash the overlay, so it fades in after this delay. */
    private static final long SHOW_DELAY_MS = 150L;
    private static final long FADE_MS = 150L;

    @NotNull
    private final ExtendedButton cancelButton;
    @Nullable
    private ProfileTask currentTask;
    private long busySinceMs = -1L;

    ProfileTaskOverlay() {
        this.cancelButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.common_components.cancel"), button -> {
            ProfileTask task = this.currentTask;
            if (task != null) task.cancel();
        });
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());
    }

    void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int width, int height, @Nullable ProfileTask task, boolean switching) {

        this.currentTask = task;
        if ((task == null) && !switching) {
            this.busySinceMs = -1L;
            return;
        }
        long now = System.currentTimeMillis();
        if (this.busySinceMs < 0L) this.busySinceMs = now;
        float alpha = Math.min(1.0F, Math.max(0.0F, (now - this.busySinceMs - SHOW_DELAY_MS) / (float) FADE_MS));
        if (!UIBase.shouldPlayAnimations()) alpha = ((now - this.busySinceMs) >= SHOW_DELAY_MS) ? 1.0F : 0.0F;
        if (alpha <= 0.0F) return;

        UIBase.fillF(graphics, 0, 0, width, height, ProfileUiRendering.withAlpha(UIBase.getUITheme().ui_interface_background_color.getColorInt(), 0.82F * alpha));

        boolean cancellable = (task != null) && task.isCancellable() && !task.getProgress().isCancelRequested();
        float smallSize = UIBase.getUITextSizeSmall();
        float normalSize = UIBase.getUITextSizeNormal();
        float cardHeight = 16 + UIBase.getUITextHeight(normalSize) + 12 + BAR_HEIGHT + 8 + UIBase.getUITextHeight(smallSize) + 16 + (cancellable ? 28 : 0);
        float cardWidth = Math.min(CARD_WIDTH, width - 40);
        float cardX = (width - cardWidth) / 2.0F;
        float cardY = (height - cardHeight) / 2.0F;
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        UIBase.renderRoundedRect(graphics, cardX, cardY, cardWidth, cardHeight, radius, radius, radius, radius, ProfileUiRendering.withAlpha(ProfileUiRendering.getAreaColor() | 0xFF000000, alpha));
        UIBase.renderRoundedBorder(graphics, cardX, cardY, cardX + cardWidth, cardY + cardHeight, 1, radius, radius, radius, radius, ProfileUiRendering.withAlpha(ProfileUiRendering.getBorderColor(), alpha));

        String titleKey = (task != null) ? ("fancymenu.profiles.task." + task.getType().name().toLowerCase(Locale.ROOT)) : "fancymenu.profiles.task.switch";
        if ((task != null) && task.getProgress().isCancelRequested()) titleKey = "fancymenu.profiles.task.cancelling";
        Component title = Component.translatable(titleKey).withStyle(Style.EMPTY.withBold(true));
        float innerX = cardX + 16;
        float innerWidth = cardWidth - 32;
        float lineY = cardY + 16;
        UIBase.renderText(graphics, title, innerX, lineY, ProfileUiRendering.withAlpha(ProfileUiRendering.getTextColor(), alpha), normalSize);
        lineY += UIBase.getUITextHeight(normalSize) + 12;

        ProfileTaskProgress progress = (task != null) ? task.getProgress() : null;
        boolean determinate = (progress != null) && (progress.getTotalBytes() > 0L);
        float barRadius = BAR_HEIGHT / 2.0F;
        UIBase.renderRoundedRect(graphics, innerX, lineY, innerWidth, BAR_HEIGHT, barRadius, barRadius, barRadius, barRadius, ProfileUiRendering.withAlpha(ProfileUiRendering.getSecondaryAreaColor() | 0xFF000000, alpha));
        int barColor = ProfileUiRendering.withAlpha(ProfileUiRendering.getAccentColor(), alpha);
        if (determinate) {
            float fillWidth = Math.max(BAR_HEIGHT, innerWidth * progress.getProgress());
            UIBase.renderRoundedRect(graphics, innerX, lineY, fillWidth, BAR_HEIGHT, barRadius, barRadius, barRadius, barRadius, barColor);
        } else {
            // Without known total, a segment sweeps across the bar, so the operation still visibly works
            float segmentWidth = innerWidth * 0.3F;
            float phase = (now % 1400L) / 1400.0F;
            float segmentX = innerX - segmentWidth + ((innerWidth + segmentWidth) * phase);
            float visibleStart = Math.max(innerX, segmentX);
            float visibleEnd = Math.min(innerX + innerWidth, segmentX + segmentWidth);
            if (visibleEnd > visibleStart) {
                UIBase.renderRoundedRect(graphics, visibleStart, lineY, visibleEnd - visibleStart, BAR_HEIGHT, barRadius, barRadius, barRadius, barRadius, barColor);
            }
        }
        lineY += BAR_HEIGHT + 8;

        int dimColor = ProfileUiRendering.withAlpha(ProfileUiRendering.getDimTextColor(), alpha);
        if (determinate) {
            String detail = I18n.get("fancymenu.profiles.task.progress", Math.round(progress.getProgress() * 100.0F), ProfileUiRendering.formatBytes(progress.getProcessedBytes()), ProfileUiRendering.formatBytes(progress.getTotalBytes()));
            UIBase.renderText(graphics, Component.literal(detail), innerX, lineY, dimColor, smallSize);
            long remaining = progress.getEstimatedRemainingMs();
            if (remaining >= 0L) {
                Component remainingText = Component.literal(ProfileUiRendering.formatRemainingTime(remaining));
                UIBase.renderText(graphics, remainingText, innerX + innerWidth - UIBase.getUITextWidth(remainingText, smallSize), lineY, dimColor, smallSize);
            }
        } else {
            UIBase.renderText(graphics, Component.translatable("fancymenu.profiles.task.please_wait"), innerX, lineY, dimColor, smallSize);
        }
        lineY += UIBase.getUITextHeight(smallSize) + 16;

        this.cancelButton.visible = cancellable;
        this.cancelButton.active = cancellable;
        if (cancellable) {
            this.cancelButton.setX((int) (cardX + ((cardWidth - this.cancelButton.getWidth()) / 2.0F)));
            this.cancelButton.setY((int) (lineY - 4));
            this.cancelButton.render(graphics, mouseX, mouseY, partial);
        }

    }

    /**
     * Consumes all clicks while visible, so nothing below the overlay can get used.
     */
    boolean mouseClicked(double mouseX, double mouseY, int button, boolean visible) {
        if (!visible) return false;
        if (this.cancelButton.visible) this.cancelButton.mouseClicked(mouseX, mouseY, button);
        return true;
    }

}
