package de.keksuccino.fancymenu.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KeyMappingUtilsTest {

    @Test
    void findsTheKeybindWithTheGivenIdentifier() {
        KeyMapping jump = keyMapping("fancymenu.test.key_mapping_utils.jump");
        KeyMapping sneak = keyMapping("fancymenu.test.key_mapping_utils.sneak");
        KeyMapping[] keyMappings = { jump, sneak };

        assertSame(jump, KeyMappingUtils.findKeyMapping(keyMappings, "fancymenu.test.key_mapping_utils.jump"));
        assertSame(sneak, KeyMappingUtils.findKeyMapping(keyMappings, "fancymenu.test.key_mapping_utils.sneak"));
    }

    @Test
    void ignoresWhitespaceAroundTheIdentifier() {
        KeyMapping attack = keyMapping("fancymenu.test.key_mapping_utils.attack");
        KeyMapping[] keyMappings = { attack };

        assertSame(attack, KeyMappingUtils.findKeyMapping(keyMappings, "  fancymenu.test.key_mapping_utils.attack\t"));
        assertSame(attack, KeyMappingUtils.findKeyMapping(keyMappings, "\nfancymenu.test.key_mapping_utils.attack "));
    }

    @Test
    void identifiersMustMatchExactly() {
        KeyMapping[] keyMappings = { keyMapping("fancymenu.test.key_mapping_utils.use") };

        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, "FANCYMENU.TEST.KEY_MAPPING_UTILS.USE"));
        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, "fancymenu.test.key_mapping_utils"));
        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, "fancymenu.test.key_mapping_utils.use.other"));
    }

    @Test
    void returnsNullIfNoKeybindMatches() {
        KeyMapping[] keyMappings = { keyMapping("fancymenu.test.key_mapping_utils.drop") };

        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, "fancymenu.test.key_mapping_utils.missing"));
        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, ""));
        assertNull(KeyMappingUtils.findKeyMapping(keyMappings, "   "));
        assertNull(KeyMappingUtils.findKeyMapping(new KeyMapping[0], "fancymenu.test.key_mapping_utils.drop"));
    }

    @Test
    void firstKeybindWithTheIdentifierWins() {
        KeyMapping first = keyMapping("fancymenu.test.key_mapping_utils.duplicate");
        KeyMapping second = keyMapping("fancymenu.test.key_mapping_utils.duplicate");

        assertSame(first, KeyMappingUtils.findKeyMapping(new KeyMapping[] { first, second }, "fancymenu.test.key_mapping_utils.duplicate"));
        assertSame(second, KeyMappingUtils.findKeyMapping(new KeyMapping[] { second, first }, "fancymenu.test.key_mapping_utils.duplicate"));
    }

    private static KeyMapping keyMapping(String name) {
        return new KeyMapping(name, InputConstants.Type.MOUSE, InputConstants.getKey("key.mouse.left").getValue(), KeyMapping.CATEGORY_MISC);
    }

}
