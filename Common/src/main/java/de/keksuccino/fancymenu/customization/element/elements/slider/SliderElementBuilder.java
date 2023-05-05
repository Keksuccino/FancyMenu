package de.keksuccino.fancymenu.customization.element.elements.slider;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import org.jetbrains.annotations.NotNull;

public class SliderElementBuilder extends ElementBuilder {

    public SliderElementBuilder() {
        super("fancymenu_customization_item_slider");
    }

    @Override
    public @NotNull CustomizationItem buildDefaultInstance() {
        SliderElement i = new SliderElement(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem deserializeElement(PropertiesSection serializedElement) {
        return new SliderElement(this, serializedElement);
    }

    @Override
    public AbstractEditorElement buildEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new SliderEditorElement(this, (SliderElement) item, handler);
    }

    @Override
    public @NotNull String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.slider");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.desc"), "%n%");
    }

}
