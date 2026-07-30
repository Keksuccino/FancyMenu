package de.keksuccino.fancymenu.util.resource;

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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /**
     * Enumerates a namespace root from an archive while resolving resource suppliers through the pack itself.
     * The archive is closed before this method returns, so suppliers must never reference the temporary {@link ZipFile}.
     */
    public static void listArchiveNamespaceRoot(@NotNull Path archive, @NotNull PackResources pack, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output) {
        String prefix = type.getDirectory() + "/" + namespace + "/";
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || !entryName.startsWith(prefix)) continue;
                String path = entryName.substring(prefix.length());
                ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
                if (location == null) continue;
                IoSupplier<InputStream> streamSupplier = pack.getResource(type, location);
                if (streamSupplier != null) output.accept(location, streamSupplier);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to enumerate archived resource pack '" + archive + "'", ex);
        }
    }

    /**
     * Enumerates only children that actually own the requested namespace. Delegating packs expose the union of their child namespaces, so querying every child creates a namespace-by-child Cartesian product.
     */
    public static void listMatchingChildNamespaceRoots(@NotNull Collection<? extends PackResources> children, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output, @NotNull NamespaceRootLister lister) {
        for (PackResources child : children) {
            if (child == null) continue;
            Set<String> namespaces = child.getNamespaces(type);
            if (namespaces != null && namespaces.contains(namespace)) lister.list(child, type, namespace, output);
        }
    }

    @FunctionalInterface
    public interface NamespaceRootLister {

        void list(@NotNull PackResources pack, @NotNull PackType type, @NotNull String namespace, @NotNull PackResources.ResourceOutput output);

    }

}
