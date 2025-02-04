package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ButtonElement extends AbstractElement implements ExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private AbstractWidget widget;
    public ResourceSupplier<IAudio> clickSound;
    public ResourceSupplier<IAudio> hoverSound;
    @Nullable
    public String label;
    @Nullable
    public String hoverLabel;
    public String tooltip;
    public ResourceSupplier<ITexture> backgroundTextureNormal;
    public ResourceSupplier<ITexture> backgroundTextureHover;
    public ResourceSupplier<ITexture> backgroundTextureInactive;
    public String backgroundAnimationNormal;
    public String backgroundAnimationHover;
    public String backgroundAnimationInactive;
    public boolean loopBackgroundAnimations = true;
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    @NotNull
    public LoadingRequirementContainer activeStateSupplier = new LoadingRequirementContainer();

    public ButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @Override
    public void tick() {

        if (this.getWidget() == null) return;

        //This is mainly to make Vanilla buttons not flicker for the first frame when hidden
        this.updateWidget();

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.getWidget() == null) return;

        this.updateWidget();

        if (!this.shouldRender()) return;

        if (isEditor()) {
            net.minecraft.client.gui.components.Tooltip cachedVanillaTooltip = this.getWidget().getTooltip();
            boolean cachedVisible = this.getWidget().visible;
            boolean cachedActive = this.getWidget().active;
            this.getWidget().visible = true;
            this.getWidget().active = true;
            this.getWidget().setTooltip(null);
            MainThreadTaskExecutor.executeInMainThread(() -> {
                this.getWidget().visible = cachedVisible;
                this.getWidget().active = cachedActive;
                this.getWidget().setTooltip(cachedVanillaTooltip);
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }

        this.renderElementWidget(graphics, mouseX, mouseY, partial);

        RenderingUtils.resetShaderColor(graphics);

    }

    @Override
    public void tickVisibleInvisible() {
        super.tickVisibleInvisible();
        if (this.getWidget() != null) this.updateWidget();
    }

    protected void renderElementWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.getWidget() != null) {
            //Prevents crashes related to dividing by zero
            if (this.getWidget().getHeight() <= 0) return;
            if (this.getWidget().getWidth() <= 0) return;
            this.getWidget().render(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.getWidget() == null) return null;
        return List.of(this.getWidget());
    }

    public void updateWidget() {
        this.updateWidgetActiveState();
        this.updateWidgetVisibility();
        this.updateWidgetAlpha();
        this.updateWidgetTooltip();
        this.updateWidgetLabels();
        this.updateWidgetHoverSound();
        this.updateWidgetClickSound();
        this.updateWidgetTexture();
        this.updateWidgetSize();
        this.updateWidgetPosition();
        this.updateWidgetNavigatable();
    }

    public void updateWidgetActiveState() {
        if (this.getWidget() == null) return;
        this.getWidget().active = this.activeStateSupplier.requirementsMet();
    }

    public void updateWidgetNavigatable() {
        if (this.getWidget() instanceof NavigatableWidget w) {
            w.setNavigatable(this.navigatable);
        }
    }

    public void updateWidgetVisibility() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHiddenFancyMenu(!this.shouldRender());
        }
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
        if ((this.tooltip != null) && (this.getWidget() != null) && this.getWidget().isHovered() && this.getWidget().visible && this.shouldRender() && !isEditor()) {
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
            w.setHoverSoundFancyMenu((this.hoverSound != null) ? this.hoverSound.get() : null);
        }
    }

    public void updateWidgetClickSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setCustomClickSoundFancyMenu((this.clickSound != null) ? this.clickSound.get() : null);
        }
    }

    public void updateWidgetTexture() {

        RenderableResource backNormal = null;
        RenderableResource backHover = null;
        RenderableResource backInactive = null;

        //Normal
        if (this.backgroundTextureNormal != null) {
            backNormal = this.backgroundTextureNormal.get();
        }
        //Hover
        if (this.backgroundTextureHover != null) {
            backHover = this.backgroundTextureHover.get();
        }
        //Inactive
        if (this.backgroundTextureInactive != null) {
            backInactive = this.backgroundTextureInactive.get();
        }

        if (this.getWidget() instanceof CustomizableWidget w) {
            if (this.getWidget() instanceof CustomizableSlider s) {
                s.setNineSliceCustomSliderBackground_FancyMenu(this.nineSliceCustomBackground);
                s.setNineSliceSliderBackgroundBorderX_FancyMenu(this.nineSliceBorderX);
                s.setNineSliceSliderBackgroundBorderY_FancyMenu(this.nineSliceBorderY);
            } else {
                w.setNineSliceCustomBackground_FancyMenu(this.nineSliceCustomBackground);
                w.setNineSliceBorderX_FancyMenu(this.nineSliceBorderX);
                w.setNineSliceBorderY_FancyMenu(this.nineSliceBorderY);
            }
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
