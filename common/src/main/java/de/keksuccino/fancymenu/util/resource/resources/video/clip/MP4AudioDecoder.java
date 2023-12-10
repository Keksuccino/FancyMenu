package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.video.clip.exceptions.MP4AudioDecodingException;
import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.util.List;
import java.util.Objects;

/**
 * Mainly used to "convert" the AAC audio of MP4 files to WAV, to make it compatible with OpenAL.<br>
 * This class borrows most of its code from JAAD's {@code WaveFileWriter} class.<br><br>
 *
 * JAAD Copyright (c) in-somnia.<br>
 * JAAD fork used in this project is by Tianscar.
 */
public class MP4AudioDecoder implements Closeable {

    private static final int HEADER_LENGTH = 44;
    private static final int RIFF = 1380533830;
    private static final long WAVE_FMT = 6287401410857104416L;
    private static final int DATA = 1684108385;
    private static final int BYTE_MASK = 0xFF;

    private final SeekableByteArrayOutputStream out;
    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;
    private int bytesWritten;

    public static byte[] decode(@NotNull File mp4File) throws MP4AudioDecodingException {
        Objects.requireNonNull(mp4File);
        MP4AudioDecoder decoder = null;
        byte[] array = new byte[0];
        Exception exception = null;
        try {
            final MP4InputStream is = MP4InputStream.open(new RandomAccessFile(mp4File, "r"));
            final MP4Container cont = new MP4Container(is);
            final Movie movie = cont.getMovie();
            final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
            if(tracks.isEmpty()) throw new Exception("No AAC tracks found in MP4 file!");
            final AudioTrack track = (AudioTrack) tracks.get(0);
            decoder = new MP4AudioDecoder(track.getSampleRate(), track.getChannelCount(), track.getSampleSize());
            final Decoder dec = Decoder.create(track.getDecoderSpecificInfo().getData());
            Frame frame;
            final SampleBuffer buf = new SampleBuffer();
            while(track.hasMoreFrames()) {
                frame = track.readNextFrame();
                dec.decodeFrame(frame.getData(), buf);
                decoder.write(buf.getData());
            }
            decoder.finishWriting();
            array = decoder.toByteArray();
        } catch (Exception ex) {
            exception = ex;
        }
        CloseableUtils.closeQuietly(decoder);
        if (exception != null) throw new MP4AudioDecodingException(exception);
        return array;
    }

    protected MP4AudioDecoder(int sampleRate, int channels, int bitsPerSample) throws IOException {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        bytesWritten = 0;
        out = new SeekableByteArrayOutputStream();
        out.write(new byte[HEADER_LENGTH]); //space for the header
    }

    protected void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    protected void write(byte[] data, int off, int len) throws IOException {
        //convert to little endian
        byte tmp;
        for (int i = off; i<off+data.length; i += 2) {
            tmp = data[i+1];
            data[i+1] = data[i];
            data[i] = tmp;
        }
        out.write(data, off, len);
        bytesWritten += data.length;
    }

    protected void write(short[] data) {
        write(data, 0);
    }

    protected void write(short[] data, int off) {
        for (int i = off; i<off+data.length; i++) {
            out.write(data[i]&BYTE_MASK);
            out.write((data[i]>>8)&BYTE_MASK);
            bytesWritten += 2;
        }
    }

    protected void writeWaveHeader() throws IOException {
        this.out.seek(0);
        final int bytesPerSec = (bitsPerSample+7)/8;
        this.writeInt(RIFF); //wave label
        this.writeInt(Integer.reverseBytes(bytesWritten+36)); //length in bytes without header
        this.writeLong(WAVE_FMT);
        this.writeInt(Integer.reverseBytes(16)); //length of pcm format declaration area
        this.writeShort(Short.reverseBytes((short) 1)); //is PCM
        this.writeShort(Short.reverseBytes((short) channels)); //number of channels
        this.writeInt(Integer.reverseBytes(sampleRate)); //sample rate
        this.writeInt(Integer.reverseBytes(sampleRate*channels*bytesPerSec)); //bytes per second
        this.writeShort(Short.reverseBytes((short) (channels*bytesPerSec))); //bytes per sample time
        this.writeShort(Short.reverseBytes((short) bitsPerSample)); //bits per sample
        this.writeInt(DATA); //data section label
        this.writeInt(Integer.reverseBytes(bytesWritten)); //length of raw pcm data in bytes
    }

    protected void writeInt(int v) {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>>  0) & 0xFF);
    }

    protected void writeLong(long v) {
        out.write((int)(v >>> 56) & 0xFF);
        out.write((int)(v >>> 48) & 0xFF);
        out.write((int)(v >>> 40) & 0xFF);
        out.write((int)(v >>> 32) & 0xFF);
        out.write((int)(v >>> 24) & 0xFF);
        out.write((int)(v >>> 16) & 0xFF);
        out.write((int)(v >>>  8) & 0xFF);
        out.write((int)(v >>>  0) & 0xFF);
    }

    protected void writeShort(int v) {
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 0) & 0xFF);
    }

    protected void finishWriting() throws IOException {
        this.writeWaveHeader();
    }

    protected byte[] toByteArray() {
        return this.out.toByteArray();
    }

    public void close() throws IOException {
        out.close();
    }

}
