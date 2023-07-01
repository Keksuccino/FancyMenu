package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InputFieldElementBuilder extends ElementBuilder<InputFieldElement, InputFieldEditorElement> {

    public InputFieldElementBuilder() {
        super("fancymenu_customization_item_input_field");
    }

    @Override
    public @NotNull InputFieldElement buildDefaultInstance() {
        InputFieldElement i = new InputFieldElement(this);
        i.baseWidth = 100;
        i.baseHeight = 20;
        i.textField = new AdvancedTextField(Minecraft.getInstance().font, i.getAbsoluteX(), i.getAbsoluteY(), i.getAbsoluteWidth(), i.getAbsoluteHeight(), true, i.type.filter);
        return i;
    }

    @Override
    public @NotNull InputFieldElement deserializeElement(@NotNull SerializedElement serialized) {

        InputFieldElement element = buildDefaultInstance();

        element.linkedVariable = serialized.getValue("linked_variable");

        String inputFieldTypeString = serialized.getValue("input_field_type");
        if (inputFieldTypeString != null) {
            InputFieldElement.InputFieldType t = InputFieldElement.InputFieldType.getByName(inputFieldTypeString);
            if (t != null) {
                element.type = t;
            }
        }

        String maxLengthString = serialized.getValue("max_text_length");
        if (maxLengthString != null) {
            if (MathUtils.isInteger(maxLengthString)) {
                element.maxTextLength = Integer.parseInt(maxLengthString);
            }
        }
        if (element.maxTextLength <= 0) {
            element.maxTextLength = 1;
        }

        element.textField = new AdvancedTextField(Minecraft.getInstance().font, element.getAbsoluteX(), element.getAbsoluteY(), element.getAbsoluteWidth(), element.getAbsoluteHeight(), true, element.type.filter);
        element.textField.setMaxLength(element.maxTextLength);
        if (element.linkedVariable != null) {
            if (VariableHandler.variableExists(element.linkedVariable)) {
                String var = Objects.requireNonNull(VariableHandler.getVariable(element.linkedVariable)).getValue();
                element.textField.setValue(var);
            }
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull InputFieldElement element, @NotNull SerializedElement serializeTo) {

        SerializedElement serialized = new SerializedElement();

        if (element.linkedVariable != null) {
            serialized.putProperty("linked_variable", element.linkedVariable);
        }
        serialized.putProperty("input_field_type", element.type.getName());
        serialized.putProperty("max_text_length", "" + element.maxTextLength);

        return serialized;

    }

    @Override
    public @NotNull InputFieldEditorElement wrapIntoEditorElement(@NotNull InputFieldElement element, @NotNull LayoutEditorScreen editor) {
        return new InputFieldEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.customization.items.input_field");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.input_field.desc");
    }

}
