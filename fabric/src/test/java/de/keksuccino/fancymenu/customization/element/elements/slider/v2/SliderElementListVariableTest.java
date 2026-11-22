package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Sliders of these tests are range sliders, because list sliders with a list variable read FancyMenu's global variables,
 * which tests must not touch. The list variable gets saved and loaded the same way for every slider type.
 */
class SliderElementListVariableTest {

    // Keeps the real serializer and deserializer, and only skips sizing the slider widget after construction, which needs the mixins of the game
    private static final SliderElementBuilder BUILDER = new SliderElementBuilder() {
        @Override
        public @NotNull SliderElement buildDefaultInstance() {
            return new SliderElement(this) {
                @Override
                public void afterConstruction() {
                }
            };
        }
    };
    private static Object previousMinecraft;

    /**
     * Elements ask the game for the current screen while they get built. A {@link Minecraft} without constructor has no GUI yet, so they get none.
     */
    @BeforeAll
    static void installMinecraftWithoutScreen() throws ReflectiveOperationException {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Minecraft minecraft = (Minecraft) ((Unsafe) unsafeField.get(null)).allocateInstance(Minecraft.class);
        Field instanceField = minecraftInstanceField();
        previousMinecraft = instanceField.get(null);
        instanceField.set(null, minecraft);
    }

    @AfterAll
    static void restoreMinecraft() throws ReflectiveOperationException {
        minecraftInstanceField().set(null, previousMinecraft);
    }

    @Test
    void listVariableGetsSavedAndLoaded() {
        SliderElement element = BUILDER.buildDefaultInstance();
        element.listVariable.set("fruits");

        SerializedElement serialized = BUILDER.serializeElementInternal(element);
        assertNotNull(serialized);
        SliderElement loaded = BUILDER.deserializeElementInternal(serialized);

        assertAll(() -> assertEquals("fruits", serialized.getValue("list_variable")), () -> assertNotNull(loaded), () -> assertEquals("fruits", loaded.getListVariableName()));
    }

    @Test
    void slidersWithoutListVariableSaveNoneAndKeepTheirListValues() {
        SliderElement element = BUILDER.buildDefaultInstance();
        element.listValues = List.of("first", "second");

        SerializedElement serialized = BUILDER.serializeElementInternal(element);
        assertNotNull(serialized);
        SliderElement loaded = BUILDER.deserializeElementInternal(serialized);

        assertAll(() -> assertNull(serialized.getValue("list_variable")), () -> assertNotNull(loaded), () -> assertNull(loaded.getListVariableName()), () -> assertEquals(List.of("first", "second"), loaded.listValues));
    }

    @Test
    void listVariableNamesGetTrimmedAndBlankNamesMeanNoListVariable() {
        SliderElement element = BUILDER.buildDefaultInstance();

        element.listVariable.set("  fruits ");
        String trimmed = element.getListVariableName();
        element.listVariable.set("   ");
        String blank = element.getListVariableName();

        assertAll(() -> assertEquals("fruits", trimmed), () -> assertNull(blank));
    }

    @Test
    void listsWithEnoughEntriesBecomeTheSliderValuesAsTheyAre() {
        List<String> entries = List.of("apple", "banana");

        assertSame(entries, SliderElement.toListSliderValues(entries));
    }

    @Test
    void tooShortOrMissingListsGetFilledUpToTwoValues() {
        List<String> single = SliderElement.toListSliderValues(List.of("apple"));
        List<String> empty = SliderElement.toListSliderValues(List.of());
        List<String> missing = SliderElement.toListSliderValues(null);

        assertAll(() -> assertEquals(2, single.size()), () -> assertEquals("apple", single.get(0)), () -> assertEquals(2, empty.size()), () -> assertEquals(empty, missing));
    }

    private static Field minecraftInstanceField() throws NoSuchFieldException {
        Field instanceField = Minecraft.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        return instanceField;
    }

}
