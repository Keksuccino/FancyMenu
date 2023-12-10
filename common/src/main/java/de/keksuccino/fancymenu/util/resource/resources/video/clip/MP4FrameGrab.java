package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;

public class MP4FrameGrab extends FrameGrab {

    @NotNull
    public static MP4FrameGrab createMP4FrameGrab(SeekableByteChannel in) throws JCodecException, IOException {
        FrameGrab grab = FrameGrab.createFrameGrab(in);
        return new MP4FrameGrab(grab.getVideoTrack(), grab.getDecoder());
    }

    public MP4FrameGrab(@NotNull SeekableDemuxerTrack videoTrack, @NotNull ContainerAdaptor decoder) {
        super(videoTrack, decoder);
    }

    @Nullable
    public MP4Frame nextFrame() throws IOException {
        Packet packet = this.getVideoTrack().nextFrame();
        if (packet == null) return null;
        Picture picture = this.getDecoder().decodeFrame(packet, getBuffer());
        return new MP4Frame(
                picture,
                new RationalLarge(packet.getPts(), packet.getTimescale()),
                new RationalLarge(packet.getDuration(), packet.getTimescale()),
                new Rational(0,0),
                (int) packet.getFrameNo(),
                packet.getTapeTimecode(),
                null
        );
    }

    protected byte[][] getBuffer() {
        try {
            Method m = FrameGrab.class.getDeclaredMethod("getBuffer");
            m.setAccessible(true);
            return (byte[][]) m.invoke(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new byte[0][];
    }

}
