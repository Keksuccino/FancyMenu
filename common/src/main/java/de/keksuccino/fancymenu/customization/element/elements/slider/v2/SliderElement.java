package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.ListSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class SliderElement extends AbstractElement implements ExecutableElement {

    public static final String VALUE_PLACEHOLDER = "$$value";

    public AbstractExtendedSlider slider;
    @NotNull
    public SliderType type = SliderType.INTEGER_RANGE;
    @Nullable
    public String preSelectedValue;
    @NotNull
    public List<String> listValues = new ArrayList<>();
    public double minRangeValue = 1;
    public double maxRangeValue = 10;
    public int roundingDecimalPlace = 2;
    @Nullable
    public String label;
    public String tooltip;
    public ResourceSupplier<ITexture> handleTextureNormal;
    public ResourceSupplier<ITexture> handleTextureHover;
    public ResourceSupplier<ITexture> handleTextureInactive;
    public ResourceSupplier<ITexture> sliderBackgroundTextureNormal;
    public ResourceSupplier<ITexture> sliderBackgroundTextureHighlighted;
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    public boolean nineSliceSliderHandle = false;
    public int nineSliceSliderHandleBorderX = 5;
    public int nineSliceSliderHandleBorderY = 5;
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock executableBlock = new GenericExecutableBlock();
    @NotNull
    public LoadingRequirementContainer activeStateSupplier = new LoadingRequirementContainer();
    public ResourceSupplier<IAudio> hoverSound;

    public SliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.buildSlider();
        this.prepareExecutableBlock();
        this.allowDepthTestManipulation = true;
    }

    /**
     * Call this after setting a new block instance.
     */
    public void prepareExecutableBlock() {
        this.executableBlock.addValuePlaceholder("value", () -> (this.slider != null) ? this.slider.getValueDisplayText() : "");
    }

    /**
     * Call this after setting a new container instance.
     */
    public void prepareLoadingRequirementContainer() {
        this.loadingRequirementContainer.addValuePlaceholder("value", () -> (this.slider != null) ? this.slider.getValueDisplayText() : "");
    }

    /**
     * This should only get called on init or in the editor, because the new slider will
     * not get registered as {@link Screen} widget by calling this method.
     */
    public void buildSlider() {

        String preSelectedString = (this.preSelectedValue != null) ? PlaceholderParser.replacePlaceholders(this.preSelectedValue) : null;

        //Build slider instance based on element's slider type
        if (this.type == SliderType.INTEGER_RANGE) {
            int min = (int) this.minRangeValue;
            int max = (int) this.maxRangeValue;
            int preSelected = min;
            if ((preSelectedString != null) && MathUtils.isDouble(preSelectedString)) {
                preSelected = (int) Double.parseDouble(preSelectedString);
            }
            this.slider = new RangeSlider(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), min, max, preSelected);
            ((RangeSlider)this.slider).setShowAsInteger(true);
        }
        if (this.type == SliderType.DECIMAL_RANGE) {
            double preSelected = this.minRangeValue;
            if ((preSelectedString != null) && MathUtils.isDouble(preSelectedString)) {
                preSelected = Double.parseDouble(preSelectedString);
            }
            this.slider = new RangeSlider(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), this.minRangeValue, this.maxRangeValue, preSelected);
            ((RangeSlider)this.slider).setRoundingDecimalPlace(this.roundingDecimalPlace);
        }
        if (this.type == SliderType.LIST) {
            if (this.listValues.isEmpty()) this.listValues.addAll(List.of("placeholder_1", "placeholder_2"));
            if (this.listValues.size() < 2) this.listValues.add("placeholder_1");
            int preSelectedIndex = (preSelectedString != null) ? this.listValues.indexOf(preSelectedString) : 0;
            if (preSelectedIndex < 0) preSelectedIndex = 0;
            this.slider = new ListSlider<>(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), this.listValues, preSelectedIndex);
        }

        //Set label supplier and value update listener
        if (this.slider != null) {
            this.slider.setLabelSupplier(this::getSliderLabel);
            this.slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> this.onChangeSliderValue());
        }

    }

    @NotNull
    protected Component getSliderLabel(@NotNull AbstractExtendedSlider slider) {
        String labelValueReplaced = (this.label != null) ? this.label.replace(VALUE_PLACEHOLDER, slider.getValueDisplayText()) : "";
        return buildComponent(labelValueReplaced);
    }

    protected void onChangeSliderValue() {
        this.executableBlock.execute();
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return !isEditor() ? List.of(this.slider) : null;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.slider == null) return;

        this.slider.setNavigatable(this.navigatable);

        this.slider.visible = this.shouldRender();
        this.slider.setAlpha(this.getOpacity());

        if (!this.shouldRender()) return;

        this.slider.setX(this.getAbsoluteX());
        this.slider.setY(this.getAbsoluteY());
        this.slider.setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget)this.slider).setHeightFancyMenu(this.getAbsoluteHeight());

        this.updateWidget();

        this.slider.render(graphics, mouseX, mouseY, partial);

    }

    public void updateWidget() {
        if (this.slider == null) return;
        this.updateWidgetHoverSound();
        this.updateWidgetActiveState();
        this.updateWidgetTooltip();
        this.updateWidgetTexture();
        this.slider.updateMessage();
    }

    public void updateWidgetHoverSound() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.getHoverSound() != null) ? this.getHoverSound().get() : null);
        }
    }

    public void updateWidgetActiveState() {
        if (this.slider == null) return;
        this.slider.active = this.activeStateSupplier.requirementsMet();
    }

    public void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.slider != null) && this.shouldRender() && !isEditor()) {
            String t = PlaceholderParser.replacePlaceholders(this.tooltip).replace("%n%", "\n").replace("\\n", "\n");
            this.slider.setTooltip(Tooltip.create(Component.literal(t)));
        }
    }

    public void updateWidgetTexture() {

        RenderableResource sliderBackNormal = null;
        RenderableResource sliderBackHighlighted = null;

        //Normal Slider Background
        if (this.getSliderBackgroundTextureNormal() != null) {
            sliderBackNormal = this.getSliderBackgroundTextureNormal().get();
        }
        //Highlighted Slider Background
        if (this.getSliderBackgroundTextureHighlighted() != null) {
            sliderBackHighlighted = this.getSliderBackgroundTextureHighlighted().get();
        }

        if (this.slider instanceof CustomizableSlider w) {
            w.setCustomSliderBackgroundNormalFancyMenu(sliderBackNormal);
            w.setCustomSliderBackgroundHighlightedFancyMenu(sliderBackHighlighted);
        }

        RenderableResource handleTextureNormal = null;
        RenderableResource handleTextureHover = null;
        RenderableResource handleTextureInactive = null;

        //Normal
        if (this.getHandleTextureNormal() != null) {
            handleTextureNormal = this.getHandleTextureNormal().get();
        }
        //Hover
        if (this.getHandleTextureHover() != null) {
            handleTextureHover = this.getHandleTextureHover().get();
        }
        //Inactive
        if (this.getHandleTextureInactive() != null) {
            handleTextureInactive = this.getHandleTextureInactive().get();
        }

        if (this.slider instanceof CustomizableWidget w) {
            if (this.slider instanceof CustomizableSlider s) {
                s.setNineSliceCustomSliderBackground_FancyMenu(this.isNineSliceCustomBackground());
                s.setNineSliceSliderBackgroundBorderX_FancyMenu(this.getNineSliceBorderX());
                s.setNineSliceSliderBackgroundBorderY_FancyMenu(this.getNineSliceBorderY());
                s.setNineSliceCustomSliderHandle_FancyMenu(this.isNineSliceSliderHandle());
                s.setNineSliceSliderHandleBorderX_FancyMenu(this.getNineSliceSliderHandleBorderX());
                s.setNineSliceSliderHandleBorderY_FancyMenu(this.getNineSliceSliderHandleBorderY());
            }
            w.setCustomBackgroundNormalFancyMenu(handleTextureNormal);
            w.setCustomBackgroundHoverFancyMenu(handleTextureHover);
            w.setCustomBackgroundInactiveFancyMenu(handleTextureInactive);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.isRestartBackgroundAnimationsOnHover() ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
        }

    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.executableBlock;
    }

    @Override
    public int getAbsoluteWidth() {
        if (this.isTemplateActive()) {
            ButtonElement template = this.getPropertySource();
            if ((template != null) && template.templateApplyWidth) return template.getAbsoluteWidth();
        }
        return super.getAbsoluteWidth();
    }

    @Override
    public int getAbsoluteHeight() {
        if (this.isTemplateActive()) {
            ButtonElement template = this.getPropertySource();
            if ((template != null) && template.templateApplyHeight) return template.getAbsoluteHeight();
        }
        return super.getAbsoluteHeight();
    }

    @Override
    public int getAbsoluteX() {
        if (this.isTemplateActive()) {
            ButtonElement template = this.getPropertySource();
            if ((template != null) && template.templateApplyPosX) return template.getAbsoluteX();
        }
        return super.getAbsoluteX();
    }

    @Override
    public int getAbsoluteY() {
        if (this.isTemplateActive()) {
            ButtonElement template = this.getPropertySource();
            if ((template != null) && template.templateApplyPosY) return template.getAbsoluteY();
        }
        return super.getAbsoluteY();
    }

    @Override
    public boolean shouldRender() {
        if (this.isTemplateActive()) {
            ButtonElement template = this.getPropertySource();
            if ((template != null) && template.templateApplyVisibility) return template.shouldRender();
        }
        return super.shouldRender();
    }

    public ResourceSupplier<IAudio> getHoverSound() {
        return this.getTemplateProperty(template -> template.hoverSound, this.hoverSound);
    }

    public ResourceSupplier<ITexture> getHandleTextureNormal() {
        return this.getTemplateProperty(template -> template.backgroundTextureNormal, this.handleTextureNormal);
    }

    public ResourceSupplier<ITexture> getHandleTextureHover() {
        return this.getTemplateProperty(template -> template.backgroundTextureHover, this.handleTextureHover);
    }

    public ResourceSupplier<ITexture> getHandleTextureInactive() {
        return this.getTemplateProperty(template -> template.backgroundTextureInactive, this.handleTextureInactive);
    }

    public ResourceSupplier<ITexture> getSliderBackgroundTextureNormal() {
        return this.getTemplateProperty(template -> template.sliderBackgroundTextureNormal, this.sliderBackgroundTextureNormal);
    }

    public ResourceSupplier<ITexture> getSliderBackgroundTextureHighlighted() {
        return this.getTemplateProperty(template -> template.sliderBackgroundTextureHighlighted, this.sliderBackgroundTextureHighlighted);
    }

    public boolean isRestartBackgroundAnimationsOnHover() {
        return this.getTemplateProperty(template -> template.restartBackgroundAnimationsOnHover, this.restartBackgroundAnimationsOnHover);
    }

    public boolean isNineSliceCustomBackground() {
        return this.getTemplateProperty(template -> template.nineSliceCustomBackground, this.nineSliceCustomBackground);
    }

    public int getNineSliceBorderX() {
        return this.getTemplateProperty(template -> template.nineSliceBorderX, this.nineSliceBorderX);
    }

    public int getNineSliceBorderY() {
        return this.getTemplateProperty(template -> template.nineSliceBorderY, this.nineSliceBorderY);
    }

    public boolean isNineSliceSliderHandle() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandle, this.nineSliceSliderHandle);
    }

    public int getNineSliceSliderHandleBorderX() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandleBorderX, this.nineSliceSliderHandleBorderX);
    }

    public int getNineSliceSliderHandleBorderY() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandleBorderY, this.nineSliceSliderHandleBorderY);
    }

    public float getOpacity() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyOpacity) return template.opacity;
        return this.opacity;
    }

    @Nullable
    public String getLabel() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) return template.label;
        return this.label;
    }

    protected <T> T getTemplateProperty(@NotNull TemplatePropertyGetter<T> templatePropertyGetter, @Nullable T defaultProperty) {
        if (this.getPropertySource() != null) {
            return templatePropertyGetter.get(this.getPropertySource());
        }
        return defaultProperty;
    }

    @Nullable
    public ButtonElement getPropertySource() {
        ButtonElement template = ButtonElement.getTopActiveTemplateElement(true);
        if (template != null) {
            if (template.templateShareWith == ButtonElement.TemplateSharing.BUTTONS) return null;
            return template;
        }
        return null;
    }

    public boolean isTemplateActive() {
        return (this.getPropertySource() != null);
    }

    @FunctionalInterface
    protected interface TemplatePropertyGetter<T> {
        T get(@NotNull ButtonElement template);
    }

    public enum SliderType implements LocalizedCycleEnum<SliderType> {

        LIST("list"),
        INTEGER_RANGE("integer_range"),
        DECIMAL_RANGE("decimal_range");

        final String name;

        SliderType(String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @Override
        public @NotNull SliderType[] getValues() {
            return SliderType.values();
        }

        @Override
        @Nullable
        public SliderType getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static SliderType getByName(@NotNull String name) {
            for (SliderType i : SliderType.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.elements.slider.v2.type";
        }

    }

}
