package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class ListUtils {

    /**
     * Moves the given object to the given index.<br>
     * Does nothing if the given object was not found in the given list.<br><br>
     *
     * If the given index is smaller than 0, the object gets moved to the beginning of the list.<br>
     * If the given index is bigger than list.size(), the object gets moved to the end of the list.
     */
    public static <T> void changeIndexOf(@NotNull List<T> list, @NotNull T object, int newIndex) {
        if (list.isEmpty()) return;
        if (newIndex < 0) newIndex = 0;
        if (newIndex > list.size()) newIndex = list.size();
        int currentIndex = list.indexOf(Objects.requireNonNull(object));
        if (currentIndex == -1) return;
        if (currentIndex == newIndex) return;
        list.remove(object);
        list.add(newIndex, object);
    }

    /**
     * Moves the given object to the given index offset.<br>
     * Does nothing if the given object was not found in the given list.<br><br>
     *
     * If the new index is smaller than 0, the object gets moved to the beginning of the list.<br>
     * If the new index is bigger than list.size(), the object gets moved to the end of the list.
     */
    public static <T> void offsetIndexOf(@NotNull List<T> list, @NotNull T object, int indexOffset) {
        int currentIndex = list.indexOf(Objects.requireNonNull(object));
        if (currentIndex == -1) return;
        int newIndex = currentIndex + indexOffset;
        changeIndexOf(list, object, newIndex);
    }

    @Nullable
    public static <T> T getLast(@NotNull List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(list.size()-1);
    }

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
    public static <T> boolean contentEqualIgnoreOrder(@NotNull List<T> list1, @NotNull List<T> list2) {
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
     *
     * @deprecated Use {@link List#removeIf(Predicate)} instead.
     */
    @NotNull
    @Deprecated
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
    @NotNull
    public static <T> List<T> of(T... entries) {
        return new ArrayList<>(Arrays.asList(entries));
    }

}
