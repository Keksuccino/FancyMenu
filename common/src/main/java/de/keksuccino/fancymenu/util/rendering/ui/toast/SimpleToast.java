package de.keksuccino.fancymenu.util.rendering.ui.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleToast implements Toast {

    protected static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/toasts.png");
    
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int PROGRESS_BAR_Y = 28;

    @NotNull
    protected final Icon icon;
    @NotNull
    protected final Component title;
    @Nullable
    protected final Component message;
    protected Toast.Visibility visibility;
    protected long lastProgressTime;
    protected float lastProgress;
    protected float progress;
    protected final boolean progressable;
    @Nullable
    protected ResourceSupplier<ITexture> customBackground;
    int width = 160;
    int height = 32;

    public SimpleToast(@NotNull Icon icon, @NotNull Component title, @Nullable Component message, boolean progressable) {
        this.visibility = Visibility.SHOW;
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.progressable = progressable;
    }

    @NotNull
    @Override
    public Toast.Visibility render(@NotNull GuiGraphics graphics, @NotNull ToastComponent toastComponent, long progressTime) {

        ResourceLocation customBack = this.getCustomBackground();
        if (customBack == null) {
            graphics.blit(TEXTURE, 0, 0, 0, 96, this.width(), this.height());
        } else {
            graphics.blit(customBack, 0, 0, 0.0F, 0.0F, this.width(), this.height(), this.width(), this.height());
        }

        this.icon.render(graphics, 6, 6);

        if (this.message == null) {
            graphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 12, -11534256, false);
        } else {
            graphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, -11534256, false);
            graphics.drawString(toastComponent.getMinecraft().font, this.message, 30, 18, -16777216, false);
        }

        if (this.progressable) {
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, -1);
            float clampProgress = Mth.clampedLerp(this.lastProgress, this.progress, (float)(progressTime - this.lastProgressTime) / 100.0F);
            int progressColor;
            if (this.progress >= this.lastProgress) {
                progressColor = -16755456;
            } else {
                progressColor = -11206656;
            }
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, (int)((float)PROGRESS_BAR_X + (float)PROGRESS_BAR_WIDTH * clampProgress), PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, progressColor);
            this.lastProgress = clampProgress;
            this.lastProgressTime = progressTime;
        }

        return this.visibility;

    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @NotNull
    public SimpleToast setHeight(int height) {
        this.height = height;
        return this;
    }

    @NotNull
    public SimpleToast setWidth(int width) {
        this.width = width;
        return this;
    }

    @Nullable
    protected ResourceLocation getCustomBackground() {
        if (this.customBackground != null) {
            ITexture tex = this.customBackground.get();
            if (tex != null) {
                return tex.getResourceLocation();
            }
        }
        return null;
    }

    @NotNull
    public SimpleToast setCustomBackground(@Nullable ResourceSupplier<ITexture> texture) {
        this.customBackground = texture;
        return this;
    }

    public void hide() {
        this.visibility = Visibility.HIDE;
    }

    public void updateProgress(float progress) {
        this.progress = progress;
    }

    public static class Icon {

        protected ResourceLocation location;
        protected ResourceSupplier<ITexture> supplier;

        /**
         * A 20x20 pixels icon texture for displaying in the toast.
         */
        public Icon(@NotNull ResourceLocation textureLocation) {
            this.location = textureLocation;
        }

        /**
         * A 20x20 pixels icon texture for displaying in the toast.
         */
        public Icon(@NotNull ResourceSupplier<ITexture> textureSupplier) {
            this.supplier = textureSupplier;
        }

        public void render(GuiGraphics graphics, int x, int y) {
            ResourceLocation icon = this.getIcon();
            if (icon != null) {
                RenderSystem.enableBlend();
                graphics.blit(icon, x, y, 0.0F, 0.0F, 20, 20, 20, 20);
            }
        }

        @Nullable
        protected ResourceLocation getIcon() {
            if (this.location != null) return this.location;
            if (this.supplier != null) {
                ITexture tex = this.supplier.get();
                if (tex != null) {
                    return tex.getResourceLocation();
                }
            }
            return null;
        }

    }

}
