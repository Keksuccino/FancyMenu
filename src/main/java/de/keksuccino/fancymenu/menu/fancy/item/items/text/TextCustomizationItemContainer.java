
package de.keksuccino.fancymenu.menu.fancy.item.items.text;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class TextCustomizationItemContainer extends CustomizationItemContainer {

    public TextCustomizationItemContainer() {
        super("fancymenu_customization_item_text");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        TextCustomizationItem i = new TextCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 200;
        i.height = 40;
        
        Screen s = Minecraft.getInstance().screen;
        if ((s != null) && (s instanceof LayoutEditorScreen)) {
            i.posY = (int)(((LayoutEditorScreen)s).ui.bar.getHeight() * UIBase.getUIScale());
        }
        
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TextCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new TextLayoutEditorElement(this, (TextCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.text");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.desc"), "%n%");
    }

}
