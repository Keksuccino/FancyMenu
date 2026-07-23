package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public final class ClientResourceIndex {

    private static final ClientResourceIndex INSTANCE = new ClientResourceIndex();

    private final Object cacheLock = new Object();
    @Nullable private volatile CacheEntry currentEntry;
    @Nullable private PreparedIndex pendingIndex;

    ClientResourceIndex() {}

    @NotNull
    public static Set<ResourceLocation> getLoadedLocations(@NotNull ResourceManager resourceManager) {
        return INSTANCE.getForManager(resourceManager);
    }

    @ApiStatus.Internal
    @NotNull
    public static PreparedIndex prepare(@NotNull ResourceManager resourceManager) {
        return INSTANCE.prepareForManager(resourceManager);
    }

    @ApiStatus.Internal
    public static void stage(@NotNull PreparedIndex preparedIndex) {
        INSTANCE.stagePrepared(preparedIndex);
    }

    @ApiStatus.Internal
    public static void onMinecraftResourceReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        INSTANCE.onReload(action);
    }

    @NotNull
    Set<ResourceLocation> getForManager(@NotNull ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        CacheEntry entry = this.currentEntry;
        if (entry != null && (entry.authoritative || entry.resourceManager == resourceManager)) return entry.locations;

        synchronized (this.cacheLock) {
            entry = this.currentEntry;
            if (entry != null && (entry.authoritative || entry.resourceManager == resourceManager)) return entry.locations;
            Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(resourceManager);
            this.currentEntry = new CacheEntry(resourceManager, locations, false);
            return locations;
        }
    }

    @NotNull
    PreparedIndex prepareForManager(@NotNull ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        return new PreparedIndex(ClientResourceIndexBuilder.build(resourceManager));
    }

    void stagePrepared(@NotNull PreparedIndex preparedIndex) {
        synchronized (this.cacheLock) {
            this.pendingIndex = Objects.requireNonNull(preparedIndex, "preparedIndex");
        }
    }

    void onReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (this.cacheLock) {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                this.pendingIndex = null;
                return;
            }
            if (this.pendingIndex != null) {
                // Minecraft exposes a stable ReloadableResourceManager wrapper while each reload uses a new internal manager. Successful lifecycle publication, not wrapper identity, is the generation boundary.
                this.currentEntry = new CacheEntry(null, this.pendingIndex.locations, true);
                this.pendingIndex = null;
            }
        }
    }

    public static final class PreparedIndex {

        private final Set<ResourceLocation> locations;

        private PreparedIndex(@NotNull Set<ResourceLocation> locations) {
            this.locations = Objects.requireNonNull(locations, "locations");
        }

    }

    private record CacheEntry(@Nullable ResourceManager resourceManager, Set<ResourceLocation> locations, boolean authoritative) {}

}
