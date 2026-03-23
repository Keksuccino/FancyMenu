package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AfmaEncodePlan {

    @NotNull
    protected final AfmaMetadata metadata;
    @NotNull
    protected final AfmaFrameIndex frameIndex;
    @NotNull
    protected final LinkedHashMap<String, byte[]> payloads;
    protected final long totalPayloadBytes;

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, byte[]> payloads) {
        this(metadata, frameIndex, payloads, calculateTotalPayloadBytes(Objects.requireNonNull(payloads)), true);
    }

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, byte[]> payloads, long totalPayloadBytes) {
        this(metadata, frameIndex, payloads, totalPayloadBytes, true);
    }

    protected AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                             @NotNull LinkedHashMap<String, byte[]> payloads, long totalPayloadBytes, boolean copyPayloads) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = copyPayloads ? new LinkedHashMap<>(Objects.requireNonNull(payloads)) : Objects.requireNonNull(payloads);
        this.totalPayloadBytes = Math.max(0L, totalPayloadBytes);
    }

    @NotNull
    public static AfmaEncodePlan lazy(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                                      @NotNull List<String> payloadPaths, long totalPayloadBytes,
                                      @NotNull PayloadLoader payloadLoader) {
        return new AfmaEncodePlan(metadata, frameIndex, new LazyPayloadMap(payloadPaths, payloadLoader), totalPayloadBytes, false);
    }

    @NotNull
    public AfmaMetadata getMetadata() {
        return this.metadata;
    }

    @NotNull
    public AfmaFrameIndex getFrameIndex() {
        return this.frameIndex;
    }

    @NotNull
    public LinkedHashMap<String, byte[]> getPayloads() {
        return this.payloads;
    }

    public long getTotalPayloadBytes() {
        return this.totalPayloadBytes;
    }

    public @NotNull AfmaEncodePlan withoutPayloads() {
        return new AfmaEncodePlan(this.metadata, this.frameIndex, new LinkedHashMap<>(), this.totalPayloadBytes);
    }

    public int countFrames(@NotNull AfmaFrameOperationType type) {
        int count = 0;
        count += countFrames(type, this.frameIndex.getFrames());
        count += countFrames(type, this.frameIndex.getIntroFrames());
        return count;
    }

    private static int countFrames(@NotNull AfmaFrameOperationType type, @NotNull java.util.List<de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor> frames) {
        int count = 0;
        for (var frame : frames) {
            if (frame != null && type == frame.getType()) {
                count++;
            }
        }
        return count;
    }

    private static long calculateTotalPayloadBytes(@NotNull Map<String, byte[]> payloads) {
        long total = 0L;
        for (byte[] payload : payloads.values()) {
            if (payload != null) {
                total += payload.length;
            }
        }
        return total;
    }

    @FunctionalInterface
    public interface PayloadLoader {
        @NotNull byte[] load(@NotNull String path) throws IOException;
    }

    protected static final class LazyPayloadMap extends LinkedHashMap<String, byte[]> {

        private final @NotNull List<String> payloadPaths;
        private final @NotNull PayloadLoader payloadLoader;

        protected LazyPayloadMap(@NotNull List<String> payloadPaths, @NotNull PayloadLoader payloadLoader) {
            this.payloadPaths = List.copyOf(payloadPaths);
            this.payloadLoader = Objects.requireNonNull(payloadLoader);
        }

        @Override
        public byte[] get(Object key) {
            if (!(key instanceof String path) || !this.containsKey(path)) {
                return null;
            }
            return this.loadPayload(path);
        }

        @Override
        public boolean containsKey(Object key) {
            return (key instanceof String path) && this.payloadPaths.contains(path);
        }

        @Override
        public int size() {
            return this.payloadPaths.size();
        }

        @Override
        public boolean isEmpty() {
            return this.payloadPaths.isEmpty();
        }

        @Override
        public @NotNull Set<String> keySet() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(this.payloadPaths));
        }

        @Override
        public @NotNull Collection<byte[]> values() {
            List<byte[]> values = new ArrayList<>(this.payloadPaths.size());
            for (String payloadPath : this.payloadPaths) {
                values.add(this.loadPayload(payloadPath));
            }
            return values;
        }

        @Override
        public @NotNull Set<Map.Entry<String, byte[]>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public @NotNull Iterator<Map.Entry<String, byte[]>> iterator() {
                    Iterator<String> iterator = LazyPayloadMap.this.payloadPaths.iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Map.Entry<String, byte[]> next() {
                            String payloadPath = iterator.next();
                            return new AbstractMap.SimpleImmutableEntry<>(payloadPath, LazyPayloadMap.this.loadPayload(payloadPath));
                        }
                    };
                }

                @Override
                public int size() {
                    return LazyPayloadMap.this.payloadPaths.size();
                }
            };
        }

        @Override
        public byte[] put(String key, byte[] value) {
            throw new UnsupportedOperationException("Lazy AFMA payload maps are read-only");
        }

        @Override
        public void putAll(@NotNull Map<? extends String, ? extends byte[]> m) {
            throw new UnsupportedOperationException("Lazy AFMA payload maps are read-only");
        }

        @Override
        public byte[] remove(Object key) {
            throw new UnsupportedOperationException("Lazy AFMA payload maps are read-only");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Lazy AFMA payload maps are read-only");
        }

        private byte[] loadPayload(@NotNull String path) {
            try {
                return this.payloadLoader.load(path);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load cached AFMA payload: " + path, ex);
            }
        }
    }

}
