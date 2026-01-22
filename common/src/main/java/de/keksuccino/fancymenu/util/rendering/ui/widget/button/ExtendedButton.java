package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinButton;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

@SuppressWarnings("unused")
public class ExtendedButton extends Button implements IExtendedWidget, UniqueWidget, NavigatableWidget, FancyMenuWidget {

    public static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.parse("widget/button"),
            ResourceLocation.parse("widget/button_disabled"),
            ResourceLocation.parse("widget/button_highlighted")
    );

    protected final Minecraft mc = Minecraft.getInstance();
    protected boolean enableLabel = true;
    protected DrawableColor labelBaseColorNormal = DrawableColor.of(new Color(0xFFFFFF));
    protected DrawableColor labelBaseColorInactive = DrawableColor.of(new Color(0xA0A0A0));
    protected boolean labelShadow = true;
    protected boolean renderLabelWithUiBase = false;
    @NotNull
    protected ConsumingSupplier<ExtendedButton, Component> labelSupplier = consumes -> Component.empty();
    protected ConsumingSupplier<ExtendedButton, UITooltip> uiTooltipSupplier = null;
    @Nullable
    protected DrawableColor backgroundColorNormal;
    @Nullable
    protected DrawableColor backgroundColorHover;
    @Nullable
    protected DrawableColor backgroundColorInactive;
    @Nullable
    protected DrawableColor borderColorNormal;
    @Nullable
    protected DrawableColor borderColorHover;
    @Nullable
    protected DrawableColor borderColorInactive;
    @Nullable
    protected ConsumingSupplier<ExtendedButton, Boolean> activeSupplier;
    protected boolean focusable = true;
    protected boolean navigatable = true;
    protected boolean roundedColorBackground = false;
    @Nullable
    protected String identifier;

    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.setLabel(Component.literal(label));
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, Component.literal(""), onPress, narration);
        this.setLabel(Component.literal(label));
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.setLabel(label);
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, Component.literal(""), onPress, narration);
        this.setLabel(label);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.updateIsActive();
        this.updateLabel();
        UITooltip tooltip = this.getUITooltip();
        if ((tooltip != null) && this.isHovered() && this.visible) {
            TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
        }
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics, partial);
        this.renderLabelText(graphics);
    }

    protected void renderBackground(@NotNull GuiGraphics graphics, float partial) {
        //Renders the custom widget background if one is present or the Vanilla background if no custom background is present
        if (this.getExtendedAsCustomizableWidget().renderCustomBackgroundFancyMenu(this, graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
            if (this.renderColorBackground(graphics, partial)) {
                graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                graphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
                RenderingUtils.resetShaderColor(graphics);
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /**
     * Returns if the button should render its Vanilla background (true) or not (false).
     */
    protected boolean renderColorBackground(@NotNull GuiGraphics graphics, float partial) {
        RenderSystem.enableBlend();
        DrawableColor background = null;
        DrawableColor border = null;
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                background = this.backgroundColorHover;
                border = this.borderColorHover;
            } else {
                background = this.backgroundColorNormal;
                border = this.borderColorNormal;
            }
        } else {
            background = this.backgroundColorInactive;
            border = this.borderColorInactive;
        }

        if (background != null) {
            renderColoredBackground(graphics, background.getColorInt(), border, partial);
            return false;
        }
        return true;
    }

    private void renderColoredBackground(@NotNull GuiGraphics graphics, int backgroundColor, @Nullable DrawableColor borderColor, float partial) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        float radius = this.roundedColorBackground ? UIBase.getWidgetCornerRoundingRadius() : 0.0F;
        int borderThickness = borderColor != null ? 1 : 0;
        int innerX = x + borderThickness;
        int innerY = y + borderThickness;
        int innerWidth = width - (borderThickness * 2);
        int innerHeight = height - (borderThickness * 2);
        if (radius > 0.0F) {
            if (innerWidth > 0 && innerHeight > 0) {
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        innerX,
                        innerY,
                        innerWidth,
                        innerHeight,
                        radius,
                        radius,
                        radius,
                        radius,
                        backgroundColor,
                        partial
                );
            }
            if (borderColor != null) {
                float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                        graphics,
                        x,
                        y,
                        width,
                        height,
                        borderThickness,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderColor.getColorInt(),
                        partial
                );
            }
        } else {
            if (innerWidth > 0 && innerHeight > 0) {
                graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, backgroundColor);
            }
            if (borderColor != null) {
                UIBase.renderBorder(graphics, x, y, x + width, y + height, 1, borderColor.getColorInt(), true, true, true, true);
            }
        }
    }

    protected void renderLabelText(@NotNull GuiGraphics graphics) {
        if (this.enableLabel) {
            int k = this.active ? this.labelBaseColorNormal.getColorIntWithAlpha(this.alpha) : this.labelBaseColorInactive.getColorIntWithAlpha(this.alpha);
            if (this.renderLabelWithUiBase) {
                this.renderScrollingLabelUiBase(this, graphics, 2, k);
            } else {
                this.renderScrollingLabel(this, graphics, mc.font, 2, this.labelShadow, k);
            }
        }
    }

    protected void updateLabel() {
        Component c = this.labelSupplier.get(this);
        if (c == null) c = Component.literal("");
        ((IMixinAbstractWidget)this).setMessageFieldFancyMenu(c);
    }

    protected void updateIsActive() {
        if (this.activeSupplier != null) {
            Boolean b = this.activeSupplier.get(this);
            if (b != null) this.active = b;
        }
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected int getHoverState() {
        if (this.isHovered) return 1;
        return 0;
    }

    public boolean isFocusable() {
        return this.focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    @Nullable
    public ConsumingSupplier<ExtendedButton, UITooltip> getUITooltipSupplier() {
        return this.uiTooltipSupplier;
    }

    public ExtendedButton setUITooltipSupplier(@Nullable ConsumingSupplier<ExtendedButton, UITooltip> tooltipSupplier) {
        this.uiTooltipSupplier = tooltipSupplier;
        return this;
    }

    @Nullable
    public UITooltip getUITooltip() {
        if (this.uiTooltipSupplier != null) {
            return this.uiTooltipSupplier.get(this);
        }
        return null;
    }

    public ExtendedButton setUITooltip(@Nullable UITooltip tooltip) {
        if (tooltip == null) {
            this.uiTooltipSupplier = null;
        } else {
            this.uiTooltipSupplier = (button) -> tooltip;
        }
        return this;
    }

    @Deprecated
    @Override
    public void setMessage(@NotNull Component msg) {
        this.setLabel(msg);
    }

    @Deprecated
    @Override
    public @NotNull Component getMessage() {
        return super.getMessage();
    }

    public ExtendedButton setLabel(@NotNull Component label) {
        this.labelSupplier = (btn) -> label;
        ((IMixinAbstractWidget)this).setMessageFieldFancyMenu(label);
        return this;
    }

    public ExtendedButton setLabel(@NotNull String label) {
        this.labelSupplier = (btn) -> Component.literal(label);
        return this;
    }

    public ExtendedButton setLabelSupplier(@NotNull ConsumingSupplier<ExtendedButton, Component> labelSupplier) {
        this.labelSupplier = labelSupplier;
        this.updateLabel();
        return this;
    }

    @NotNull
    public ConsumingSupplier<ExtendedButton, Component> getLabelSupplier() {
        return this.labelSupplier;
    }

    @NotNull
    public Component getLabel() {
        Component c = this.getLabelSupplier().get(this);
        if (c == null) c = Component.empty();
        return c;
    }

    public boolean isLabelEnabled() {
        return this.enableLabel;
    }

    public ExtendedButton setLabelEnabled(boolean enabled) {
        this.enableLabel = enabled;
        return this;
    }

    public DrawableColor getLabelBaseColorNormal() {
        return this.labelBaseColorNormal;
    }

    public void setLabelBaseColorNormal(DrawableColor labelBaseColorNormal) {
        this.labelBaseColorNormal = labelBaseColorNormal;
    }

    public DrawableColor getLabelBaseColorInactive() {
        return this.labelBaseColorInactive;
    }

    public void setLabelBaseColorInactive(DrawableColor labelBaseColorInactive) {
        this.labelBaseColorInactive = labelBaseColorInactive;
    }

    public boolean isLabelShadowEnabled() {
        return this.labelShadow;
    }

    public ExtendedButton setLabelShadowEnabled(boolean enabled) {
        this.labelShadow = enabled;
        return this;
    }

    public boolean isLabelRenderedWithUiBase() {
        return this.renderLabelWithUiBase;
    }

    public ExtendedButton setLabelRenderedWithUiBase(boolean renderLabelWithUiBase) {
        this.renderLabelWithUiBase = renderLabelWithUiBase;
        return this;
    }

    public ExtendedButton setIsActiveSupplier(@Nullable ConsumingSupplier<ExtendedButton, Boolean> isActiveSupplier) {
        this.activeSupplier = isActiveSupplier;
        return this;
    }

    @Nullable
    public ConsumingSupplier<ExtendedButton, Boolean> getIsActiveSupplier() {
        return this.activeSupplier;
    }

    @Nullable
    public DrawableColor getBackgroundColorNormal() {
        return this.backgroundColorNormal;
    }

    public void setBackgroundColor(@Nullable DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorInactive, @Nullable DrawableColor borderColorNormal, @Nullable DrawableColor borderColorHover, @Nullable DrawableColor borderColorInactive) {
        this.backgroundColorNormal = backgroundColorNormal;
        this.backgroundColorHover = backgroundColorHover;
        this.backgroundColorInactive = backgroundColorInactive;
        this.borderColorNormal = borderColorNormal;
        this.borderColorHover = borderColorHover;
        this.borderColorInactive = borderColorInactive;
    }

    public void setBackgroundColorNormal(@Nullable DrawableColor backgroundColorNormal) {
        this.backgroundColorNormal = backgroundColorNormal;
    }

    @Nullable
    public DrawableColor getBackgroundColorHover() {
        return this.backgroundColorHover;
    }

    public void setBackgroundColorHover(@Nullable DrawableColor backgroundColorHover) {
        this.backgroundColorHover = backgroundColorHover;
    }

    @Nullable
    public DrawableColor getBackgroundColorInactive() {
        return this.backgroundColorInactive;
    }

    public void setBackgroundColorInactive(@Nullable DrawableColor backgroundColorInactive) {
        this.backgroundColorInactive = backgroundColorInactive;
    }

    @Nullable
    public DrawableColor getBorderColorNormal() {
        return this.borderColorNormal;
    }

    public void setBorderColorNormal(@Nullable DrawableColor borderColorNormal) {
        this.borderColorNormal = borderColorNormal;
    }

    @Nullable
    public DrawableColor getBorderColorHover() {
        return this.borderColorHover;
    }

    public void setBorderColorHover(@Nullable DrawableColor borderColorHover) {
        this.borderColorHover = borderColorHover;
    }

    @Nullable
    public DrawableColor getBorderColorInactive() {
        return this.borderColorInactive;
    }

    public void setBorderColorInactive(@Nullable DrawableColor borderColorInactive) {
        this.borderColorInactive = borderColorInactive;
    }

    public boolean isRoundedColorBackgroundEnabled() {
        return this.roundedColorBackground;
    }

    public ExtendedButton setRoundedColorBackgroundEnabled(boolean roundedColorBackground) {
        this.roundedColorBackground = roundedColorBackground;
        return this;
    }

    @Nullable
    public RenderableResource getBackgroundNormal() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundNormalFancyMenu();
    }

    public ExtendedButton setBackgroundNormal(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundNormalFancyMenu(background);
        return this;
    }

    @Nullable
    public RenderableResource getBackgroundHover() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundHoverFancyMenu();
    }

    public ExtendedButton setBackgroundHover(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundHoverFancyMenu(background);
        return this;
    }

    @Nullable
    public RenderableResource getBackgroundInactive() {
        return this.getExtendedAsCustomizableWidget().getCustomBackgroundInactiveFancyMenu();
    }

    public ExtendedButton setBackgroundInactive(@Nullable RenderableResource background) {
        this.getExtendedAsCustomizableWidget().setCustomBackgroundInactiveFancyMenu(background);
        return this;
    }

    public OnPress getPressAction() {
        return this.onPress;
    }

    public ExtendedButton setPressAction(@NotNull OnPress pressAction) {
        ((IMixinButton)this).setPressActionFancyMenu(pressAction);
        return this;
    }

    public CustomizableWidget getExtendedAsCustomizableWidget() {
        return (CustomizableWidget) this;
    }

    //This is to make the button work in FocuslessEventHandlers
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean isFocused() {
        if (!this.focusable) return false;
        return super.isFocused();
    }

    @Override
    public void setFocused(boolean $$0) {
        if (!this.focusable) {
            super.setFocused(false);
            return;
        }
        super.setFocused($$0);
    }

    @Override
    public ExtendedButton setWidgetIdentifierFancyMenu(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public @Nullable String getWidgetIdentifierFancyMenu() {
        return this.identifier;
    }

}
