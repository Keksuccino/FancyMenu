package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.util.resource.resources.texture.GatedZipChannel;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The real decoder, which can hold its reads and its loading at gates, and remembers the file it opened and every read from it.
 * Legacy ZIP archives get read through a {@link GatedZipChannel}.
 */
final class GatedAfmaDecoder extends AfmaDecoder {

    /** Holds the decoder after it opened its file and before it reads anything from it, like a loading thread that is busy with the archive. **/
    @Nullable
    private final ReadGate loadingGate;
    @Nullable
    private volatile GatedFile file;
    @Nullable
    private volatile GatedZipChannel zipChannel;

    GatedAfmaDecoder(@Nullable ReadGate loadingGate) {
        this.loadingGate = loadingGate;
    }

    GatedAfmaDecoder() {
        this(null);
    }

    /**
     * The file the decoder opened, or NULL if it never opened one.
     */
    @Nullable
    GatedFile openedFile() {
        return this.file;
    }

    @NotNull
    GatedFile requireOpenedFile() {
        GatedFile opened = this.file;
        assertNotNull(opened, "The decoder didn't open its file");
        return opened;
    }

    /**
     * The channel of the legacy ZIP archive the decoder opened.
     */
    @NotNull
    GatedZipChannel requireOpenedZipChannel() {
        GatedZipChannel opened = this.zipChannel;
        assertNotNull(opened, "The decoder didn't open a legacy ZIP archive");
        return opened;
    }

    /**
     * Forgets the cached payload chunks, so the next payload reads come from the file again.
     */
    void clearPayloadCache() {
        synchronized (this.payloadChunkCacheLock) {
            this.payloadChunkCache.clear();
        }
    }

    @Override
    protected @NotNull RandomAccessFile openContainerFile(@NotNull File archiveFile) throws IOException {
        GatedFile opened = new GatedFile(archiveFile);
        this.file = opened;
        return opened;
    }

    @Override
    protected @NotNull ZipFile openZipFile(@NotNull File archiveFile) throws IOException {
        GatedZipChannel opened = new GatedZipChannel(archiveFile.toPath());
        this.zipChannel = opened;
        return opened.openZipFile();
    }

    @Override
    protected void initializeArchiveState() throws IOException {
        if (this.loadingGate != null) this.loadingGate.pass();
        super.initializeArchiveState();
    }

    /**
     * An archive file whose reads can wait at a gate.
     */
    static final class GatedFile extends RandomAccessFile {

        /** The length of every read, which are the pieces the decoder reads while holding its file lock. **/
        final List<Integer> readLengths = new CopyOnWriteArrayList<>();
        @Nullable
        volatile ReadGate readGate;
        private volatile boolean closed;
        private volatile boolean readAfterClose;

        private GatedFile(@NotNull File file) throws FileNotFoundException {
            super(file, "r");
        }

        @Override
        public int read(@NotNull byte[] buffer, int offset, int length) throws IOException {
            ReadGate gate = this.readGate;
            if (gate != null) {
                // Only the first read waits
                this.readGate = null;
                gate.pass();
            }
            if (this.closed) this.readAfterClose = true;
            this.readLengths.add(length);
            return super.read(buffer, offset, length);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

        /**
         * If the file is open, which is what the system sees as open file.
         */
        boolean isOpen() throws IOException {
            return this.getFD().valid();
        }

        /**
         * If something tried to read the file after it got closed.
         */
        boolean wasReadAfterClose() {
            return this.readAfterClose;
        }

        int largestRead() {
            int largest = 0;
            for (int length : this.readLengths) largest = Math.max(largest, length);
            return largest;
        }

    }

}
