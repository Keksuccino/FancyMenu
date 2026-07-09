package de.keksuccino.fancymenu.util.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NbtNumericValueFormatter {

    public static final double DEFAULT_SCALE = 1.0D;

    private NbtNumericValueFormatter() {
    }

    public static double parseScale(@Nullable String scaleString) {
        Double scale = parseOptionalScale(scaleString);
        return scale != null ? scale : DEFAULT_SCALE;
    }

    @Nullable
    public static Double parseOptionalScale(@Nullable String scaleString) {
        if (scaleString == null || scaleString.isBlank()) return null;
        try {
            double scale = Double.parseDouble(scaleString.trim());
            return Double.isFinite(scale) ? scale : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Formats a scaled numeric NBT value while retaining the source tag's SNBT type suffix.
     * Unit scale uses the tag's canonical SNBT representation so exact long values never pass through a double.
     * In Minecraft 1.19.2, {@link NumericTag#getAsString()} produces the same representation through
     * {@code StringTagVisitor}; using {@link NumericTag#toString()} here makes the no-conversion requirement explicit.
     * Non-finite scales are treated as the default unit scale so they cannot break packet JSON serialization or
     * produce surprising wrapped integral values.
     */
    @NotNull
    public static String format(@NotNull NumericTag tag, double scale) {
        if (!Double.isFinite(scale) || scale == DEFAULT_SCALE) {
            return tag.toString();
        }

        double scaled = tag.getAsDouble() * scale;
        if (tag instanceof FloatTag) {
            return Float.toString((float) scaled) + "f";
        }
        if (tag instanceof DoubleTag) {
            return Double.toString(scaled) + "d";
        }

        long rounded = Math.round(scaled);
        if (tag instanceof ByteTag) {
            return Byte.toString((byte) rounded) + "b";
        }
        if (tag instanceof ShortTag) {
            return Short.toString((short) rounded) + "s";
        }
        if (tag instanceof IntTag) {
            return Integer.toString((int) rounded);
        }
        if (tag instanceof LongTag) {
            return Long.toString(rounded) + "L";
        }
        return Long.toString(rounded);
    }

}
