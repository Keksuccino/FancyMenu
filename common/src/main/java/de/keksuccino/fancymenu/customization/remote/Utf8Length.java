package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;

final class Utf8Length {

    static final long MALFORMED_UTF16 = -1L;

    private Utf8Length() {
    }

    /**
     * Counts the bytes produced by strict UTF-8 encoding without allocating an encoded copy. WebSocket text writes
     * reject malformed UTF-16, so isolated surrogates deliberately return {@link #MALFORMED_UTF16} instead of using
     * the replacement behavior of {@link String#getBytes(java.nio.charset.Charset)}.
     */
    static long count(@NotNull CharSequence value) {
        long utf8Bytes = 0L;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current <= 0x7F) {
                utf8Bytes++;
            } else if (current <= 0x7FF) {
                utf8Bytes += 2L;
            } else if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return MALFORMED_UTF16;
                }
                utf8Bytes += 4L;
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return MALFORMED_UTF16;
            } else {
                utf8Bytes += 3L;
            }
        }
        return utf8Bytes;
    }
}
