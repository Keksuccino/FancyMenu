package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The bytes of an animated image file, like an APNG or GIF image. Parsed images only know where the compressed data of their frames lies,
 * so every decoded frame reads its data from here again.<br>
 * Reads are positional, so they never depend on earlier reads. Only one thread reads at a time: the parsing thread first, then one decoding task after the other.
 * Closing can happen on another thread, see {@link #close()}.
 */
public interface AnimatedImageSource extends AutoCloseable {

    /**
     * Reads bytes, starting at the given position of the source, into the remaining space of the buffer, like {@link java.nio.channels.FileChannel#read(ByteBuffer, long)}.
     *
     * @return How many bytes got read, or -1 if the position is at or after the end of the source.
     */
    int read(@NotNull ByteBuffer target, long position) throws IOException;

    /**
     * How many bytes the source has right now.
     */
    long size() throws IOException;

    /**
     * Releases the source. Closing it more than once does nothing.<br>
     * Works on any thread, also while another thread reads from the source: that read finishes or fails first, and all later reads fail,
     * so textures can release the file of their image right away when they get closed.
     */
    @Override
    void close();

}
