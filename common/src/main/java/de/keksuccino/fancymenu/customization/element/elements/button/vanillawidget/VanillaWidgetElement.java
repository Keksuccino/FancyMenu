package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class VanillaWidgetElement extends ButtonElement implements HideableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public WidgetMeta widgetMeta;
    public boolean vanillaButtonHidden = false;
    public int automatedButtonClicks = 0;
    public ResourceSupplier<ITexture> sliderBackgroundTextureNormal;
    public ResourceSupplier<ITexture> sliderBackgroundTextureHighlighted;
    public String sliderBackgroundAnimationNormal;
    public String sliderBackgroundAnimationHighlighted;
    protected boolean automatedButtonClicksDone = false;

    public VanillaWidgetElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @SuppressWarnings("all")
    @Override
    protected void renderElementWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        ((CustomizableWidget)this.getWidget()).setHiddenFancyMenu((isEditor() || this.isCopyrightButton()) ? false : this.vanillaButtonHidden);
        if (isEditor()) {
            //Only render button in editor
            super.renderElementWidget(pose, mouseX, mouseY, partial);
        }
        if (this.anchorPoint == ElementAnchorPoints.VANILLA) {
            this.resetVanillaWidgetSizeAndPosition();
            this.mirrorVanillaWidgetSizeAndPosition();
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return null;
    }

    @Override
    public void updateWidget() {

        //Auto-click the vanilla button on menu load (don't add this to updateWidget(), because it could break stuff!)
        if (!isEditor() && !this.automatedButtonClicksDone && (this.automatedButtonClicks > 0)) {
            for (int i = 0; i < this.automatedButtonClicks; i++) {
                if (this.getWidget() != null) this.getWidget().onClick(this.getWidget().getX() + 1, this.getWidget().getY() + 1);
            }
            this.automatedButtonClicksDone = true;
        }

        this.updateWidgetVisibility();

        super.updateWidget();

    }

    @Override
    public void updateWidgetTexture() {

        super.updateWidgetTexture();

        RenderableResource sliderBackNormal = null;
        RenderableResource sliderBackHighlighted = null;

        //Normal
        if ((this.sliderBackgroundAnimationNormal != null) && AnimationHandler.animationExists(this.sliderBackgroundAnimationNormal)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.sliderBackgroundAnimationNormal);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                sliderBackNormal = a;
            }
        }
        if ((sliderBackNormal == null) && (this.sliderBackgroundTextureNormal != null)) {
            sliderBackNormal = this.sliderBackgroundTextureNormal.get();
        }
        //Highlighted
        if ((this.sliderBackgroundAnimationHighlighted != null) && AnimationHandler.animationExists(this.sliderBackgroundAnimationHighlighted)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.sliderBackgroundAnimationHighlighted);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                sliderBackHighlighted = a;
            }
        }
        if ((sliderBackHighlighted == null) && (this.sliderBackgroundTextureHighlighted != null)) {
            sliderBackHighlighted = this.sliderBackgroundTextureHighlighted.get();
        }

        if (this.getWidget() instanceof CustomizableSlider w) {
            w.setCustomSliderBackgroundNormalFancyMenu(sliderBackNormal);
            w.setCustomSliderBackgroundHighlightedFancyMenu(sliderBackHighlighted);
        }

    }

    public void updateWidgetVisibility() {
        if (this.getWidget() == null) return;
        boolean forceVisible = isEditor() || this.isCopyrightButton();
        ((CustomizableWidget)this.getWidget()).setHiddenFancyMenu(!forceVisible && this.vanillaButtonHidden);
    }

    @Override
    public void updateWidgetPosition() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            if (this.anchorPoint != ElementAnchorPoints.VANILLA) {
                w.setCustomXFancyMenu(this.getAbsoluteX());
                w.setCustomYFancyMenu(this.getAbsoluteY());
            } else {
                w.setCustomXFancyMenu(null);
                w.setCustomYFancyMenu(null);
            }
        }
    }

    @Override
    public void updateWidgetSize() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            if (this.anchorPoint != ElementAnchorPoints.VANILLA) {
                w.setCustomWidthFancyMenu(this.getAbsoluteWidth());
                w.setCustomHeightFancyMenu(this.getAbsoluteHeight());
            } else {
                w.setCustomWidthFancyMenu(null);
                w.setCustomHeightFancyMenu(null);
            }
        }
    }

    @Override
    public void updateWidgetLabels() {
        if (this.getWidget() == null) return;
        ((CustomizableWidget)this.getWidget()).setCustomLabelFancyMenu((this.label != null) ? buildComponent(this.label) : null);
        ((CustomizableWidget)this.getWidget()).setHoverLabelFancyMenu((this.hoverLabel != null) ? buildComponent(this.hoverLabel) : null);
    }

    @Override
    public @NotNull String getInstanceIdentifier() {
        if (this.widgetMeta != null) {
            return "vanillabtn:" + this.widgetMeta.getIdentifier();
        }
        return super.getInstanceIdentifier();
    }

    public void setVanillaWidget(WidgetMeta data, boolean mirrorWidgetSizeAndPos) {
        this.widgetMeta = data;
        this.setWidget(data.getWidget());
        if (mirrorWidgetSizeAndPos) this.mirrorVanillaWidgetSizeAndPosition();
    }

    public void mirrorVanillaWidgetSizeAndPosition() {
        this.mirrorVanillaWidgetSize();
        this.mirrorVanillaWidgetPosition();
    }

    public void mirrorVanillaWidgetSize() {
        if (this.getWidget() == null) return;
        this.baseWidth = this.getWidget().getWidth();
        this.baseHeight = this.getWidget().getHeight();
    }

    public void mirrorVanillaWidgetPosition() {
        if (this.getWidget() == null) return;
        this.posOffsetX = this.getWidget().getX();
        this.posOffsetY = this.getWidget().getY();
    }

    public void resetVanillaWidgetSizeAndPosition() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.resetWidgetSizeAndPositionFancyMenu();
        }
    }

    @Override
    public boolean isHidden() {
        if (this.isCopyrightButton()) return false;
        return this.vanillaButtonHidden;
    }

    @Override
    public void setHidden(boolean hidden) {
        if (this.isCopyrightButton()) hidden = false;
        this.vanillaButtonHidden = hidden;
    }

    public boolean isCopyrightButton() {
        if (this.widgetMeta == null) return false;
        String compId = this.widgetMeta.getUniversalIdentifier();
        return ((compId != null) && compId.equals("mc_titlescreen_copyright_button"));
    }

}
