package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

/**
 * Reads the compressed data of APNG frames from the source of their image and hands it to an {@link Inflater}, piece by piece, whenever the inflater needs more input.<br>
 * Chunks that lie right after each other in the source get read together, with as few reads as the reusable buffer allows. Bytes outside the chunks of a frame never get read.<br><br>
 *
 * Every chunk gets checked again while it gets read: its length field and type must match what got parsed, and its CRC must still match its bytes.
 * The buffer is smaller than big chunks, so the inflater gets the data of a chunk before its CRC can get checked at the end of the chunk.
 * That's why {@link #finishChunk()} must get called once decoding a frame stopped, also when its data turned out corrupted, so changed bytes never go unnoticed
 * and never get mistaken for corrupted data. A mismatch, or a source that ends too early, throws an {@link ApngSourceChangedException}.<br>
 * Not thread-safe, every decoder has its own instance.
 */
final class ApngFrameDataInput {

    /** Biggest read buffer. Images whose data lies in smaller runs of chunks get a buffer that just fits their biggest run. **/
    private static final int MAX_BUFFER_SIZE = 64 * 1024;
    /** Smallest read buffer, which always fits a chunk header with sequence number or a CRC, plus the bytes before it that the end of the buffer cut off. **/
    private static final int MIN_BUFFER_SIZE = 64;
    private static final int STATE_HEADER = 0;
    private static final int STATE_DATA = 1;
    private static final int STATE_CRC = 2;
    private static final ApngFrame.DataChunks NO_CHUNKS = new ApngFrame.DataChunks(ApngReader.CHUNK_IDAT, new long[0], new int[0]);

    @NotNull
    private final ApngSource source;
    @NotNull
    private final byte[] buffer;
    @NotNull
    private final ByteBuffer bufferView;
    private final CRC32 crc = new CRC32();

    @NotNull
    private ApngFrame.DataChunks chunks = NO_CHUNKS;
    private int chunkIndex;
    private int state = STATE_HEADER;
    /** Bytes of zlib data of the current chunk that did not get read yet. **/
    private int remainingData;
    /** The source position of the first byte in the buffer. **/
    private long bufferStart;
    private int bufferPosition;
    private int bufferLimit;
    /** Where the run of chunks that lie right after each other ends, which the buffer reads right now. Reads never go past it. **/
    private long runEnd;

    ApngFrameDataInput(@NotNull ApngImage image) {
        this.source = image.getSource();
        this.buffer = new byte[resolveBufferSize(image)];
        this.bufferView = ByteBuffer.wrap(this.buffer);
    }

    /**
     * Starts reading the data of a frame at its first chunk.
     */
    void start(@NotNull ApngFrame.DataChunks dataChunks) {
        this.chunks = dataChunks;
        this.chunkIndex = 0;
        this.state = STATE_HEADER;
        this.remainingData = 0;
        this.bufferPosition = 0;
        this.bufferLimit = 0;
    }

    /**
     * Gives the inflater the next data of the frame. Must only get called once the inflater used all data it got before, because the buffer gets reused.
     *
     * @return False if the frame has no data left.
     */
    boolean feed(@NotNull Inflater inflater) throws IOException {
        while (true) {
            if (this.state == STATE_HEADER) {
                if (this.chunkIndex >= this.chunks.count()) return false;
                this.readHeader();
            } else if ((this.state == STATE_DATA) && (this.remainingData > 0)) {
                int count = this.takeData();
                inflater.setInput(this.buffer, this.bufferPosition - count, count);
                return true;
            } else {
                this.checkCrc();
            }
        }
    }

    /**
     * Checks the chunk the inflater got data from last: reads the rest of its data that the inflater didn't need and compares its CRC.
     * Must get called once decoding the frame stopped, no matter why.
     */
    void finishChunk() throws IOException {
        if (this.state == STATE_HEADER) return;
        while (this.remainingData > 0) {
            this.takeData();
        }
        this.checkCrc();
    }

    private void readHeader() throws IOException {
        ApngFrame.DataChunks dataChunks = this.chunks;
        int index = this.chunkIndex;
        if ((index == 0) || (dataChunks.positions()[index] != dataChunks.end(index - 1))) this.startRun(index);
        int headerLength = dataChunks.headerLength();
        this.ensureBuffered(headerLength);
        int length = ApngReader.readInt(this.buffer, this.bufferPosition);
        int type = ApngReader.readInt(this.buffer, this.bufferPosition + 4);
        if ((length != dataChunks.lengths()[index]) || (type != dataChunks.type())) {
            throw new ApngSourceChangedException("A data chunk of the APNG image does not match the chunk that got parsed anymore");
        }
        // The CRC covers the type and all data, the sequence number of fdAT chunks included
        this.crc.reset();
        this.crc.update(this.buffer, this.bufferPosition + 4, headerLength - 4);
        this.bufferPosition += headerLength;
        this.remainingData = length - (headerLength - 8);
        this.state = STATE_DATA;
    }

    /**
     * Runs the next data bytes of the current chunk that are in the buffer through the CRC, after reading more if the buffer has none left.
     *
     * @return How many bytes got taken. They end at the new buffer position.
     */
    private int takeData() throws IOException {
        if (this.bufferPosition == this.bufferLimit) this.fillBuffer(1);
        int count = Math.min(this.remainingData, this.bufferLimit - this.bufferPosition);
        this.crc.update(this.buffer, this.bufferPosition, count);
        this.bufferPosition += count;
        this.remainingData -= count;
        return count;
    }

    private void checkCrc() throws IOException {
        this.ensureBuffered(4);
        int storedCrc = ApngReader.readInt(this.buffer, this.bufferPosition);
        this.bufferPosition += 4;
        if (storedCrc != (int) this.crc.getValue()) throw new ApngSourceChangedException("The CRC of a data chunk of the APNG image does not match its bytes anymore");
        this.chunkIndex++;
        this.state = STATE_HEADER;
    }

    /**
     * Points the buffer at a chunk that does not follow the chunk before it, and finds where the run of chunks that follow it ends.
     */
    private void startRun(int index) {
        ApngFrame.DataChunks dataChunks = this.chunks;
        long end = dataChunks.end(index);
        for (int next = index + 1; (next < dataChunks.count()) && (dataChunks.positions()[next] == end); next++) {
            end = dataChunks.end(next);
        }
        this.runEnd = end;
        this.bufferStart = dataChunks.positions()[index];
        this.bufferPosition = 0;
        this.bufferLimit = 0;
    }

    private void ensureBuffered(int count) throws IOException {
        if ((this.bufferLimit - this.bufferPosition) < count) this.fillBuffer(count);
    }

    /**
     * Moves the bytes that did not get used yet to the start of the buffer and reads as much of the rest of the run after them as fits.
     *
     * @param count How many bytes the buffer must have at least afterwards.
     */
    private void fillBuffer(int count) throws IOException {
        int unused = this.bufferLimit - this.bufferPosition;
        if (unused > 0) System.arraycopy(this.buffer, this.bufferPosition, this.buffer, 0, unused);
        this.bufferStart += this.bufferPosition;
        this.bufferPosition = 0;
        this.bufferLimit = unused;
        int wanted = (int) Math.max(0L, Math.min(this.buffer.length - unused, this.runEnd - (this.bufferStart + unused)));
        this.bufferView.limit(unused + wanted).position(unused);
        while (this.bufferLimit < count) {
            int read;
            try {
                read = this.source.read(this.bufferView, this.bufferStart + this.bufferLimit);
            } catch (IOException ex) {
                throw new ApngSourceChangedException("Failed to read the frame data of the APNG image from its source", ex);
            }
            if (read <= 0) throw new ApngSourceChangedException("The source of the APNG image ends before the frame data, so it got shorter");
            this.bufferLimit += read;
        }
    }

    private static int resolveBufferSize(@NotNull ApngImage image) {
        long biggestRun = 0L;
        for (int frame = 0; (frame < image.getFrameCount()) && (biggestRun < MAX_BUFFER_SIZE); frame++) {
            ApngFrame.DataChunks dataChunks = image.getFrame(frame).dataChunks();
            long runStart = 0L;
            for (int i = 0; i < dataChunks.count(); i++) {
                if ((i == 0) || (dataChunks.positions()[i] != dataChunks.end(i - 1))) runStart = dataChunks.positions()[i];
                biggestRun = Math.max(biggestRun, dataChunks.end(i) - runStart);
            }
        }
        return (int) Math.max(MIN_BUFFER_SIZE, Math.min(MAX_BUFFER_SIZE, biggestRun));
    }

}
