package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.util.rendering.IconAnimation;
import de.keksuccino.fancymenu.util.rendering.IconAnimations;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class UIIconButton implements Renderable, GuiEventListener, NarratableEntry {

    private static final float DEFAULT_ICON_PADDING = 6.0F;
    private static final float DEFAULT_HOVER_BACKGROUND_PADDING = 2.0F;

    private float x;
    private float y;
    private float width;
    private float height;
    private float iconPadding = DEFAULT_ICON_PADDING;
    private float hoverBackgroundPadding = DEFAULT_HOVER_BACKGROUND_PADDING;
    private float iconAlpha = 1.0F;
    @Nonnull
    private MaterialIcon icon;
    @Nullable
    private Consumer<UIIconButton> clickAction;
    @Nullable
    private IconAnimation iconHoverAnimation = IconAnimations.SHORT_DIAGONAL_BOUNCE;
    @Nullable
    private IconAnimation.Instance iconHoverAnimationInstance = this.iconHoverAnimation.createInstance();
    private boolean hovered = false;
    private boolean focused = false;

    public UIIconButton(float x, float y, float width, float height, @Nonnull MaterialIcon icon, @Nullable Consumer<UIIconButton> clickAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.icon = Objects.requireNonNull(icon, "icon");
        this.clickAction = clickAction;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean wasHovered = this.hovered;
        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.updateHoverAnimation(wasHovered);

        RenderSystem.enableBlend();
        if (this.hovered) {
            float padding = Math.max(0.0F, this.hoverBackgroundPadding);
            float backgroundWidth = this.width - (padding * 2.0F);
            float backgroundHeight = this.height - (padding * 2.0F);
            if (backgroundWidth > 0.0F && backgroundHeight > 0.0F) {
                UIBase.renderIconButtonHoverBackground(graphics, this.x + padding, this.y + padding, backgroundWidth, backgroundHeight);
            }
        }

        IconAnimation.Offset offset = this.getHoverOffset();
        this.renderIcon(graphics, offset);
    }

    private void updateHoverAnimation(boolean wasHovered) {
        if (this.iconHoverAnimationInstance == null) {
            return;
        }
        if (!UIBase.shouldPlayAnimations()) {
            this.iconHoverAnimationInstance.reset();
            return;
        }
        if (this.hovered) {
            if (!wasHovered) {
                this.iconHoverAnimationInstance.start();
            }
        } else {
            this.iconHoverAnimationInstance.reset();
        }
    }

    @Nonnull
    private IconAnimation.Offset getHoverOffset() {
        if (!UIBase.shouldPlayAnimations()) {
            return IconAnimation.Offset.ZERO;
        }
        if (this.iconHoverAnimationInstance == null) {
            return IconAnimation.Offset.ZERO;
        }
        return this.iconHoverAnimationInstance.getOffset();
    }

    private void renderIcon(@Nonnull GuiGraphics graphics, @Nonnull IconAnimation.Offset offset) {
        float maxSize = Math.min(this.width, this.height);
        float padding = Math.max(0.0F, this.iconPadding);
        float baseSize = Math.max(1.0F, maxSize - (padding * 2.0F));
        float areaWidth = Math.max(1.0F, baseSize + offset.widthOffset());
        float areaHeight = Math.max(1.0F, baseSize + offset.heightOffset());
        IconRenderData iconData = resolveMaterialIconData(this.icon, areaWidth, areaHeight);
        if (iconData == null) {
            return;
        }
        float baseX = this.x + (this.width - areaWidth) * 0.5F;
        float baseY = this.y + (this.height - areaHeight) * 0.5F;
        float drawX = baseX + offset.x();
        float drawY = baseY + offset.y();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        UIBase.getUITheme().setUITextureShaderColor(graphics, this.iconAlpha);
        blitScaledIcon(graphics, iconData, drawX, drawY, areaWidth, areaHeight, offset.rotationDegrees());
        UIBase.resetShaderColor(graphics);
    }

    @Nullable
    private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon, float renderWidth, float renderHeight) {
        if (icon == null) {
            return null;
        }
        float safeRenderWidth = Math.max(1.0F, renderWidth);
        float safeRenderHeight = Math.max(1.0F, renderHeight);
        ResourceLocation location = icon.getTextureLocationForUI(safeRenderWidth, safeRenderHeight);
        if (location == null) {
            return null;
        }
        int size = icon.getTextureSizeForUI(safeRenderWidth, safeRenderHeight);
        int width = icon.getWidth(size);
        int height = icon.getHeight(size);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new IconRenderData(location, width, height);
    }

    private static void blitScaledIcon(@Nonnull GuiGraphics graphics, @Nonnull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight, float rotationDegrees) {
        if (areaWidth <= 0.0F || areaHeight <= 0.0F || iconData.width <= 0 || iconData.height <= 0) {
            return;
        }
        float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return;
        }
        float scaledWidth = iconData.width * scale;
        float scaledHeight = iconData.height * scale;
        float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
        float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        if (rotationDegrees != 0.0F) {
            graphics.pose().translate(iconData.width * 0.5F, iconData.height * 0.5F, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
            graphics.pose().translate(-iconData.width * 0.5F, -iconData.height * 0.5F, 0.0F);
        }
        graphics.blit(iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
        graphics.pose().popPose();
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public float getIconPadding() {
        return this.iconPadding;
    }

    public float getHoverBackgroundPadding() {
        return this.hoverBackgroundPadding;
    }

    public float getIconAlpha() {
        return this.iconAlpha;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    @Nonnull
    public MaterialIcon getIcon() {
        return this.icon;
    }

    @Nullable
    public IconAnimation getIconHoverAnimation() {
        return this.iconHoverAnimation;
    }

    @Nullable
    public Consumer<UIIconButton> getClickAction() {
        return this.clickAction;
    }

    public UIIconButton setX(float x) {
        this.x = x;
        return this;
    }

    public UIIconButton setY(float y) {
        this.y = y;
        return this;
    }

    public UIIconButton setWidth(float width) {
        this.width = width;
        return this;
    }

    public UIIconButton setHeight(float height) {
        this.height = height;
        return this;
    }

    public UIIconButton setIconPadding(float iconPadding) {
        this.iconPadding = Math.max(0.0F, iconPadding);
        return this;
    }

    public UIIconButton setHoverBackgroundPadding(float hoverBackgroundPadding) {
        this.hoverBackgroundPadding = Math.max(0.0F, hoverBackgroundPadding);
        return this;
    }

    public UIIconButton setIconAlpha(float iconAlpha) {
        if (Float.isFinite(iconAlpha)) {
            this.iconAlpha = Math.max(0.0F, Math.min(1.0F, iconAlpha));
        }
        return this;
    }

    public UIIconButton setIcon(@Nonnull MaterialIcon icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
        return this;
    }

    public UIIconButton setIconHoverAnimation(@Nullable IconAnimation iconHoverAnimation) {
        this.iconHoverAnimation = iconHoverAnimation;
        this.iconHoverAnimationInstance = (iconHoverAnimation != null) ? iconHoverAnimation.createInstance() : null;
        if (this.hovered && this.iconHoverAnimationInstance != null && UIBase.shouldPlayAnimations()) {
            this.iconHoverAnimationInstance.start();
        }
        return this;
    }

    public UIIconButton setClickAction(@Nullable Consumer<UIIconButton> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (this.clickAction != null) {
            this.clickAction.accept(this);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.width <= 0.0F || this.height <= 0.0F) {
            return false;
        }
        return UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    @Nonnull
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@Nonnull NarrationElementOutput var1) {
    }

    private static final class IconRenderData {
        private final ResourceLocation texture;
        private final int width;
        private final int height;

        private IconRenderData(@Nonnull ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }
}
