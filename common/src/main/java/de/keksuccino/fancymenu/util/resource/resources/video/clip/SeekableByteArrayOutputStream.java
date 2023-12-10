package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * A simple seekable version of Java's {@link ByteArrayOutputStream}.
 */
public class SeekableByteArrayOutputStream extends ByteArrayOutputStream {

    public void seek(int position) {
        this.count = position;
    }

    @Override
    public synchronized void reset() {
        throw new RuntimeException("Reset is not supported!");
    }

    @Override
    public byte[] toByteArray() {
        return Arrays.copyOf(this.buf, this.buf.length);
    }

}
