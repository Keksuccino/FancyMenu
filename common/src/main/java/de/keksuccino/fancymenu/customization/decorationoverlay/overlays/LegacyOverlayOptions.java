package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries options of older overlay versions over to the shared overlay options, for builders that load layouts saved before an overlay
 * got its {@link DecorationOverlayOptions.Amount Amount} or {@link DecorationOverlayOptions.Size Size} option.<br>
 * A legacy value only counts while the layout has no value for the new option yet, so a layout saved with the new option keeps it.
 * Call these from the builder's {@code deserialize}, which runs after all properties got their saved values.
 */
public final class LegacyOverlayOptions {

    private LegacyOverlayOptions() {
    }

    /**
     * Reads a number an older version saved under a key that is gone now. Placeholders in it get resolved once, at load time.
     *
     * @return the number, or NULL if the key is missing or holds no finite number
     */
    @Nullable
    public static Float readFloat(@NotNull PropertyContainer from, @NotNull String legacyKey) {
        String raw = from.getValue(legacyKey);
        if (raw == null) return null;
        Float value = parseFloat(raw);
        if ((value == null) && raw.contains("{")) value = parseFloat(PlaceholderParser.replacePlaceholders(raw));
        return value;
    }

    @Nullable
    private static Float parseFloat(@NotNull String text) {
        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Whether the layout has no value for a key yet, which is the case for options that got added after the layout was saved.
     */
    public static boolean isMissing(@NotNull PropertyContainer from, @NotNull String key) {
        return from.getValue(key) == null;
    }

    /**
     * Turns a legacy multiplier (1.0 is the normal amount) into the Amount option, if the layout has no Amount value yet.
     */
    public static void migrateAmount(@NotNull PropertyContainer from, @NotNull DecorationOverlayOptions.Amount amount, @NotNull String legacyKey) {
        if (!isMissing(from, amount.property.getKey())) return;
        Float legacy = readFloat(from, legacyKey);
        if (legacy != null) amount.property.set(toAmountPercent(legacy));
    }

    /**
     * Turns a legacy scale (1.0 is the normal size) into the Size option, if the layout has no Size value yet.
     */
    public static void migrateSize(@NotNull PropertyContainer from, @NotNull DecorationOverlayOptions.Size size, @NotNull String legacyKey) {
        if (!isMissing(from, size.property.getKey())) return;
        Float legacy = readFloat(from, legacyKey);
        if (legacy != null) size.property.set(toSize(legacy));
    }

    /**
     * A legacy multiplier as Amount percentage, clamped to the range of the Amount option.
     */
    public static int toAmountPercent(float multiplier) {
        int percent = Math.round(multiplier * 100.0F);
        return Math.max(DecorationOverlayOptions.Amount.MIN_PERCENT, Math.min(DecorationOverlayOptions.Amount.MAX_PERCENT, percent));
    }

    /**
     * A legacy free scale as the nearest whole Size factor, clamped to the range of the Size option.
     */
    public static int toSize(float scale) {
        int size = Math.round(scale);
        return Math.max(DecorationOverlayOptions.Size.MIN, Math.min(DecorationOverlayOptions.Size.MAX, size));
    }

}
