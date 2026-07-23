package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Utf8LengthTest {

    @Test
    void countsUtf8BytesWithoutReplacingValidCharacters() {
        assertEquals(0L, Utf8Length.count(""));
        assertEquals(1L, Utf8Length.count("A"));
        assertEquals(2L, Utf8Length.count("é"));
        assertEquals(3L, Utf8Length.count("€"));
        assertEquals(4L, Utf8Length.count("😀"));
        assertEquals(10L, Utf8Length.count("Aé€😀"));
    }

    @Test
    void rejectsIsolatedSurrogates() {
        assertEquals(Utf8Length.MALFORMED_UTF16, Utf8Length.count("\uD83D"));
        assertEquals(Utf8Length.MALFORMED_UTF16, Utf8Length.count("\uDE00"));
        assertEquals(Utf8Length.MALFORMED_UTF16, Utf8Length.count("\uD83Dx"));
        assertEquals(Utf8Length.MALFORMED_UTF16, Utf8Length.count("x\uDE00"));
    }
}
