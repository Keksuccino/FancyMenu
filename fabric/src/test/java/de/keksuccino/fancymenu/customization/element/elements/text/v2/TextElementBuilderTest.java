package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollDirection;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSettings;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSnapMode;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextElementBuilderTest {

    /** Loads the scroll settings of a text element from what a layout saved for it. */
    private static ScrollSettings load(SerializedElement saved) {
        TextElementBuilder.upgradeLegacyScrolling(saved);
        TestElement element = new TestElement();
        ScrollSettings settings = new ScrollSettings(element, "fancymenu.elements.text", ScrollDirection.BOTH, EnumSet.allOf(ScrollSnapMode.class));
        for (Property<?> property : element.getProperties()) {
            property.deserialize(saved);
        }
        return settings;
    }

    @Test
    void aTextThatGotSavedWithScrollingSwitchedOffCanNotBeScrolledByPlayers() {
        SerializedElement saved = new SerializedElement();
        saved.putProperty("enable_scrolling", "false");
        assertFalse(load(saved).userScrolling.getBoolean());
    }

    @Test
    void aTextThatGotSavedWithScrollingSwitchedOnCanBeScrolledByPlayers() {
        SerializedElement saved = new SerializedElement();
        saved.putProperty("enable_scrolling", "true");
        assertTrue(load(saved).userScrolling.getBoolean());
        assertTrue(load(new SerializedElement()).userScrolling.getBoolean());
    }

    @Test
    void whatTheTextGotSavedWithSinceThenWinsOverItsOldSwitch() {
        SerializedElement saved = new SerializedElement();
        saved.putProperty("enable_scrolling", "false");
        saved.putProperty("user_scrolling", "true");
        assertTrue(load(saved).userScrolling.getBoolean());
    }

    private static final class TestElement implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();

        @Override
        public @NotNull Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

    }

}
