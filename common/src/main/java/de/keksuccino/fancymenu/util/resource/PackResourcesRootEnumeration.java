package de.keksuccino.fancymenu.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public final class PackResourcesRootEnumeration {

    private PackResourcesRootEnumeration() {}

    @NotNull
    public static String normalizeArchivePrefix(@NotNull String prefix, @NotNull String directory) {
        if (!directory.isEmpty() || !prefix.endsWith("//")) return prefix;
        return prefix.substring(0, prefix.length() - 1);
    }

    @NotNull
    public static DataResult<List<String>> decomposeDirectory(@NotNull String directory, @NotNull Operation<DataResult<List<String>>> original) {
        return directory.isEmpty() ? DataResult.success(List.of()) : original.call(directory);
    }

    /**
     * Enumerates a path-backed namespace root without sending an invalid empty path through {@code FileUtil.decomposePath}.
     * The explicit namespace root must stay aligned with the loader pack's source root and {@link PackType#getDirectory()}.
     */
    public static void listPathNamespaceRoot(@NotNull Path packRoot, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output) {
        Path namespaceRoot = packRoot.resolve(type.getDirectory()).resolve(namespace).toAbsolutePath();
        PathPackResources.listPath(namespace, namespaceRoot, List.of(), output);
    }

}
