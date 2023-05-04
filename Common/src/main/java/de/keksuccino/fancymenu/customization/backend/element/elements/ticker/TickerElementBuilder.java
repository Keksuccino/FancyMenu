package de.keksuccino.fancymenu.customization.backend.element.elements.ticker;

import de.keksuccino.fancymenu.customization.backend.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.element.AbstractElement;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TickerElementBuilder extends ElementBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    public static volatile List<TickerElement.TickerElementThreadController> cachedThreadControllers = new ArrayList<>();
    public static volatile List<String> cachedOncePerSessionItems = new ArrayList<>();

    public TickerElementBuilder() {
        super("fancymenu_customization_item_ticker");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    //Stop threads of old ticker items
    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        List<TickerElement.TickerElementThreadController> activeControllers = new ArrayList<>();
        if (Minecraft.getInstance().screen != null) {
            ScreenCustomizationLayer m = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
            if (m != null) {
                List<AbstractElement> items = new ArrayList<>();
                items.addAll(m.backgroundElements);
                items.addAll(m.foregroundElements);
                for (AbstractElement i : items) {
                    if (i instanceof TickerElement) {
                        if (((TickerElement)i).asyncThreadController != null) {
                            activeControllers.add(((TickerElement)i).asyncThreadController);
                        }
                    }
                }
            }
        }
        List<TickerElement.TickerElementThreadController> keep = new ArrayList<>();
        for (TickerElement.TickerElementThreadController c : cachedThreadControllers) {
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
    public @NotNull CustomizationItem buildDefaultInstance() {
        TickerElement i = new TickerElement(this, new PropertiesSection("dummy"));
        i.width = 70;
        i.height = 70;
        return i;
    }

    @Override
    public CustomizationItem deserializeElement(PropertiesSection serializedElement) {
        return new TickerElement(this, serializedElement);
    }

    @Override
    public AbstractEditorElement buildEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new TickerEditorElement(this, (TickerElement) item, handler);
    }

    @Override
    public @NotNull String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.ticker");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.desc"), "%n%");
    }

}
