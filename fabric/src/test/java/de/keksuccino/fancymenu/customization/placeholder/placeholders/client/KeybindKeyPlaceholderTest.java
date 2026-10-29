package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Only binds mouse buttons, because the display names of keyboard keys get resolved by the native windowing library.
 */
class KeybindKeyPlaceholderTest {

    private static final String LEFT_BUTTON = "key.mouse.left";
    private static final String RIGHT_BUTTON = "key.mouse.right";

    @Test
    void returnsTheDisplayNameOfTheKeyBoundToTheKeybind() {
        KeyMapping jump = keyMapping("fancymenu.test.keybind_key.jump", LEFT_BUTTON);
        KeyMapping sneak = keyMapping("fancymenu.test.keybind_key.sneak", RIGHT_BUTTON);
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(() -> new KeyMapping[] { jump, sneak });

        assertNotEquals(displayName(LEFT_BUTTON), displayName(RIGHT_BUTTON));
        assertEquals(displayName(LEFT_BUTTON), replacement(placeholder, "fancymenu.test.keybind_key.jump"));
        assertEquals(displayName(RIGHT_BUTTON), replacement(placeholder, "fancymenu.test.keybind_key.sneak"));
    }

    @Test
    void followsRebindingOfTheKeybind() {
        KeyMapping attack = keyMapping("fancymenu.test.keybind_key.attack", LEFT_BUTTON);
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(() -> new KeyMapping[] { attack });

        assertEquals(displayName(LEFT_BUTTON), replacement(placeholder, "fancymenu.test.keybind_key.attack"));
        attack.setKey(InputConstants.getKey(RIGHT_BUTTON));
        assertEquals(displayName(RIGHT_BUTTON), replacement(placeholder, "fancymenu.test.keybind_key.attack"));
    }

    @Test
    void readsTheRegisteredKeybindsOnEveryReplacement() {
        KeyMapping lateKeybind = keyMapping("fancymenu.test.keybind_key.late", LEFT_BUTTON);
        AtomicReference<KeyMapping[]> registered = new AtomicReference<>(new KeyMapping[0]);
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(registered::get);

        assertEquals("", replacement(placeholder, "fancymenu.test.keybind_key.late"));
        registered.set(new KeyMapping[] { lateKeybind });
        assertEquals(displayName(LEFT_BUTTON), replacement(placeholder, "fancymenu.test.keybind_key.late"));
    }

    @Test
    void unknownKeybindReturnsEmptyText() {
        KeyMapping use = keyMapping("fancymenu.test.keybind_key.use", LEFT_BUTTON);
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(() -> new KeyMapping[] { use });

        assertEquals("", replacement(placeholder, "fancymenu.test.keybind_key.missing"));
        assertEquals("", replacement(placeholder, ""));
    }

    @Test
    void missingKeybindValueHasNoReplacement() {
        KeyMapping drop = keyMapping("fancymenu.test.keybind_key.drop", LEFT_BUTTON);
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(() -> new KeyMapping[] { drop });

        assertNull(placeholder.getReplacementFor(new DeserializedPlaceholderString(placeholder.getIdentifier(), new HashMap<>(), "")));
    }

    @Test
    void defaultPlaceholderStringProvidesAllValues() {
        KeybindKeyPlaceholder placeholder = new KeybindKeyPlaceholder(() -> new KeyMapping[0]);
        DeserializedPlaceholderString dps = placeholder.getDefaultPlaceholderString();

        assertEquals(placeholder.getIdentifier(), dps.placeholderIdentifier);
        assertEquals(placeholder.getValueNames(), new ArrayList<>(dps.values.keySet()));
    }

    private static KeyMapping keyMapping(String name, String boundKey) {
        return new KeyMapping(name, InputConstants.Type.MOUSE, InputConstants.getKey(boundKey).getValue(), KeyMapping.CATEGORY_MISC);
    }

    private static String displayName(String key) {
        return InputConstants.getKey(key).getDisplayName().getString();
    }

    private static String replacement(KeybindKeyPlaceholder placeholder, String keybind) {
        HashMap<String, String> values = new HashMap<>();
        values.put("keybind", keybind);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString(placeholder.getIdentifier(), values, ""));
    }

}
