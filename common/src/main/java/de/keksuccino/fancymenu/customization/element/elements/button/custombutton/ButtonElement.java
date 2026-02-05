package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
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
    public final Property.ColorProperty labelBaseColor = putProperty(Property.hexColorProperty("label_base_color", null, true, "fancymenu.elements.widgets.label.base_color"));
    public final Property.ColorProperty labelHoverColor = putProperty(Property.hexColorProperty("label_hover_color", null, true, "fancymenu.elements.widgets.label.hover_color"));
    public final Property.FloatProperty labelScale = putProperty(Property.floatProperty("label_scale", 1.0F, "fancymenu.elements.widgets.label.scale"));
    public final Property.BooleanProperty labelShadow = putProperty(Property.booleanProperty("label_shadow", true, "fancymenu.elements.widgets.label.shadow"));
    public String tooltip;
    public ResourceSupplier<ITexture> backgroundTextureNormal;
    public ResourceSupplier<ITexture> backgroundTextureHover;
    public ResourceSupplier<ITexture> backgroundTextureInactive;
    public boolean underlineLabelOnHover = false;
    public boolean transparentBackground = false;
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public final Property.IntegerProperty nineSliceBorderX = putProperty(Property.integerProperty("nine_slice_border_x", 5, "fancymenu.elements.buttons.textures.nine_slice.border_x"));
    public final Property.IntegerProperty nineSliceBorderY = putProperty(Property.integerProperty("nine_slice_border_y", 5, "fancymenu.elements.buttons.textures.nine_slice.border_y"));
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    @NotNull
    public RequirementContainer activeStateSupplier = new RequirementContainer();
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
    public final Property.IntegerProperty nineSliceSliderHandleBorderX = putProperty(Property.integerProperty("nine_slice_slider_handle_border_x", 5, "fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"));
    public final Property.IntegerProperty nineSliceSliderHandleBorderY = putProperty(Property.integerProperty("nine_slice_slider_handle_border_y", 5, "fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"));
    public final Property<ResourceSupplier<IAudio>> unhoverAudio = putProperty(Property.resourceSupplierProperty(IAudio.class, "unhover_audio", null, "fancymenu.elements.widgets.unhover_audio", true, true, true, null));

    protected static long lastTemplateUpdateButton = -1L;
    protected static ButtonElement lastTemplateButton = null;
    protected static long lastTemplateUpdateSlider = -1L;
    protected static ButtonElement lastTemplateSlider = null;

    public ButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void afterConstruction() {

        resetTemplateCache();

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
        this.updateWidgetLabelUnderline();
        this.updateWidgetLabelShadow();
        this.updateWidgetLabelBaseColor();
        this.updateWidgetLabelHoverColor();
        this.updateWidgetLabelScale();
        this.updateWidgetHoverSound();
        this.updateWidgetUnhoverSound();
        this.updateWidgetClickSound();
        this.updateWidgetTexture();
        this.updateWidgetSize();
        this.updateWidgetPosition();
        this.updateWidgetHitboxRotation();
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

    public void updateWidgetHitboxRotation() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHitboxRotationFancyMenu(this.getRotationDegrees(), this.getVerticalTiltDegrees(), this.getHorizontalTiltDegrees());
        }
    }

    public void updateWidgetSize() {
        if (this.getWidget() == null) return;
        this.getWidget().setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget) this.getWidget()).setHeightFancyMenu(this.getAbsoluteHeight());
    }

    public void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.getWidget() != null) && this.shouldRender() && !isEditor()) {
            String t = PlaceholderParser.replacePlaceholders(this.tooltip).replace("%n%", "\n").replace("\\n", "\n");
            this.getWidget().setTooltip(Tooltip.create(Component.literal(t)));
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

    public void updateWidgetLabelUnderline() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            boolean underline = this.isUnderlineLabelOnHover();
            if (underline || w.isUnderlineLabelOnHoverFancyMenu()) {
                w.setUnderlineLabelOnHoverFancyMenu(underline);
            }
        }
    }

    public void updateWidgetLabelShadow() {
        AbstractWidget widget = this.getWidget();
        if (widget == null) return;
        ButtonElement template = this.getPropertySource();
        boolean templateAppliesLabel = (template != null) && template.templateApplyLabel && (template != this);
        boolean shadow = this.isLabelShadowEnabled();
        boolean shouldApply = templateAppliesLabel ? !template.labelShadow.isDefault() : !this.labelShadow.isDefault();
        if (!shouldApply && (widget instanceof CustomizableWidget w)) {
            shouldApply = w.isLabelShadowFancyMenu() != shadow;
        }
        if (shouldApply) {
            if (widget instanceof ExtendedButton b) {
                b.setLabelShadowEnabled(shadow);
            }
            if (widget instanceof AbstractExtendedSlider s) {
                s.setLabelShadow(shadow);
            }
            if (widget instanceof CustomizableWidget w) {
                w.setLabelShadowFancyMenu(shadow);
            }
        }
    }

    public void updateWidgetLabelHoverColor() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            DrawableColor color = this.getLabelHoverColor();
            if (color != null) {
                w.setLabelHoverColorFancyMenu(color);
            } else if (w.getLabelHoverColorFancyMenu() != null) {
                w.setLabelHoverColorFancyMenu(null);
            }
        }
    }

    public void updateWidgetLabelBaseColor() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            DrawableColor color = this.getLabelBaseColor();
            if (color != null) {
                w.setLabelBaseColorFancyMenu(color);
            } else if (w.getLabelBaseColorFancyMenu() != null) {
                w.setLabelBaseColorFancyMenu(null);
            }
        }
    }

    public void updateWidgetLabelScale() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            ButtonElement template = this.getPropertySource();
            boolean templateAppliesLabel = (template != null) && template.templateApplyLabel && (template != this);
            float scale = this.getLabelScale();
            boolean shouldApply = templateAppliesLabel ? !template.labelScale.isDefault() : !this.labelScale.isDefault();
            if (!shouldApply && Float.compare(w.getLabelScaleFancyMenu(), scale) != 0) {
                shouldApply = true;
            }
            if (shouldApply) {
                w.setLabelScaleFancyMenu(scale);
            }
        }
    }

    public void updateWidgetHoverSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.getPropertySource().hoverSound != null) ? this.getPropertySource().hoverSound.get() : null);
        }
    }

    public void updateWidgetUnhoverSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            ResourceSupplier<IAudio> supplier = this.getPropertySource().unhoverAudio.get();
            w.setUnhoverSoundFancyMenu((supplier != null) ? supplier.get() : null);
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
        boolean transparentBackground = this.getPropertySource().transparentBackground;
        RenderableResource transparentResource = null;

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
        if (transparentBackground && !(this.getWidget() instanceof CustomizableSlider)) {
            transparentResource = PngTexture.FULLY_TRANSPARENT_PNG_TEXTURE_SUPPLIER.get();
            backNormal = transparentResource;
            backHover = transparentResource;
            backInactive = transparentResource;
        }

        if (this.getWidget() instanceof CustomizableWidget w) {
            if (this.getWidget() instanceof CustomizableSlider s) {
                s.setNineSliceCustomSliderBackground_FancyMenu(!transparentBackground && this.getPropertySource().nineSliceCustomBackground);
                s.setNineSliceSliderBackgroundBorderX_FancyMenu(this.getPropertySource().nineSliceBorderX.getInteger());
                s.setNineSliceSliderBackgroundBorderY_FancyMenu(this.getPropertySource().nineSliceBorderY.getInteger());
            } else {
                w.setNineSliceCustomBackground_FancyMenu(!transparentBackground && this.getPropertySource().nineSliceCustomBackground);
                w.setNineSliceBorderX_FancyMenu(this.getPropertySource().nineSliceBorderX.getInteger());
                w.setNineSliceBorderY_FancyMenu(this.getPropertySource().nineSliceBorderY.getInteger());
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
        if (transparentBackground && (this.getWidget() instanceof CustomizableSlider)) {
            if (transparentResource == null) {
                transparentResource = PngTexture.FULLY_TRANSPARENT_PNG_TEXTURE_SUPPLIER.get();
            }
            sliderBackNormal = transparentResource;
            sliderBackHighlighted = transparentResource;
        }

        if (this.getWidget() instanceof CustomizableSlider w) {
            w.setNineSliceCustomSliderHandle_FancyMenu(this.getPropertySource().nineSliceSliderHandle);
            w.setNineSliceSliderHandleBorderX_FancyMenu(this.getPropertySource().nineSliceSliderHandleBorderX.getInteger());
            w.setNineSliceSliderHandleBorderY_FancyMenu(this.getPropertySource().nineSliceSliderHandleBorderY.getInteger());
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

    public boolean isUnderlineLabelOnHover() {
        if (this.isTemplate) return this.underlineLabelOnHover;
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) return template.underlineLabelOnHover;
        return this.underlineLabelOnHover;
    }

    public boolean isLabelShadowEnabled() {
        if (this.isTemplate) return this.labelShadow.getBoolean();
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) return template.labelShadow.getBoolean();
        return this.labelShadow.getBoolean();
    }

    @Nullable
    public DrawableColor getLabelHoverColor() {
        if (this.isTemplate) {
            return (this.labelHoverColor.get() != null) ? this.labelHoverColor.getDrawable() : null;
        }
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) {
            return (template.labelHoverColor.get() != null) ? template.labelHoverColor.getDrawable() : null;
        }
        return (this.labelHoverColor.get() != null) ? this.labelHoverColor.getDrawable() : null;
    }

    @Nullable
    public DrawableColor getLabelBaseColor() {
        if (this.isTemplate) {
            return (this.labelBaseColor.get() != null) ? this.labelBaseColor.getDrawable() : null;
        }
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) {
            return (template.labelBaseColor.get() != null) ? template.labelBaseColor.getDrawable() : null;
        }
        return (this.labelBaseColor.get() != null) ? this.labelBaseColor.getDrawable() : null;
    }

    public float getLabelScale() {
        if (this.isTemplate) return this.labelScale.getFloat();
        ButtonElement template = getTopActiveTemplateElement(this.isSlider());
        if ((template != null) && template.templateApplyLabel) {
            return template.labelScale.getFloat();
        }
        return this.labelScale.getFloat();
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
