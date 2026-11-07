package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TitleScreenWidgetIdentificationContextTest {

    private final TitleScreenWidgetIdentificationContext context = new TitleScreenWidgetIdentificationContext();

    @Test
    void vanillaLanguageAndAccessibilityButtonsGetUniversalIdentifiers() {
        assertEquals("mc_titlescreen_language_button", this.identify(CommonButtons.language(20, button -> {}, true)));
        assertEquals("mc_titlescreen_accessibility_button", this.identify(CommonButtons.accessibility(20, button -> {}, true)));
    }

    @ParameterizedTest
    @CsvSource({
            "fml.menu.mods, titlescreen_mods_button",
            "narrator.button.language, mc_titlescreen_language_button",
            "options.language, mc_titlescreen_language_button",
            "menu.options, mc_titlescreen_options_button",
            "menu.quit, mc_titlescreen_quit_button",
            "narrator.button.accessibility, mc_titlescreen_accessibility_button",
            "options.accessibility, mc_titlescreen_accessibility_button",
            "menu.singleplayer, mc_titlescreen_singleplayer_button",
            "menu.multiplayer, mc_titlescreen_multiplayer_button",
            "menu.online, mc_titlescreen_realms_button",
            "menu.playdemo, mc_titlescreen_playdemo_button",
            "modmenu.title, titlescreen_mods_button"
    })
    void everyKnownLocalizationKeyResolvesToItsIdentifier(String localizationKey, String expectedIdentifier) {
        assertEquals(expectedIdentifier, this.identify(button(Component.translatable(localizationKey))));
    }

    @Test
    void unknownLocalizationKeyHasNoIdentifier() {
        assertNull(this.identify(button(Component.translatable("menu.unknown_title_screen_button"))));
    }

    @Test
    void literalLabelMatchingAKnownKeyHasNoIdentifier() {
        assertNull(this.identify(button(Component.literal("options.language"))));
    }

    @Nullable
    @SuppressWarnings("DataFlowIssue")
    private String identify(AbstractWidget widget) {
        // The Title screen context only inspects the widget, so it never dereferences the parent screen.
        return this.context.getUniversalIdentifierForWidget(new WidgetMeta(widget, 0L, null));
    }

    private static AbstractWidget button(Component message) {
        return Button.builder(message, button -> {}).build();
    }

}
