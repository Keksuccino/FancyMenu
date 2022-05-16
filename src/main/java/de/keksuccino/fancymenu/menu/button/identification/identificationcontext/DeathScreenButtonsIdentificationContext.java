//TODO übernehmen
package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class DeathScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return DeathScreen.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        Widget b = data.getButton();
        if (b != null) {
            ITextComponent c = b.getMessage();
            if (c instanceof TranslationTextComponent) {
                String key = ((TranslationTextComponent)c).getKey();
                if (key.equals("deathScreen.spectate") || key.equals("deathScreen.respawn")) {
                    return "mc_deathscreen_respawn_button";
                }
                if (key.equals("deathScreen.titleScreen")) {
                    return "mc_deathscreen_titlemenu_button";
                }
            }
        }
        return null;
    }

}
