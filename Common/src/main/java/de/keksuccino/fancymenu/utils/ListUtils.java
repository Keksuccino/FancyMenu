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

}
