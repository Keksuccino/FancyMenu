//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.items.ticker;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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
        MinecraftForge.EVENT_BUS.register(this);
    }

    //Stop threads of old ticker items
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            List<TickerCustomizationItem.TickerItemThreadController> activeControllers = new ArrayList<>();
            if (Minecraft.getMinecraft().currentScreen != null) {
                MenuHandlerBase m = MenuHandlerRegistry.getHandlerFor(Minecraft.getMinecraft().currentScreen);
                if (m != null) {
                    List<CustomizationItemBase> items = new ArrayList<>();
                    items.addAll(m.backgroundRenderItems);
                    items.addAll(m.frontRenderItems);
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
    }

    @SubscribeEvent
    public void onMenuReload(MenuReloadedEvent e) {
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
