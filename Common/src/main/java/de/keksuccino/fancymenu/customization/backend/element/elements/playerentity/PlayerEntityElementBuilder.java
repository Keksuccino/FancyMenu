package de.keksuccino.fancymenu.customization.backend.element.elements.playerentity;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.customization.backend.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerEntityElementBuilder extends ElementBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, PlayerEntityElement> ELEMENT_CACHE = new HashMap<>();

    public PlayerEntityElementBuilder() {
        super("fancymenu_customization_player_entity");
        EventHandler.INSTANCE.registerListenersOf(this);
    }
    
    @EventListener
    public void onMenuReload(MenuReloadEvent e) {
        ELEMENT_CACHE.clear();
        LOGGER.info("[FANCYMENU] PlayerEntity element cache cleared!");
    }

    @Override
    public @NotNull CustomizationItem buildDefaultInstance() {
        PlayerEntityElement i = new PlayerEntityElement(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 300;
        return i;
    }

    @Override
    public CustomizationItem deserializeElement(PropertiesSection serializedElement) {
        return new PlayerEntityElement(this, serializedElement);
    }

    @Override
    public AbstractEditorElement buildEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new PlayerEntityEditorElement(this, (PlayerEntityElement) item, handler);
    }

    @Override
    public @NotNull String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.playerentity");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.desc"), "%n%");
    }

}
