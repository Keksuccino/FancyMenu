package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public class InputFieldElement extends AbstractElement {

    public String linkedVariable;
    public InputFieldType type = InputFieldType.TEXT;
    public int maxTextLength = 10000;
    public ExtendedEditBox editBox;
    public String lastValue = "";
    public boolean navigatable = true;

    public InputFieldElement(ElementBuilder<InputFieldElement, InputFieldEditorElement> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

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

            this.editBox.setX(this.getAbsoluteX());
            this.editBox.setY(this.getAbsoluteY());
            this.editBox.setWidth(this.getAbsoluteWidth());
            ((IMixinAbstractWidget)this.editBox).setHeightFancyMenu(this.getAbsoluteHeight());
            this.editBox.render(graphics, mouseX, mouseY, partial);

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

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return ListUtils.of(this.editBox);
    }

    public enum InputFieldType {

        INTEGER_ONLY("integer", CharacterFilter.buildIntegerFiler()),
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
