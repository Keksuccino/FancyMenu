package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Replaces placeholders in raw values like URLs, file paths and resource sources.
 *
 * <p>Raw values are not formatted text, so they must not use the default parse, which converts "&" formatting codes to
 * "§" and would turn URL parameters like "&embed" into "§embed". The formatting-preserving parse bypasses the placeholder
 * cache, so it only runs when the cached default parse of the value changes. That keeps per-frame calls as cheap as a
 * normal cached parse. Because the default parse is the change check, a placeholder result that only switches between
 * an "&" code and the matching "§" code is not seen as a change.</p>
 *
 * <p>Instances can be shared between threads: every resolution gets published as one immutable snapshot.</p>
 */
public final class RawValuePlaceholderResolver {

    @Nullable
    private volatile Resolution lastResolution;

    @NotNull
    public String resolve(@NotNull String rawValue) {
        Objects.requireNonNull(rawValue);
        String parsedValue = PlaceholderParser.replacePlaceholders(rawValue);
        Resolution last = this.lastResolution;
        if ((last != null) && last.parsedValue().equals(parsedValue) && last.rawValue().equals(rawValue)) return last.resolvedValue();
        Resolution resolution = new Resolution(rawValue, parsedValue, PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(rawValue));
        this.lastResolution = resolution;
        return resolution.resolvedValue();
    }

    private record Resolution(@NotNull String rawValue, @NotNull String parsedValue, @NotNull String resolvedValue) {
    }

}
