package de.keksuccino.fancymenu.customization.widget;

import de.keksuccino.fancymenu.customization.widget.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.screeninstancefactory.ScreenInstanceFactory;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonMimeHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final Map<String, ButtonPackage> CACHED_BUTTONS = new HashMap<>();

    public static boolean tryCache(String menuIdentifier, boolean overrideCache) {
        if (!CACHED_BUTTONS.containsKey(menuIdentifier) || overrideCache) {
            try {
                Screen s = ScreenInstanceFactory.tryConstruct(menuIdentifier);
                if (s != null) {
                    ButtonPackage p = new ButtonPackage();
                    if (p.init(s)) {
                        CACHED_BUTTONS.put(menuIdentifier, p);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (CACHED_BUTTONS.containsKey(menuIdentifier)) {
            return true;
        }
        LOGGER.warn("[FANCYMENU] ButtonMimeHandler#tryCache: Failed to cache buttons of screen!");
        return false;
    }

    public static boolean tryCache(Screen screen, boolean overrideCache) {
        String menuIdentifier = screen.getClass().getName();
        if (!CACHED_BUTTONS.containsKey(menuIdentifier) || overrideCache) {
            try {
                ButtonPackage p = new ButtonPackage();
                if (p.init(screen)) {
                    CACHED_BUTTONS.put(menuIdentifier, p);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (CACHED_BUTTONS.containsKey(menuIdentifier)) {
            return true;
        }
        LOGGER.warn("[FANCYMENU] ButtonMimeHandler#cacheFromInstance: Failed to cache buttons of screen!");
        return false;
    }

    @Nullable
    public static WidgetMeta getButton(String buttonLocator) {
        if (buttonLocator.contains(":")) {
            String menuIdentifier = buttonLocator.split(":", 2)[0];
            menuIdentifier = ScreenCustomization.findValidMenuIdentifierFor(menuIdentifier);
            String buttonId = buttonLocator.split(":", 2)[1];
            if (MathUtils.isLong(buttonId) || (buttonId.startsWith("button_compatibility_id:"))) {
                Screen current = Minecraft.getInstance().screen;
                if ((current != null) && (menuIdentifier.equals(current.getClass().getName()))) {
                    if (CACHED_BUTTONS.containsKey(menuIdentifier)) {
                        ButtonPackage pack = CACHED_BUTTONS.get(menuIdentifier);
                        WidgetMeta d = pack.getButton(buttonId);
                        if (d != null) {
                            if (d.getScreen() != current) {
                                tryCache(current, true);
                                Minecraft.getInstance().setScreen(current);
                            }
                        }
                    } else {
                        tryCache(current, true);
                        Minecraft.getInstance().setScreen(current);
                    }
                } else if (!CACHED_BUTTONS.containsKey(menuIdentifier)) {
                    tryCache(menuIdentifier, false);
                }
                ButtonPackage p = CACHED_BUTTONS.get(menuIdentifier);
                if (p != null) {
                    return p.getButton(buttonId);
                }
            }
        }
        return null;
    }

    public static boolean executeButtonAction(String buttonLocator) {
        try {
            WidgetMeta d = getButton(buttonLocator);
            if (d != null) {
                AbstractWidget b = d.getWidget();
                b.onClick(b.x + 1, b.y + 1);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.warn("[FANCYMENU] ButtonMimeHandler: Failed to execute button click action!");
        return false;
    }

    public static void clearCache() {
        CACHED_BUTTONS.clear();
    }

    public static class ButtonPackage {

        protected Map<Long, WidgetMeta> buttons = new HashMap<>();

        public boolean init(Screen screenToGetButtonsFrom) {
            if (screenToGetButtonsFrom != null) {
                List<String> compIds = new ArrayList<>();
                for (WidgetMeta d : ScreenWidgetDiscoverer.getWidgetMetasOfScreenInternal(screenToGetButtonsFrom, 1000, 1000)) {
                    ButtonIdentificator.setCompatibilityIdentifierOfWidgetMeta(d);
                    if (compIds.contains(d.compatibilityId)) {
                        d.compatibilityId = null;
                    } else {
                        compIds.add(d.compatibilityId);
                    }
                    this.buttons.put(d.getLongIdentifier(), d);
                }
                return true;
            } else {
                LOGGER.error("[FANCYMENU] ButtonMimeHandler: Failed to setup ButtonPackage instance! Screen was NULL!");
            }
            return false;
        }

        public Map<Long, WidgetMeta> getButtons() {
            return this.buttons;
        }

        public WidgetMeta getButton(String id) {
            if (MathUtils.isLong(id)) {
                return this.buttons.get(Long.parseLong(id));
            } else if (id.startsWith("button_compatibility_id:")) {
                for (WidgetMeta d : this.buttons.values()) {
                    if ((d.getCompatibilityIdentifier() != null) && d.getCompatibilityIdentifier().equals(id)) {
                        return d;
                    }
                }
            }
            return null;
        }

    }

}
