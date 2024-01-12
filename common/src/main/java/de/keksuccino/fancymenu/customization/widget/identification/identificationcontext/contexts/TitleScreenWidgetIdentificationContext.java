package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;

public class TitleScreenWidgetIdentificationContext extends WidgetIdentificationContext {

    public TitleScreenWidgetIdentificationContext() {

        this.addUniversalIdentifierProvider(meta -> {
            Component c = meta.getWidget().getMessage();
            if ((c instanceof MutableComponent) && (c.getContents() instanceof TranslatableContents)) {
                String key = meta.getWidgetLocalizationKey();
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
            } else if (c.getContents() instanceof PlainTextContents.LiteralContents l) {
                String label = l.text();
                if (label.equals("Copyright Mojang AB. Do not distribute!")) {
                    return "mc_titlescreen_copyright_button";
                }
            }
            return null;
        });

    }

    @Override
    public @NotNull Class<? extends Screen> getTargetScreen() {
        return TitleScreen.class;
    }

}
