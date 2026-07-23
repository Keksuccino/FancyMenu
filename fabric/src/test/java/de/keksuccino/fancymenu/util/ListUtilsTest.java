package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListUtilsTest {

    @Test
    void equalListsMatch() {
        assertTrue(ListUtils.contentEqualIgnoreOrder(List.of("alpha", "beta"), List.of("alpha", "beta")));
    }

    @Test
    void reorderedListsMatch() {
        assertTrue(ListUtils.contentEqualIgnoreOrder(List.of("alpha", "beta", "gamma"), List.of("gamma", "alpha", "beta")));
    }

    @Test
    void duplicateOccurrencesAreMatchedExactlyOnce() {
        assertTrue(ListUtils.contentEqualIgnoreOrder(List.of("alpha", "alpha", "beta"), List.of("beta", "alpha", "alpha")));
        assertFalse(ListUtils.contentEqualIgnoreOrder(List.of("alpha", "alpha", "beta"), List.of("alpha", "beta", "beta")));
    }

    @Test
    void nullElementsAreMatchedExactlyOnce() {
        assertTrue(ListUtils.contentEqualIgnoreOrder(Arrays.asList(null, "alpha", null), Arrays.asList(null, null, "alpha")));
        assertFalse(ListUtils.contentEqualIgnoreOrder(Arrays.asList(null, null, "alpha"), Arrays.asList(null, "alpha", "alpha")));
    }

    @Test
    void emptyListsMatch() {
        assertTrue(ListUtils.contentEqualIgnoreOrder(List.of(), List.of()));
    }

    @Test
    void listsWithDifferentElementsDoNotMatch() {
        assertFalse(ListUtils.contentEqualIgnoreOrder(List.of("alpha", "beta"), List.of("alpha", "gamma")));
    }

    @Test
    void listsWithDifferentSizesDoNotMatch() {
        assertFalse(ListUtils.contentEqualIgnoreOrder(List.of("alpha"), List.of("alpha", "alpha")));
    }

    @Test
    void nullListsAreRejected() {
        assertThrows(NullPointerException.class, () -> ListUtils.contentEqualIgnoreOrder(null, List.of()));
        assertThrows(NullPointerException.class, () -> ListUtils.contentEqualIgnoreOrder(List.of(), null));
    }

    @Test
    void comparisonDoesNotDependOnHashCodesOrConcreteElementTypes() {
        List<Object> first = List.of(new LeftValue("alpha"), new RightValue("beta"));
        List<Object> second = List.of(new LeftValue("beta"), new RightValue("alpha"));

        assertTrue(ListUtils.contentEqualIgnoreOrder(first, second));
    }

    @Test
    void inputListsAreNotModified() {
        List<String> first = new ArrayList<>(List.of("alpha", "alpha", "beta"));
        List<String> second = new ArrayList<>(List.of("beta", "alpha", "alpha"));
        List<String> originalFirst = List.copyOf(first);
        List<String> originalSecond = List.copyOf(second);

        assertTrue(ListUtils.contentEqualIgnoreOrder(first, second));
        assertTrue(first.equals(originalFirst));
        assertTrue(second.equals(originalSecond));
    }

    private interface NamedValue {

        String name();

    }

    private static final class LeftValue implements NamedValue {

        private final String name;

        private LeftValue(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof NamedValue value) && this.name.equals(value.name());
        }

    }

    private static final class RightValue implements NamedValue {

        private final String name;

        private RightValue(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof NamedValue value) && this.name.equals(value.name());
        }

    }

}
