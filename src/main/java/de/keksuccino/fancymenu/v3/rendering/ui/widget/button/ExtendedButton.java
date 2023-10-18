package de.keksuccino.fancymenu.v3.rendering.ui.widget.button;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.mixin.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.client.IMixinButton;
import de.keksuccino.fancymenu.v3.ConsumingSupplier;
import de.keksuccino.fancymenu.v3.rendering.DrawableColor;
import de.keksuccino.fancymenu.v3.rendering.ui.UIBase;
import de.keksuccino.fancymenu.v3.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

@SuppressWarnings("unused")
public class ExtendedButton extends Button implements UniqueWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/button"), new ResourceLocation("widget/button_disabled"), new ResourceLocation("widget/button_highlighted"));

    protected final Minecraft mc = Minecraft.getInstance();
    protected boolean enableLabel = true;
    @NotNull
    protected ButtonBackground background = VanillaButtonBackground.create().setParent(this);
    protected DrawableColor labelBaseColorNormal = DrawableColor.of(new Color(0xFFFFFF));
    protected DrawableColor labelBaseColorInactive = DrawableColor.of(new Color(0xA0A0A0));
    protected boolean labelShadow = true;
    @NotNull
    protected ConsumingSupplier<ExtendedButton, Component> labelSupplier = consumes -> Component.empty();
//    protected ConsumingSupplier<ExtendedButton, Tooltip> tooltipSupplier = null;
    protected boolean forceDefaultTooltipStyle = false;
    @Nullable
    protected ConsumingSupplier<ExtendedButton, Boolean> activeSupplier;
    protected boolean focusable = true;
    @Nullable
    protected String identifier;

    protected int lastHoverState = -1;

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
//        Tooltip tooltip = this.getTooltip();
//        if ((tooltip != null) && this.isHovered() && this.visible) {
//            if (this.forceDefaultTooltipStyle) {
//                tooltip.setDefaultStyle();
//            }
//            TooltipHandler.INSTANCE.addTooltip(tooltip, () -> true, false, true);
//        }
        this.handleHover();
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.background.render(graphics, mouseX, mouseY, partial);
        this.renderLabelText(graphics);
    }

    protected void renderLabelText(GuiGraphics graphics) {
        if (this.enableLabel) {
            int k = this.active ? this.labelBaseColorNormal.getColorIntWithAlpha(this.alpha) : this.labelBaseColorInactive.getColorIntWithAlpha(this.alpha);
            this.renderScrollingLabel(graphics, mc.font, 2, k);
        }
    }

    protected void renderScrollingLabel(@NotNull GuiGraphics graphics, @NotNull Font font, int spaceLeftRight, int textColor) {
        int xMin = this.getX() + spaceLeftRight;
        int xMax = this.getX() + this.getWidth() - spaceLeftRight;
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        this.renderScrollingLabelInternal(graphics, font, this.getMessage(), xMin, this.getY(), xMax, this.getY() + this.getHeight(), textColor);
    }

    protected void renderScrollingLabelInternal(@NotNull GuiGraphics graphics, Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor) {
        int textWidth = font.width(text);
        int textPosY = (yMin + yMax - 9) / 2 + 1;
        int maxTextWidth = xMax - xMin;
        if (textWidth > maxTextWidth) {
            int diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double $$13 = Math.max((double)diffTextWidth * 0.5D, 3.0D);
            double $$14 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / $$13)) / 2.0D + 0.5D;
            double textPosX = Mth.lerp($$14, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            graphics.drawString(font, text, xMin - (int)textPosX, textPosY, textColor, this.labelShadow);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, text, (int)(((xMin + xMax) / 2F) - (font.width(text) / 2F)), textPosY, textColor, this.labelShadow);
        }
    }

    protected void handleHover() {
        if ((this.lastHoverState != -1) && (this.lastHoverState != this.getHoverState())) {
            if (this.isHovered) {
                this.background.onHover();
            } else {
                this.background.onEndHover();
            }
        }
        this.lastHoverState = this.getHoverState();
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

    public ExtendedButton setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

//    @Nullable
//    public ConsumingSupplier<ExtendedButton, Tooltip> getTooltipSupplier() {
//        return this.tooltipSupplier;
//    }
//
//    public ExtendedButton setTooltipSupplier(@Nullable ConsumingSupplier<ExtendedButton, Tooltip> tooltipSupplier) {
//        this.tooltipSupplier = tooltipSupplier;
//        return this;
//    }
//
//    @Nullable
//    public Tooltip getTooltip() {
//        if (this.tooltipSupplier != null) {
//            return this.tooltipSupplier.get(this);
//        }
//        return null;
//    }
//
//    public ExtendedButton setTooltip(@Nullable Tooltip tooltip) {
//        if (tooltip == null) {
//            this.tooltipSupplier = null;
//        } else {
//            this.tooltipSupplier = (button) -> tooltip;
//        }
//        return this;
//    }

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

    @NotNull
    public ButtonBackground getBackground() {
        return this.background;
    }

    public ExtendedButton setBackground(@NotNull ButtonBackground background) {
        this.background = background;
        this.background.setParent(this);
        return this;
    }

    public OnPress getPressAction() {
        return this.onPress;
    }

    public ExtendedButton setPressAction(@NotNull OnPress pressAction) {
        ((IMixinButton)this).setPressActionFancyMenu(pressAction);
        return this;
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

    public static abstract class ButtonBackground implements Renderable {

        protected ExtendedButton parent;

        @Override
        public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

        public abstract void onHover();

        public abstract void onEndHover();

        public ButtonBackground setParent(ExtendedButton parent) {
            this.parent = parent;
            return this;
        }

    }

    public static class VanillaButtonBackground extends ButtonBackground {

        public static VanillaButtonBackground create() {
            return new VanillaButtonBackground();
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            Minecraft minecraft = Minecraft.getInstance();
            graphics.setColor(1.0F, 1.0F, 1.0F, parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            graphics.blitSprite(SPRITES.get(parent.active, parent.isHoveredOrFocused()), parent.getX(), parent.getY(), parent.getWidth(), parent.getHeight());
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void onHover() {
        }

        @Override
        public void onEndHover() {
        }

    }

    @SuppressWarnings("all")
    public static class ColorButtonBackground extends ButtonBackground {

        @NotNull
        protected DrawableColor backgroundColorNormal;
        @Nullable
        protected DrawableColor backgroundColorHover;
        @Nullable
        protected DrawableColor backgroundColorInactive;
        @Nullable
        protected DrawableColor backgroundColorBorderNormal;
        @Nullable
        protected DrawableColor backgroundColorBorderHover;
        @Nullable
        protected DrawableColor backgroundColorBorderInactive;
        protected int borderThickness;

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal) {
            return new ColorButtonBackground(backgroundColorNormal, null, null, null, null, null, 1);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, null, backgroundColorBorderNormal, backgroundColorBorderHover, null, 1);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, int borderThickness) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, null, backgroundColorBorderNormal, backgroundColorBorderHover, null, borderThickness);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorInactive, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, @Nullable DrawableColor backgroundColorBorderInactive) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, backgroundColorInactive, backgroundColorBorderNormal, backgroundColorBorderHover, backgroundColorBorderInactive, 1);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorInactive, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, @Nullable DrawableColor backgroundColorBorderInactive, int borderThickness) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, backgroundColorInactive, backgroundColorBorderNormal, backgroundColorBorderHover, backgroundColorBorderInactive, borderThickness);
        }

        public ColorButtonBackground(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorInactive, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, @Nullable DrawableColor backgroundColorBorderInactive, int borderThickness) {
            this.backgroundColorNormal = backgroundColorNormal;
            this.backgroundColorHover = backgroundColorHover;
            this.backgroundColorInactive = backgroundColorInactive;
            this.backgroundColorBorderNormal = backgroundColorBorderNormal;
            this.backgroundColorBorderHover = backgroundColorBorderHover;
            this.backgroundColorBorderInactive = backgroundColorBorderInactive;
            this.borderThickness = borderThickness;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            this.renderColorBackground(graphics);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public void onHover() {
        }

        @Override
        public void onEndHover() {
        }

        protected void renderColorBackground(GuiGraphics graphics) {
            int w = this.parent.getWidth() - (this.borderThickness * 2);
            int h = this.parent.getHeight() - (this.borderThickness * 2);
            graphics.fill(this.parent.getX() + this.borderThickness, this.parent.getY() + this.borderThickness, this.parent.getX() + this.borderThickness + w, this.parent.getY() + this.borderThickness + h, this.getBackgroundColor().getColorIntWithAlpha(this.parent.alpha));
            UIBase.renderBorder(graphics.pose(), this.parent.getX(), this.parent.getY(), this.parent.getX() + this.parent.getWidth(), this.parent.getY() + this.parent.getHeight(), this.borderThickness, this.getBorderColor().getColor(), true, true, true, true);
        }

        @NotNull
        protected DrawableColor getBorderColor() {
            if (!this.parent.active && (this.backgroundColorBorderInactive != null)) {
                return this.backgroundColorBorderInactive;
            }
            if (this.parent.isHovered && this.parent.isActive()) {
                if (this.backgroundColorBorderHover != null) return this.backgroundColorBorderHover;
                if (this.backgroundColorBorderNormal != null) return this.backgroundColorBorderNormal;
                return this.getBackgroundColor();
            }
            if (this.backgroundColorBorderNormal != null) return this.backgroundColorBorderNormal;
            return this.getBackgroundColor();
        }

        @NotNull
        protected DrawableColor getBackgroundColor() {
            if (!this.parent.active && (this.backgroundColorInactive != null)) {
                return this.backgroundColorInactive;
            }
            if (this.parent.isHovered && this.parent.isActive()) {
                if (this.backgroundColorHover != null) return this.backgroundColorHover;
            }
            return this.backgroundColorNormal;
        }

        @NotNull
        public DrawableColor getBackgroundColorNormal() {
            return this.backgroundColorNormal;
        }

        public ColorButtonBackground setBackgroundColorNormal(@NotNull DrawableColor backgroundColorNormal) {
            Objects.requireNonNull(backgroundColorNormal);
            this.backgroundColorNormal = backgroundColorNormal;
            return this;
        }

        @Nullable
        public DrawableColor getBackgroundColorInactive() {
            return this.backgroundColorInactive;
        }

        public ColorButtonBackground setBackgroundColorInactive(@Nullable DrawableColor backgroundColorInactive) {
            this.backgroundColorInactive = backgroundColorInactive;
            return this;
        }

        @Nullable
        public DrawableColor getBackgroundColorBorderInactive() {
            return this.backgroundColorBorderInactive;
        }

        public ColorButtonBackground setBackgroundColorBorderInactive(@Nullable DrawableColor backgroundColorBorderInactive) {
            this.backgroundColorBorderInactive = backgroundColorBorderInactive;
            return this;
        }

        @Nullable
        public DrawableColor getBackgroundColorHover() {
            return this.backgroundColorHover;
        }

        public ColorButtonBackground setBackgroundColorHover(@Nullable DrawableColor backgroundColorHover) {
            this.backgroundColorHover = backgroundColorHover;
            return this;
        }

        @Nullable
        public DrawableColor getBackgroundColorBorderNormal() {
            return this.backgroundColorBorderNormal;
        }

        public ColorButtonBackground setBackgroundColorBorderNormal(@Nullable DrawableColor backgroundColorBorderNormal) {
            this.backgroundColorBorderNormal = backgroundColorBorderNormal;
            return this;
        }

        @Nullable
        public DrawableColor getBackgroundColorBorderHover() {
            return this.backgroundColorBorderHover;
        }

        public ColorButtonBackground setBackgroundColorBorderHover(@Nullable DrawableColor backgroundColorBorderHover) {
            this.backgroundColorBorderHover = backgroundColorBorderHover;
            return this;
        }

        public int getBorderThickness() {
            return this.borderThickness;
        }

        public ColorButtonBackground setBorderThickness(int borderThickness) {
            this.borderThickness = borderThickness;
            return this;
        }

    }

    public static class ImageButtonBackground extends ButtonBackground {

        @NotNull
        protected ResourceLocation backgroundTextureNormal;
        @Nullable
        protected ResourceLocation backgroundTextureHover;
        @Nullable
        protected ResourceLocation backgroundTextureInactive;

        public static ImageButtonBackground create(@NotNull ResourceLocation backgroundTextureNormal) {
            return new ImageButtonBackground(backgroundTextureNormal, null, null);
        }

        public static ImageButtonBackground create(@NotNull ResourceLocation backgroundTextureNormal, @Nullable ResourceLocation backgroundTextureHover) {
            return new ImageButtonBackground(backgroundTextureNormal, backgroundTextureHover, null);
        }

        public static ImageButtonBackground create(@NotNull ResourceLocation backgroundTextureNormal, @Nullable ResourceLocation backgroundTextureHover, @Nullable ResourceLocation backgroundTextureInactive) {
            return new ImageButtonBackground(backgroundTextureNormal, backgroundTextureHover, backgroundTextureInactive);
        }

        public ImageButtonBackground(@NotNull ResourceLocation backgroundTextureNormal, @Nullable ResourceLocation backgroundTextureHover, @Nullable ResourceLocation backgroundTextureInactive) {
            this.backgroundTextureNormal = backgroundTextureNormal;
            this.backgroundTextureHover = backgroundTextureHover;
            this.backgroundTextureInactive = backgroundTextureInactive;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            graphics.blit(this.getBackgroundTexture(), this.parent.getX(), this.parent.getY(), 0.0F, 0.0F, this.parent.getWidth(), this.parent.getHeight(), this.parent.getWidth(), this.parent.getHeight());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public void onHover() {
        }

        @Override
        public void onEndHover() {
        }

        @NotNull
        protected ResourceLocation getBackgroundTexture() {
            if (!this.parent.active && (this.backgroundTextureInactive != null)) {
                return this.backgroundTextureInactive;
            }
            if (this.parent.isHovered && this.parent.isActive()) {
                if (this.backgroundTextureHover != null) return this.backgroundTextureHover;
            }
            return this.backgroundTextureNormal;
        }

        @Nullable
        public ResourceLocation getBackgroundTextureInactive() {
            return this.backgroundTextureInactive;
        }

        public ImageButtonBackground setBackgroundTextureInactive(@Nullable ResourceLocation backgroundTextureInactive) {
            this.backgroundTextureInactive = backgroundTextureInactive;
            return this;
        }

        @NotNull
        public ResourceLocation getBackgroundTextureNormal() {
            return this.backgroundTextureNormal;
        }

        public ImageButtonBackground setBackgroundTextureNormal(@NotNull ResourceLocation backgroundTextureNormal) {
            this.backgroundTextureNormal = backgroundTextureNormal;
            return this;
        }

        @Nullable
        public ResourceLocation getBackgroundTextureHover() {
            return this.backgroundTextureHover;
        }

        public ImageButtonBackground setBackgroundTextureHover(@Nullable ResourceLocation backgroundTextureHover) {
            this.backgroundTextureHover = backgroundTextureHover;
            return this;
        }

    }

    public static class AnimationButtonBackground extends ButtonBackground {

        @NotNull
        protected IAnimationRenderer backgroundAnimationNormal;
        @Nullable
        protected IAnimationRenderer backgroundAnimationHover;
        @Nullable
        protected IAnimationRenderer backgroundAnimationInactive;
        protected boolean loop = true;
        protected boolean restartOnHover = true;

        public static AnimationButtonBackground create(@NotNull IAnimationRenderer backgroundAnimationNormal) {
            return new AnimationButtonBackground(backgroundAnimationNormal, null, null);
        }

        public static AnimationButtonBackground create(@NotNull IAnimationRenderer backgroundAnimationNormal, @Nullable IAnimationRenderer backgroundAnimationHover) {
            return new AnimationButtonBackground(backgroundAnimationNormal, backgroundAnimationHover, null);
        }

        public static AnimationButtonBackground create(@NotNull IAnimationRenderer backgroundAnimationNormal, @Nullable IAnimationRenderer backgroundAnimationHover, @Nullable IAnimationRenderer backgroundAnimationInactive) {
            return new AnimationButtonBackground(backgroundAnimationNormal, backgroundAnimationHover, backgroundAnimationInactive);
        }

        public AnimationButtonBackground(@NotNull IAnimationRenderer backgroundAnimationNormal, @Nullable IAnimationRenderer backgroundAnimationHover, @Nullable IAnimationRenderer backgroundAnimationInactive) {
            this.backgroundAnimationNormal = backgroundAnimationNormal;
            this.backgroundAnimationHover = backgroundAnimationHover;
            this.backgroundAnimationInactive = backgroundAnimationInactive;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            this.renderBackgroundAnimation(graphics);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public void onHover() {
            //Restart animations on hover
            if (this.restartOnHover) {
                this.backgroundAnimationNormal.resetAnimation();
                if (this.backgroundAnimationNormal instanceof AdvancedAnimation aa) {
                    aa.stopAudio();
                    aa.resetAudio();
                }
                if (this.backgroundAnimationHover != null) {
                    this.backgroundAnimationHover.resetAnimation();
                    if (this.backgroundAnimationHover instanceof AdvancedAnimation aa) {
                        aa.stopAudio();
                        aa.resetAudio();
                    }
                }
            }
            //Stop audio of normal animation when hovered
            if (this.backgroundAnimationNormal instanceof AdvancedAnimation aa) {
                aa.stopAudio();
                aa.resetAudio();
            }
        }

        @Override
        public void onEndHover() {
            //Stop audio of hover animation when NOT hovered
            if (this.backgroundAnimationHover instanceof AdvancedAnimation aa) {
                aa.stopAudio();
                aa.resetAudio();
            }
        }

        protected void renderBackgroundAnimation(GuiGraphics graphics) {
            IAnimationRenderer a = this.getBackgroundAnimation();
            int cachedW = a.getWidth();
            int cachedH = a.getHeight();
            int cachedX = a.getPosX();
            int cachedY = a.getPosY();
            boolean cachedLoop = a.isGettingLooped();
            a.setWidth(this.parent.getWidth());
            a.setHeight(this.parent.getHeight());
            a.setPosX(this.parent.getX());
            a.setPosY(this.parent.getY());
            a.setLooped(this.loop);
            a.setOpacity(this.parent.alpha);
            a.render(graphics);
            a.setWidth(cachedW);
            a.setHeight(cachedH);
            a.setPosX(cachedX);
            a.setPosY(cachedY);
            a.setLooped(cachedLoop);
            a.setOpacity(1.0F);
        }

        @NotNull
        protected IAnimationRenderer getBackgroundAnimation() {
            if (!this.parent.active && (this.backgroundAnimationInactive != null)) {
                return this.backgroundAnimationInactive;
            }
            if (this.parent.isHovered && this.parent.isActive()) {
                if (this.backgroundAnimationHover != null) return this.backgroundAnimationHover;
            }
            return this.backgroundAnimationNormal;
        }

        @Nullable
        public IAnimationRenderer getBackgroundAnimationInactive() {
            return this.backgroundAnimationInactive;
        }

        public AnimationButtonBackground setBackgroundAnimationInactive(@Nullable IAnimationRenderer backgroundAnimationInactive) {
            this.backgroundAnimationInactive = backgroundAnimationInactive;
            return this;
        }

        @NotNull
        public IAnimationRenderer getBackgroundAnimationNormal() {
            return this.backgroundAnimationNormal;
        }

        public AnimationButtonBackground setBackgroundAnimationNormal(@NotNull IAnimationRenderer backgroundAnimationNormal) {
            this.backgroundAnimationNormal = backgroundAnimationNormal;
            return this;
        }

        @Nullable
        public IAnimationRenderer getBackgroundAnimationHover() {
            return this.backgroundAnimationHover;
        }

        public AnimationButtonBackground setBackgroundAnimationHover(@Nullable IAnimationRenderer backgroundAnimationHover) {
            this.backgroundAnimationHover = backgroundAnimationHover;
            return this;
        }

        public boolean isLooped() {
            return this.loop;
        }

        public AnimationButtonBackground setLooped(boolean loop) {
            this.loop = loop;
            return this;
        }

        public boolean isRestartOnHover() {
            return this.restartOnHover;
        }

        public AnimationButtonBackground setRestartOnHover(boolean restartOnHover) {
            this.restartOnHover = restartOnHover;
            return this;
        }

    }

    public static class MultiTypeButtonBackground extends ButtonBackground {

        @NotNull
        protected ButtonBackground backgroundNormal;
        @Nullable
        protected ButtonBackground backgroundHover;
        @Nullable
        protected ButtonBackground backgroundInactive;

        @SuppressWarnings("all")
        public static MultiTypeButtonBackground build(@Nullable Object backgroundNormal, @Nullable Object backgroundHover, @Nullable Object backgroundInactive) {
            MultiTypeButtonBackground b = new MultiTypeButtonBackground(null, null, null);
            if (backgroundNormal == null) {
                b.setBackgroundNormal(VanillaButtonBackground.create());
            } else {
                if (backgroundNormal instanceof ResourceLocation r) {
                    b.setBackgroundNormal(ImageButtonBackground.create(r, r));
                } else if (backgroundNormal instanceof IAnimationRenderer a) {
                    b.setBackgroundNormal(AnimationButtonBackground.create(a, a));
                } else {
                    b.setBackgroundNormal(VanillaButtonBackground.create());
                }
            }
            if (backgroundHover == null) {
                b.setBackgroundHover(VanillaButtonBackground.create());
            } else {
                if (backgroundHover instanceof ResourceLocation r) {
                    b.setBackgroundHover(ImageButtonBackground.create(r, r));
                } else if (backgroundHover instanceof IAnimationRenderer a) {
                    b.setBackgroundHover(AnimationButtonBackground.create(a, a));
                } else {
                    b.setBackgroundHover(VanillaButtonBackground.create());
                }
            }
            if (backgroundInactive == null) {
                b.setBackgroundInactive(VanillaButtonBackground.create());
            } else {
                if (backgroundInactive instanceof ResourceLocation r) {
                    b.setBackgroundInactive(ImageButtonBackground.create(r, r));
                } else if (backgroundInactive instanceof IAnimationRenderer a) {
                    b.setBackgroundInactive(AnimationButtonBackground.create(a, a));
                } else {
                    b.setBackgroundInactive(VanillaButtonBackground.create());
                }
            }
            return b;
        }

        public static MultiTypeButtonBackground create(@NotNull ButtonBackground backgroundNormal) {
            return new MultiTypeButtonBackground(backgroundNormal, null, null);
        }

        public static MultiTypeButtonBackground create(@NotNull ButtonBackground backgroundNormal, @Nullable ButtonBackground backgroundHover) {
            return new MultiTypeButtonBackground(backgroundNormal, backgroundHover, null);
        }

        public static MultiTypeButtonBackground create(@NotNull ButtonBackground backgroundNormal, @Nullable ButtonBackground backgroundHover, @Nullable ButtonBackground backgroundInactive) {
            return new MultiTypeButtonBackground(backgroundNormal, backgroundHover, backgroundInactive);
        }

        @SuppressWarnings("all")
        public MultiTypeButtonBackground(@NotNull ButtonBackground backgroundNormal, @Nullable ButtonBackground backgroundHover, @Nullable ButtonBackground backgroundInactive) {
            this.backgroundNormal = backgroundNormal;
            this.backgroundHover = backgroundHover;
            this.backgroundInactive = backgroundInactive;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.getBackground().setParent(this.parent).render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public void onHover() {
            this.backgroundNormal.onHover();
            if (this.backgroundHover != null) this.backgroundHover.onHover();
        }

        @Override
        public void onEndHover() {
            this.backgroundNormal.onEndHover();
            if (this.backgroundHover != null) this.backgroundHover.onEndHover();
        }

        @NotNull
        protected ButtonBackground getBackground() {
            if (!this.parent.active && (this.backgroundInactive != null)) {
                return this.backgroundInactive;
            }
            if (this.parent.isHovered && this.parent.isActive()) {
                if (this.backgroundHover != null) return this.backgroundHover;
            }
            return this.backgroundNormal;
        }

        @Nullable
        public ButtonBackground getBackgroundInactive() {
            return this.backgroundInactive;
        }

        public MultiTypeButtonBackground setBackgroundInactive(@Nullable ButtonBackground backgroundInactive) {
            this.backgroundInactive = backgroundInactive;
            return this;
        }

        @NotNull
        public ButtonBackground getBackgroundNormal() {
            return this.backgroundNormal;
        }

        public MultiTypeButtonBackground setBackgroundNormal(@NotNull ButtonBackground backgroundNormal) {
            this.backgroundNormal = backgroundNormal;
            return this;
        }

        @Nullable
        public ButtonBackground getBackgroundHover() {
            return backgroundHover;
        }

        public MultiTypeButtonBackground setBackgroundHover(@Nullable ButtonBackground backgroundHover) {
            this.backgroundHover = backgroundHover;
            return this;
        }

    }

}
