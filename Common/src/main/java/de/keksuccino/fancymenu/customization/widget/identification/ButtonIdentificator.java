package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.DeathScreenButtonsIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.MenuButtonsIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.PauseScreenButtonsIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.TitleScreenButtonsIdentificationContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ButtonIdentificator {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Class<?>, MenuButtonsIdentificationContext> CONTEXTS = new HashMap<>();

    public static void init() {

        registerContext(new TitleScreenButtonsIdentificationContext());
        registerContext(new DeathScreenButtonsIdentificationContext());
        registerContext(new PauseScreenButtonsIdentificationContext());

    }

    public static void registerContext(MenuButtonsIdentificationContext context) {
        CONTEXTS.put(context.getMenu(), context);
    }

    public static String getCompatibilityIdentifierForButton(WidgetMeta data) {
        try {
            Screen s = data.getScreen();
            if (s != null) {
                MenuButtonsIdentificationContext c = CONTEXTS.get(s.getClass());
                if (c != null) {
                    return c.getCompatibilityIdentifierForButton(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCompatibilityIdentifierToWidgetMeta(WidgetMeta data) {
        if (data != null) {
            data.setCompatibilityId(getCompatibilityIdentifierForButton(data));
        }
    }

    @Nullable
    public static String getLocalizationKeyForButton(AbstractWidget widget) {

        if (widget.getMessage() instanceof MutableComponent) {
            ComponentContents cc = widget.getMessage().getContents();
            if (cc instanceof TranslatableContents) {
                return ((TranslatableContents)cc).getKey();
            }
        }

        return null;

    }

}
