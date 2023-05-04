package de.keksuccino.fancymenu.customization.backend.element.elements.inputfield;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.customization.backend.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import org.jetbrains.annotations.NotNull;

public class InputFieldElementBuilder extends ElementBuilder {

    public InputFieldElementBuilder() {
        super("fancymenu_customization_item_input_field");
    }

    @Override
    public @NotNull CustomizationItem buildDefaultInstance() {
        InputFieldElement i = new InputFieldElement(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem deserializeElement(PropertiesSection serializedElement) {
        return new InputFieldElement(this, serializedElement);
    }

    @Override
    public AbstractEditorElement buildEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new InputFieldEditorElement(this, (InputFieldElement) item, handler);
    }

    @Override
    public @NotNull String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.input_field");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.desc"), "%n%");
    }

}
