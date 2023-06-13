package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.identification.ButtonIdentificator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

public class TitleScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return TitleScreen.class;
    }


    @Nullable
    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        AbstractWidget b = data.getButton();
        if (b != null) {
            Component c = b.getMessage();
            if ((c instanceof MutableComponent) && (c.getContents() instanceof TranslatableContents)) {
                String key = ButtonIdentificator.getLocalizationKeyForButton(b);
                if (key != null) {
                    if (key.equals("fml.menu.mods")) {
                        return "forge_titlescreen_mods_button";
                    }
                    if (key.equals("narrator.button.language")) {
                        return "mc_titlescreen_language_button";
                    }
                    if (key.equals("menu.options")) {
                        return "mc_titlescreen_options_button";
                    }
                    if (key.equals("menu.quit")) {
                        return "mc_titlescreen_quit_button";
                    }
                    if (key.equals("narrator.button.accessibility")) {
                        return "mc_titlescreen_accessibility_button";
                    }
                    if (key.equals("menu.singleplayer")) {
                        return "mc_titlescreen_singleplayer_button";
                    }
                    if (key.equals("menu.multiplayer")) {
                        return "mc_titlescreen_multiplayer_button";
                    }
                    if (key.equals("menu.online")) {
                        return "mc_titlescreen_realms_button";
                    }
                    if (key.equals("menu.playdemo")) {
                        return "mc_titlescreen_playdemo_button";
                    }
                    if (key.equals("modmenu.title")) {
                        return "modmenu_titlescreen_mods_button";
                    }
                }
            } else if (c.getContents() instanceof LiteralContents) {
                String label = ((LiteralContents) c.getContents()).text();
                if (label.equals("Copyright Mojang AB. Do not distribute!")) {
                    return "mc_titlescreen_copyright_button";
                }
            }
        }
        return null;
    }

}
