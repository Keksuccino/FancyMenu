package de.keksuccino.fancymenu.menu.button;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonMimeHandler {

    private static Logger LOGGER = LogManager.getLogger("fancymenu/ButtonMimeHandler");

    protected static Map<String, ButtonPackage> cachedButtons = new HashMap<>();

    public static boolean tryCache(String menuIdentifier, boolean overrideCache) {
        if (!cachedButtons.containsKey(menuIdentifier) || overrideCache) {
            try {
                GuiScreen s = GuiConstructor.tryToConstruct(menuIdentifier);
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
        LOGGER.warn("tryCache: Failed to cache buttons of screen!");
        return false;
    }

    public static boolean cacheFromInstance(GuiScreen screen, boolean overrideCache) {
        String menuIdentifier = screen.getClass().getName();
        if (!cachedButtons.containsKey(menuIdentifier) || overrideCache) {
            try {
                ButtonPackage p = new ButtonPackage();
                if (p.init(screen)) {
                    cachedButtons.put(menuIdentifier, p);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cachedButtons.containsKey(menuIdentifier)) {
            return true;
        }
        LOGGER.warn("cacheFromInstance: Failed to cache buttons of screen!");
        return false;
    }

    public static ButtonData getButton(String buttonLocator) {
        if (buttonLocator.contains(":")) {
            String menuIdentifier = buttonLocator.split("[:]", 2)[0];
            menuIdentifier = MenuCustomization.getValidMenuIdentifierFor(menuIdentifier);
            String buttonId = buttonLocator.split("[:]", 2)[1];
            if (MathUtils.isLong(buttonId) || (buttonId.startsWith("button_compatibility_id:"))) {
                GuiScreen current = Minecraft.getMinecraft().currentScreen;
                if ((current != null) && (menuIdentifier.equals(current.getClass().getName()))) {
                    if (cachedButtons.containsKey(menuIdentifier)) {
                        ButtonPackage pack = cachedButtons.get(menuIdentifier);
                        ButtonData d = pack.getButton(buttonId);
                        if (d != null) {
                            if (d.getScreen() != current) {
                                cacheFromInstance(current, true);
                                Minecraft.getMinecraft().displayGuiScreen(current);
                            }
                        }
                    } else {
                        cacheFromInstance(current, true);
                        Minecraft.getMinecraft().displayGuiScreen(current);
                    }
                } else if (!cachedButtons.containsKey(menuIdentifier)) {
                    tryCache(menuIdentifier, false);
                }
                ButtonPackage p = cachedButtons.get(menuIdentifier);
                if (p != null) {
                    return p.getButton(buttonId);
                }
            }
        }
        return null;
    }

    public static boolean executeButtonAction(String buttonLocator) {
        try {
            ButtonData d = getButton(buttonLocator);
            if (d != null) {
                GuiButton b = d.getButton();
                if (b != null) {
                    performActionForButton(b, d.getScreen());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.warn("Failed to execute button click action!");
        return false;
    }

    public static void clearCache() {
        cachedButtons.clear();
    }

    protected static void performActionForButton(GuiButton btn, GuiScreen parentScreen) throws InvocationTargetException, IllegalAccessException {
        Method m = ObfuscationReflectionHelper.findMethod(GuiScreen.class, "func_146284_a", Void.class, GuiButton.class);
        m.invoke(parentScreen, btn);
    }

    public static class ButtonPackage {

        protected Map<Long, ButtonData> buttons = new HashMap<>();

        public boolean init(GuiScreen screenToGetButtonsFrom) {
            if (screenToGetButtonsFrom != null) {
                List<String> compIds = new ArrayList<>();
                for (ButtonData d : ButtonCache.cacheButtons(screenToGetButtonsFrom, 1000, 1000)) {
                    ButtonIdentificator.setCompatibilityIdentifierToData(d);
                    if (compIds.contains(d.compatibilityId)) {
                        d.compatibilityId = null;
                    } else {
                        compIds.add(d.compatibilityId);
                    }
                    this.buttons.put(d.getId(), d);
                }
                return true;
            } else {
                LOGGER.error("Failed to set up ButtonPackage instance! Screen is null!");
            }
            return false;
        }

        public Map<Long, ButtonData> getButtons() {
            return this.buttons;
        }

        public ButtonData getButton(String id) {
            if (MathUtils.isLong(id)) {
                return this.buttons.get(Long.parseLong(id));
            } else if (id.startsWith("button_compatibility_id:")) {
                for (ButtonData d : this.buttons.values()) {
                    if ((d.getCompatibilityId() != null) && d.getCompatibilityId().equals(id)) {
                        return d;
                    }
                }
            }
            return null;
        }

    }

}
