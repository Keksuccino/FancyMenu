package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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

    private static final float DEFAULT_ICON_PADDING = 4.0F;
    private static final float INACTIVE_ICON_ALPHA = 0.35F;
    private float x;
    private float y;
    private float width;
    private float height;
    private float iconPadding = DEFAULT_ICON_PADDING;
    private float iconAlpha = 1.0F;
    @Nonnull
    private MaterialIcon icon;
    @Nullable
    private Consumer<UIIconButton> clickAction;
    @Nullable
    private ConsumingSupplier<UIIconButton, UITooltip> tooltipSupplier;
    @Nullable
    private ConsumingSupplier<UIIconButton, Boolean> activeSupplier;
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
        this.hovered = this.isMouseOver(mouseX, mouseY);
        boolean active = this.isActive();

        RenderSystem.enableBlend();
        if (this.hovered && active) {
            UIBase.renderIconButtonHoverBackground(graphics, this.x, this.y, this.width, this.height);
        }

        this.renderIcon(graphics, active ? this.iconAlpha : (this.iconAlpha * INACTIVE_ICON_ALPHA));

        // Inactive buttons keep their tooltip, so it's still clear what they do
        UITooltip tooltip = this.hovered ? this.getTooltip() : null;
        if (tooltip != null) TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
    }

    private void renderIcon(@Nonnull GuiGraphics graphics, float alpha) {
        float maxSize = Math.min(this.width, this.height);
        float padding = Math.max(0.0F, this.iconPadding);
        float iconSize = Math.max(1.0F, maxSize - (padding * 2.0F));
        IconRenderData iconData = resolveMaterialIconData(this.icon, iconSize, iconSize);
        if (iconData == null) {
            return;
        }
        float drawX = this.x + (this.width - iconSize) * 0.5F;
        float drawY = this.y + (this.height - iconSize) * 0.5F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        UIBase.getUITheme().setUITextureShaderColor(graphics, alpha);
        blitScaledIcon(graphics, iconData, drawX, drawY, iconSize, iconSize);
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
        int size = icon.calculateBestTextureSizeForUI(safeRenderWidth, safeRenderHeight);
        int width = icon.getWidth(size);
        int height = icon.getHeight(size);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new IconRenderData(location, width, height);
    }

    private static void blitScaledIcon(@Nonnull GuiGraphics graphics, @Nonnull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
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

    public float getIconAlpha() {
        return this.iconAlpha;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isActive() {
        if (this.activeSupplier == null) return true;
        return !Boolean.FALSE.equals(this.activeSupplier.get(this));
    }

    @Nullable
    public UITooltip getTooltip() {
        return (this.tooltipSupplier != null) ? this.tooltipSupplier.get(this) : null;
    }

    @Nonnull
    public MaterialIcon getIcon() {
        return this.icon;
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

    public UIIconButton setClickAction(@Nullable Consumer<UIIconButton> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    /**
     * Inactive buttons render their icon faded out and ignore clicks, but still show their tooltip.
     */
    public UIIconButton setIsActiveSupplier(@Nullable ConsumingSupplier<UIIconButton, Boolean> activeSupplier) {
        this.activeSupplier = activeSupplier;
        return this;
    }

    public UIIconButton setTooltipSupplier(@Nullable ConsumingSupplier<UIIconButton, UITooltip> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public UIIconButton setTooltip(@Nullable UITooltip tooltip) {
        this.tooltipSupplier = (tooltip != null) ? consumes -> tooltip : null;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!this.isMouseOver(mouseX, mouseY) || !this.isActive()) {
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
