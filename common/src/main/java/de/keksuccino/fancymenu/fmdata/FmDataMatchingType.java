package de.keksuccino.fancymenu.fmdata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum FmDataMatchingType {

    EQUALS("equals"),
    CONTAINS("contains"),
    STARTS_WITH("starts_with"),
    ENDS_WITH("ends_with");

    private final String identifier;

    FmDataMatchingType(@NotNull String identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public static List<String> identifiers() {
        return Arrays.stream(values()).map(FmDataMatchingType::getIdentifier).toList();
    }

    @Nullable
    public static FmDataMatchingType fromIdentifier(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (FmDataMatchingType type : values()) {
            if (type.identifier.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    public boolean matches(@Nullable String incomingValue, @Nullable String expectedValue, boolean ignoreCase) {
        String expected = Objects.requireNonNullElse(expectedValue, "");
        if ("*".equals(expected)) {
            return true;
        }

        String incoming = Objects.requireNonNullElse(incomingValue, "");
        if (ignoreCase) {
            incoming = incoming.toLowerCase(Locale.ROOT);
            expected = expected.toLowerCase(Locale.ROOT);
        }

        return switch (this) {
            case EQUALS -> incoming.equals(expected);
            case CONTAINS -> incoming.contains(expected);
            case STARTS_WITH -> incoming.startsWith(expected);
            case ENDS_WITH -> incoming.endsWith(expected);
        };
    }

}
