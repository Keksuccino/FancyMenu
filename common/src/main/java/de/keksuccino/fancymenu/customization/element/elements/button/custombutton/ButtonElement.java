package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ButtonElement extends AbstractElement implements ExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private AbstractWidget widget;
    private final RangeSlider templateDummySlider = new RangeSlider(0, 0, 0, 0, Component.empty(), 0.0D, 1.0D, 0.5D);
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
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    @NotNull
    public LoadingRequirementContainer activeStateSupplier = new LoadingRequirementContainer();
    public boolean isTemplate = false;
    public boolean templateApplyWidth = false;
    public boolean templateApplyHeight = false;
    public boolean templateApplyPosX = false;
    public boolean templateApplyPosY = false;
    public boolean templateApplyOpacity = false;
    public boolean templateApplyVisibility = false;
    public boolean templateApplyLabel = false;
    @NotNull
    public TemplateSharing templateShareWith = TemplateSharing.BUTTONS;
    public ResourceSupplier<ITexture> sliderBackgroundTextureNormal;
    public ResourceSupplier<ITexture> sliderBackgroundTextureHighlighted;
    public boolean nineSliceSliderHandle = false;
    public int nineSliceSliderHandleBorderX = 5;
    public int nineSliceSliderHandleBorderY = 5;

    protected static long lastTemplateUpdateButton = -1L;
    protected static ButtonElement lastTemplateButton = null;
    protected static long lastTemplateUpdateSlider = -1L;
    protected static ButtonElement lastTemplateSlider = null;

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
    public void afterConstruction() {
        resetTemplateCache();
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
        if ((this.getWidget() != null) && (!this.isTemplate || isEditor())) {
            //Prevents crashes related to dividing by zero
            if (this.getWidget().getHeight() <= 0) return;
            if (this.getWidget().getWidth() <= 0) return;
            this.getWidget().render(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.isTemplate) return null;
        if (this.getWidget() == null) return null;
        return List.of(this.getWidget());
    }

    @Override
    public int getAbsoluteWidth() {
        if (!this.isTemplate && this.isTemplateActive()) {
            ButtonElement template = getTopActiveTemplateElement(this.isSlider());
            if ((template != null) && template.templateApplyWidth) return template.getAbsoluteWidth();
        }
        return super.getAbsoluteWidth();
    }

    @Override
    public int getAbsoluteHeight() {
        if (!this.isTemplate && this.isTemplateActive()) {
            ButtonElement template = getTopActiveTemplateElement(this.isSlider());
            if ((template != null) && template.templateApplyHeight) return template.getAbsoluteHeight();
        }
        return super.getAbsoluteHeight();
    }

    @Override
    public int getAbsoluteX() {
        if (!this.isTemplate && this.isTemplateActive()) {
            ButtonElement template = getTopActiveTemplateElement(this.isSlider());
            if ((template != null) && template.templateApplyPosX) return template.getAbsoluteX();
        }
        return super.getAbsoluteX();
    }

    @Override
    public int getAbsoluteY() {
        if (!this.isTemplate && this.isTemplateActive()) {
            ButtonElement template = getTopActiveTemplateElement(this.isSlider());
            if ((template != null) && template.templateApplyPosY) return template.getAbsoluteY();
        }
        return super.getAbsoluteY();
    }

    @Override
    public boolean shouldRender() {
        if (!this.isTemplate && this.isTemplateActive()) {
            ButtonElement template = getTopActiveTemplateElement(this.isSlider());
            if ((template != null) && template.templateApplyVisibility) return template.shouldRender();
        }
        return super.shouldRender();
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
        this.getWidget().setAlpha(this.getOpacity());
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
        String l = this.getLabel();
        String h = this.getHoverLabel();
        if (this.getWidget() == null) return;
        if (l != null) {
            this.getWidget().setMessage(buildComponent(l));
        } else {
            this.getWidget().setMessage(Component.empty());
        }
        if ((h != null) && this.getWidget().isHoveredOrFocused() && this.getWidget().active) {
            this.getWidget().setMessage(buildComponent(h));
        }
    }

    public void updateWidgetHoverSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.getPropertySource().hoverSound != null) ? this.getPropertySource().hoverSound.get() : null);
        }
    }

    public void updateWidgetClickSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setCustomClickSoundFancyMenu((this.getPropertySource().clickSound != null) ? this.getPropertySource().clickSound.get() : null);
        }
    }

    public void updateWidgetTexture() {

        RenderableResource backNormal = null;
        RenderableResource backHover = null;
        RenderableResource backInactive = null;

        //Normal
        if (this.getPropertySource().backgroundTextureNormal != null) {
            backNormal = this.getPropertySource().backgroundTextureNormal.get();
        }
        //Hover
        if (this.getPropertySource().backgroundTextureHover != null) {
            backHover = this.getPropertySource().backgroundTextureHover.get();
        }
        //Inactive
        if (this.getPropertySource().backgroundTextureInactive != null) {
            backInactive = this.getPropertySource().backgroundTextureInactive.get();
        }

        if (this.getWidget() instanceof CustomizableWidget w) {
            if (this.getWidget() instanceof CustomizableSlider s) {
                s.setNineSliceCustomSliderBackground_FancyMenu(this.getPropertySource().nineSliceCustomBackground);
                s.setNineSliceSliderBackgroundBorderX_FancyMenu(this.getPropertySource().nineSliceBorderX);
                s.setNineSliceSliderBackgroundBorderY_FancyMenu(this.getPropertySource().nineSliceBorderY);
            } else {
                w.setNineSliceCustomBackground_FancyMenu(this.getPropertySource().nineSliceCustomBackground);
                w.setNineSliceBorderX_FancyMenu(this.getPropertySource().nineSliceBorderX);
                w.setNineSliceBorderY_FancyMenu(this.getPropertySource().nineSliceBorderY);
            }
            w.setCustomBackgroundNormalFancyMenu(backNormal);
            w.setCustomBackgroundHoverFancyMenu(backHover);
            w.setCustomBackgroundInactiveFancyMenu(backInactive);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.getPropertySource().restartBackgroundAnimationsOnHover ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
        }

        //-------------------------------------

        RenderableResource sliderBackNormal = null;
        RenderableResource sliderBackHighlighted = null;

        //Normal
        if (this.getPropertySource().sliderBackgroundTextureNormal != null) {
            sliderBackNormal = this.getPropertySource().sliderBackgroundTextureNormal.get();
        }
        //Highlighted
        if (this.getPropertySource().sliderBackgroundTextureHighlighted != null) {
            sliderBackHighlighted = this.getPropertySource().sliderBackgroundTextureHighlighted.get();
        }

        if (this.getWidget() instanceof CustomizableSlider w) {
            w.setNineSliceCustomSliderHandle_FancyMenu(this.getPropertySource().nineSliceSliderHandle);
            w.setNineSliceSliderHandleBorderX_FancyMenu(this.getPropertySource().nineSliceSliderHandleBorderX);
            w.setNineSliceSliderHandleBorderY_FancyMenu(this.getPropertySource().nineSliceSliderHandleBorderY);
            w.setCustomSliderBackgroundNormalFancyMenu(sliderBackNormal);
            w.setCustomSliderBackgroundHighlightedFancyMenu(sliderBackHighlighted);
        }

    }

    @Nullable
    public AbstractWidget getWidget() {
        if (isEditor() && this.isTemplate && (this.templateShareWith == TemplateSharing.SLIDERS)) {
            return this.templateDummySlider;
        }
        return this.widget;
    }

    public void setWidget(@Nullable AbstractWidget widget) {
        this.widget = widget;
    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

    public boolean isButton() {
        return (this.getWidget() instanceof AbstractButton);
    }

    public boolean isSlider() {
        return (this.getWidget() instanceof CustomizableSlider);
    }

    public float getOpacity() {
        if (this.isTemplate) return this.opacity;
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyOpacity) return template.opacity;
        return this.opacity;
    }

    @Nullable
    public String getLabel() {
        if (this.isTemplate) return this.label;
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) return template.label;
        return this.label;
    }

    @Nullable
    public String getHoverLabel() {
        if (this.isTemplate) return this.hoverLabel;
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) return template.hoverLabel;
        return this.hoverLabel;
    }

    @NotNull
    public ButtonElement getPropertySource() {
        if (this.isTemplate) return this;
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if (template != null) {
            if (this.isSlider() && (template.templateShareWith == TemplateSharing.BUTTONS)) return this;
            if (this.isButton() && (template.templateShareWith == TemplateSharing.SLIDERS)) return this;
            return template;
        }
        return this;
    }

    public boolean isTemplateActive() {
        if (this.isTemplate) return false;
        return (getTopActiveTemplateElement(this.isSlider()) != null);
    }

    @Nullable
    public static ButtonElement getTopActiveTemplateElement(boolean forSlider) {
        long now = System.currentTimeMillis();
        if (!forSlider && ((lastTemplateUpdateButton + 100L) > now)) {
            return lastTemplateButton;
        }
        if (forSlider && ((lastTemplateUpdateSlider + 100L) > now)) {
            return lastTemplateSlider;
        }
        ButtonElement template = null;
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (layer != null) {
            for (AbstractElement e : layer.allElements) {
                if (e instanceof ButtonElement b) {
                    boolean validTemplate = true;
                    if (forSlider && (b.templateShareWith == TemplateSharing.BUTTONS)) validTemplate = false;
                    if (!forSlider && (b.templateShareWith == TemplateSharing.SLIDERS)) validTemplate = false;
                    if (b.isTemplate && b.shouldRender() && validTemplate) {
                        template = b;
                        break;
                    }
                }
            }
        }
        if (!forSlider) {
            lastTemplateButton = template;
            lastTemplateUpdateButton = now;
        } else {
            lastTemplateSlider = template;
            lastTemplateUpdateSlider = now;
        }
        return template;
    }

    public static void resetTemplateCache() {
        lastTemplateButton = null;
        lastTemplateUpdateButton = -1L;
        lastTemplateSlider = null;
        lastTemplateUpdateSlider = -1L;
    }

    public enum TemplateSharing implements LocalizedCycleEnum<TemplateSharing> {

        BUTTONS("buttons"),
        SLIDERS("sliders");

        private final String name;

        TemplateSharing(@NotNull String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.elements.button.template.share_with";
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull TemplateSharing[] getValues() {
            return TemplateSharing.values();
        }

        @Override
        public @Nullable TemplateSharing getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static TemplateSharing getByName(@NotNull String name) {
            for (TemplateSharing sharing : TemplateSharing.values()) {
                if (sharing.name.equals(name)) return sharing;
            }
            return null;
        }

    }

}
