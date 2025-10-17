package de.keksuccino.fancymenu.customization.widget;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class WidgetLocatorHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final Map<String, ScreenWidgetCollection> CACHED_WIDGETS = new HashMap<>();

    protected static void tryCache(@NotNull String screenIdentifier, boolean overrideCache) {
        Screen instance = ScreenInstanceFactory.tryConstruct(screenIdentifier);
        if (instance != null) {
            instance.init(Minecraft.getInstance(), 1000, 1000);
            tryCache(instance, overrideCache);
        } else {
            LOGGER.error("[FANCYMENU] WidgetLocatorHandler failed to construct instance of '" + screenIdentifier + "'! Unable to cache widgets!");
        }
    }

    protected static void tryCache(@NotNull Screen screen, boolean overrideCache) {
        if (ScreenCustomization.isScreenBlacklisted(screen)) return;
        String screenIdentifier = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
        if (UniversalScreenIdentifierRegistry.universalIdentifierExists(screenIdentifier)) {
            screenIdentifier = Objects.requireNonNull(UniversalScreenIdentifierRegistry.getScreenForUniversalIdentifier(screenIdentifier));
        }
        if (!CACHED_WIDGETS.containsKey(screenIdentifier) || overrideCache) {
            Screen current = Minecraft.getInstance().screen;
            if (current == screen) {
                if (!ScreenCustomization.isCustomizationEnabledForScreen(current)) return;
                ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(current);
                if (layer != null) {
                    ScreenWidgetCollection collection = new ScreenWidgetCollection();
                    collection.setWidgets(layer.cachedScreenWidgetMetas);
                    CACHED_WIDGETS.put(screenIdentifier, collection);
                } else {
                    LOGGER.error("[FANCYMENU] WidgetLocatorHandler failed to cache widgets of current screen '" + screenIdentifier + "'! Customization layer was NULL!");
                }
            } else {
                ScreenWidgetCollection collection = new ScreenWidgetCollection();
                collection.setWidgets(ScreenWidgetDiscoverer.getWidgetsOfScreen(screen));
                CACHED_WIDGETS.put(screenIdentifier, collection);
            }
        }
    }

    @Nullable
    public static WidgetMeta getWidget(@NotNull String widgetLocator) {
        if (widgetLocator.contains(":")) {
            String screenIdentifier = widgetLocator.split(":", 2)[0];
            if (UniversalScreenIdentifierRegistry.universalIdentifierExists(screenIdentifier)) {
                screenIdentifier = Objects.requireNonNull(UniversalScreenIdentifierRegistry.getScreenForUniversalIdentifier(screenIdentifier));
            }
            screenIdentifier = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(screenIdentifier);
            String widgetIdentifier = widgetLocator.split(":", 2)[1];
            Screen current = Minecraft.getInstance().screen;
            //Check if the button locator is pointing to a button in the current screen
            if ((current != null) && ScreenIdentifierHandler.isIdentifierOfScreen(screenIdentifier, current)) {
                if (ScreenCustomization.isCustomizationEnabledForScreen(current)) {
                    ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(current);
                    if (layer != null) {
                        if (!CACHED_WIDGETS.containsKey(screenIdentifier)) tryCache(current, false);
                        for (WidgetMeta meta : layer.cachedScreenWidgetMetas) {
                            if (WidgetIdentifierHandler.isIdentifierOfWidget(widgetIdentifier, meta)) return meta;
                        }
                    }
                }
            } else if (!CACHED_WIDGETS.containsKey(screenIdentifier)) {
                tryCache(screenIdentifier, false);
            }
            ScreenWidgetCollection collection = CACHED_WIDGETS.get(screenIdentifier);
            if (collection != null) {
                return collection.getWidget(widgetIdentifier);
            }
        }
        return null;
    }

    public static boolean invokeWidgetOnClick(@NotNull String widgetLocator) {
        WidgetMeta meta = getWidget(widgetLocator);
        if (meta != null) {
            return invokeWidgetOnClick(meta);
        }
        return false;
    }

    public static boolean invokeWidgetOnClick(@NotNull WidgetMeta meta) {
        try {
            AbstractWidget w = meta.getWidget();
            w.onClick(new MouseButtonEvent(w.getX() + 1, w.getY() + 1, new MouseButtonInfo(0, -1)), false);
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to invoke widget's onClick() method!", ex);
        }
        LOGGER.error("[FANCYMENU] WidgetLocatorHandler failed to invoke widget onClick!");
        return false;
    }

    public static void clearCache() {
        CACHED_WIDGETS.clear();
    }

    protected static class ScreenWidgetCollection {

        @NotNull
        protected List<WidgetMeta> widgets = new ArrayList<>();

        protected void setWidgets(@NotNull List<WidgetMeta> metas) {
            this.widgets = metas;
        }

        @NotNull
        protected List<WidgetMeta> getWidgets() {
            return this.widgets;
        }

        @Nullable
        protected WidgetMeta getWidget(@NotNull String widgetIdentifier) {
            for (WidgetMeta meta : this.widgets) {
                if (WidgetIdentifierHandler.isIdentifierOfWidget(widgetIdentifier, meta)) return meta;
            }
            return null;
        }

    }

}
