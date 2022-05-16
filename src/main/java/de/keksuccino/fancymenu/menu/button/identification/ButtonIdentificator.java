//TODO übernehmen
package de.keksuccino.fancymenu.menu.button.identification;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.identification.identificationcontext.DeathScreenButtonsIdentificationContext;
import de.keksuccino.fancymenu.menu.button.identification.identificationcontext.MenuButtonsIdentificationContext;
import de.keksuccino.fancymenu.menu.button.identification.identificationcontext.PauseScreenButtonsIdentificationContext;
import de.keksuccino.fancymenu.menu.button.identification.identificationcontext.TitleScreenButtonsIdentificationContext;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ButtonIdentificator {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ButtonIdentificator");

    private static Map<Class, MenuButtonsIdentificationContext> contexts = new HashMap<>();

    public static void init() {

        registerContext(new TitleScreenButtonsIdentificationContext());
        registerContext(new DeathScreenButtonsIdentificationContext());
        registerContext(new PauseScreenButtonsIdentificationContext());

    }

    public static void registerContext(MenuButtonsIdentificationContext context) {
        contexts.put(context.getMenu(), context);
    }

    public static String getCompatibilityIdentifierForButton(ButtonData data) {
        try {
            Screen s = data.getScreen();
            if (s != null) {
                MenuButtonsIdentificationContext c = contexts.get(s.getClass());
                if (c != null) {
                    return c.getCompatibilityIdentifierForButton(data);
                } else {
//                    LOGGER.warn("WARNING: No identification context found for menu: " + s.getClass().getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCompatibilityIdentifierToData(ButtonData data) {
        if (data != null) {
            data.setCompatibilityId(getCompatibilityIdentifierForButton(data));
        }
    }

}
