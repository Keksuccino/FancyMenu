package de.keksuccino.fancymenu.customization.element.scrolling;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollSettingsTest {

    private static final Set<ScrollSnapMode> TEXT_SNAP_MODES = EnumSet.of(ScrollSnapMode.NONE, ScrollSnapMode.LINES, ScrollSnapMode.HEADLINES, ScrollSnapMode.PAGES);
    private static final Set<ScrollSnapMode> ELEMENT_SNAP_MODES = EnumSet.of(ScrollSnapMode.NONE, ScrollSnapMode.ELEMENTS, ScrollSnapMode.PAGES);

    private static ScrollSettings settings(TestElement element, ScrollDirection defaultDirection, Set<ScrollSnapMode> snapModes) {
        return new ScrollSettings(element, "fancymenu.elements.text", defaultDirection, snapModes);
    }

    @Test
    void everySettingHasANameInTheEnglishLanguageFile() throws IOException {
        JsonObject english;
        try (InputStream stream = ScrollSettingsTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            english = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        TestElement element = new TestElement();
        settings(element, ScrollDirection.BOTH, TEXT_SNAP_MODES);
        for (Property<?> property : element.getProperties()) {
            assertTrue(english.has(property.getContextMenuEntryLocalizationKeyBase()), property::getKey);
        }
        for (ScrollSnapMode mode : ScrollSnapMode.values()) {
            assertTrue(english.has(mode.getLocalizationKey()), mode::name);
        }
    }

    @Test
    void anElementScrollsInItsOwnDirectionUntilItGetsAnotherOne() {
        assertEquals(ScrollDirection.BOTH, settings(new TestElement(), ScrollDirection.BOTH, TEXT_SNAP_MODES).scrollDirection.get());
        assertEquals(ScrollDirection.VERTICAL, settings(new TestElement(), ScrollDirection.VERTICAL, ELEMENT_SNAP_MODES).scrollDirection.get());

        // What got saved wins over what an element starts with
        TestElement element = new TestElement();
        ScrollSettings settings = settings(element, ScrollDirection.BOTH, TEXT_SNAP_MODES);
        settings.scrollDirection.set(ScrollDirection.HORIZONTAL);
        SerializedElement saved = new SerializedElement();
        for (Property<?> property : element.getProperties()) {
            property.serialize(saved);
        }
        TestElement loadedElement = new TestElement();
        ScrollSettings loaded = settings(loadedElement, ScrollDirection.BOTH, TEXT_SNAP_MODES);
        for (Property<?> property : loadedElement.getProperties()) {
            property.deserialize(saved);
        }
        assertEquals(ScrollDirection.HORIZONTAL, loaded.scrollDirection.get());
    }

    @Test
    void goingThroughTheSnapModesSkipsTheOnesAnElementDoesNotHave() {
        ScrollSettings text = settings(new TestElement(), ScrollDirection.BOTH, TEXT_SNAP_MODES);
        // Setting a mode the element does not have makes it the next one it has, which is what going through them one by one does
        text.snapMode.set(ScrollSnapMode.ELEMENTS);
        assertEquals(ScrollSnapMode.LINES, text.getSnapMode());
        text.snapMode.set(ScrollSnapMode.HEADLINES);
        assertEquals(ScrollSnapMode.HEADLINES, text.getSnapMode());

        ScrollSettings area = settings(new TestElement(), ScrollDirection.VERTICAL, ELEMENT_SNAP_MODES);
        area.snapMode.set(ScrollSnapMode.ELEMENTS);
        assertEquals(ScrollSnapMode.ELEMENTS, area.getSnapMode());
        area.snapMode.set(ScrollSnapMode.LINES);
        assertEquals(ScrollSnapMode.PAGES, area.getSnapMode());
        area.snapMode.set(ScrollSnapMode.HEADLINES);
        assertEquals(ScrollSnapMode.PAGES, area.getSnapMode());
    }

    @Test
    void everyModeAnElementHasCanBeReachedByGoingThroughThemOneByOne() {
        for (Set<ScrollSnapMode> supported : Set.of(TEXT_SNAP_MODES, ELEMENT_SNAP_MODES)) {
            ScrollSettings settings = settings(new TestElement(), ScrollDirection.BOTH, supported);
            Set<ScrollSnapMode> reached = EnumSet.noneOf(ScrollSnapMode.class);
            ScrollSnapMode[] values = ScrollSnapMode.values();
            for (int i = 0; i < (values.length * 2); i++) {
                ScrollSnapMode current = settings.getSnapMode();
                reached.add(current);
                settings.snapMode.set(values[(current.ordinal() + 1) % values.length]);
            }
            assertEquals(supported, reached);
        }
    }

    @Test
    void aSavedModeTheElementDoesNotHaveNeverGetsLoaded() {
        ScrollSettings settings = settings(new TestElement(), ScrollDirection.VERTICAL, ELEMENT_SNAP_MODES);
        SerializedElement saved = new SerializedElement();
        saved.putProperty(ScrollSnapMode.PROPERTY_KEY, "headlines");
        settings.snapMode.deserialize(saved);
        assertTrue(ELEMENT_SNAP_MODES.contains(settings.getSnapMode()));

        // Something that is no mode at all is none
        saved.putProperty(ScrollSnapMode.PROPERTY_KEY, "nonsense");
        settings.snapMode.deserialize(saved);
        assertEquals(ScrollSnapMode.NONE, settings.getSnapMode());
    }

    private static final class TestElement implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();

        @Override
        public @NotNull Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

    }

}
