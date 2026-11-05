package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsInTagRequirementTest {

    private static Registry<String> registry;

    @BeforeAll
    static void setUp() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MappedRegistry<String> testRegistry = new MappedRegistry<>(ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("fancymenu", "is_in_tag_test")), Lifecycle.stable());
        Holder<String> oakLog = register(testRegistry, Identifier.withDefaultNamespace("oak_log"));
        Holder<String> moddedLog = register(testRegistry, Identifier.fromNamespaceAndPath("fancymenu", "test_log"));
        Holder<String> stone = register(testRegistry, Identifier.withDefaultNamespace("stone"));
        Map<TagKey<String>, List<Holder<String>>> tags = Map.of(TagKey.create(testRegistry.key(), Identifier.withDefaultNamespace("logs")), List.of(oakLog, moddedLog), TagKey.create(testRegistry.key(), Identifier.fromNamespaceAndPath("c", "stones")), List.of(stone));
        tags.forEach(testRegistry::bindTag);
        registry = testRegistry.freeze();
    }

    private static Holder<String> register(MappedRegistry<String> testRegistry, Identifier id) {
        return testRegistry.register(ResourceKey.create(testRegistry.key(), id), id.getPath(), RegistrationInfo.BUILT_IN);
    }

    @ParameterizedTest
    @CsvSource({
            "minecraft:oak_log, minecraft:logs",
            "fancymenu:test_log, minecraft:logs",
            "minecraft:stone, c:stones",
            "minecraft:oak_log, #minecraft:logs",
            "oak_log, logs",
            "'  Minecraft:Oak_Log ', ' #Minecraft:Logs '"
    })
    void entryInTagMatches(String key, String tag) {
        assertTrue(IsInTagRequirement.isInTag(registry, key, tag));
    }

    @ParameterizedTest
    @CsvSource({
            "minecraft:stone, minecraft:logs",
            "minecraft:oak_log, c:stones",
            "minecraft:birch_log, minecraft:logs",
            "minecraft:oak_log, minecraft:planks",
            "$$block_key, minecraft:logs",
            "EMPTY, minecraft:logs",
            "minecraft:oak_log, '#'",
            "'', minecraft:logs",
            "minecraft:oak_log, ''"
    })
    void entryOutsideOfTagOrInvalidInputDoesNotMatch(String key, String tag) {
        assertFalse(IsInTagRequirement.isInTag(registry, key, tag));
    }

    @Test
    void parseReadsTypeTagAndKey() {
        IsInTagRequirement.TagCheck check = IsInTagRequirement.TagCheck.parse(" item |c:tools|minecraft:diamond_axe");
        assertNotNull(check);
        assertSame(IsInTagRequirement.TagType.ITEM, check.type());
        assertEquals("c:tools", check.tag());
        assertEquals("minecraft:diamond_axe", check.key());
    }

    @Test
    void parseKeepsSeparatorsInKey() {
        IsInTagRequirement.TagCheck check = IsInTagRequirement.TagCheck.parse("block|minecraft:logs|a|b");
        assertNotNull(check);
        assertEquals("a|b", check.key());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"block", "block|minecraft:logs", "entity|minecraft:logs|minecraft:pig"})
    void parseRejectsMalformedValues(String value) {
        assertNull(IsInTagRequirement.TagCheck.parse(value));
    }

    @Test
    void serializedValueParsesBackToSameCheck() {
        IsInTagRequirement.TagCheck check = new IsInTagRequirement.TagCheck(IsInTagRequirement.TagType.ITEM, "#minecraft:axes", "{\"placeholder\":\"some|value\"}");
        assertEquals(check, IsInTagRequirement.TagCheck.parse(check.serialize()));
    }

    @Test
    void presetIsAValidCheck() {
        assertNotNull(IsInTagRequirement.TagCheck.parse(new IsInTagRequirement().getValuePreset()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"block|minecraft:logs", "block|minecraft:logs|minecraft:not_a_block", "item|minecraft:axes|$$broke_with_item_key"})
    void requirementIsNotMetForMalformedOrUnknownValues(String value) {
        assertFalse(new IsInTagRequirement().isRequirementMet(value));
    }

    @Test
    void typesUseTheirBuiltInRegistries() {
        assertSame(BuiltInRegistries.BLOCK, IsInTagRequirement.TagType.BLOCK.getRegistry());
        assertSame(BuiltInRegistries.ITEM, IsInTagRequirement.TagType.ITEM.getRegistry());
    }

    @Test
    void keySuggestionsAreSortedRegistryKeys() {
        List<String> blockKeys = IsInTagRequirement.TagType.BLOCK.getKeySuggestions();
        List<String> itemKeys = IsInTagRequirement.TagType.ITEM.getKeySuggestions();
        assertTrue(blockKeys.contains("minecraft:oak_log"));
        assertFalse(blockKeys.contains("minecraft:diamond_axe"));
        assertTrue(itemKeys.contains("minecraft:diamond_axe"));
        assertEquals(blockKeys.stream().sorted().toList(), blockKeys);
        assertEquals(itemKeys.stream().sorted().toList(), itemKeys);
    }

    @Test
    void tagSuggestionsAreSorted() {
        for (IsInTagRequirement.TagType type : IsInTagRequirement.TagType.values()) {
            List<String> tags = new ArrayList<>(type.getTagSuggestions());
            assertEquals(tags.stream().sorted().toList(), tags);
        }
    }

}
