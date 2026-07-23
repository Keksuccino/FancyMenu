package de.keksuccino.fancymenu.util.resource;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class PackResourcesRootEnumeration {

    private PackResourcesRootEnumeration() {}

    @NotNull
    public static String normalizeArchivePrefix(@NotNull String prefix, @NotNull String directory) {
        if (!directory.isEmpty() || !prefix.endsWith("//")) return prefix;
        return prefix.substring(0, prefix.length() - 1);
    }

    @NotNull
    public static String normalizeFolderPrefix(@NotNull String prefix, @NotNull String directory) {
        return directory.isEmpty() && prefix.equals("/") ? "" : prefix;
    }

}
