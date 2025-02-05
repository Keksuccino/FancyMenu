package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinButton;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

@SuppressWarnings("all")
public class ExtendedButton extends Button implements IExtendedWidget, UniqueWidget, NavigatableWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Minecraft mc = Minecraft.getInstance();
    protected boolean enableLabel = true;
    protected DrawableColor labelBaseColorNormal = DrawableColor.of(new Color(0xFFFFFF));
    protected DrawableColor labelBaseColorInactive = DrawableColor.of(new Color(0xA0A0A0));
    protected boolean labelShadow = true;
    @NotNull
    protected ConsumingSupplier<ExtendedButton, Component> labelSupplier = consumes -> Component.empty();
    protected ConsumingSupplier<ExtendedButton, Tooltip> tooltipSupplier = null;
    protected boolean forceDefaultTooltipStyle = false;
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
    @Nullable
    protected String identifier;

    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress);
        this.setLabel(Component.literal(label));
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(""), onPress);
        this.setLabel(label);
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.updateIsActive();
        this.updateLabel();
        Tooltip tooltip = this.getTooltipFancyMenu();
        if ((tooltip != null) && ((IMixinAbstractWidget)this).getIsHoveredFancyMenu() && this.visible) {
            if (this.forceDefaultTooltipStyle) {
                tooltip.setDefaultStyle();
            }
            TooltipHandler.INSTANCE.addTooltip(tooltip, () -> true, false, true);
        }
        super.render(graphics.pose(), mouseX, mouseY, partial);
    }

    @Deprecated
    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.render(GuiGraphics.currentGraphics(), mouseX, mouseY, partial);
    }

    @Override
    public void renderButton(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(GuiGraphics.currentGraphics());
        this.renderLabelText(GuiGraphics.currentGraphics());
    }

    protected void renderBackground(@NotNull GuiGraphics graphics) {
        //Renders the custom widget background if one is present or the Vanilla background if no custom background is present
        if (this.getExtendedAsCustomizableWidget().renderCustomBackgroundFancyMenu(this, graphics, this.x, this.y, this.getWidth(), this.getHeight())) {
            if (this.renderColorBackground(graphics)) {
                graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                graphics.blitNineSliced(WIDGETS_LOCATION, this.x, this.y, this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
                RenderingUtils.resetShaderColor(graphics);
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /**
     * Returns if the button should render its Vanilla background (true) or not (false).
     */
    protected boolean renderColorBackground(@NotNull GuiGraphics graphics) {
        RenderSystem.enableBlend();
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                if (this.backgroundColorHover != null) {
                    graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColorHover.getColorInt());
                    if (this.borderColorHover != null) {
                        UIBase.renderBorder(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), 1, this.borderColorHover.getColorInt(), true, true, true, true);
                    }
                    return false;
                }
            } else {
                if (this.backgroundColorNormal != null) {
                    graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColorNormal.getColorInt());
                    if (this.borderColorNormal != null) {
                        UIBase.renderBorder(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), 1, this.borderColorNormal.getColorInt(), true, true, true, true);
                    }
                    return false;
                }
            }
        } else {
            if (this.backgroundColorInactive != null) {
                graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColorInactive.getColorInt());
                if (this.borderColorInactive != null) {
                    UIBase.renderBorder(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), 1, this.borderColorInactive.getColorInt(), true, true, true, true);
                }
                return false;
            }
        }
        return true;
    }

    protected void renderLabelText(@NotNull GuiGraphics graphics) {
        if (this.enableLabel) {
            int k = this.active ? this.labelBaseColorNormal.getColorIntWithAlpha(this.alpha) : this.labelBaseColorInactive.getColorIntWithAlpha(this.alpha);
            this.renderScrollingLabel(this, graphics, mc.font, 2, this.labelShadow, k);
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

    protected int getHoverState() {
        if (this.isHovered) return 1;
        return 0;
    }

    protected int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }
        return 46 + i * 20;
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
    public ConsumingSupplier<ExtendedButton, Tooltip> getTooltipSupplier() {
        return this.tooltipSupplier;
    }

    public ExtendedButton setTooltipSupplier(@Nullable ConsumingSupplier<ExtendedButton, Tooltip> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    @Nullable
    public Tooltip getTooltipFancyMenu() {
        if (this.tooltipSupplier != null) {
            return this.tooltipSupplier.get(this);
        }
        return null;
    }

    public ExtendedButton setTooltip(@Nullable Tooltip tooltip) {
        if (tooltip == null) {
            this.tooltipSupplier = null;
        } else {
            this.tooltipSupplier = (button) -> tooltip;
        }
        return this;
    }

    public boolean isForceDefaultTooltipStyle() {
        return this.forceDefaultTooltipStyle;
    }

    public ExtendedButton setForceDefaultTooltipStyle(boolean forceDefaultTooltipStyle) {
        this.forceDefaultTooltipStyle = forceDefaultTooltipStyle;
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
