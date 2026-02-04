package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.List;
import java.util.Objects;

public class InputFieldElement extends AbstractElement {

    public String linkedVariable;
    public InputFieldType type = InputFieldType.TEXT;
    public final Property.IntegerProperty maxTextLength = putProperty(Property.integerProperty("max_text_length", 10000, "fancymenu.elements.input_field.editor.set_max_length",
            Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.ColorProperty backgroundColor = putProperty(Property.hexColorProperty("background_color", DrawableColor.of(new Color(0, 0, 0)).getHex(), true, "fancymenu.elements.input_field.background_color"));
    public final Property.ColorProperty borderColorNormal = putProperty(Property.hexColorProperty("border_color", DrawableColor.of(new Color(-6250336)).getHex(), true, "fancymenu.elements.input_field.border_color"));
    public final Property.ColorProperty borderColorFocused = putProperty(Property.hexColorProperty("border_color_focused", DrawableColor.of(new Color(255, 255, 255)).getHex(), true, "fancymenu.elements.input_field.border_color_focused"));
    public final Property.FloatProperty borderRoundingRadius = putProperty(Property.floatProperty("border_rounding_radius", 0.0F, "fancymenu.elements.input_field.border_rounding_radius",
            Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 100.0F).build()));
    public final Property.ColorProperty textColor = putProperty(Property.hexColorProperty("text_color", DrawableColor.of(new Color(14737632)).getHex(), true, "fancymenu.elements.input_field.text_color"));
    public final Property.StringProperty hintText = putProperty(Property.stringProperty("hint_text", "", false, true, "fancymenu.elements.input_field.hint_text"));
    public final Property.ColorProperty hintTextColor = putProperty(Property.hexColorProperty("hint_text_color", null, true, "fancymenu.elements.input_field.hint_text_color"));
    public ExtendedEditBox editBox;
    public String lastValue = "";
    public boolean navigatable = true;
    public final Property<ResourceSupplier<IAudio>> hoverSound = putProperty(Property.resourceSupplierProperty(IAudio.class, "hoversound", null, "fancymenu.elements.button.hoversound", true, true, true, null));
    public final Property<ResourceSupplier<IAudio>> unhoverAudio = putProperty(Property.resourceSupplierProperty(IAudio.class, "unhover_audio", null, "fancymenu.elements.widgets.unhover_audio", true, true, true, null));
    public final Property<ResourceSupplier<IAudio>> clickSound = putProperty(Property.resourceSupplierProperty(IAudio.class, "clicksound", null, "fancymenu.elements.button.clicksound", true, true, true, null));

    public InputFieldElement(ElementBuilder<InputFieldElement, InputFieldEditorElement> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
        this.maxTextLength.addValueSetListener((oldValue, newValue) -> this.updateWidgetMaxLength());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.editBox == null) return;

        if (this.shouldRender()) {

            RenderSystem.enableBlend();

            //Handle editor mode for text field
            if (isEditor()) {
                this.editBox.active = false;
                this.editBox.setEditable(false);
                if (this.linkedVariable != null) {
                    if (VariableHandler.variableExists(this.linkedVariable)) {
                        String var = Objects.requireNonNull(VariableHandler.getVariable(this.linkedVariable)).getValue();
                        this.editBox.setValue(var);
                    }
                }
            }

            this.editBox.setNavigatable(this.navigatable);

            this.updateWidgetBounds();
            this.updateWidgetSounds();
            this.updateWidgetMaxLength();
            this.updateWidgetStyle();

            this.editBox.render(graphics, mouseX, mouseY, partial);

            this.updateValue();

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    public void updateValue() {
        //Update variable value on change
        if (!isEditor()) {
            if (this.linkedVariable != null) {
                if (!this.lastValue.equals(this.editBox.getValue())) {
                    VariableHandler.setVariable(linkedVariable, this.editBox.getValue());
                }
                if (VariableHandler.variableExists(this.linkedVariable)) {
                    String val = Objects.requireNonNull(VariableHandler.getVariable(this.linkedVariable)).getValue();
                    if (!this.editBox.getValue().equals(val)) {
                        this.editBox.setValue(val);
                    }
                } else {
                    this.editBox.setValue("");
                }
            }
            this.lastValue = this.editBox.getValue();
        }
    }

    public void updateWidgetBounds() {
        if (this.editBox == null) return;
        this.editBox.setX(this.getAbsoluteX());
        this.editBox.setY(this.getAbsoluteY());
        this.editBox.setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget)this.editBox).setHeightFancyMenu(this.getAbsoluteHeight());
    }

    public void updateWidgetMaxLength() {
        if (this.editBox == null) return;
        int maxLength = Math.max(1, this.maxTextLength.getInteger());
        this.editBox.setMaxLength(maxLength);
    }

    public void updateWidgetStyle() {
        if (this.editBox == null) return;
        this.editBox.setBackgroundColor(this.backgroundColor.getDrawable());
        this.editBox.setBorderNormalColor(this.borderColorNormal.getDrawable());
        this.editBox.setBorderFocusedColor(this.borderColorFocused.getDrawable());
        this.editBox.setTextColor(this.textColor.getDrawable());
        String hintColorRaw = this.hintTextColor.get();
        if ((hintColorRaw != null) && !hintColorRaw.isEmpty()) {
            this.editBox.setHintTextColor(this.hintTextColor.getDrawable());
        } else {
            this.editBox.setHintTextColor(null);
        }
        String hint = this.hintText.getString();
        if ((hint != null) && !hint.isEmpty()) {
            this.editBox.setHintFancyMenu(consumes -> Component.literal(hint));
        } else {
            this.editBox.setHintFancyMenu(null);
        }
        float radius = Math.max(0.0F, this.borderRoundingRadius.getFloat());
        this.editBox.setRoundedColorBackgroundEnabled(radius > 0.0F);
        this.editBox.setRoundedColorBackgroundRadius(radius);
    }

    public void updateWidgetSounds() {
        if (this.editBox instanceof CustomizableWidget w) {
            ResourceSupplier<IAudio> hover = this.hoverSound.get();
            ResourceSupplier<IAudio> unhover = this.unhoverAudio.get();
            ResourceSupplier<IAudio> click = this.clickSound.get();
            w.setHoverSoundFancyMenu((hover != null) ? hover.get() : null);
            w.setUnhoverSoundFancyMenu((unhover != null) ? unhover.get() : null);
            w.setCustomClickSoundFancyMenu((click != null) ? click.get() : null);
        }
    }

    @Override
    public void afterConstruction() {
        this.updateWidgetBounds();
        this.updateWidgetMaxLength();
        this.updateWidgetStyle();
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return ListUtils.of(this.editBox);
    }

    public enum InputFieldType {

        INTEGER_ONLY("integer", CharacterFilter.buildIntegerFilter()),
        DECIMAL_ONLY("decimal", CharacterFilter.buildDecimalFiler()),
        URL("url", CharacterFilter.buildUrlFilter()),
        TEXT("text", null);

        final String name;
        final CharacterFilter filter;

        InputFieldType(String name, CharacterFilter filter) {
            this.name = name;
            this.filter = filter;
        }

        public String getName() {
            return this.name;
        }

        public CharacterFilter getFilter() {
            return this.filter;
        }

        public static InputFieldType getByName(String name) {
            for (InputFieldType i : InputFieldType.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

}
