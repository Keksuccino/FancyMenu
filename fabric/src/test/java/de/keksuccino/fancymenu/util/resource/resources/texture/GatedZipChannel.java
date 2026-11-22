package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The channel of a ZIP archive whose reads can wait at a gate, and which remembers every read. A ZIP file opened with {@link #openZipFile()} reads its archive through it.<br>
 * Reads go to a real {@link FileChannel}, so they behave like the reads of real archives: a read of an interrupted thread closes the channel for good.
 */
public final class GatedZipChannel implements SeekableByteChannel {

    /** The length of every read, which are the pieces that got read from the archive. **/
    public final List<Integer> readLengths = new CopyOnWriteArrayList<>();
    @NotNull
    private final FileChannel channel;
    @Nullable
    public volatile ReadGate readGate;
    private volatile boolean closed;
    private volatile boolean readAfterClose;

    public GatedZipChannel(@NotNull Path file) throws IOException {
        this.channel = FileChannel.open(file, StandardOpenOption.READ);
    }

    /**
     * Opens a ZIP file that reads its archive through this channel.
     */
    @SuppressWarnings("deprecation")
    @NotNull
    public ZipFile openZipFile() throws IOException {
        return new ZipFile(this);
    }

    @Override
    public int read(ByteBuffer target) throws IOException {
        ReadGate gate = this.readGate;
        if (gate != null) {
            // Only the first read waits
            this.readGate = null;
            gate.pass();
        }
        if (this.closed) this.readAfterClose = true;
        this.readLengths.add(target.remaining());
        return this.channel.read(target);
    }

    @Override
    public int write(ByteBuffer source) {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        return this.channel.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        this.channel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    /**
     * If the archive is open, which is what the system sees as open file.
     */
    @Override
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.channel.close();
    }

    /**
     * If something tried to read the archive after it got closed.
     */
    public boolean wasReadAfterClose() {
        return this.readAfterClose;
    }

    public int largestRead() {
        int largest = 0;
        for (int length : this.readLengths) largest = Math.max(largest, length);
        return largest;
    }

}
