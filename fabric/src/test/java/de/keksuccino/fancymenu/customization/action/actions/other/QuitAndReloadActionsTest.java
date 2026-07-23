package de.keksuccino.fancymenu.customization.action.actions.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuitAndReloadActionsTest {

    @Test
    void englishQuitDescriptionDocumentsNormalClientShutdown() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Stops the Minecraft client and begins its normal shutdown sequence.", localizations.get("fancymenu.actions.quitgame.desc").getAsString());
    }

    @Test
    void englishReloadDescriptionDocumentsCooldownAndIgnoredTriggers() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Reloads resource packs, just like pressing F3 + T.\n\nAfter a reload is triggered, this action has a five-second cooldown. Triggers during the cooldown are ignored.", localizations.get("fancymenu.actions.reload_resource_packs.desc").getAsString());
    }

    @Test
    void actionIdentifiersRemainBackwardCompatible() {
        assertEquals("quitgame", new QuitGameAction().getIdentifier());
        assertEquals("reload_resource_packs", new ReloadResourcePacksAction().getIdentifier());
    }

    @Test
    void reloadCooldownRejectsTriggersUntilTheInclusiveFiveSecondBoundary() {
        assertEquals(5000L, ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS);
        ReloadResourcePacksAction.ReloadCooldown cooldown = new ReloadResourcePacksAction.ReloadCooldown(ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS);
        long firstTrigger = 10_000L;

        assertTrue(cooldown.tryTrigger(firstTrigger));
        assertFalse(cooldown.tryTrigger(firstTrigger));
        assertFalse(cooldown.tryTrigger(firstTrigger + ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS - 1L));
        assertTrue(cooldown.tryTrigger(firstTrigger + ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS));
    }

    @Test
    void rejectedReloadTriggersDoNotExtendTheCooldown() {
        ReloadResourcePacksAction.ReloadCooldown cooldown = new ReloadResourcePacksAction.ReloadCooldown(ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS);
        long firstTrigger = 10_000L;

        assertTrue(cooldown.tryTrigger(firstTrigger));
        assertFalse(cooldown.tryTrigger(firstTrigger + 4000L));
        assertTrue(cooldown.tryTrigger(firstTrigger + ReloadResourcePacksAction.RELOAD_COOLDOWN_MILLIS));
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = QuitAndReloadActionsTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
