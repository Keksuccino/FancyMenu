package de.keksuccino.fancymenu.util.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * A stream of a file that any thread can close, also while another thread reads from it, like a texture that gets closed on the render thread while its loading thread
 * still reads its file. Closing waits for a read that runs right now, so the file is never open anymore once {@link #close()} returns. All reads after closing fail.<br><br>
 *
 * Reads and closing take turns through a lock, because a {@link FileInputStream} must never get closed while another thread reads from it: the system can give the number
 * of the closed file to the next file that gets opened, so the read could continue in that other file. Every read reads at most {@link #MAX_READ_BYTES}, so closing never waits long.
 */
public final class ReleasableFileInputStream extends InputStream {

    /** Most bytes a single read reads, which is the longest read that closing can wait for. **/
    public static final int MAX_READ_BYTES = 64 * 1024;

    private final Object lock = new Object();
    @NotNull
    private final FileInputStream in;
    /** Guarded by the lock. **/
    private boolean closed;

    public ReleasableFileInputStream(@NotNull File file) throws FileNotFoundException {
        this.in = new FileInputStream(Objects.requireNonNull(file));
    }

    @Override
    public int read() throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return this.in.read();
        }
    }

    @Override
    public int read(@NotNull byte[] buffer, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, buffer.length);
        if (length == 0) return 0;
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return this.in.read(buffer, offset, Math.min(length, MAX_READ_BYTES));
        }
    }

    @Override
    public long skip(long count) throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return this.in.skip(count);
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (this.lock) {
            this.ensureOpenLocked();
            return this.in.available();
        }
    }

    /**
     * Closes the file, from any thread. Waits for a read that another thread does right now. Closing the stream more than once does nothing.
     */
    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.closed) return;
            this.closed = true;
            this.in.close();
        }
    }

    private void ensureOpenLocked() throws IOException {
        if (this.closed) throw new IOException("The stream got closed");
    }

}
