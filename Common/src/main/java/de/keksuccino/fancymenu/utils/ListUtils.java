package de.keksuccino.fancymenu.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

    @SafeVarargs
    public static <T> List<T> mergeLists(@NotNull List<T>... lists) {
        List<T> l = new ArrayList<>();
        for (List<T> list : lists) {
            l.addAll(list);
        }
        return l;
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

}
