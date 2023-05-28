package de.keksuccino.fancymenu.utils;

import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ObjectUtils {

    public static <T> boolean isTrueForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        for (T object : objects) {
            if (!checkFor.get(object)) return false;
        }
        return true;
    }

    public static <T> boolean isFalseForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        for (T object : objects) {
            if (checkFor.get(object)) return false;
        }
        return true;
    }

    public static <T> boolean isTrueOrFalseForAll(List<T> objects, ConsumingSupplier<T, Boolean> checkFor) {
        return isTrueForAll(objects, checkFor) || isFalseForAll(objects, checkFor);
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

    @NotNull
    public static List<Object> getOfAllUnsafe(List<Object> objects, ConsumingSupplier<Object, Object> getter) {
        return getOfAll(Object.class, objects, getter);
    }

}
