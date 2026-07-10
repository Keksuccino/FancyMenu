package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TickerElementBuilder extends ElementBuilder<TickerElement, TickerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public static volatile List<TickerElement.TickerElementThreadController> cachedThreadControllers = new ArrayList<>();
    public static volatile List<String> cachedOncePerSessionItems = new ArrayList<>();

    private static final TickerLifecycleListener LIFECYCLE_LISTENER = new TickerLifecycleListener();

    static {
        EventHandler.INSTANCE.registerListenersOf(LIFECYCLE_LISTENER);
    }

    public TickerElementBuilder() {
        super("fancymenu_customization_item_ticker");
    }

    static boolean tryMarkOncePerSessionItem(@NotNull String identifier) {
        List<String> cache = cachedOncePerSessionItems;
        synchronized (cache) {
            if (cache.contains(identifier)) {
                return false;
            }
            cache.add(identifier);
            return true;
        }
    }

    static void removeOncePerSessionItem(@NotNull String identifier) {
        List<String> cache = cachedOncePerSessionItems;
        synchronized (cache) {
            cache.remove(identifier);
        }
    }

    static void clearOncePerSessionItems() {
        List<String> cache = cachedOncePerSessionItems;
        synchronized (cache) {
            cache.clear();
        }
    }

    public static final class TickerLifecycleListener {

        private TickerLifecycleListener() {
        }

        //Stop threads of old ticker elements
        @EventListener
        public void onClientTickPost(ClientTickEvent.Post e) {
            List<TickerElement.TickerElementThreadController> activeControllers = new ArrayList<>();
            if (ScreenUtils.getScreen() != null) {
                ScreenCustomizationLayer m = ScreenCustomizationLayerHandler.getLayerOfScreen(ScreenUtils.getScreen());
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
        public void onInitOrResizeStarting(InitOrResizeScreenStartingEvent e) {
            TickerRuntimeStateTransfer.bindTarget(e.getScreen(), ScreenIdentifierHandler.getIdentifierOfScreen(e.getScreen()));
        }

        /**
         * Acara dispatches higher priorities first. The normal-priority customization layer constructs its new
         * ticker instances before this low-priority listener restores their continuation state.
         */
        @EventListener(priority = EventPriority.LOW)
        public void onInitOrResizePost(InitOrResizeScreenEvent.Post e) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(e.getScreen());
            if (layer == null) {
                return;
            }
            for (AbstractElement element : layer.allElements) {
                if (element instanceof TickerElement ticker) {
                    ticker.restoreRuntimeState(e.getScreen());
                }
            }
        }

        @EventListener
        public void onInitOrResizeCompleted(InitOrResizeScreenCompletedEvent e) {
            TickerRuntimeStateTransfer.finishInitialization(e.getScreen());
        }

        @EventListener
        public void onModReload(ModReloadEvent e) {
            clearOncePerSessionItems();
            TickerRuntimeStateTransfer.clear();
            LOGGER.info("[FancyMenu] Successfully cleared cached once-per-session ticker elements.");
        }

    }

    @Override
    public @NotNull TickerElement buildDefaultInstance() {
        TickerElement i = new TickerElement(this);
        i.baseWidth = 70;
        i.baseHeight = 70;
        i.inEditorColor.setDefault(DrawableColor.of(Color.ORANGE).getHex()).set(DrawableColor.of(Color.ORANGE).getHex());
        return i;
    }

    @Override
    public TickerElement deserializeElement(@NotNull SerializedElement serialized) {

        TickerElement element = this.buildDefaultInstance();

        String tickerExecutableBlockId = serialized.getValue("ticker_element_executable_block_identifier");
        if (tickerExecutableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, tickerExecutableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.actionExecutor = g;
            }
        } else {
            //Legacy support for old ticker action format
            GenericExecutableBlock g = new GenericExecutableBlock();
            g.getExecutables().addAll(ActionInstance.deserializeAll(serialized));
            element.actionExecutor = g;
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
        serializeTo.putProperty("tick_mode", element.tickMode.name);

        serializeTo.putProperty("ticker_element_executable_block_identifier", element.actionExecutor.identifier);
        element.actionExecutor.serializeToExistingPropertyContainer(serializeTo);

        return serializeTo;
        
    }

    @Override
    public @NotNull TickerEditorElement wrapIntoEditorElement(@NotNull TickerElement element, @NotNull LayoutEditorScreen editor) {
        return new TickerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.ticker");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.desc");
    }

}
