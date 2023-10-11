package de.keksuccino.fancymenu.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils extends de.keksuccino.konkrete.math.MathUtils {

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
