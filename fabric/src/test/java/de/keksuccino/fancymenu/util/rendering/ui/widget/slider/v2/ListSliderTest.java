package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListSliderTest {

    @Test
    void everyIndexReadsBackAsTheIndexThatGotSelected() {
        for (int size = 2; size <= 200; size++) {
            List<String> values = values(size);
            for (int index = 0; index < size; index++) {
                ListSlider<String> slider = slider(values, index);
                assertEquals(index, slider.getSelectedIndex(), "index " + index + " of " + size + " values");
                assertEquals(values.get(index), slider.getSelectedListValue());
            }
        }
    }

    @Test
    void draggingKeepsTheIndexRangesOfTheTruncatedValue() {
        ListSlider<String> slider = slider(List.of("a", "b", "c"), 0);

        slider.setValue(0.49D);
        int justBelowHalf = slider.getSelectedIndex();
        slider.setValue(0.5D);
        int atHalf = slider.getSelectedIndex();
        slider.setValue(0.99D);
        int justBelowEnd = slider.getSelectedIndex();
        slider.setValue(1.0D);
        int atEnd = slider.getSelectedIndex();

        assertAll(() -> assertEquals(0, justBelowHalf), () -> assertEquals(1, atHalf), () -> assertEquals(1, justBelowEnd), () -> assertEquals(2, atEnd));
    }

    @Test
    void replacingValuesKeepsTheSelectedValueWhenItMoved() {
        ListSlider<String> slider = slider(List.of("apple", "banana", "cherry"), 2);

        slider.replaceListValues(List.of("kiwi", "cherry", "apple", "banana"));

        assertAll(() -> assertEquals("cherry", slider.getSelectedListValue()), () -> assertEquals(1, slider.getSelectedIndex()));
    }

    @Test
    void replacingValuesKeepsTheIndexWhenTheValueIsGone() {
        ListSlider<String> slider = slider(List.of("a", "b", "c", "d"), 2);

        slider.replaceListValues(List.of("a", "b", "x", "y"));

        assertAll(() -> assertEquals(2, slider.getSelectedIndex()), () -> assertEquals("x", slider.getSelectedListValue()));
    }

    @Test
    void replacingWithFewerValuesSelectsTheLastOneWhenTheIndexIsGone() {
        ListSlider<String> slider = slider(List.of("a", "b", "c", "d"), 3);

        slider.replaceListValues(List.of("x", "y"));

        assertAll(() -> assertEquals(1, slider.getSelectedIndex()), () -> assertEquals("y", slider.getSelectedListValue()));
    }

    @Test
    void replacingValuesKeepsTheSelectedOneOfEqualValues() {
        ListSlider<String> slider = slider(List.of("a", "b", "a"), 2);

        slider.replaceListValues(List.of("a", "b", "a", "c"));

        assertEquals(2, slider.getSelectedIndex());
    }

    @Test
    void replacingValuesKeepsTheSelectionOfLongListsExact() {
        List<String> values = values(60);
        ListSlider<String> slider = slider(values, 1);
        List<String> withNewFirstValue = new ArrayList<>(values);
        withNewFirstValue.add(0, "new");

        slider.replaceListValues(withNewFirstValue);

        assertAll(() -> assertEquals(2, slider.getSelectedIndex()), () -> assertEquals(values.get(1), slider.getSelectedListValue()));
    }

    @Test
    void replacingValuesDoesNotNotifyTheValueUpdateListener() {
        ListSlider<String> slider = slider(List.of("a", "b", "c"), 1);
        AtomicInteger updates = new AtomicInteger();
        slider.setSliderValueUpdateListener((s, valueDisplayText, value) -> updates.incrementAndGet());

        slider.replaceListValues(List.of("b", "c"));
        slider.replaceListValues(List.of("x", "y", "z"));

        assertEquals(0, updates.get());
    }

    @Test
    void replacingValuesUpdatesTheLabel() {
        ListSlider<String> slider = slider(List.of("a", "b"), 0);

        slider.replaceListValues(List.of("x", "y"));

        assertEquals("x", slider.getMessage().getString());
    }

    @Test
    void needsAtLeastTwoValues() {
        ListSlider<String> slider = slider(List.of("a", "b"), 0);

        assertAll(() -> assertThrows(RuntimeException.class, () -> slider.replaceListValues(List.of("a"))), () -> assertThrows(RuntimeException.class, () -> slider(List.of("a"), 0)));
    }

    private static ListSlider<String> slider(List<String> values, int selectedIndex) {
        return new ListSlider<>(0, 0, 100, 20, Component.empty(), values, selectedIndex);
    }

    private static List<String> values(int size) {
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add("value_" + i);
        return values;
    }

}
