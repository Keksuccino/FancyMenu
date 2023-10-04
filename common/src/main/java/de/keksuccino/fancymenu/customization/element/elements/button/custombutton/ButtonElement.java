package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.widget.VanillaButtonHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IExecutableElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinButton;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.List;
import java.util.Objects;

public class ButtonElement extends AbstractElement implements IExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private AbstractWidget widget;
    public String clickSound;
    public String hoverSound;
    @Nullable
    public String label;
    @Nullable
    public String hoverLabel;
    public String tooltip;
    public String backgroundTextureNormal;
    public String backgroundTextureHover;
    public String backgroundAnimationNormal;
    public String backgroundAnimationHover;
    public String backgroundTextureInactive;
    public String backgroundAnimationInactive;
    public boolean loopBackgroundAnimations = true;
    public boolean restartBackgroundAnimationsOnHover = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    protected Object lastBackgroundNormal;
    protected Object lastBackgroundHover;
    protected Object lastBackgroundInactive;
    protected boolean lastLoopBackgroundAnimations = true;
    protected boolean lastRestartBackgroundAnimationsOnHover = true;
    protected boolean hovered = false;

    public ButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;
        if (this.getWidget() == null) return;

        this.updateWidget();

        if (isEditor()) {
            this.getWidget().visible = true;
            if (this.getWidget() instanceof ExtendedButton e) {
                e.setPressAction((button) -> {});
                e.active = true;
            } else if (this.getWidget() instanceof Button b) {
                ((IMixinButton)b).setPressActionFancyMenu((button) -> {});
                b.active = true;
            } else {
                this.getWidget().active = false;
            }
            this.getWidget().setTooltip(null);
        }

        //The hover state of buttons gets updated in their render method, so make sure to update this field BEFORE the
        //button gets rendered, because otherwise renderTick() wouldn't work correctly.
        this.hovered = this.getWidget().isHoveredOrFocused();

        this.renderElementWidget(pose, mouseX, mouseY, partial);

        RenderingUtils.resetShaderColor();

    }

    protected void renderElementWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (this.getWidget() != null) {
            this.getWidget().render(pose, mouseX, mouseY, partial);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.getWidget() == null) return null;
        return List.of(this.getWidget());
    }

    public void updateWidget() {
        this.updateWidgetTooltip();
        this.updateWidgetLabels();
        this.updateWidgetHoverSound();
        this.updateWidgetClickSound();
        this.updateWidgetButtonBackground();
        this.updateWidgetAlpha();
        this.updateWidgetSize();
        this.updateWidgetPosition();
    }

    public void updateWidgetAlpha() {
        if (this.getWidget() == null) return;
        this.getWidget().setAlpha(this.opacity);
    }

    public void updateWidgetPosition() {
        if (this.getWidget() == null) return;
        this.getWidget().setX(this.getAbsoluteX());
        this.getWidget().setY(this.getAbsoluteY());
    }

    public void updateWidgetSize() {
        if (this.getWidget() == null) return;
        this.getWidget().setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget) this.getWidget()).setHeightFancyMenu(this.getAbsoluteHeight());
    }

    public void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.getWidget() != null) && this.getWidget().isHoveredOrFocused() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.getWidget(), Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }
    }

    public void updateWidgetLabels() {
        if (this.getWidget() == null) return;
        if (this.label != null) {
            this.getWidget().setMessage(buildComponent(this.label));
        } else {
            this.getWidget().setMessage(Component.empty());
        }
        if ((this.hoverLabel != null) && this.getWidget().isHoveredOrFocused() && this.getWidget().active) {
            this.getWidget().setMessage(buildComponent(this.hoverLabel));
        }
    }

    public void updateWidgetHoverSound() {
        if (this.getWidget() != null) {
            ((CustomizableWidget)this.getWidget()).setHoverSoundFancyMenu(this.hoverSound);
        }
    }

    public void updateWidgetClickSound() {
        if (this.getWidget() != null) {
            ((CustomizableWidget)this.getWidget()).setCustomClickSoundFancyMenu(this.clickSound);
        }
    }

    public void updateWidgetButtonBackground() {
        if (this.getWidget() != null) {
            Object backgroundNormal = null;
            Object backgroundHover = null;
            Object backgroundInactive = null;
            if (this.backgroundTextureNormal != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureNormal));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = TextureHandler.INSTANCE.getGifTexture(f.getPath());
                        if (ani != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundNormal = ani;
                            } else {
                                if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                if (!this.getWidget().isHoveredOrFocused()) {
                                    VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                                }
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        ITexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundNormal = back.getResourceLocation();
                            } else if (!this.getWidget().isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.getWidget(), back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationNormal != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationNormal)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationNormal);
                    if (ani != null) {
                        if (this.getWidget() instanceof ExtendedButton) {
                            backgroundNormal = ani;
                        } else {
                            if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            if (!this.getWidget().isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    }
                }
            }
            if (this.backgroundTextureHover != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureHover));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = TextureHandler.INSTANCE.getGifTexture(f.getPath());
                        if (ani != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundHover = ani;
                            } else if (this.getWidget().isHoveredOrFocused()) {
                                if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        ITexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundHover = back.getResourceLocation();
                            } else if (this.getWidget().isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.getWidget(), back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationHover != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationHover)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationHover);
                    if (ani != null) {
                        if (this.getWidget() instanceof ExtendedButton) {
                            backgroundHover = ani;
                        } else if (this.getWidget().isHoveredOrFocused()) {
                            if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                        }
                    }
                }
            }
            if (this.backgroundTextureInactive != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureInactive));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = TextureHandler.INSTANCE.getGifTexture(f.getPath());
                        if (ani != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundInactive = ani;
                            } else if (!this.getWidget().active) {
                                if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        ITexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.getWidget() instanceof ExtendedButton) {
                                backgroundInactive = back.getResourceLocation();
                            } else if (!this.getWidget().active) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.getWidget(), back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationInactive != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationInactive)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationInactive);
                    if (ani != null) {
                        if (this.getWidget() instanceof ExtendedButton) {
                            backgroundInactive = ani;
                        } else if (!this.getWidget().active) {
                            if (this.restartBackgroundAnimationsOnHover && this.getWidget().isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            VanillaButtonHandler.setRenderTickBackgroundAnimation(this.getWidget(), ani, this.loopBackgroundAnimations, this.opacity);
                        }
                    }
                }
            }
            if (this.getWidget() instanceof ExtendedButton e) {
                if (!Objects.equals(backgroundNormal, this.lastBackgroundNormal) || !Objects.equals(backgroundHover, this.lastBackgroundHover) || !Objects.equals(backgroundInactive, this.lastBackgroundInactive) || (this.lastLoopBackgroundAnimations != this.loopBackgroundAnimations) || (this.lastRestartBackgroundAnimationsOnHover != this.restartBackgroundAnimationsOnHover)) {
                    e.setBackground(ExtendedButton.MultiTypeButtonBackground.build(backgroundNormal, backgroundHover, backgroundInactive));
                    if (e.getBackground() instanceof ExtendedButton.MultiTypeButtonBackground b) {
                        if (b.getBackgroundNormal() instanceof ExtendedButton.AnimationButtonBackground a) {
                            a.getBackgroundAnimationNormal().resetAnimation();
                            if (a.getBackgroundAnimationNormal() instanceof AdvancedAnimation aa) {
                                aa.stopAudio();
                                aa.resetAudio();
                            }
                            a.setLooped(this.loopBackgroundAnimations);
                            a.setRestartOnHover(this.restartBackgroundAnimationsOnHover);
                        }
                        if (b.getBackgroundHover() instanceof ExtendedButton.AnimationButtonBackground a) {
                            a.getBackgroundAnimationNormal().resetAnimation();
                            if (a.getBackgroundAnimationNormal() instanceof AdvancedAnimation aa) {
                                aa.stopAudio();
                                aa.resetAudio();
                            }
                            a.setLooped(this.loopBackgroundAnimations);
                            a.setRestartOnHover(this.restartBackgroundAnimationsOnHover);
                        }
                        if (b.getBackgroundInactive() instanceof ExtendedButton.AnimationButtonBackground a) {
                            a.getBackgroundAnimationNormal().resetAnimation();
                            if (a.getBackgroundAnimationNormal() instanceof AdvancedAnimation aa) {
                                aa.stopAudio();
                                aa.resetAudio();
                            }
                            a.setLooped(this.loopBackgroundAnimations);
                            a.setRestartOnHover(this.restartBackgroundAnimationsOnHover);
                        }
                    }
                }
            }
            this.lastBackgroundNormal = backgroundNormal;
            this.lastBackgroundHover = backgroundHover;
            this.lastBackgroundInactive = backgroundInactive;
            this.lastLoopBackgroundAnimations = this.loopBackgroundAnimations;
            this.lastRestartBackgroundAnimationsOnHover = this.restartBackgroundAnimationsOnHover;
        }
    }

    @Nullable
    public AbstractWidget getWidget() {
        return this.widget;
    }

    public void setWidget(@Nullable AbstractWidget widget) {
        this.widget = widget;
    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

}
