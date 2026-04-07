package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

public final class AfmaPayloadMetricsHelper {

    private static final Map<byte[], Long> ESTIMATED_ARCHIVE_BYTES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<byte[], String> FINGERPRINTS = Collections.synchronizedMap(new WeakHashMap<>());

    private AfmaPayloadMetricsHelper() {
    }

    public static long estimateArchiveBytes(@Nullable byte[] payloadBytes) {
        if (payloadBytes == null) {
            return 0L;
        }
        if (payloadBytes.length < 1024) {
            return payloadBytes.length;
        }

        Long cachedValue = ESTIMATED_ARCHIVE_BYTES.get(payloadBytes);
        if (cachedValue != null) {
            return cachedValue;
        }

        long computedValue = estimateArchiveBytesUncached(payloadBytes);
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

        String computedValue = fingerprintPayloadUncached(payloadBytes);
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

}
