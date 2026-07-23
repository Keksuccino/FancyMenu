package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResourceIndexTest {

    @Test
    void enumeratesGenericPacksByNamespaceWithoutOpeningResourceSuppliers() {
        AtomicInteger openedStreams = new AtomicInteger();
        FakePackResources pack = new FakePackResources("generic", openedStreams);
        pack.add("alpha:root.txt");
        pack.add("alpha:textures/gui/button.png");
        pack.add("alpha:textures/gui/button.png.mcmeta");
        pack.add("beta:sounds/click.ogg");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(pack));

        assertEquals(Set.of(id("alpha:root.txt"), id("alpha:textures/gui/button.png"), id("beta:sounds/click.ogg")), locations);
        assertEquals(List.of("alpha", "beta"), pack.enumeratedNamespaces);
        assertEquals(List.of("", ""), pack.enumeratedDirectories);
        assertEquals(0, openedStreams.get());
        assertThrows(UnsupportedOperationException.class, () -> locations.add(id("alpha:other.txt")));
    }

    @Test
    void appliesPackFiltersBeforeHigherPriorityResourcesAndCollapsesDuplicates() {
        FakePackResources low = new FakePackResources("low", new AtomicInteger());
        low.add("alpha:blocked/removed.png");
        low.add("alpha:blocked/replaced.png");
        low.add("alpha:kept.png");
        low.add("beta:blocked/still_present.png");
        FakePackResources high = new FakePackResources("high", new AtomicInteger());
        high.filter = filter("alpha", "blocked/");
        high.add("alpha:blocked/replaced.png");
        high.add("alpha:added.png");
        high.add("alpha:kept.png");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(low, high));

        assertEquals(Set.of(id("alpha:blocked/replaced.png"), id("alpha:kept.png"), id("alpha:added.png"), id("beta:blocked/still_present.png")), locations);
        assertFalse(locations.contains(id("alpha:blocked/removed.png")));
    }

    @Test
    void appliesFiltersFromPacksThatDoNotProvideTheFilteredNamespace() {
        FakePackResources low = new FakePackResources("low", new AtomicInteger());
        low.add("alpha:blocked/removed.png");
        low.add("alpha:kept.png");
        FakePackResources filterOnly = new FakePackResources("filter-only", new AtomicInteger());
        filterOnly.filter = filter("alpha", "blocked/");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(low, filterOnly));

        assertEquals(Set.of(id("alpha:kept.png")), locations);
        assertEquals(0, filterOnly.listCalls.get());
    }

    @Test
    void acceptsGenericAggregatingPacksWithoutOpeningOrClosingOwnedResources() {
        AtomicInteger openedStreams = new AtomicInteger();
        FakePackResources aggregate = new FakePackResources("aggregate", openedStreams);
        aggregate.add("base:shared.txt");
        aggregate.add("base:primary_only.txt");
        aggregate.add("overlay:one.txt");
        aggregate.add("overlay:two.txt");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(aggregate));

        assertEquals(Set.of(id("base:shared.txt"), id("base:primary_only.txt"), id("overlay:one.txt"), id("overlay:two.txt")), locations);
        assertEquals(0, openedStreams.get());
        assertEquals(0, aggregate.closeCalls.get());
    }

    @Test
    void keepsValidPartialResultsWhenOneGenericPackReportsMalformedDataAndFails() {
        AtomicInteger openedStreams = new AtomicInteger();
        FakePackResources healthy = new FakePackResources("healthy", openedStreams);
        healthy.add("alpha:healthy.txt");
        FakePackResources malformed = new FakePackResources("malformed", openedStreams) {
            @Override
            public Set<String> getNamespaces(PackType type) {
                return new LinkedHashSet<>(List.of("alpha", "INVALID"));
            }

            @Override
            public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
                if (!namespace.equals("alpha")) return;
                output.accept(id("alpha:partial.txt"), unopenedSupplier(openedStreams));
                output.accept(id("other:wrong_namespace.txt"), unopenedSupplier(openedStreams));
                throw new IllegalStateException("synthetic enumeration failure");
            }
        };

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(healthy, malformed));

        assertEquals(Set.of(id("alpha:healthy.txt"), id("alpha:partial.txt")), locations);
        assertEquals(0, openedStreams.get());
    }

    @Test
    void reusesOneLazyIndexAcrossConcurrentCallers() throws Exception {
        ClientResourceIndex index = new ClientResourceIndex();
        FakePackResources pack = new FakePackResources("concurrent", new AtomicInteger());
        pack.add("alpha:only.txt");
        FakeResourceManager manager = new FakeResourceManager(pack);
        int callerCount = 24;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Set<ResourceLocation>>> results = new ArrayList<>();
        try {
            for (int i = 0; i < callerCount; i++) results.add(executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return index.getForManager(manager);
            }));
            start.countDown();
            Set<ResourceLocation> expected = results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<Set<ResourceLocation>> result : results) assertSame(expected, result.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(Set.of(id("alpha:only.txt")), index.getForManager(manager));
        assertEquals(1, manager.listPackCalls.get());
        assertEquals(1, pack.listCalls.get());
    }

    @Test
    void publishesOnlySuccessfulPreparedGenerationsForAStableWrapperIdentity() {
        ClientResourceIndex index = new ClientResourceIndex();
        FakePackResources firstPack = new FakePackResources("first", new AtomicInteger());
        firstPack.add("alpha:first.txt");
        FakePackResources failedPack = new FakePackResources("failed", new AtomicInteger());
        failedPack.add("alpha:failed.txt");
        FakePackResources recoveryPack = new FakePackResources("recovery", new AtomicInteger());
        recoveryPack.add("alpha:recovery.txt");
        FakeResourceManager stableWrapper = new FakeResourceManager();

        index.onReload(MinecraftResourceReloadObserver.ReloadAction.STARTING);
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(firstPack)));
        index.onReload(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
        assertEquals(Set.of(id("alpha:first.txt")), index.getForManager(stableWrapper));

        index.onReload(MinecraftResourceReloadObserver.ReloadAction.STARTING);
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(failedPack)));
        assertEquals(Set.of(id("alpha:first.txt")), index.getForManager(stableWrapper));

        // Recovery reloads reuse the same ResourceLoadStateTracker reload and therefore replace the failed pending generation without another STARTING notification.
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(recoveryPack)));
        assertEquals(Set.of(id("alpha:first.txt")), index.getForManager(stableWrapper));
        index.onReload(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
        assertEquals(Set.of(id("alpha:recovery.txt")), index.getForManager(stableWrapper));
        assertEquals(0, stableWrapper.listPackCalls.get());
    }

    private static ResourceFilterSection filter(String namespace, String pathPrefix) {
        return new ResourceFilterSection(List.of()) {
            @Override
            public boolean isNamespaceFiltered(String candidate) {
                return namespace.equals(candidate);
            }

            @Override
            public boolean isPathFiltered(String path) {
                return path.startsWith(pathPrefix);
            }
        };
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private static IoSupplier<InputStream> unopenedSupplier(AtomicInteger openedStreams) {
        return () -> {
            openedStreams.incrementAndGet();
            return InputStream.nullInputStream();
        };
    }

    private static class FakePackResources implements PackResources {

        private final String id;
        private final AtomicInteger openedStreams;
        private final Map<ResourceLocation, IoSupplier<InputStream>> resources = new LinkedHashMap<>();
        private final List<String> enumeratedNamespaces = new ArrayList<>();
        private final List<String> enumeratedDirectories = new ArrayList<>();
        private final AtomicInteger listCalls = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();
        private ResourceFilterSection filter;

        private FakePackResources(String id, AtomicInteger openedStreams) {
            this.id = id;
            this.openedStreams = openedStreams;
        }

        private void add(String location) {
            this.resources.put(id(location), unopenedSupplier(this.openedStreams));
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... path) {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            return type == PackType.CLIENT_RESOURCES ? this.resources.get(location) : null;
        }

        @Override
        public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
            this.listCalls.incrementAndGet();
            this.enumeratedNamespaces.add(namespace);
            this.enumeratedDirectories.add(directory);
            if (type != PackType.CLIENT_RESOURCES) return;
            String prefix = directory.isEmpty() ? "" : directory + "/";
            this.resources.forEach((location, supplier) -> {
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) output.accept(location, supplier);
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            if (type != PackType.CLIENT_RESOURCES) return Set.of();
            LinkedHashSet<String> namespaces = new LinkedHashSet<>();
            this.resources.keySet().forEach(location -> namespaces.add(location.getNamespace()));
            return namespaces;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMetadataSection(MetadataSectionSerializer<T> metadataSerializer) {
            if (metadataSerializer == ResourceFilterSection.TYPE) return (T)this.filter;
            return null;
        }

        @Override
        public String packId() {
            return this.id;
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

    private static class FakeResourceManager implements ResourceManager {

        private final List<PackResources> packs;
        private final AtomicInteger listPackCalls = new AtomicInteger();

        private FakeResourceManager(PackResources... packs) {
            this.packs = List.of(packs);
        }

        @Override
        public Set<String> getNamespaces() {
            LinkedHashSet<String> namespaces = new LinkedHashSet<>();
            this.packs.forEach(pack -> namespaces.addAll(pack.getNamespaces(PackType.CLIENT_RESOURCES)));
            return namespaces;
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            return Optional.empty();
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            return List.of();
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String directory, Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String directory, Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            this.listPackCalls.incrementAndGet();
            return this.packs.stream();
        }

    }

}
