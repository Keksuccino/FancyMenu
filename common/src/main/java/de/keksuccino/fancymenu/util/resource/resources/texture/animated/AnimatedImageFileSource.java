package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An {@link AnimatedImageSource} that reads a file through a {@link FileChannel}, which stays open while the image is alive.<br><br>
 *
 * The file gets opened with NIO on purpose: on Windows, NIO channels share reading, writing and deleting with other programs
 * (only the NOSHARE options of {@code com.sun.nio.file.ExtendedOpenOption} forbid that), while {@link java.io.FileInputStream} and {@link java.io.RandomAccessFile}
 * don't share deleting. So users can still rename, replace and delete a file while it plays. Reads then continue with the bytes the channel has open,
 * and decoding notices bytes that changed in place through the checksums of the frame data.<br><br>
 *
 * A channel gets closed for good when a thread that reads from it gets interrupted, so sources must only be read by threads that never get interrupted,
 * like the loading threads and the decoding threads of animated textures. Closing it from another thread is safe though: the channel waits for a read
 * that runs right now and makes all later reads fail, so the file never gets read after it got closed.
 */
public final class AnimatedImageFileSource implements AnimatedImageSource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int COPY_BUFFER_SIZE = 64 * 1024;
    private static final int MAX_TEMP_FILE_ATTEMPTS = 16;

    @NotNull
    private final FileChannel channel;
    @NotNull
    private final Path path;

    private AnimatedImageFileSource(@NotNull FileChannel channel, @NotNull Path path) {
        this.channel = channel;
        this.path = path;
    }

    /**
     * Opens a file to read the image straight from it, without copying it.
     */
    @NotNull
    public static AnimatedImageFileSource open(@NotNull Path file) throws IOException {
        return new AnimatedImageFileSource(FileChannel.open(file, StandardOpenOption.READ), file);
    }

    /**
     * Copies the stream into a new temporary file in the directory and reads the image from there. The file gets deleted when the source gets closed. Does not close the stream.<br><br>
     *
     * The file gets created and opened with {@link StandardOpenOption#DELETE_ON_CLOSE} in one step, so it never exists without getting deleted later:
     * Linux and macOS remove its directory entry right after opening it (the open channel keeps its data readable), and Windows deletes it once its last handle closes,
     * which the system also does when the game crashes. Files that survive a power loss on Windows get removed together with FancyMenu's whole temp directory at the next start.
     *
     * @param filePrefix Start of the file name, like the format of the image, so leftover files can get told apart.
     */
    @NotNull
    public static AnimatedImageFileSource copyToTempFile(@NotNull InputStream in, @NotNull Path directory, @NotNull String filePrefix) throws IOException {
        Path file = null;
        FileChannel channel = null;
        for (int attempt = 1; channel == null; attempt++) {
            file = directory.resolve(filePrefix + "_" + Long.toHexString(ThreadLocalRandom.current().nextLong()) + ".tmp");
            try {
                channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);
            } catch (FileAlreadyExistsException ex) {
                if (attempt >= MAX_TEMP_FILE_ATTEMPTS) throw ex;
            }
        }
        try {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            ByteBuffer view = ByteBuffer.wrap(buffer);
            int read;
            while ((read = in.read(buffer)) >= 0) {
                view.limit(read).position(0);
                while (view.hasRemaining()) {
                    channel.write(view);
                }
            }
        } catch (Throwable throwable) {
            closeChannel(channel, file);
            throw throwable;
        }
        return new AnimatedImageFileSource(channel, file);
    }

    @Override
    public int read(@NotNull ByteBuffer target, long position) throws IOException {
        return this.channel.read(target, position);
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    @Override
    public void close() {
        closeChannel(this.channel, this.path);
    }

    private static void closeChannel(@NotNull FileChannel channel, @NotNull Path path) {
        try {
            channel.close();
        } catch (IOException ex) {
            LOGGER.warn("[FANCYMENU] Failed to close the file of an animated image: {}", path, ex);
        }
    }

}
