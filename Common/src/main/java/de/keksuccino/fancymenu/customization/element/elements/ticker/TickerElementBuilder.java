package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class TickerElementBuilder extends ElementBuilder<TickerElement, TickerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public static volatile List<TickerElement.TickerElementThreadController> cachedThreadControllers = new ArrayList<>();
    public static volatile List<String> cachedOncePerSessionItems = new ArrayList<>();

    public TickerElementBuilder() {
        super("fancymenu_customization_item_ticker");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    //Stop threads of old ticker elements
    @EventListener
    public void onClientTickPost(ClientTickEvent.Post e) {
        List<TickerElement.TickerElementThreadController> activeControllers = new ArrayList<>();
        if (Minecraft.getInstance().screen != null) {
            ScreenCustomizationLayer m = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
            if (m != null) {
                List<AbstractElement> elements = new ArrayList<>();
                elements.addAll(m.normalElements.backgroundElements);
                elements.addAll(m.normalElements.foregroundElements);
                for (AbstractElement element : elements) {
                    if (element instanceof TickerElement te) {
                        if (te.asyncThreadController != null) {
                            activeControllers.add(te.asyncThreadController);
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
    public void onModReload(ModReloadEvent e) {
        cachedOncePerSessionItems.clear();
        LOGGER.info("[FancyMenu] Successfully cleared cached once-per-session ticker elements.");
    }

    @Override
    public @NotNull TickerElement buildDefaultInstance() {
        TickerElement i = new TickerElement(this);
        i.baseWidth = 70;
        i.baseHeight = 70;
        return i;
    }

    @Override
    public TickerElement deserializeElement(@NotNull SerializedElement serialized) {

        TickerElement element = this.buildDefaultInstance();

        Map<Integer, ActionInstance> tempActions = new HashMap<>();
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            //tickeraction_<index>_ACTION
            if (m.getKey().startsWith("tickeraction_")) {
                String index = m.getKey().split("_", 3)[1];
                String tickerAction = m.getKey().split("_", 3)[2];
                String actionValue = m.getValue();
                if (MathUtils.isInteger(index)) {
                    Action a = ActionRegistry.getAction(tickerAction);
                    if (a != null) {
                        tempActions.put(Integer.parseInt(index), new ActionInstance(a, actionValue));
                    }
                }
            }
        }
        List<Integer> indexes = new ArrayList<>(tempActions.keySet());
        Collections.sort(indexes);
        element.actions.clear();
        for (int i : indexes) {
            element.actions.add(tempActions.get(i));
        }

        String tickDelayMsString = serialized.getValue("tick_delay");
        if ((tickDelayMsString != null) && MathUtils.isLong(tickDelayMsString)) {
            element.tickDelayMs = Long.parseLong(tickDelayMsString);
        }

        String isAsyncString = serialized.getValue("is_async");
        if ((isAsyncString != null) && isAsyncString.equalsIgnoreCase("true")) {
            element.isAsync = true;
        }

        String tickModeString = serialized.getValue("tick_mode");
        if (tickModeString != null) {
            TickerElement.TickMode t = TickerElement.TickMode.getByName(tickModeString);
            if (t != null) {
                element.tickMode = t;
            }
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull TickerElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("is_async", "" + element.isAsync);
        serializeTo.putProperty("tick_delay", "" + element.tickDelayMs);
        serializeTo.putProperty("tick_mode", "" + element.tickMode.name);
        int index = 0;
        for (ActionInstance c : element.actions) {
            String v = c.value;
            if (v == null) {
                v = "";
            }
            serializeTo.putProperty("tickeraction_" + index + "_" + c.action.getIdentifier(), v);
            index++;
        }

        return serializeTo;
        
    }

    @Override
    public @NotNull TickerEditorElement wrapIntoEditorElement(@NotNull TickerElement element, @NotNull LayoutEditorScreen editor) {
        return new TickerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.customization.items.ticker");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.ticker.desc");
    }

}
