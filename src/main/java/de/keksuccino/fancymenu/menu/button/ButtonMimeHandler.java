package de.keksuccino.fancymenu.menu.button;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.HashMap;
import java.util.Map;

public class ButtonMimeHandler {

    protected static Map<String, ButtonPackage> cachedButtons = new HashMap<>();

    public static boolean tryCache(String menuIdentifier, boolean overrideCache) {
        if (!cachedButtons.containsKey(menuIdentifier) || overrideCache) {
            try {
                Screen s = GuiConstructor.tryToConstruct(menuIdentifier);
                if (s != null) {
                    ButtonPackage p = new ButtonPackage();
                    if (p.init(s)) {
                        cachedButtons.put(menuIdentifier, p);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cachedButtons.containsKey(menuIdentifier)) {
            return true;
        }
        FancyMenu.LOGGER.warn("[FANCYMENU] ButtonMimeHandler: Failed to cache buttons of screen!");
        return false;
    }

    public static ButtonData getButton(String buttonLocator) {
        if (buttonLocator.contains(":")) {
            String menuIdentifier = buttonLocator.split("[:]", 2)[0];
            menuIdentifier = MenuCustomization.getValidMenuIdentifierFor(menuIdentifier);
            String buttonId = buttonLocator.split("[:]", 2)[1];
            if (MathUtils.isLong(buttonId)) {
                if (!cachedButtons.containsKey(menuIdentifier)) {
                    tryCache(menuIdentifier, false);
                }
                ButtonPackage p = cachedButtons.get(menuIdentifier);
                if (p != null) {
                    return p.getButton(Long.parseLong(buttonId));
                }
            }
        }
        return null;
    }

    public static boolean executeButtonAction(String buttonLocator) {
        try {
            ButtonData d = getButton(buttonLocator);
            if (d != null) {
                ClickableWidget b = d.getButton();
                if (b != null) {
                    b.onClick(b.x+1, b.y+1);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        FancyMenu.LOGGER.warn("[FANCYMENU] ButtonMimeHandler: Failed to execute button click action!");
        return false;
    }

    public static void clearCache() {
        cachedButtons.clear();
    }

    public static class ButtonPackage {

        protected Map<Long, ButtonData> buttons = new HashMap<>();

        public boolean init(Screen screenToGetButtonsFrom) {
            if (screenToGetButtonsFrom != null) {
                for (ButtonData d : ButtonCache.cacheButtons(screenToGetButtonsFrom, 1000, 1000)) {
                    this.buttons.put(d.getId(), d);
                }
                return true;
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ButtonMimeHandler: Failed to set up ButtonPackage instance! Screen is null!");
            }
            return false;
        }

        public Map<Long, ButtonData> getButtons() {
            return this.buttons;
        }

        public ButtonData getButton(long id) {
            return this.buttons.get(id);
        }

    }

}
