package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class DeathScreenWidgetIdentificationContext extends WidgetIdentificationContext {

    public DeathScreenWidgetIdentificationContext() {

        this.addUniversalIdentifierProvider(meta -> {
            String key = meta.getWidgetLocalizationKey();
            if (key != null) {
                if (key.equals("deathScreen.spectate") || key.equals("deathScreen.respawn")) {
                    return "mc_deathscreen_respawn_button";
                }
                if (key.equals("deathScreen.titleScreen")) {
                    return "mc_deathscreen_titlemenu_button";
                }
            }
            return null;
        });

    }

    @Override
    public @NotNull Class<? extends Screen> getTargetScreen() {
        return DeathScreen.class;
    }

}
