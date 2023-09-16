package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.widget.VanillaButtonHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IExecutableElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinButton;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.ListUtils;
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

    public AbstractWidget button;

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
        if (this.getButton() == null) return;

        this.renderTick();

        this.getButton().setAlpha(this.opacity);
        this.getButton().setX(this.getAbsoluteX());
        this.getButton().setY(this.getAbsoluteY());
        this.getButton().setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget)this.getButton()).setHeightFancyMenu(this.getAbsoluteHeight());

        if (isEditor()) {
            this.button.visible = true;
            if (this.button instanceof ExtendedButton) {
                ((ExtendedButton)this.button).setPressAction((b) -> {});
                this.button.active = true;
            } else if (this.button instanceof Button) {
                ((IMixinButton)this.button).setPressActionFancyMenu((b) -> {});
                this.button.active = true;
            } else {
                this.button.active = false;
            }
            this.button.setTooltip(null);
        }

        //The hover state of buttons gets updated in their render method, so make sure to update this field BEFORE the
        //button gets rendered, because otherwise renderTick() wouldn't work correctly.
        this.hovered = this.getButton().isHoveredOrFocused();

        this.renderElementWidget(pose, mouseX, mouseY, partial);

        RenderingUtils.resetShaderColor();

    }

    protected void renderElementWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.getButton().render(pose, mouseX, mouseY, partial);
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.getButton() == null) return null;
        return ListUtils.build(this.getButton());
    }

    protected void renderTick() {
        if ((this.tooltip != null) && (this.getButton() != null) && this.getButton().isHoveredOrFocused() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.getButton(), Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }
        this.updateLabels();
        this.updateHoverSound();
        this.updateClickSound();
        this.updateButtonBackground();
    }

    protected void updateLabels() {
        if (this.button == null) return;
        if (this.label != null) {
            this.getButton().setMessage(Component.literal(PlaceholderParser.replacePlaceholders(this.label)));
        } else {
            this.button.setMessage(Component.empty());
        }
        if ((this.hoverLabel != null) && this.getButton().isHoveredOrFocused() && this.getButton().active) {
            this.getButton().setMessage(Component.literal(PlaceholderParser.replacePlaceholders(this.hoverLabel)));
        }
    }

    protected void updateHoverSound() {
//        if ((this.getButton() != null) && this.getButton().isHoveredOrFocused() && this.getButton().active && (this.hoverSound != null) && !this.hovered) {
//            SoundHandler.resetSound(this.hoverSound);
//            SoundHandler.playSound(this.hoverSound);
//        }
        if (this.button != null) {
            ((CustomizableWidget)this.button).setHoverSoundFancyMenu(this.hoverSound);
        }
    }

    protected void updateClickSound() {
        if (this.button != null) {
            ((CustomizableWidget)this.button).setCustomClickSoundFancyMenu(this.clickSound);
        }
    }

    protected void updateButtonBackground() {
        if (this.button != null) {
            Object backgroundNormal = null;
            Object backgroundHover = null;
            Object backgroundInactive = null;
            if (this.backgroundTextureNormal != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureNormal));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = TextureHandler.INSTANCE.getGifTexture(f.getPath());
                        if (ani != null) {
                            if (this.button instanceof ExtendedButton) {
                                backgroundNormal = ani;
                            } else {
                                if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                if (!this.getButton().isHoveredOrFocused()) {
                                    VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                                }
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        LocalTexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.button instanceof ExtendedButton) {
                                backgroundNormal = back.getResourceLocation();
                            } else if (!this.getButton().isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.button, back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationNormal != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationNormal)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationNormal);
                    if (ani != null) {
                        if (this.button instanceof ExtendedButton) {
                            backgroundNormal = ani;
                        } else {
                            if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            if (!this.getButton().isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
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
                            if (this.button instanceof ExtendedButton) {
                                backgroundHover = ani;
                            } else if (this.button.isHoveredOrFocused()) {
                                if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        LocalTexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.button instanceof ExtendedButton) {
                                backgroundHover = back.getResourceLocation();
                            } else if (this.button.isHoveredOrFocused()) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.button, back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationHover != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationHover)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationHover);
                    if (ani != null) {
                        if (this.button instanceof ExtendedButton) {
                            backgroundHover = ani;
                        } else if (this.button.isHoveredOrFocused()) {
                            if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
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
                            if (this.button instanceof ExtendedButton) {
                                backgroundInactive = ani;
                            } else if (!this.button.active) {
                                if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        LocalTexture back = TextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (this.button instanceof ExtendedButton) {
                                backgroundInactive = back.getResourceLocation();
                            } else if (!this.button.active) {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.button, back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationInactive != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationInactive)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationInactive);
                    if (ani != null) {
                        if (this.button instanceof ExtendedButton) {
                            backgroundInactive = ani;
                        } else if (!this.button.active) {
                            if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                if (ani instanceof AdvancedAnimation) {
                                    ((AdvancedAnimation)ani).stopAudio();
                                    ((AdvancedAnimation)ani).resetAudio();
                                }
                                ani.resetAnimation();
                            }
                            VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                        }
                    }
                }
            }
            if (this.button instanceof ExtendedButton e) {
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

    public AbstractWidget getButton() {
        return this.button;
    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

}
