package de.keksuccino.fancymenu.utils;

import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            if (!obj.equals(first)) return false;
        }
        return true;
    }

    public static boolean contentEquals(List<?> list1, List<?> list2) {
        if (list1.size() != list2.size()) return false;
        for (Object o : list1) {
            boolean b = false;
            for (Object o2 : list2) {
                if (o.equals(o2)) {
                    b = true;
                    break;
                }
            }
            if (!b) return false;
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
