package de.keksuccino.fancymenu.util.file;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

/**
 * A ZIP file whose entries any thread can read and that any thread can close, also while other threads read from it, like an animated texture that gets closed on the render thread
 * while its stream thread still reads a frame from its archive. Closing waits for a read that runs right now, so the file is never open anymore once {@link #close()} returns. All reads after closing fail.<br><br>
 *
 * Reads and closing take turns through a lock, and reads hold it for at most {@link #MAX_LOCKED_READ_BYTES} at a time, so closing never waits long.<br>
 * <b>Never interrupt a thread while it reads entries:</b> the ZIP file reads its file through a {@link java.nio.channels.FileChannel}, which closes for good when the thread
 * that uses it gets interrupted, so all later reads of every thread would fail.
 */
public final class ReleasableZipFile implements Closeable {

    /** Most bytes that get read while holding the lock, which is the longest read that closing can wait for. **/
    public static final int MAX_LOCKED_READ_BYTES = 64 * 1024;
    /** Entries get read right into an array of the size they claim, up to this size. Broken archives can claim any size, so bigger entries grow their array while they get read. **/
    private static final long MAX_TRUSTED_ENTRY_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_ARRAY_BYTES = Integer.MAX_VALUE - 8;

    private final Object lock = new Object();
    @NotNull
    private final ZipFile zipFile;
    /** Guarded by the lock. **/
    private boolean closed;

    public ReleasableZipFile(@NotNull ZipFile zipFile) {
        this.zipFile = Objects.requireNonNull(zipFile);
    }

    /**
     * All entries of the ZIP file. They got read when the ZIP file got opened, so listing them doesn't read the file.
     */
    @NotNull
    public Enumeration<ZipArchiveEntry> getEntries() throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
        }
        return this.zipFile.getEntries();
    }

    /**
     * Reads the whole uncompressed content of the entry.
     */
    @NotNull
    public byte[] readEntry(@NotNull ZipArchiveEntry entry) throws IOException {
        try (InputStream in = this.openEntry(entry)) {
            long size = entry.getSize();
            // The array only gets copied if the entry doesn't know its size or claims a wrong one
            byte[] bytes = new byte[((size >= 0L) && (size <= MAX_TRUSTED_ENTRY_BYTES)) ? (int) size : MAX_LOCKED_READ_BYTES];
            int length = 0;
            while (true) {
                if (length == bytes.length) {
                    int next = this.readLocked(in);
                    if (next < 0) return bytes;
                    if (bytes.length >= MAX_ARRAY_BYTES) throw new IOException("The ZIP entry is too big to read: " + entry.getName());
                    bytes = Arrays.copyOf(bytes, (int) Math.min(MAX_ARRAY_BYTES, Math.max(bytes.length * 2L, bytes.length + (long) MAX_LOCKED_READ_BYTES)));
                    bytes[length++] = (byte) next;
                    continue;
                }
                int read = this.readLocked(in, bytes, length, Math.min(MAX_LOCKED_READ_BYTES, bytes.length - length));
                if (read < 0) return (length == bytes.length) ? bytes : Arrays.copyOf(bytes, length);
                length += read;
            }
        }
    }

    /**
     * Reads the start of the uncompressed content of the entry into the array, until the array is full or the entry ends.
     *
     * @return How many bytes got read.
     */
    public int readEntryStart(@NotNull ZipArchiveEntry entry, @NotNull byte[] target) throws IOException {
        try (InputStream in = this.openEntry(entry)) {
            int length = 0;
            while (length < target.length) {
                int read = this.readLocked(in, target, length, Math.min(MAX_LOCKED_READ_BYTES, target.length - length));
                if (read < 0) break;
                length += read;
            }
            return length;
        }
    }

    @NotNull
    private InputStream openEntry(@NotNull ZipArchiveEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        InputStream in;
        synchronized (this.lock) {
            this.ensureOpenLocked();
            in = this.zipFile.getInputStream(entry);
        }
        if (in == null) throw new IOException("The ZIP entry doesn't belong to the ZIP file: " + entry.getName());
        return in;
    }

    private int readLocked(@NotNull InputStream in, @NotNull byte[] target, int offset, int length) throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return in.read(target, offset, length);
        }
    }

    private int readLocked(@NotNull InputStream in) throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return in.read();
        }
    }

    private void ensureOpenLocked() throws IOException {
        if (this.closed) throw new IOException("The ZIP file got closed");
    }

    /**
     * Closes the ZIP file, from any thread. Waits for a read that another thread does right now. Closing it more than once does nothing.
     */
    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.closed) return;
            this.closed = true;
            this.zipFile.close();
        }
    }

}
