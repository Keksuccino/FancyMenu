package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.ListSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
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
    public final Property.DoubleProperty minRangeValue = putProperty(Property.doubleProperty("min_range_value", 0.0D, "fancymenu.elements.slider.v2.type.range.set_min"));
    public final Property.DoubleProperty maxRangeValue = putProperty(Property.doubleProperty("max_range_value", 20.0D, "fancymenu.elements.slider.v2.type.range.set_max"));
    public final Property.IntegerProperty roundingDecimalPlace = putProperty(Property.integerProperty("rounding_decimal_place", 2, "fancymenu.elements.slider.v2.type.range.decimal.round"));
    @Nullable
    public String label;
    public final Property.ColorProperty labelBaseColor = putProperty(Property.hexColorProperty("label_base_color", null, true, "fancymenu.elements.widgets.label.base_color"));
    public final Property.ColorProperty labelHoverColor = putProperty(Property.hexColorProperty("label_hover_color", null, true, "fancymenu.elements.widgets.label.hover_color"));
    public final Property.FloatProperty labelScale = putProperty(Property.floatProperty("label_scale", 1.0F, "fancymenu.elements.widgets.label.scale"));
    public final Property.BooleanProperty labelShadow = putProperty(Property.booleanProperty("label_shadow", true, "fancymenu.elements.widgets.label.shadow"));
    public String tooltip;
    public ResourceSupplier<ITexture> handleTextureNormal;
    public ResourceSupplier<ITexture> handleTextureHover;
    public ResourceSupplier<ITexture> handleTextureInactive;
    public ResourceSupplier<ITexture> sliderBackgroundTextureNormal;
    public ResourceSupplier<ITexture> sliderBackgroundTextureHighlighted;
    public boolean underlineLabelOnHover = false;
    public boolean transparentBackground = false;
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public final Property.IntegerProperty nineSliceBorderX = putProperty(Property.integerProperty("nine_slice_border_x", 5, "fancymenu.elements.buttons.textures.nine_slice.border_x"));
    public final Property.IntegerProperty nineSliceBorderY = putProperty(Property.integerProperty("nine_slice_border_y", 5, "fancymenu.elements.buttons.textures.nine_slice.border_y"));
    public boolean nineSliceSliderHandle = false;
    public final Property.IntegerProperty nineSliceSliderHandleBorderX = putProperty(Property.integerProperty("nine_slice_slider_handle_border_x", 5, "fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"));
    public final Property.IntegerProperty nineSliceSliderHandleBorderY = putProperty(Property.integerProperty("nine_slice_slider_handle_border_y", 5, "fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"));
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock executableBlock = new GenericExecutableBlock();
    @NotNull
    public RequirementContainer activeStateSupplier = new RequirementContainer();
    public ResourceSupplier<IAudio> hoverSound;

    public final Property<ResourceSupplier<IAudio>> unhoverAudio = putProperty(Property.resourceSupplierProperty(IAudio.class, "unhover_audio", null, "fancymenu.elements.widgets.unhover_audio", true, true, true, null));

    public SliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.buildSlider();
        this.prepareExecutableBlock();
        this.allowDepthTestManipulation = true;
        this.minRangeValue.addValueSetListener((oldValue, newValue) -> this.buildSlider());
        this.maxRangeValue.addValueSetListener((oldValue, newValue) -> this.buildSlider());
        this.roundingDecimalPlace.addValueSetListener((oldValue, newValue) -> this.buildSlider());
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
        this.requirementContainer.addValuePlaceholder("value", () -> (this.slider != null) ? this.slider.getValueDisplayText() : "");
    }

    /**
     * This should only get called on init or in the editor, because the new slider will
     * not get registered as {@link Screen} widget by calling this method.
     */
    public void buildSlider() {

        String preSelectedString = (this.preSelectedValue != null) ? PlaceholderParser.replacePlaceholders(this.preSelectedValue) : null;

        //Build slider instance based on element's slider type
        if (this.type == SliderType.INTEGER_RANGE) {
            int min = (int) this.minRangeValue.getDouble();
            int max = (int) this.maxRangeValue.getDouble();
            int preSelected = min;
            if ((preSelectedString != null) && MathUtils.isDouble(preSelectedString)) {
                preSelected = (int) Double.parseDouble(preSelectedString);
            }
            this.slider = new RangeSlider(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), min, max, preSelected);
            ((RangeSlider)this.slider).setShowAsInteger(true);
        }
        if (this.type == SliderType.DECIMAL_RANGE) {
            double minRange = this.minRangeValue.getDouble();
            double maxRange = this.maxRangeValue.getDouble();
            double preSelected = minRange;
            if ((preSelectedString != null) && MathUtils.isDouble(preSelectedString)) {
                preSelected = Double.parseDouble(preSelectedString);
            }
            this.slider = new RangeSlider(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), minRange, maxRange, preSelected);
            ((RangeSlider)this.slider).setRoundingDecimalPlace(this.roundingDecimalPlace.getInteger());
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

        this.updateWidgetBounds();

        this.updateWidget();

        this.slider.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void afterConstruction() {
        this.updateWidgetBounds();
    }

    public void updateWidgetBounds() {
        if (this.slider == null) return;
        this.slider.setX(this.getAbsoluteX());
        this.slider.setY(this.getAbsoluteY());
        this.slider.setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget)this.slider).setHeightFancyMenu(this.getAbsoluteHeight());
    }

    public void updateWidget() {
        if (this.slider == null) return;
        this.updateWidgetHoverSound();
        this.updateWidgetUnhoverSound();
        this.updateWidgetActiveState();
        this.updateWidgetTooltip();
        this.updateWidgetTexture();
        this.updateWidgetLabelUnderline();
        this.updateWidgetLabelShadow();
        this.updateWidgetLabelBaseColor();
        this.updateWidgetLabelHoverColor();
        this.updateWidgetLabelScale();
        this.updateWidgetHitboxRotation();
        this.slider.updateMessage();
    }

    public void updateWidgetHoverSound() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.getHoverSound() != null) ? this.getHoverSound().get() : null);
        }
    }

    public void updateWidgetUnhoverSound() {
        if (this.slider instanceof CustomizableWidget w) {
            ResourceSupplier<IAudio> supplier = this.getUnhoverSound();
            w.setUnhoverSoundFancyMenu((supplier != null) ? supplier.get() : null);
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
        boolean transparentBackground = this.isTransparentBackground();
        RenderableResource transparentResource = null;

        //Normal Slider Background
        if (this.getSliderBackgroundTextureNormal() != null) {
            sliderBackNormal = this.getSliderBackgroundTextureNormal().get();
        }
        //Highlighted Slider Background
        if (this.getSliderBackgroundTextureHighlighted() != null) {
            sliderBackHighlighted = this.getSliderBackgroundTextureHighlighted().get();
        }
        if (transparentBackground) {
            transparentResource = PngTexture.FULLY_TRANSPARENT_PNG_TEXTURE_SUPPLIER.get();
            sliderBackNormal = transparentResource;
            sliderBackHighlighted = transparentResource;
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

    public ResourceSupplier<IAudio> getUnhoverSound() {
        return this.getTemplateProperty(template -> template.unhoverAudio.get(), this.unhoverAudio.get());
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

    public void updateWidgetLabelUnderline() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setUnderlineLabelOnHoverFancyMenu(this.isUnderlineLabelOnHover());
        }
    }

    public void updateWidgetLabelShadow() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setLabelShadowFancyMenu(this.isLabelShadowEnabled());
        }
        if (this.slider != null) {
            this.slider.setLabelShadow(this.isLabelShadowEnabled());
        }
    }

    public void updateWidgetLabelHoverColor() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setLabelHoverColorFancyMenu(this.getLabelHoverColor());
        }
    }

    public void updateWidgetLabelBaseColor() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setLabelBaseColorFancyMenu(this.getLabelBaseColor());
        }
    }

    public void updateWidgetLabelScale() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setLabelScaleFancyMenu(this.getLabelScale());
        }
    }

    public void updateWidgetHitboxRotation() {
        if (this.slider instanceof CustomizableWidget w) {
            w.setHitboxRotationFancyMenu(this.getRotationDegrees(), this.getVerticalTiltDegrees(), this.getHorizontalTiltDegrees());
        }
    }

    public boolean isTransparentBackground() {
        return this.getTemplateProperty(template -> template.transparentBackground, this.transparentBackground);
    }

    public boolean isRestartBackgroundAnimationsOnHover() {
        return this.getTemplateProperty(template -> template.restartBackgroundAnimationsOnHover, this.restartBackgroundAnimationsOnHover);
    }

    public boolean isNineSliceCustomBackground() {
        return this.getTemplateProperty(template -> template.nineSliceCustomBackground, this.nineSliceCustomBackground);
    }

    public int getNineSliceBorderX() {
        return this.getTemplateProperty(template -> template.nineSliceBorderX.getInteger(), this.nineSliceBorderX.getInteger());
    }

    public int getNineSliceBorderY() {
        return this.getTemplateProperty(template -> template.nineSliceBorderY.getInteger(), this.nineSliceBorderY.getInteger());
    }

    public boolean isNineSliceSliderHandle() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandle, this.nineSliceSliderHandle);
    }

    public int getNineSliceSliderHandleBorderX() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandleBorderX.getInteger(), this.nineSliceSliderHandleBorderX.getInteger());
    }

    public int getNineSliceSliderHandleBorderY() {
        return this.getTemplateProperty(template -> template.nineSliceSliderHandleBorderY.getInteger(), this.nineSliceSliderHandleBorderY.getInteger());
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

    public boolean isUnderlineLabelOnHover() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) return template.underlineLabelOnHover;
        return this.underlineLabelOnHover;
    }

    public boolean isLabelShadowEnabled() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) return template.labelShadow.getBoolean();
        return this.labelShadow.getBoolean();
    }

    @Nullable
    public DrawableColor getLabelHoverColor() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) {
            return (template.labelHoverColor.get() != null) ? template.labelHoverColor.getDrawable() : null;
        }
        return (this.labelHoverColor.get() != null) ? this.labelHoverColor.getDrawable() : null;
    }

    @Nullable
    public DrawableColor getLabelBaseColor() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) {
            return (template.labelBaseColor.get() != null) ? template.labelBaseColor.getDrawable() : null;
        }
        return (this.labelBaseColor.get() != null) ? this.labelBaseColor.getDrawable() : null;
    }

    public float getLabelScale() {
        ButtonElement template = this.getPropertySource();
        if ((template != null) && template.templateApplyLabel) {
            return template.labelScale.getFloat();
        }
        return this.labelScale.getFloat();
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
