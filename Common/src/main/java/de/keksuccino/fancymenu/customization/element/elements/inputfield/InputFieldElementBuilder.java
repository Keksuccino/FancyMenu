package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InputFieldElementBuilder extends ElementBuilder<InputFieldElement, InputFieldEditorElement> {

    public InputFieldElementBuilder() {
        super("fancymenu_customization_item_input_field");
    }

    @Override
    public @NotNull InputFieldElement buildDefaultInstance() {
        InputFieldElement i = new InputFieldElement(this);
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public @NotNull InputFieldElement deserializeElement(@NotNull SerializedElement serializedElement) {

        InputFieldElement element = buildDefaultInstance();

        element.linkedVariable = serializedElement.getEntryValue("linked_variable");

        String inputFieldTypeString = serializedElement.getEntryValue("input_field_type");
        if (inputFieldTypeString != null) {
            InputFieldElement.InputFieldType t = InputFieldElement.InputFieldType.getByName(inputFieldTypeString);
            if (t != null) {
                element.type = t;
            }
        }

        String maxLengthString = serializedElement.getEntryValue("max_text_length");
        if (maxLengthString != null) {
            if (MathUtils.isInteger(maxLengthString)) {
                element.maxTextLength = Integer.parseInt(maxLengthString);
            }
        }
        if (element.maxTextLength <= 0) {
            element.maxTextLength = 1;
        }

        element.textField = new AdvancedTextField(Minecraft.getInstance().font, element.getX(), element.getY(), element.getWidth(), element.getHeight(), true, element.type.filter);
        element.textField.setMaxLength(element.maxTextLength);
        if (element.linkedVariable != null) {
            String var = VariableHandler.getVariable(element.linkedVariable);
            if (var != null) {
                element.textField.setValue(var);
            }
        }

        return element;

    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull InputFieldElement element) {

        SerializedElement serialized = new SerializedElement();

        if (element.linkedVariable != null) {
            serialized.addEntry("linked_variable", element.linkedVariable);
        }
        serialized.addEntry("input_field_type", element.type.getName());
        serialized.addEntry("max_text_length", "" + element.maxTextLength);

        return serialized;

    }

    @Override
    public @NotNull InputFieldEditorElement wrapIntoEditorElement(@NotNull InputFieldElement element, @NotNull LayoutEditorScreen editor) {
        return new InputFieldEditorElement(this, element, editor);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal(Locals.localize("fancymenu.customization.items.input_field"));
    }

    @Override
    public Component[] getDescription() {
        List<Component> l = new ArrayList<>();
        for (String s : StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.desc"), "%n%")) {
            l.add(Component.literal(s));
        }
        return l.toArray(new Component[]{});
    }

}
