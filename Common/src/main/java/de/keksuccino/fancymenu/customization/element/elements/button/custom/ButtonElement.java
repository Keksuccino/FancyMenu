package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.button.VanillaButtonHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IActionExecutorElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinButton;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ButtonElement extends AbstractElement implements IActionExecutorElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public AbstractWidget button;

    public String clickSound;
    public String hoverSound;
    public String label;
    public String hoverLabel;
    public String tooltip;
    public String backgroundTextureNormal;
    public String backgroundTextureHover;
    public String backgroundAnimationNormal;
    public String backgroundAnimationHover;
    public boolean loopBackgroundAnimations = true;
    public boolean restartBackgroundAnimationsOnHover = true;
    public final List<ActionExecutor.ActionContainer> actions = new ArrayList<>();

    protected boolean hovered = false;

    public ButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;
        if (this.getButton() == null) return;

        this.tick();

        this.getButton().setAlpha(this.opacity);
        this.getButton().setX(this.getX());
        this.getButton().setY(this.getY());
        this.getButton().setWidth(this.getWidth());
        ((IMixinAbstractWidget)this.getButton()).setHeightFancyMenu(this.getHeight());

        if (isEditor()) {
            this.button.visible = true;
            if (this.button instanceof AdvancedButton) {
                ((AdvancedButton)this.button).setPressAction((b) -> {});
                this.button.active = true;
            } else if (this.button instanceof Button) {
                ((IMixinButton)this.button).setPressActionFancyMenu((b) -> {});
                this.button.active = true;
            } else {
                this.button.active = false;
            }
        }

        this.getButton().render(pose, mouseX, mouseY, partial);

        this.hovered = this.button.isHoveredOrFocused();

    }

    protected void tick() {
        if ((this.tooltip != null) && (this.getButton() != null) && this.getButton().isHoveredOrFocused()) {
            TooltipHandler.INSTANCE.addWidgetTooltip(this.getButton(), Tooltip.create(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(this.tooltip), "%n%")), false, true);
        }
        if ((this.label != null) && (this.getButton() != null)) {
            this.getButton().setMessage(Component.literal(PlaceholderParser.replacePlaceholders(this.label)));
        }
        if ((this.hoverLabel != null) && (this.getButton() != null) && this.getButton().isHoveredOrFocused() && this.getButton().active) {
            this.getButton().setMessage(Component.literal(PlaceholderParser.replacePlaceholders(this.hoverLabel)));
        }
        this.updateHoverSound();
        this.updateClickSound();
        this.updateButtonBackground();
    }

    protected void updateHoverSound() {
        if ((this.getButton() != null) && this.getButton().isHoveredOrFocused() && this.getButton().active && (this.hoverSound != null) && !this.hovered) {
            SoundHandler.resetSound(this.hoverSound);
            SoundHandler.playSound(this.hoverSound);
        }
    }

    protected void updateClickSound() {
        if ((this.button != null) && (this.clickSound != null)) {
            if (this.button instanceof AdvancedButton) {
                ((AdvancedButton)this.button).setClickSound(this.clickSound);
            } else {
                VanillaButtonHandler.setRenderTickClickSound(this.button, this.clickSound);
            }
        }
    }

    protected void updateButtonBackground() {
        if (this.button != null) {
            if (this.backgroundTextureNormal != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureNormal));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = ExternalTextureHandler.INSTANCE.getGif(f.getPath());
                        if (ani != null) {
                            if (this.button instanceof AdvancedButton) {
                                ((AdvancedButton)this.button).setBackgroundNormal(ani);
                                ((AdvancedButton)this.button).restartBackgroundAnimationsOnHover = this.restartBackgroundAnimationsOnHover;
                                ((AdvancedButton)this.button).loopBackgroundAnimations = this.loopBackgroundAnimations;
                            } else {
                                if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        ExternalTextureResourceLocation back = ExternalTextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (!back.isReady()) {
                                back.loadTexture();
                            }
                            if (this.button instanceof AdvancedButton) {
                                ((AdvancedButton)this.button).setBackgroundNormal(back.getResourceLocation());
                            } else {
                                VanillaButtonHandler.setRenderTickBackgroundTexture(this.button, back.getResourceLocation());
                            }
                        }
                    }
                }
            } else if (this.backgroundAnimationNormal != null) {
                if (AnimationHandler.animationExists(this.backgroundAnimationNormal)) {
                    IAnimationRenderer ani = AnimationHandler.getAnimation(this.backgroundAnimationNormal);
                    if (ani != null) {
                        if (this.button instanceof AdvancedButton) {
                            ((AdvancedButton)this.button).setBackgroundNormal(ani);
                            ((AdvancedButton)this.button).restartBackgroundAnimationsOnHover = this.restartBackgroundAnimationsOnHover;
                            ((AdvancedButton)this.button).loopBackgroundAnimations = this.loopBackgroundAnimations;
                        } else {
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
            if (this.backgroundTextureHover != null) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.backgroundTextureHover));
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".gif")) {
                        IAnimationRenderer ani = ExternalTextureHandler.INSTANCE.getGif(f.getPath());
                        if (ani != null) {
                            if (this.button instanceof AdvancedButton) {
                                ((AdvancedButton)this.button).setBackgroundHover(ani);
                                ((AdvancedButton)this.button).restartBackgroundAnimationsOnHover = this.restartBackgroundAnimationsOnHover;
                                ((AdvancedButton)this.button).loopBackgroundAnimations = this.loopBackgroundAnimations;
                            } else if (this.button.isHoveredOrFocused()) {
                                if (this.restartBackgroundAnimationsOnHover && this.button.isHoveredOrFocused() && !this.hovered) {
                                    ani.resetAnimation();
                                }
                                VanillaButtonHandler.setRenderTickBackgroundAnimation(this.button, ani, this.loopBackgroundAnimations, this.opacity);
                            }
                        }
                    } else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                        ExternalTextureResourceLocation back = ExternalTextureHandler.INSTANCE.getTexture(f.getPath());
                        if (back != null) {
                            if (!back.isReady()) {
                                back.loadTexture();
                            }
                            if (this.button instanceof AdvancedButton) {
                                ((AdvancedButton)this.button).setBackgroundHover(back.getResourceLocation());
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
                        if (this.button instanceof AdvancedButton) {
                            ((AdvancedButton)this.button).setBackgroundHover(ani);
                            ((AdvancedButton)this.button).restartBackgroundAnimationsOnHover = this.restartBackgroundAnimationsOnHover;
                            ((AdvancedButton)this.button).loopBackgroundAnimations = this.loopBackgroundAnimations;
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
        }
    }

    public AbstractWidget getButton() {
        return this.button;
    }

    @Override
    public List<ActionExecutor.ActionContainer> getActionList() {
        return this.actions;
    }

}
