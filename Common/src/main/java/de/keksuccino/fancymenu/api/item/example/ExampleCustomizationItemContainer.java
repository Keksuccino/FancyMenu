package de.keksuccino.fancymenu.api.item.example;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;

//This needs to be registered to the CustomizationItemRegistry at mod init
public class ExampleCustomizationItemContainer extends CustomizationItemContainer {

    public ExampleCustomizationItemContainer() {
        super("example_item_identifier");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        ExampleCustomizationItem i = new ExampleCustomizationItem(this, new PropertiesSection("dummy"));
        //The default size of 10x10 would be a bit too small for the item, so I set a new default size of 100x100 to the default instance.
        //This means that now every new item of this type will have a size of 100x100 by default.
        i.width = 100;
        i.height = 100;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new ExampleCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new ExampleLayoutEditorElement(this, (ExampleCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return "Example Item";
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                "This is a description",
                "with 2 lines of text."
        };
    }

}
