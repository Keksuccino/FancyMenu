package de.keksuccino.fancymenu.util.resource.resources.audio.wav;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;

public class WavHeader {

    private static final Logger LOGGER = LogManager.getLogger();

    private int chunkSize;
    private int dataSize;
    private int sampleRate;
    private int bitsPerSample;
    private int channels;
    private int byteRate;
    private int blockAlign;

    // Modified to read from InputStream
    public static WavHeader read(InputStream inputStream) throws IOException {

        WavHeader header = new WavHeader();
        byte[] buffer = new byte[44]; // Buffer for initial header data

        // Read basic header (first 36 bytes)
        if (inputStream.read(buffer, 0, 36) != 36) {
            throw new IOException("Invalid WAV header - too short");
        }

        // Verify RIFF header
        String riff = new String(buffer, 0, 4);
        if (!riff.equals("RIFF")) {
            throw new IOException("Invalid RIFF header: " + riff);
        }

        // Read chunk size (file size - 8)
        header.chunkSize = readLittleEndian(buffer, 4, 4);

        // Verify WAVE format
        String wave = new String(buffer, 8, 4);
        if (!wave.equals("WAVE")) {
            throw new IOException("Invalid WAVE header: " + wave);
        }

        // Read format subchunk
        String fmt = new String(buffer, 12, 4);
        if (!fmt.equals("fmt ")) {
            throw new IOException("Invalid fmt header: " + fmt);
        }

        // Read format parameters
        header.channels = readLittleEndian(buffer, 22, 2);
        header.sampleRate = readLittleEndian(buffer, 24, 4);
        header.byteRate = readLittleEndian(buffer, 28, 4);
        header.blockAlign = readLittleEndian(buffer, 32, 2);
        header.bitsPerSample = readLittleEndian(buffer, 34, 2);

        // Skip remaining format chunk data (if any)
        int fmtChunkSize = readLittleEndian(buffer, 16, 4);
        int extraBytes = fmtChunkSize - 16; // Standard fmt chunk is 16 bytes
        if (extraBytes > 0) {
            inputStream.skip(extraBytes);
        }

        // Scan through chunks to find 'data' chunk
        byte[] chunkHeader = new byte[8];
        boolean foundData = false;

        while (inputStream.read(chunkHeader) == 8) {
            String chunkId = new String(chunkHeader, 0, 4);
            int chunkSize = readLittleEndian(chunkHeader, 4, 4);

            if (chunkId.equals("data")) {
                header.dataSize = chunkSize;
                foundData = true;
                break;
            }

            // Skip chunk data
            long skipped = inputStream.skip(chunkSize);
            if (skipped != chunkSize) {
                LOGGER.warn("[FANCYMENU] WAV Header - Failed to skip full chunk data");
                break;
            }

            // Handle word alignment padding
            if (chunkSize % 2 != 0) {
                inputStream.skip(1);
            }
        }

        if (!foundData) {
            LOGGER.warn("[FANCYMENU] WAV Header - No data chunk found");
        }

        return header;
    }

    private static int readLittleEndian(byte[] data, int offset, int bytes) {
        int value = 0;
        for (int i = 0; i < bytes; i++) {
            value |= (data[offset + i] & 0xFF) << (i * 8);
        }
        return value;
    }

    public float getDurationInSeconds() {
        if (sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0 || dataSize <= 0) {
            LOGGER.warn("[FANCYMENU] WAV Header - Invalid values for duration calculation:");
            LOGGER.warn("  Sample Rate: {}", sampleRate);
            LOGGER.warn("  Channels: {}", channels);
            LOGGER.warn("  Bits Per Sample: {}", bitsPerSample);
            LOGGER.warn("  Data Size: {}", dataSize);
            return 0.0f;
        }
        int bytesPerSample = (bitsPerSample / 8) * channels;
        long totalSamples = dataSize / bytesPerSample;
        return (float) totalSamples / sampleRate;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getChannels() {
        return channels;
    }

}