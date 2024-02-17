package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.ListSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
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
    public String handleAnimationNormal;
    public String handleAnimationHover;
    public String handleAnimationInactive;
    public ResourceSupplier<ITexture> sliderBackgroundTextureNormal;
    public ResourceSupplier<ITexture> sliderBackgroundTextureHighlighted;
    public String sliderBackgroundAnimationNormal;
    public String sliderBackgroundAnimationHighlighted;
    public boolean loopBackgroundAnimations = true;
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

    public SliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.buildSlider();
        this.prepareExecutableBlock();
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
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {

        if (this.slider == null) return;

        this.slider.setNavigatable(this.navigatable);

        this.slider.visible = this.shouldRender();
        this.slider.setAlpha(this.opacity);

        if (!this.shouldRender()) return;

        this.slider.x = this.getAbsoluteX();
        this.slider.y = this.getAbsoluteY();
        this.slider.setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget)this.slider).setHeightFancyMenu(this.getAbsoluteHeight());

        this.updateWidget();

        this.slider.render(graphics, mouseX, mouseY, partial);

    }

    public void updateWidget() {
        if (this.slider == null) return;
        this.updateWidgetTooltip();
        this.updateWidgetTexture();
        this.slider.updateMessage();
    }

    public void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.slider != null) && ((IMixinAbstractWidget)this.slider).getIsHoveredFancyMenu() && this.slider.visible && this.shouldRender() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.slider, Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }
    }

    public void updateWidgetTexture() {

        RenderableResource sliderBackNormal = null;
        RenderableResource sliderBackHighlighted = null;

        //Normal Slider Background
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
        //Highlighted Slider Background
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

        if (this.slider instanceof CustomizableSlider w) {
            w.setCustomSliderBackgroundNormalFancyMenu(sliderBackNormal);
            w.setCustomSliderBackgroundHighlightedFancyMenu(sliderBackHighlighted);
        }

        RenderableResource handleTextureNormal = null;
        RenderableResource handleTextureHover = null;
        RenderableResource handleTextureInactive = null;

        //Normal
        if ((this.handleAnimationNormal != null) && AnimationHandler.animationExists(this.handleAnimationNormal)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.handleAnimationNormal);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                handleTextureNormal = a;
            }
        }
        if ((handleTextureNormal == null) && (this.handleTextureNormal != null)) {
            handleTextureNormal = this.handleTextureNormal.get();
        }
        //Hover
        if ((this.handleAnimationHover != null) && AnimationHandler.animationExists(this.handleAnimationHover)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.handleAnimationHover);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                handleTextureHover = a;
            }
        }
        if ((handleTextureHover == null) && (this.handleTextureHover != null)) {
            handleTextureHover = this.handleTextureHover.get();
        }
        //Inactive
        if ((this.handleAnimationInactive != null) && AnimationHandler.animationExists(this.handleAnimationInactive)) {
            IAnimationRenderer r = AnimationHandler.getAnimation(this.handleAnimationInactive);
            if (r instanceof AdvancedAnimation a) {
                a.setLooped(this.loopBackgroundAnimations);
                handleTextureInactive = a;
            }
        }
        if ((handleTextureInactive == null) && (this.handleTextureInactive != null)) {
            handleTextureInactive = this.handleTextureInactive.get();
        }

        if (this.slider instanceof CustomizableWidget w) {
            if (this.slider instanceof CustomizableSlider s) {
                s.setNineSliceCustomSliderBackground_FancyMenu(this.nineSliceCustomBackground);
                s.setNineSliceSliderBackgroundBorderX_FancyMenu(this.nineSliceBorderX);
                s.setNineSliceSliderBackgroundBorderY_FancyMenu(this.nineSliceBorderY);
                s.setNineSliceCustomSliderHandle_FancyMenu(this.nineSliceSliderHandle);
                s.setNineSliceSliderHandleBorderX_FancyMenu(this.nineSliceSliderHandleBorderX);
                s.setNineSliceSliderHandleBorderY_FancyMenu(this.nineSliceSliderHandleBorderY);
            }
            w.setCustomBackgroundNormalFancyMenu(handleTextureNormal);
            w.setCustomBackgroundHoverFancyMenu(handleTextureHover);
            w.setCustomBackgroundInactiveFancyMenu(handleTextureInactive);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.restartBackgroundAnimationsOnHover ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
        }

    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.executableBlock;
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
