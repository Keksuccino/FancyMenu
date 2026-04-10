package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

public final class AfmaPayloadMetricsHelper {

    private static final int MAX_CONTENT_CACHE_ENTRIES = 512;
    private static final Map<byte[], Long> ESTIMATED_ARCHIVE_BYTES = Collections.synchronizedMap(new WeakHashMap<>());
    // Keep a small recent-content cache so fresh byte arrays can still reuse expensive metric work.
    private static final Map<PayloadBytesKey, Long> ESTIMATED_ARCHIVE_BYTES_BY_CONTENT = Collections.synchronizedMap(newBoundedContentCache());
    private static final Map<byte[], String> FINGERPRINTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<PayloadBytesKey, String> FINGERPRINTS_BY_CONTENT = Collections.synchronizedMap(newBoundedContentCache());

    private AfmaPayloadMetricsHelper() {
    }

    public static long estimateArchiveBytes(@Nullable byte[] payloadBytes) {
        if (payloadBytes == null) {
            return 0L;
        }

        Long cachedValue = ESTIMATED_ARCHIVE_BYTES.get(payloadBytes);
        if (cachedValue != null) {
            return cachedValue;
        }

        PayloadBytesKey cacheKey = new PayloadBytesKey(payloadBytes);
        cachedValue = ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.get(cacheKey);
        if (cachedValue != null) {
            ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, cachedValue);
            return cachedValue;
        }

        long computedValue = estimateArchiveBytesUncached(payloadBytes);
        synchronized (ESTIMATED_ARCHIVE_BYTES_BY_CONTENT) {
            Long existingContentValue = ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.get(cacheKey);
            if (existingContentValue != null) {
                ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, existingContentValue);
                return existingContentValue;
            }
            ESTIMATED_ARCHIVE_BYTES_BY_CONTENT.put(cacheKey, computedValue);
        }
        synchronized (ESTIMATED_ARCHIVE_BYTES) {
            Long existingValue = ESTIMATED_ARCHIVE_BYTES.get(payloadBytes);
            if (existingValue != null) {
                return existingValue;
            }
            ESTIMATED_ARCHIVE_BYTES.put(payloadBytes, computedValue);
        }
        return computedValue;
    }

    @NotNull
    public static String fingerprintPayload(@NotNull byte[] payloadBytes) {
        String cachedValue = FINGERPRINTS.get(payloadBytes);
        if (cachedValue != null) {
            return cachedValue;
        }

        PayloadBytesKey cacheKey = new PayloadBytesKey(payloadBytes);
        cachedValue = FINGERPRINTS_BY_CONTENT.get(cacheKey);
        if (cachedValue != null) {
            FINGERPRINTS.put(payloadBytes, cachedValue);
            return cachedValue;
        }

        String computedValue = fingerprintPayloadUncached(payloadBytes);
        synchronized (FINGERPRINTS_BY_CONTENT) {
            String existingContentValue = FINGERPRINTS_BY_CONTENT.get(cacheKey);
            if (existingContentValue != null) {
                FINGERPRINTS.put(payloadBytes, existingContentValue);
                return existingContentValue;
            }
            FINGERPRINTS_BY_CONTENT.put(cacheKey, computedValue);
        }
        synchronized (FINGERPRINTS) {
            String existingValue = FINGERPRINTS.get(payloadBytes);
            if (existingValue != null) {
                return existingValue;
            }
            FINGERPRINTS.put(payloadBytes, computedValue);
        }
        return computedValue;
    }

    protected static long estimateArchiveBytesUncached(@NotNull byte[] payloadBytes) {
        Deflater deflater = new Deflater(9, true);
        byte[] buffer = new byte[8192];
        long compressedBytes = 0L;
        try {
            deflater.setInput(payloadBytes);
            deflater.finish();
            while (!deflater.finished()) {
                compressedBytes += deflater.deflate(buffer);
            }
        } finally {
            deflater.end();
        }
        return Math.max(1L, compressedBytes);
    }

    @NotNull
    protected static String fingerprintPayloadUncached(@NotNull byte[] payloadBytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payloadBytes);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte digestByte : digest) {
                builder.append(Character.forDigit((digestByte >>> 4) & 0xF, 16));
                builder.append(Character.forDigit(digestByte & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return payloadBytes.length + ":" + Arrays.hashCode(payloadBytes);
        }
    }

    @NotNull
    protected static <T> LinkedHashMap<PayloadBytesKey, T> newBoundedContentCache() {
        return new LinkedHashMap<>(MAX_CONTENT_CACHE_ENTRIES, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<PayloadBytesKey, T> eldest) {
                return this.size() > MAX_CONTENT_CACHE_ENTRIES;
            }
        };
    }

    protected static final class PayloadBytesKey {

        @NotNull
        protected final byte[] payloadBytes;
        protected final int payloadHash;

        protected PayloadBytesKey(@NotNull byte[] payloadBytes) {
            this.payloadBytes = payloadBytes;
            this.payloadHash = Arrays.hashCode(payloadBytes);
        }

        @Override
        public int hashCode() {
            return (31 * this.payloadBytes.length) + this.payloadHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PayloadBytesKey other)) {
                return false;
            }
            return this.payloadBytes.length == other.payloadBytes.length
                    && this.payloadHash == other.payloadHash
                    && Arrays.equals(this.payloadBytes, other.payloadBytes);
        }

    }

}
