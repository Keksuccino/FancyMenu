package de.keksuccino.fancymenu.customization.backend.item.v2.items.ticker;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.item.CustomizationItemBase;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TickerCustomizationItemContainer extends CustomizationItemContainer {

    private static final Logger LOGGER = LogManager.getLogger();

    public static volatile List<TickerCustomizationItem.TickerItemThreadController> cachedThreadControllers = new ArrayList<>();
    public static volatile List<String> cachedOncePerSessionItems = new ArrayList<>();

    public TickerCustomizationItemContainer() {
        super("fancymenu_customization_item_ticker");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    //Stop threads of old ticker items
    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        List<TickerCustomizationItem.TickerItemThreadController> activeControllers = new ArrayList<>();
        if (Minecraft.getInstance().screen != null) {
            ScreenCustomizationLayer m = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
            if (m != null) {
                List<CustomizationItemBase> items = new ArrayList<>();
                items.addAll(m.backgroundElements);
                items.addAll(m.foregroundElements);
                for (CustomizationItemBase i : items) {
                    if (i instanceof TickerCustomizationItem) {
                        if (((TickerCustomizationItem)i).asyncThreadController != null) {
                            activeControllers.add(((TickerCustomizationItem)i).asyncThreadController);
                        }
                    }
                }
            }
        }
        List<TickerCustomizationItem.TickerItemThreadController> keep = new ArrayList<>();
        for (TickerCustomizationItem.TickerItemThreadController c : cachedThreadControllers) {
            if (!activeControllers.contains(c)) {
                c.running = false;
            } else {
                keep.add(c);
            }
        }
        cachedThreadControllers = keep;
    }

    @EventListener
    public void onMenuReload(MenuReloadEvent e) {
        cachedOncePerSessionItems.clear();
        LOGGER.info("[FancyMenu] Successfully cleared cached once-per-session ticker elements.");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        TickerCustomizationItem i = new TickerCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 70;
        i.height = 70;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TickerCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new TickerLayoutEditorElement(this, (TickerCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.ticker");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.desc"), "%n%");
    }

}
