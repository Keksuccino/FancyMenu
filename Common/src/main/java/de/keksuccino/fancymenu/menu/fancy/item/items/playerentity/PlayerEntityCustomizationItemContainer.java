package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.events.MenuReloadEvent;
import de.keksuccino.fancymenu.events.acara.EventHandler;
import de.keksuccino.fancymenu.events.acara.SubscribeEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class PlayerEntityCustomizationItemContainer extends CustomizationItemContainer {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, PlayerEntityCustomizationItem> ELEMENT_CACHE = new HashMap<>();

    public PlayerEntityCustomizationItemContainer() {
        super("fancymenu_customization_player_entity");
        EventHandler.INSTANCE.registerListenersOf(this);
    }
    
    @SubscribeEvent
    public void onMenuReload(MenuReloadEvent e) {
        ELEMENT_CACHE.clear();
        LOGGER.info("[FANCYMENU] PlayerEntity element cache cleared!");
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
