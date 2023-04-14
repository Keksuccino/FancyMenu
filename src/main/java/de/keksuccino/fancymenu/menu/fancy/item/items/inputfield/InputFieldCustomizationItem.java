package de.keksuccino.fancymenu.menu.fancy.item.items.inputfield;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;

public class InputFieldCustomizationItem extends CustomizationItem {

    public String linkedVariable;
    public InputFieldType type = InputFieldType.TEXT;
    public int maxTextLength = 10000;

    public AdvancedTextField textField;
    public String lastValue = "";

    public InputFieldCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        this.linkedVariable = item.getEntryValue("linked_variable");

        String inputFieldTypeString = item.getEntryValue("input_field_type");
        if (inputFieldTypeString != null) {
            InputFieldType t = InputFieldType.getByName(inputFieldTypeString);
            if (t != null) {
                this.type = t;
            }
        }

        String maxLengthString = item.getEntryValue("max_text_length");
        if (maxLengthString != null) {
            if (MathUtils.isInteger(maxLengthString)) {
                this.maxTextLength = Integer.parseInt(maxLengthString);
            }
        }
        if (this.maxTextLength <= 0) {
            this.maxTextLength = 1;
        }

        Screen current = Minecraft.getInstance().screen;
        this.textField = new AdvancedTextField(Minecraft.getInstance().font, this.getPosX(current), this.getPosY(current), this.getWidth(), this.getHeight(), true, this.type.filter);
        this.textField.setMaxLength(this.maxTextLength);
        if (this.linkedVariable != null) {
            String var = VariableHandler.getVariable(this.linkedVariable);
            if (var != null) {
                this.textField.setValue(var);
            }
        }

    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        if (this.shouldRender()) {

            RenderSystem.enableBlend();

            //Handle editor mode for text field
            if (isEditorActive()) {
                this.textField.active = false;
                this.textField.setEditable(false);
                if (this.linkedVariable != null) {
                    String var = VariableHandler.getVariable(this.linkedVariable);
                    if (var != null) {
                        this.textField.setValue(var);
                    }
                }
            }

            this.textField.x = this.getPosX(menu);
            this.textField.y = this.getPosY(menu);
            this.textField.setWidth(this.getWidth());
            this.textField.setHeight(this.getHeight());
            this.textField.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime());

            //Update variable value on change
            if (!isEditorActive()) {
                if (this.linkedVariable != null) {
                    if (!this.lastValue.equals(this.textField.getValue())) {
                        VariableHandler.setVariable(linkedVariable, this.textField.getValue());
                    }
                    String val = VariableHandler.getVariable(this.linkedVariable);
                    if (val != null) {
                        if (!this.textField.getValue().equals(val)) {
                            this.textField.setValue(val);
                        }
                    } else {
                        this.textField.setValue("");
                    }
                }
                this.lastValue = this.textField.getValue();
            }

        }

    }

    public static enum InputFieldType {

        INTEGER_ONLY("integer", CharacterFilter.getIntegerCharacterFiler()),
        DECIMAL_ONLY("decimal", CharacterFilter.getDoubleCharacterFiler()),
        URL("url", CharacterFilter.getUrlCharacterFilter()),
        TEXT("text", null);

        String name;
        CharacterFilter filter;

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
