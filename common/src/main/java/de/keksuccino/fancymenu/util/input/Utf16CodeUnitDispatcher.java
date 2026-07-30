package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.CharacterEvent;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Adapts one Unicode code point to consumers that accept UTF-16 code units.
 *
 * <p>GLFW supplies one code point per character callback, while legacy consumers such as Rinku accept a Java
 * {@code char}. Every code unit is dispatched even when an earlier unit was handled; otherwise a handled high
 * surrogate could truncate a supplementary character. The return value is the logical OR of every handler result,
 * and the caller-provided modifier value is forwarded unchanged with each unit.</p>
 *
 * <p>GLFW does not emit malformed values. Integers outside the Unicode range are rejected explicitly, while an
 * isolated surrogate value from a manually constructed event is preserved as one UTF-16 unit.</p>
 */
public final class Utf16CodeUnitDispatcher {

    private Utf16CodeUnitDispatcher() {
    }

    /** Reuses the original event for BMP input and allocates derived events only for supplementary code points. */
    public static boolean dispatch(@Nonnull CharacterEvent event, @Nonnull Predicate<CharacterEvent> handler) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(handler, "handler");

        int codePoint = event.codepoint();
        validateCodePoint(codePoint);
        if (Character.charCount(codePoint) == 1) {
            return handler.test(event);
        }
        return dispatch(codePoint, 0, (codeUnit, modifiers) -> handler.test(new CharacterEvent(codeUnit)));
    }

    public static boolean dispatch(int codePoint, int modifiers, @Nonnull Handler handler) {
        Objects.requireNonNull(handler, "handler");
        validateCodePoint(codePoint);
        if (Character.charCount(codePoint) == 1) {
            return handler.handle((char)codePoint, modifiers);
        }

        boolean highSurrogateHandled = handler.handle(Character.highSurrogate(codePoint), modifiers);
        boolean lowSurrogateHandled = handler.handle(Character.lowSurrogate(codePoint), modifiers);
        return highSurrogateHandled || lowSurrogateHandled;
    }

    private static void validateCodePoint(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) throw new IllegalArgumentException("Code point is outside the Unicode range: " + codePoint);
    }

    @FunctionalInterface
    public interface Handler {

        boolean handle(char codeUnit, int modifiers);

    }

}
