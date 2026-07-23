package de.keksuccino.fancymenu.util.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class ClientResourceIndexBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientResourceIndexBuilder() {}

    @NotNull
    static Set<ResourceLocation> build(@NotNull ResourceManager resourceManager) {
        LinkedHashSet<ResourceLocation> locations = new LinkedHashSet<>();
        List<PackResources> packs;
        try (Stream<PackResources> packStream = resourceManager.listPacks()) {
            packs = packStream.toList();
        } catch (RuntimeException ex) {
            LOGGER.error("[FANCYMENU] Failed to obtain the active client resource-pack list for the resource picker!", ex);
            return Set.of();
        }

        for (PackResources pack : packs) {
            if (pack == null) continue;
            applyFilter(pack, locations);
            collectPackLocations(pack, locations);
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(locations));
    }

    private static void applyFilter(@NotNull PackResources pack, @NotNull Set<ResourceLocation> locations) {
        ResourceFilterSection filter;
        try {
            filter = pack.getMetadataSection(ResourceFilterSection.TYPE);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read client resource filters from pack '{}'; continuing without its filter.", getPackName(pack), ex);
            return;
        }
        if (filter == null || locations.isEmpty()) return;

        try {
            // Pack order is low to high. Like FallbackResourceManager, a pack filters already collected lower-priority entries before contributing its own files.
            locations.removeIf(location -> filter.isNamespaceFiltered(location.getNamespace()) && filter.isPathFiltered(location.getPath()));
        } catch (RuntimeException ex) {
            LOGGER.error("[FANCYMENU] Failed to apply client resource filters from pack '{}'; continuing with the entries filtered so far.", getPackName(pack), ex);
        }
    }

    private static void collectPackLocations(@NotNull PackResources pack, @NotNull Set<ResourceLocation> locations) {
        Set<String> packNamespaces;
        try {
            packNamespaces = pack.getNamespaces(PackType.CLIENT_RESOURCES);
        } catch (RuntimeException ex) {
            LOGGER.error("[FANCYMENU] Failed to obtain client resource namespaces from pack '{}'; skipping its resources.", getPackName(pack), ex);
            return;
        }
        if (packNamespaces == null || packNamespaces.isEmpty()) return;

        List<String> namespaces;
        try {
            namespaces = new ArrayList<>(packNamespaces);
        } catch (RuntimeException ex) {
            LOGGER.error("[FANCYMENU] Failed to snapshot client resource namespaces from pack '{}'; skipping its resources.", getPackName(pack), ex);
            return;
        }

        for (String namespace : namespaces) {
            if (namespace == null || !ResourceLocation.isValidNamespace(namespace)) {
                LOGGER.warn("[FANCYMENU] Ignoring invalid client resource namespace '{}' reported by pack '{}'.", namespace, getPackName(pack));
                continue;
            }
            try {
                pack.listResources(PackType.CLIENT_RESOURCES, namespace, "", (location, streamSupplier) -> collectLocation(pack, namespace, location, locations));
            } catch (RuntimeException ex) {
                LOGGER.error("[FANCYMENU] Failed to enumerate client resources in namespace '{}' from pack '{}'; keeping the valid entries reported before the failure.", namespace, getPackName(pack), ex);
            }
        }
    }

    private static void collectLocation(@NotNull PackResources pack, @NotNull String requestedNamespace, @Nullable ResourceLocation location, @NotNull Set<ResourceLocation> locations) {
        if (location == null) {
            LOGGER.warn("[FANCYMENU] Ignoring a null client resource reported by pack '{}' for namespace '{}'.", getPackName(pack), requestedNamespace);
            return;
        }
        if (!requestedNamespace.equals(location.getNamespace())) {
            LOGGER.warn("[FANCYMENU] Ignoring client resource '{}' reported by pack '{}' while enumerating namespace '{}'.", location, getPackName(pack), requestedNamespace);
            return;
        }
        if (!location.getPath().endsWith(PackResources.METADATA_EXTENSION)) locations.add(location);
    }

    @NotNull
    private static String getPackName(@NotNull PackResources pack) {
        try {
            String packId = pack.packId();
            if (packId != null) return packId;
        } catch (RuntimeException ignored) {}
        return pack.getClass().getName();
    }

}
