package de.keksuccino.fancymenu.util.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
     * Unit scale must use {@link NumericTag#toString()} because numeric tags intentionally do not implement
     * {@link NumericTag#asString()} in current Minecraft versions. It also preserves long values exactly instead
     * of converting them through a double. Non-finite scales are treated as the default unit scale so they cannot
     * create invalid packet JSON or surprising wrapped integral values.
     */
    @Nonnull
    public static String format(@Nonnull NumericTag tag, double scale) {
        if (!Double.isFinite(scale) || scale == DEFAULT_SCALE) {
            return tag.toString();
        }

        double scaled = tag.doubleValue() * scale;
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
