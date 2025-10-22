package de.keksuccino.fancymenu.util.rendering.ui.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class SimpleToast implements Toast {

    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/tutorial");
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int PROGRESS_BAR_Y = 28;

    @NotNull
    private final Icon icon;
    @NotNull
    private final Component title;
    @Nullable
    private final Component message;
    private Toast.Visibility visibility;
    private long lastProgressTime;
    private float lastProgress;
    private float progress;
    private final boolean progressable;

    public SimpleToast(@NotNull Icon icon, @NotNull Component title, @Nullable Component message, boolean progressable) {
        this.visibility = Visibility.SHOW;
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.progressable = progressable;
    }

    @NotNull
    @Override
    public Toast.Visibility render(GuiGraphics graphics, @NotNull ToastComponent toastComponent, long l) {

        graphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());

        this.icon.render(graphics, 6, 6);

        if (this.message == null) {
            graphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 12, -11534256, false);
        } else {
            graphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, -11534256, false);
            graphics.drawString(toastComponent.getMinecraft().font, this.message, 30, 18, -16777216, false);
        }

        if (this.progressable) {
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, -1);
            float clampProgress = Mth.clampedLerp(this.lastProgress, this.progress, (float)(l - this.lastProgressTime) / 100.0F);
            int progressColor;
            if (this.progress >= this.lastProgress) {
                progressColor = -16755456;
            } else {
                progressColor = -11206656;
            }
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, (int)((float)PROGRESS_BAR_X + (float)PROGRESS_BAR_WIDTH * clampProgress), PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, progressColor);
            this.lastProgress = clampProgress;
            this.lastProgressTime = l;
        }

        return this.visibility;

    }

    public void hide() {
        this.visibility = Visibility.HIDE;
    }

    public void updateProgress(float progress) {
        this.progress = progress;
    }

    public static class Icon {

        @NotNull
        protected ResourceLocation location;

        /**
         * A 20x20 pixels icon texture for displaying in the toast.
         */
        public Icon(@NotNull ResourceLocation location) {
            this.location = location;
        }

        public void render(GuiGraphics graphics, int x, int y) {
            RenderSystem.enableBlend();
            graphics.blit(this.location, x, y, 0.0F, 0.0F, 20, 20, 20, 20);
        }

    }

}
