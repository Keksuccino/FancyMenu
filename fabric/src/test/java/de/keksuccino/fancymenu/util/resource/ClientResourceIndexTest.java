package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
    void enumeratesGenericPacksByNamespaceWithoutOpeningResources() {
        FakePackResources pack = new FakePackResources("generic");
        pack.add("alpha:root.txt");
        pack.add("alpha:textures/gui/button.png");
        pack.add("alpha:textures/gui/button.png.mcmeta");
        pack.add("beta:sounds/click.ogg");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(pack));

        assertEquals(Set.of(id("alpha:root.txt"), id("alpha:textures/gui/button.png"), id("beta:sounds/click.ogg")), locations);
        assertEquals(List.of("alpha", "beta"), pack.enumeratedNamespaces);
        assertEquals(0, pack.openCalls.get());
        assertThrows(UnsupportedOperationException.class, () -> locations.add(id("alpha:other.txt")));
    }

    @Test
    void appliesPackFiltersBeforeHigherPriorityResourcesAndCollapsesDuplicates() {
        FakePackResources low = new FakePackResources("low");
        low.add("alpha:blocked/removed.png");
        low.add("alpha:blocked/replaced.png");
        low.add("alpha:kept.png");
        FakePackResources high = new FakePackResources("high");
        high.filter = filter("alpha", "blocked/");
        high.add("alpha:blocked/replaced.png");
        high.add("alpha:added.png");
        high.add("alpha:kept.png");

        Set<ResourceLocation> locations = ClientResourceIndexBuilder.build(new FakeResourceManager(low, high));

        assertEquals(Set.of(id("alpha:blocked/replaced.png"), id("alpha:kept.png"), id("alpha:added.png")), locations);
        assertFalse(locations.contains(id("alpha:blocked/removed.png")));
    }

    @Test
    void skipsMalformedAndFailingPackDataWithoutDiscardingHealthyPacks() {
        FakePackResources healthy = new FakePackResources("healthy");
        healthy.add("alpha:healthy.txt");
        FakePackResources malformed = new FakePackResources("malformed") {
            @Override
            public Set<String> getNamespaces(PackType type) {
                return new LinkedHashSet<>(List.of("alpha", "INVALID"));
            }

            @Override
            public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter) {
                throw new IllegalStateException("synthetic enumeration failure");
            }
        };

        assertEquals(Set.of(id("alpha:healthy.txt")), ClientResourceIndexBuilder.build(new FakeResourceManager(healthy, malformed)));
    }

    @Test
    void reusesOneLazyIndexAcrossConcurrentCallers() throws Exception {
        ClientResourceIndex index = new ClientResourceIndex();
        FakePackResources pack = new FakePackResources("concurrent");
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

        assertEquals(1, manager.listPackCalls.get());
        assertEquals(1, pack.listCalls.get());
    }

    @Test
    void publishesOnlySuccessfulPreparedGenerationsForAStableWrapperIdentity() {
        ClientResourceIndex index = new ClientResourceIndex();
        FakePackResources firstPack = new FakePackResources("first");
        firstPack.add("alpha:first.txt");
        FakePackResources failedPack = new FakePackResources("failed");
        failedPack.add("alpha:failed.txt");
        FakePackResources recoveryPack = new FakePackResources("recovery");
        recoveryPack.add("alpha:recovery.txt");
        FakeResourceManager stableWrapper = new FakeResourceManager();

        index.onReload(MinecraftResourceReloadObserver.ReloadAction.STARTING);
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(firstPack)));
        index.onReload(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
        assertEquals(Set.of(id("alpha:first.txt")), index.getForManager(stableWrapper));

        index.onReload(MinecraftResourceReloadObserver.ReloadAction.STARTING);
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(failedPack)));
        assertEquals(Set.of(id("alpha:first.txt")), index.getForManager(stableWrapper));
        index.stagePrepared(index.prepareForManager(new FakeResourceManager(recoveryPack)));
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

    private static class FakePackResources implements PackResources {

        private final String name;
        private final Map<ResourceLocation, byte[]> resources = new LinkedHashMap<>();
        private final List<String> enumeratedNamespaces = new ArrayList<>();
        private final AtomicInteger listCalls = new AtomicInteger();
        private final AtomicInteger openCalls = new AtomicInteger();
        private ResourceFilterSection filter;

        private FakePackResources(String name) {
            this.name = name;
        }

        private void add(String location) {
            this.resources.put(id(location), new byte[0]);
        }

        @Override
        public InputStream getRootResource(String fileName) {
            return null;
        }

        @Override
        public InputStream getResource(PackType type, ResourceLocation location) {
            this.openCalls.incrementAndGet();
            return InputStream.nullInputStream();
        }

        @Override
        public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter) {
            this.listCalls.incrementAndGet();
            this.enumeratedNamespaces.add(namespace);
            List<ResourceLocation> matches = new ArrayList<>();
            String prefix = path.isEmpty() ? "" : path + "/";
            this.resources.keySet().forEach(location -> {
                if (type == PackType.CLIENT_RESOURCES && location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix) && filter.test(location)) matches.add(location);
            });
            return matches;
        }

        @Override
        public boolean hasResource(PackType type, ResourceLocation location) {
            return type == PackType.CLIENT_RESOURCES && this.resources.containsKey(location);
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
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
            return serializer == ResourceFilterSection.SERIALIZER ? (T)this.filter : null;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public void close() {
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
        public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            this.listPackCalls.incrementAndGet();
            return this.packs.stream();
        }
    }
}
