package de.keksuccino.fancymenu.util.resource;

import com.google.common.base.Joiner;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ApiStatus.Internal
public final class PackResourcesRootEnumeration {

    private static final Joiner PATH_JOINER = Joiner.on('/');

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

    /**
     * Enumerates a namespace root from an archive while resolving resource suppliers through the pack itself.
     * Forge's jar-in-jar roots report regular-file attributes but have no usable {@code File} representation. Open them through NIO instead.
     * Only this method's archive view is closed; suppliers must come from the owning pack and remain usable after enumeration.
     */
    public static void listArchiveNamespaceRoot(@NotNull Path archive, @NotNull PackResources pack, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output) {
        try (FileSystem fileSystem = FileSystems.newFileSystem(archive)) {
            Path namespaceRoot = fileSystem.getPath(type.getDirectory(), namespace);
            if (!Files.isDirectory(namespaceRoot)) return;
            try (Stream<Path> entries = Files.find(namespaceRoot, Integer.MAX_VALUE, (path, attributes) -> attributes.isRegularFile())) {
                entries.forEach(entry -> {
                    ResourceLocation location = ResourceLocation.tryBuild(namespace, PATH_JOINER.join(namespaceRoot.relativize(entry)));
                    if (location == null) return;
                    IoSupplier<InputStream> streamSupplier = pack.getResource(type, location);
                    if (streamSupplier != null) output.accept(location, streamSupplier);
                });
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to enumerate archived resource pack '" + archive.toUri() + "'", ex);
        }
    }

    /**
     * Enumerates only children that actually own the requested namespace. Delegating packs expose the union of their child namespaces, so querying every child creates a namespace-by-child Cartesian product.
     */
    public static void listMatchingChildNamespaceRoots(@NotNull Collection<? extends PackResources> children, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output, @NotNull NamespaceRootLister lister) {
        RuntimeException failure = null;
        for (PackResources child : children) {
            if (child == null) continue;
            try {
                Set<String> namespaces = child.getNamespaces(type);
                if (namespaces != null && namespaces.contains(namespace)) lister.list(child, type, namespace, output);
            } catch (RuntimeException ex) {
                // Delay reporting until all siblings have contributed their resources. Throwing here would hide every later child in this namespace.
                if (failure == null) failure = new IllegalStateException("Failed to enumerate child resource packs for namespace '" + namespace + "'");
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    @FunctionalInterface
    public interface NamespaceRootLister {

        void list(@NotNull PackResources pack, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output);

    }

}
