package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class PlayerEntityCustomizationItemContainer extends CustomizationItemContainer {

    public PlayerEntityCustomizationItemContainer() {
        super("fancymenu_customization_player_entity");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        PlayerEntityCustomizationItem i = new PlayerEntityCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 300;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new PlayerEntityCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new PlayerEntityLayoutEditorElement(this, (PlayerEntityCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.playerentity");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.desc"), "%n%");
    }

}
