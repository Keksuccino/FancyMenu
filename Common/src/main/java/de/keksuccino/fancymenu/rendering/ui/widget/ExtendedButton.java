package de.keksuccino.fancymenu.rendering.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinButton;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
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
public class ExtendedButton extends Button {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Minecraft mc = Minecraft.getInstance();
    protected boolean enableLabel = true;
    @NotNull
    protected ButtonBackground background = VanillaButtonBackground.create().setParent(this);
    protected DrawableColor labelBaseColorNormal = DrawableColor.of(new Color(0xFFFFFF));
    protected DrawableColor labelBaseColorInactive = DrawableColor.of(new Color(0xA0A0A0));
    protected boolean labelShadow = true;
    protected boolean autoRegister = false;
    protected Tooltip tooltip = null;

    protected int lastHoverState = -1;

    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, width, height, Component.literal(label), onPress, DEFAULT_NARRATION);
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull String label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, Component.literal(label), onPress, narration);
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    public ExtendedButton(int x, int y, int width, int height, @NotNull Component label, @NotNull OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, label, onPress, narration);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if ((this.tooltip != null) && this.isHovered() && this.isActive() && this.visible) {
            TooltipHandler.INSTANCE.addTooltip(this.tooltip, () -> true, false, true);
        }
        this.handleAutoRegister();
        this.handleHover();
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    public void renderWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.background.render(pose, mouseX, mouseY, partial);
        this.renderLabelText(pose);
    }

    protected void renderLabelText(PoseStack pose) {
        if (this.enableLabel) {
            int k = this.active ? this.labelBaseColorNormal.getColorIntWithAlpha(this.alpha) : this.labelBaseColorInactive.getColorIntWithAlpha(this.alpha);
            this.renderScrollingLabel(pose, mc.font, 2, k);
        }
    }

    protected void renderScrollingLabel(@NotNull PoseStack pose, @NotNull Font font, int spaceLeftRight, int textColor) {
        int xMin = this.getX() + spaceLeftRight;
        int xMax = this.getX() + this.getWidth() - spaceLeftRight;
        this.renderScrollingLabelInternal(pose, font, this.getMessage(), xMin, this.getY(), xMax, this.getY() + this.getHeight(), textColor);
    }

    protected void renderScrollingLabelInternal(@NotNull PoseStack pose, Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor) {
        int textWidth = font.width(text);
        int textPosY = (yMin + yMax - 9) / 2 + 1;
        int maxTextWidth = xMax - xMin;
        if (textWidth > maxTextWidth) {
            int diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double $$13 = Math.max((double)diffTextWidth * 0.5D, 3.0D);
            double $$14 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / $$13)) / 2.0D + 0.5D;
            double textPosX = Mth.lerp($$14, 0.0D, diffTextWidth);
            enableScissor(xMin, yMin, xMax, yMax);
            if (!this.labelShadow) {
                font.draw(pose, text, xMin - (int)textPosX, textPosY, textColor);
            } else {
                font.drawShadow(pose, text, xMin - (int)textPosX, textPosY, textColor);
            }
            disableScissor();
        } else {
            if (!this.labelShadow) {
                font.draw(pose, text, ((xMin + xMax) / 2F) - (font.width(text) / 2F), textPosY, textColor);
            } else {
                font.drawShadow(pose, text, ((xMin + xMax) / 2F) - (font.width(text) / 2F), textPosY, textColor);
            }
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

    protected int getHoverState() {
        if (this.isHovered) return 1;
        return 0;
    }

    protected void handleAutoRegister() {
        if (this.autoRegister) {
            Screen s = Minecraft.getInstance().screen;
            if ((s != null) && !((IMixinScreen)s).getChildrenFancyMenu().contains(this)) {
                ((IMixinScreen)s).invokeAddWidgetFancyMenu(this);
            }
        }
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

    public ExtendedButton setMessage(String message) {
        this.setMessage(Component.literal(message));
        return this;
    }

    public Tooltip getTooltip() {
        return this.tooltip;
    }

    public ExtendedButton setTooltip(@Nullable Tooltip tooltip) {
        this.tooltip = tooltip;
        return this;
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

    public void setLabelShadowEnabled(boolean enabled) {
        this.labelShadow = enabled;
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

    public boolean isAutoRegisteringToScreen() {
        return this.autoRegister;
    }

    public ExtendedButton setAutoRegisterToScreen(boolean autoRegister) {
        this.autoRegister = autoRegister;
        return this;
    }

    public OnPress getPressAction() {
        return this.onPress;
    }

    public ExtendedButton setPressAction(@NotNull OnPress pressAction) {
        ((IMixinButton)this).setPressActionFancyMenu(pressAction);
        return this;
    }

    public static abstract class ButtonBackground extends GuiComponent implements Renderable {

        protected ExtendedButton parent;

        @Override
        public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

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
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            AbstractButton.blitNineSliced(pose, this.parent.getX(), this.parent.getY(), this.parent.getWidth(), this.parent.getHeight(), 20, 4, 200, 20, 0, this.parent.getTextureY());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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
        protected DrawableColor backgroundColorBorderNormal;
        @Nullable
        protected DrawableColor backgroundColorBorderHover;
        protected int borderThickness;

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal) {
            return new ColorButtonBackground(backgroundColorNormal, null, null, null, 1);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, backgroundColorBorderNormal, backgroundColorBorderHover, 1);
        }

        public static ColorButtonBackground create(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, int borderThickness) {
            return new ColorButtonBackground(backgroundColorNormal, backgroundColorHover, backgroundColorBorderNormal, backgroundColorBorderHover, borderThickness);
        }

        public ColorButtonBackground(@NotNull DrawableColor backgroundColorNormal, @Nullable DrawableColor backgroundColorHover, @Nullable DrawableColor backgroundColorBorderNormal, @Nullable DrawableColor backgroundColorBorderHover, int borderThickness) {
            this.backgroundColorNormal = backgroundColorNormal;
            this.backgroundColorHover = backgroundColorHover;
            this.backgroundColorBorderNormal = backgroundColorBorderNormal;
            this.backgroundColorBorderHover = backgroundColorBorderHover;
            this.borderThickness = borderThickness;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            this.renderColorBackground(pose);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public void onHover() {
        }

        @Override
        public void onEndHover() {
        }

        protected void renderColorBackground(PoseStack pose) {
            int w = this.parent.getWidth() - (this.borderThickness * 2);
            int h = this.parent.getHeight() - (this.borderThickness * 2);
            fill(pose, this.parent.getX() + this.borderThickness, this.parent.getY() + this.borderThickness, this.parent.getX() + this.borderThickness + w, this.parent.getY() + this.borderThickness + h, this.getBackgroundColor().getColorIntWithAlpha(this.parent.alpha));
            UIBase.renderBorder(pose, this.parent.getX(), this.parent.getY(), this.parent.getX() + this.parent.getWidth(), this.parent.getY() + this.parent.getHeight(), this.borderThickness, this.getBorderColor().getColor(), true, true, true, true);
        }

        @NotNull
        protected DrawableColor getBorderColor() {
            if (this.parent.isHovered) {
                if (this.backgroundColorBorderHover != null) return this.backgroundColorBorderHover;
                if (this.backgroundColorBorderNormal != null) return this.backgroundColorBorderNormal;
                return this.getBackgroundColor();
            }
            if (this.backgroundColorBorderNormal != null) return this.backgroundColorBorderNormal;
            return this.getBackgroundColor();
        }

        @NotNull
        protected DrawableColor getBackgroundColor() {
            if (this.parent.isHovered) {
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

        public static ImageButtonBackground create(@NotNull ResourceLocation backgroundTextureNormal) {
            return new ImageButtonBackground(backgroundTextureNormal, null);
        }

        public static ImageButtonBackground create(@NotNull ResourceLocation backgroundTextureNormal, @Nullable ResourceLocation backgroundTextureHover) {
            return new ImageButtonBackground(backgroundTextureNormal, backgroundTextureHover);
        }

        public ImageButtonBackground(@NotNull ResourceLocation backgroundTextureNormal, @Nullable ResourceLocation backgroundTextureHover) {
            this.backgroundTextureNormal = backgroundTextureNormal;
            this.backgroundTextureHover = backgroundTextureHover;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderUtils.bindTexture(this.getBackgroundTexture());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            blit(pose, this.parent.getX(), this.parent.getY(), 0.0F, 0.0F, this.parent.getWidth(), this.parent.getHeight(), this.parent.getWidth(), this.parent.getHeight());
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
            if (this.parent.isHovered) {
                if (this.backgroundTextureHover != null) return this.backgroundTextureHover;
            }
            return this.backgroundTextureNormal;
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
        protected boolean loop = true;
        protected boolean restartOnHover = true;

        public static AnimationButtonBackground create(@NotNull IAnimationRenderer backgroundAnimationNormal) {
            return new AnimationButtonBackground(backgroundAnimationNormal, null);
        }

        public static AnimationButtonBackground create(@NotNull IAnimationRenderer backgroundAnimationNormal, @Nullable IAnimationRenderer backgroundAnimationHover) {
            return new AnimationButtonBackground(backgroundAnimationNormal, backgroundAnimationHover);
        }

        public AnimationButtonBackground(@NotNull IAnimationRenderer backgroundAnimationNormal, @Nullable IAnimationRenderer backgroundAnimationHover) {
            this.backgroundAnimationNormal = backgroundAnimationNormal;
            this.backgroundAnimationHover = backgroundAnimationHover;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.parent.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            this.renderBackgroundAnimation(pose);
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

        protected void renderBackgroundAnimation(PoseStack pose) {
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
            a.render(pose);
            a.setWidth(cachedW);
            a.setHeight(cachedH);
            a.setPosX(cachedX);
            a.setPosY(cachedY);
            a.setLooped(cachedLoop);
            a.setOpacity(1.0F);
        }

        @NotNull
        protected IAnimationRenderer getBackgroundAnimation() {
            if (this.parent.isHovered) {
                if (this.backgroundAnimationHover != null) return this.backgroundAnimationHover;
            }
            return this.backgroundAnimationNormal;
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

        @SuppressWarnings("all")
        public static MultiTypeButtonBackground build(@Nullable Object backgroundNormal, @Nullable Object backgroundHover) {
            MultiTypeButtonBackground b = new MultiTypeButtonBackground(null, null);
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
            return b;
        }

        public static MultiTypeButtonBackground create(@NotNull ButtonBackground backgroundNormal) {
            return new MultiTypeButtonBackground(backgroundNormal, null);
        }

        public static MultiTypeButtonBackground create(@NotNull ButtonBackground backgroundNormal, @Nullable ButtonBackground backgroundHover) {
            return new MultiTypeButtonBackground(backgroundNormal, backgroundHover);
        }

        @SuppressWarnings("all")
        public MultiTypeButtonBackground(@NotNull ButtonBackground backgroundNormal, @Nullable ButtonBackground backgroundHover) {
            this.backgroundNormal = backgroundNormal;
            this.backgroundHover = backgroundHover;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.getBackground().setParent(this.parent).render(pose, mouseX, mouseY, partial);
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
            if (this.parent.isHovered) {
                if (this.backgroundHover != null) return this.backgroundHover;
            }
            return this.backgroundNormal;
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
