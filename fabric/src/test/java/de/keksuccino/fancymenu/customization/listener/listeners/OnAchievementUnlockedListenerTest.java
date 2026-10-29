package de.keksuccino.fancymenu.customization.listener.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnAchievementUnlockedListenerTest {

    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");
    private static final Identifier ACHIEVEMENT_ID = Identifier.parse("minecraft:story/mine_diamond");

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void englishDescriptionDocumentsRuntimeVariableNames() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        assertTrue(localizations.has("fancymenu.listeners.on_achievement_unlocked"));
        String description = localizations.get("fancymenu.listeners.on_achievement_unlocked.desc").getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        List<String> runtimeVariableNames = new OnAchievementUnlockedListener().getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(List.of("$$achievement_key", "$$achievement_label", "$$achievement_description", "$$achievement_icon_item_key", "$$achievement_type"), runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
    }

    @Test
    void unlockMapsAchievementToRuntimeVariables() {
        OnAchievementUnlockedListener listener = new OnAchievementUnlockedListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onAchievementUnlocked(ACHIEVEMENT_ID, display(Items.DIAMOND, "Diamonds!", "Acquire diamonds", AdvancementType.TASK));

        assertEquals("minecraft:story/mine_diamond", variable(instance, "achievement_key"));
        assertEquals("Diamonds!", variable(instance, "achievement_label"));
        assertEquals("Acquire diamonds", variable(instance, "achievement_description"));
        assertEquals("minecraft:diamond", variable(instance, "achievement_icon_item_key"));
        assertEquals("advancement", variable(instance, "achievement_type"));
    }

    @Test
    void achievementTypesMapToDocumentedNames() {
        OnAchievementUnlockedListener listener = new OnAchievementUnlockedListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        for (Map.Entry<AdvancementType, String> expected : Map.of(AdvancementType.TASK, "advancement", AdvancementType.GOAL, "goal", AdvancementType.CHALLENGE, "challenge").entrySet()) {
            listener.onAchievementUnlocked(ACHIEVEMENT_ID, display(Items.ELYTRA, "Title", "Description", expected.getKey()));
            assertEquals(expected.getValue(), variable(instance, "achievement_type"));
            assertEquals("minecraft:elytra", variable(instance, "achievement_icon_item_key"));
        }
    }

    @Test
    void unlockWithoutListeningInstancesIsIgnored() {
        OnAchievementUnlockedListener listener = new OnAchievementUnlockedListener();

        listener.onAchievementUnlocked(ACHIEVEMENT_ID, display(Items.DIAMOND, "Diamonds!", "Acquire diamonds", AdvancementType.TASK));
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        for (AbstractListener.CustomVariable customVariable : listener.getCustomVariables()) {
            assertEquals("", variable(instance, customVariable.name()));
        }
    }

    private static DisplayInfo display(ItemLike icon, String title, String description, AdvancementType type) {
        return Advancement.Builder.advancement().display(icon, Component.literal(title), Component.literal(description), null, type, true, true, false).build(ACHIEVEMENT_ID).value().display().orElseThrow();
    }

    private static String variable(ListenerInstance instance, String name) {
        return instance.getActionScript().getValuePlaceholders().get(name).get();
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OnAchievementUnlockedListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
