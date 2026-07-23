package de.keksuccino.fancymenu.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils extends de.keksuccino.konkrete.math.MathUtils {

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        if (!Double.isFinite(value)) return value;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Formats finite whole-number doubles as plain integer text without narrowing them to the range of a {@code long}.
     * Non-finite and fractional values retain Java's canonical double representation so callers do not silently change their semantics.
     */
    public static String formatWholeNumber(double value) {
        if (!Double.isFinite(value) || value != Math.rint(value)) return Double.toString(value);
        // The exact double constructor is intentional; valueOf would round large integers through Double.toString before formatting them.
        return new BigDecimal(value).toBigIntegerExact().toString();
    }

}
