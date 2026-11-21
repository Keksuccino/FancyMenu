package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.jetbrains.annotations.NotNull;

/**
 * One frame of an APNG image: the canvas region it covers, how long it stays visible, how it gets composed onto the canvas and where its compressed image data lies in the source.<br>
 * Frames keep no image data, so a parsed image needs only a few bytes per frame, no matter how big its file is.
 *
 * @param delayNanos How long the frame stays visible, exactly as stored in the file.
 * @param dataChunks The chunks of the source that hold the zlib stream of the frame.
 */
public record ApngFrame(int x, int y, int width, int height, long delayNanos, int disposeOp, int blendOp, @NotNull DataChunks dataChunks) {

    /** The frame stays on the canvas as it is when the next frame gets composed. **/
    public static final int DISPOSE_OP_NONE = 0;
    /** The frame region gets cleared to fully transparent black before the next frame gets composed. **/
    public static final int DISPOSE_OP_BACKGROUND = 1;
    /** The frame region gets reverted to what it showed before this frame got composed. **/
    public static final int DISPOSE_OP_PREVIOUS = 2;
    /** The frame replaces all pixels of its region, alpha included. **/
    public static final int BLEND_OP_SOURCE = 0;
    /** The frame gets alpha-composited over the pixels of its region. **/
    public static final int BLEND_OP_OVER = 1;

    public boolean coversCanvas(int canvasWidth, int canvasHeight) {
        return (this.x == 0) && (this.y == 0) && (this.width == canvasWidth) && (this.height == canvasHeight);
    }

    /**
     * Where the zlib stream of a frame lies in the source: the IDAT or fdAT chunks it got split into, in file order. Every chunk got its CRC verified when the image got parsed.
     *
     * @param type The chunk type of all chunks, IDAT or fdAT, as 4-byte big-endian value. The data of fdAT chunks starts with a 4-byte sequence number.
     * @param positions The source position of every chunk, where its length field starts.
     * @param lengths The value of the length field of every chunk, so the sequence numbers of fdAT chunks are included.
     */
    public record DataChunks(int type, @NotNull long[] positions, @NotNull int[] lengths) {

        /** Bytes of a chunk around its data: the length, the type and the CRC. **/
        static final int CHUNK_OVERHEAD = 12;
        static final int SEQUENCE_NUMBER_LENGTH = 4;

        public int count() {
            return this.positions.length;
        }

        /**
         * Bytes before the zlib data of every chunk: the length and type fields, and the sequence number of fdAT chunks.
         */
        int headerLength() {
            return (this.type == ApngReader.CHUNK_FDAT) ? 8 + SEQUENCE_NUMBER_LENGTH : 8;
        }

        /**
         * The source position right after the CRC of a chunk.
         */
        long end(int index) {
            return this.positions[index] + CHUNK_OVERHEAD + this.lengths[index];
        }

        /**
         * How many bytes of zlib data the chunks hold together.
         */
        public long dataSize() {
            long size = 0L;
            int prefix = this.headerLength() - 8;
            for (int length : this.lengths) {
                size += length - prefix;
            }
            return size;
        }

    }

}
