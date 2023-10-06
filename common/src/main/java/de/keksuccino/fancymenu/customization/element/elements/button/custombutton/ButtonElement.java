package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IExecutableElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinButton;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
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
import java.util.List;

public class ButtonElement extends AbstractElement implements IExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private AbstractWidget widget;
    public ResourceFile clickSound;
    public ResourceFile hoverSound;
    @Nullable
    public String label;
    @Nullable
    public String hoverLabel;
    public String tooltip;
    public ResourceFile backgroundTextureNormal;
    public ResourceFile backgroundTextureHover;
    public ResourceFile backgroundTextureInactive;
    public String backgroundAnimationNormal;
    public String backgroundAnimationHover;
    public String backgroundAnimationInactive;
    public boolean loopBackgroundAnimations = true;
    public boolean restartBackgroundAnimationsOnHover = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
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
        this.updateWidgetTexture();
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
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.hoverSound != null) ? this.hoverSound.getAbsolutePath() : null);
        }
    }

    public void updateWidgetClickSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setCustomClickSoundFancyMenu((this.clickSound != null) ? this.clickSound.getAbsolutePath() : null);
        }
    }

    public void updateWidgetTexture() {

        RenderableResource backNormal = null;
        RenderableResource backHover = null;
        RenderableResource backInactive = null;

        //Normal
        if ((this.backgroundAnimationNormal != null) && AnimationHandler.animationExists(this.backgroundAnimationNormal)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.backgroundAnimationNormal);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                backNormal = a;
            }
        }
        if ((backNormal == null) && (this.backgroundTextureNormal != null) && this.backgroundTextureNormal.exists() && FileFilter.IMAGE_AND_GIF_FILE_FILTER.checkFile(this.backgroundTextureNormal.getFile())) {
            backNormal = TextureHandler.INSTANCE.getTexture(this.backgroundTextureNormal.getAbsolutePath());
        }
        //Hover
        if ((this.backgroundAnimationHover != null) && AnimationHandler.animationExists(this.backgroundAnimationHover)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.backgroundAnimationHover);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                backHover = a;
            }
        }
        if ((backHover == null) && (this.backgroundTextureHover != null) && this.backgroundTextureHover.exists() && FileFilter.IMAGE_AND_GIF_FILE_FILTER.checkFile(this.backgroundTextureHover.getFile())) {
            backHover = TextureHandler.INSTANCE.getTexture(this.backgroundTextureHover.getAbsolutePath());
        }
        //Inactive
        if ((this.backgroundAnimationInactive != null) && AnimationHandler.animationExists(this.backgroundAnimationInactive)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.backgroundAnimationInactive);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                backInactive = a;
            }
        }
        if ((backInactive == null) && (this.backgroundTextureInactive != null) && this.backgroundTextureInactive.exists() && FileFilter.IMAGE_AND_GIF_FILE_FILTER.checkFile(this.backgroundTextureInactive.getFile())) {
            backInactive = TextureHandler.INSTANCE.getTexture(this.backgroundTextureInactive.getAbsolutePath());
        }

        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setCustomBackgroundNormalFancyMenu(backNormal);
            w.setCustomBackgroundHoverFancyMenu(backHover);
            w.setCustomBackgroundInactiveFancyMenu(backInactive);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.restartBackgroundAnimationsOnHover ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
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
