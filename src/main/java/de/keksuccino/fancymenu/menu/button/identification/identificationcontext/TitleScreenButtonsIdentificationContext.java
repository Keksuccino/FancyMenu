package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;

public class TitleScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return GuiMainMenu.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        GuiButton b = data.getButton();
        if (b != null) {
            String msg = b.displayString;
            if (msg.equals(I18n.format("fml.menu.mods"))) {
                return "forge_titlescreen_mods_button";
            }
            if (b instanceof GuiButtonLanguage) {
                return "mc_titlescreen_language_button";
            }
            if (msg.equals(I18n.format("menu.options"))) {
                return "mc_titlescreen_options_button";
            }
            if (msg.equals(I18n.format("menu.quit"))) {
                return "mc_titlescreen_quit_button";
            }
            if (msg.equals(I18n.format("menu.singleplayer"))) {
                return "mc_titlescreen_singleplayer_button";
            }
            if (msg.equals(I18n.format("menu.multiplayer"))) {
                return "mc_titlescreen_multiplayer_button";
            }
            if (msg.equals(I18n.format("menu.online").replace("Minecraft", "").trim())) {
                return "mc_titlescreen_realms_button";
            }
            if (msg.equals(I18n.format("menu.playdemo"))) {
                return "mc_titlescreen_playdemo_button";
            }
        }
        return null;
    }

}
