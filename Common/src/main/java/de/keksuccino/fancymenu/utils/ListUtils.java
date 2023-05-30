package de.keksuccino.fancymenu.utils;

import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class ListUtils {

    @SafeVarargs
    public static <T> List<T> mergeLists(@NotNull List<T>... lists) {
        List<T> l = new ArrayList<>();
        for (List<T> list : lists) {
            l.addAll(list);
        }
        return l;
    }

    public static boolean allInListEqual(@NotNull List<?> list) {
        if (list.size() < 2) {
            return true;
        }
        Object first = list.get(0);
        for (Object obj : list.subList(1, list.size())) {
            if (!Objects.equals(obj, first)) return false;
        }
        return true;
    }

    /**
     * Checks if the elements of both lists are equal, but ignores their order.
     */
    public static <T> boolean contentEqual(@NotNull List<T> list1, @NotNull List<T> list2) {
        Objects.requireNonNull(list1);
        Objects.requireNonNull(list2);
        if (list1.size() != list2.size()) {
            return false;
        }
        if (list1 == list2) {
            return true;
        }
        for (T obj1 : list1) {
            boolean foundMatch = false;
            for (T obj2 : list2) {
                if (obj1.equals(obj2)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }
        return true;
    }

    /**
     * Filters the given list and returns it.<br>
     * The filter checks every entry of the given list and if it returns FALSE, the entry will get REMOVED from the list.
     */
    @NotNull
    public static <T> List<T> filterList(@NotNull List<T> listToFilter, @NotNull ConsumingSupplier<T, Boolean> filter) {
        List<T> l = new ArrayList<>();
        for (T object : listToFilter) {
            if (filter.get(object)) l.add(object);
        }
        listToFilter.clear();
        listToFilter.addAll(l);
        return listToFilter;
    }

    @SafeVarargs
    public static <T> List<T> build(T... entries) {
        return new ArrayList<>(Arrays.asList(entries));
    }

}
