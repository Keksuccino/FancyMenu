package de.keksuccino.fancymenu.util.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//TODO maybe trash this..
/**
 * When enums just aren't enough!
 */
public abstract class ConstantCollection {

    private static final Map<Class<?>, List<ConstantCollection>> COLLECTIONS = new HashMap<>();

    protected ConstantCollection() {
    }

    @NotNull
    protected static <T extends ConstantCollection> T putEntry(T entry) {
        registerCollection(entry.getClass());
        Objects.requireNonNull(getGenericCollection(entry.getClass())).add(entry);
        return entry;
    }

    @SuppressWarnings("all")
    @NotNull
    public static <T extends ConstantCollection> List<T> getCollection(@NotNull Class<T> collectionType) {
        List<ConstantCollection> collection = getGenericCollection(collectionType);
        if (collection != null) {
            return (List<T>) collection;
        }
        return new ArrayList<>();
    }

    @Nullable
    public static List<ConstantCollection> getGenericCollection(@NotNull Class<?> collectionType) {
        try {
            if (COLLECTIONS.containsKey(collectionType)) {
                return new ArrayList<>(COLLECTIONS.get(collectionType));
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static void registerCollection(@NotNull Class<? extends ConstantCollection> collectionType) {
        if (!COLLECTIONS.containsKey(collectionType)) {
            COLLECTIONS.put(collectionType, new ArrayList<>());
        }
    }

}
