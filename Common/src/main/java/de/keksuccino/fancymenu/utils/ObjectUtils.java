package de.keksuccino.fancymenu.utils;

import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ObjectUtils {

    public static boolean equalsAll(Object first, Object... others) {
        for (Object o : others) {
            if (!first.equals(o)) return false;
        }
        return true;
    }

    /**
     * Gets the same field/method value of all given objects and returns them as a list.
     */
    @NotNull
    public static <O, F> List<F> getOfAll(Class<? extends F> getType, List<O> objects, ConsumingSupplier<O, F> getter) {
        List<F> l = new ArrayList<>();
        for (O obj : objects) {
            l.add(getter.get(obj));
        }
        return l;
    }

}
