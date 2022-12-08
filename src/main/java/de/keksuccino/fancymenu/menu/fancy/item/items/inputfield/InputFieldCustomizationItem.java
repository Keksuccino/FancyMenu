//---
package de.keksuccino.fancymenu.menu.fancy.item.items.inputfield;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

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

        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        this.textField = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, this.getPosX(current), this.getPosY(current), this.getWidth(), this.getHeight(), true, this.type.filter);
        this.textField.setMaxStringLength(this.maxTextLength);
        if (this.linkedVariable != null) {
            String var = VariableHandler.getVariable(this.linkedVariable);
            if (var != null) {
                this.textField.setText(var);
            }
        }

    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        if (this.shouldRender()) {

            GlStateManager.enableBlend();

            //Handle editor mode for text field
            if (isEditorActive()) {
                this.textField.setEnabled(false);
                if (this.linkedVariable != null) {
                    String var = VariableHandler.getVariable(this.linkedVariable);
                    if (var != null) {
                        this.textField.setText(var);
                    }
                }
            }

            this.textField.x = this.getPosX(menu);
            this.textField.y = this.getPosY(menu);
            this.textField.width = this.getWidth();
            this.textField.height = this.getHeight();
            this.textField.drawTextBox();

            //Update variable value on change
            if (!isEditorActive()) {
                if (this.linkedVariable != null) {
                    if (!this.lastValue.equals(this.textField.getText())) {
                        VariableHandler.setVariable(linkedVariable, this.textField.getText());
                    }
                    String val = VariableHandler.getVariable(this.linkedVariable);
                    if (val != null) {
                        if (!this.textField.getText().equals(val)) {
                            this.textField.setText(val);
                        }
                    }
                }
                this.lastValue = this.textField.getText();
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
