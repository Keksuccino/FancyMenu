package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public class TitleScreenWidgetIdentificationContext extends WidgetIdentificationContext {

    /**
     * Forge/NeoForge and Mod Menu label their Mods buttons with different localization keys, but both get this identifier,
     * so customizing the Mods button on one loader also customizes it on the other ones.
     */
    public static final String MODS_BUTTON_IDENTIFIER = "titlescreen_mods_button";

    /**
     * Maps the localization keys of Title screen buttons to their universal identifiers.<br>
     * The Language and Accessibility buttons have two keys each, because Minecraft labels them with {@code narrator.button.*} keys
     * up to 1.20.1 and with the {@code options.*} keys of {@code CommonButtons} in later versions.
     * Both keys need to stay, so layouts using these identifiers keep working in all Minecraft versions.
     */
    private static final Map<String, String> IDENTIFIERS_BY_LOCALIZATION_KEY = Map.ofEntries(
            Map.entry("fml.menu.mods", MODS_BUTTON_IDENTIFIER),
            Map.entry("narrator.button.language", "mc_titlescreen_language_button"),
            Map.entry("options.language", "mc_titlescreen_language_button"),
            Map.entry("menu.options", "mc_titlescreen_options_button"),
            Map.entry("menu.quit", "mc_titlescreen_quit_button"),
            Map.entry("narrator.button.accessibility", "mc_titlescreen_accessibility_button"),
            Map.entry("options.accessibility", "mc_titlescreen_accessibility_button"),
            Map.entry("menu.singleplayer", "mc_titlescreen_singleplayer_button"),
            Map.entry("menu.multiplayer", "mc_titlescreen_multiplayer_button"),
            Map.entry("menu.online", "mc_titlescreen_realms_button"),
            Map.entry("menu.playdemo", "mc_titlescreen_playdemo_button"),
            Map.entry("modmenu.title", MODS_BUTTON_IDENTIFIER)
    );

    public TitleScreenWidgetIdentificationContext() {

        this.addUniversalIdentifierProvider(meta -> {
            String key = meta.getWidgetLocalizationKey();
            return (key != null) ? IDENTIFIERS_BY_LOCALIZATION_KEY.get(key) : null;
        });

    }

    @Override
    public @NotNull Class<? extends Screen> getTargetScreen() {
        return TitleScreen.class;
    }

}
