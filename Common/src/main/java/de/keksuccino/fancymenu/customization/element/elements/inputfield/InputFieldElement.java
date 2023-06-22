package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InputFieldElement extends AbstractElement {

    public String linkedVariable;
    public InputFieldType type = InputFieldType.TEXT;
    public int maxTextLength = 10000;

    public AdvancedTextField textField;
    public String lastValue = "";

    public InputFieldElement(ElementBuilder<InputFieldElement, InputFieldEditorElement> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            RenderSystem.enableBlend();

            //Handle editor mode for text field
            if (isEditor()) {
                this.textField.active = false;
                this.textField.setEditable(false);
                if (this.linkedVariable != null) {
                    if (VariableHandler.variableExists(this.linkedVariable)) {
                        String var = Objects.requireNonNull(VariableHandler.getVariable(this.linkedVariable)).value;
                        if (var != null) {
                            this.textField.setValue(var);
                        }
                    }
                }
            }

            this.textField.x = this.getX();
            this.textField.y = this.getY();
            this.textField.setWidth(this.getWidth());
            ((IMixinAbstractWidget)this.textField).setHeightFancyMenu(this.getHeight());
            this.textField.render(pose, mouseX, mouseY, partial);

            //Update variable value on change
            if (!isEditor()) {
                if (this.linkedVariable != null) {
                    if (!this.lastValue.equals(this.textField.getValue())) {
                        VariableHandler.setVariable(linkedVariable, this.textField.getValue());
                    }
                    if (VariableHandler.variableExists(this.linkedVariable)) {
                        String val = Objects.requireNonNull(VariableHandler.getVariable(this.linkedVariable)).value;
                        if (val != null) {
                            if (!this.textField.getValue().equals(val)) {
                                this.textField.setValue(val);
                            }
                        } else {
                            this.textField.setValue("");
                        }
                    } else {
                        this.textField.setValue("");
                    }
                }
                this.lastValue = this.textField.getValue();
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        }

    }

    public enum InputFieldType {

        INTEGER_ONLY("integer", CharacterFilter.getIntegerCharacterFiler()),
        DECIMAL_ONLY("decimal", CharacterFilter.getDoubleCharacterFiler()),
        URL("url", CharacterFilter.getUrlCharacterFilter()),
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
