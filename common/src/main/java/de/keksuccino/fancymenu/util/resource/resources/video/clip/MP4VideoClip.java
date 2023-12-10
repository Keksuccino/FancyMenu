package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.video.clip.exceptions.MP4VideoException;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL11;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;

@SuppressWarnings("unused")
public class MP4VideoClip implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static int videoInstanceCount = 0;

    @NotNull
    protected volatile PlayState playState = PlayState.STOPPED;
    protected volatile MP4FrameGrab frameGrab = null;
    protected volatile File sourceFile;
    protected int currentFrame = -1;
    protected long startTime = -1;
    @NotNull
    protected ALAudioClip audioClip;
    protected float audioSampleRate = -1;
    protected final int audioSource;
    protected volatile boolean audioReady = false;
    protected volatile boolean audioFailed = false;
    protected volatile float audioVolume = 1.0F;
    @NotNull
    protected volatile SoundSource audioSoundSource = SoundSource.MASTER;
    @NotNull
    protected MP4MetaData metaData = new MP4MetaData();
    @NotNull
    protected ResourceLocation renderLocation;
    @NotNull
    protected MP4FrameImage renderFrame;
    protected boolean firstFrameUploaded = false;
    @Nullable
    protected volatile BufferedImage queuedFrameForUpload;
    protected volatile boolean videoThreadRunning = false;
    protected volatile boolean audioStartedAfterUnpauseOrPlay = false;
    protected volatile boolean closed = false;

    public MP4VideoClip(@NotNull File sourceFile) throws Exception {

        RenderSystem.assertOnRenderThread(); //ALAudioClips need to get created on the main thread

        this.sourceFile = Objects.requireNonNull(sourceFile);
        if (!this.sourceFile.isFile()) throw new FileNotFoundException("Source MP4 file not found!");

        ALAudioClip clip = ALAudioClip.create();
        if (clip == null) throw new MP4VideoException("Failed to create audio clip for MP4!");
        this.audioClip = clip;
        this.audioSource = getOpenAlAudioSource(this.audioClip);
        if (this.audioSource == -1) throw new MP4VideoException("Failed to get OpenAL source of audio clip!");

        this.parseMetaData();

        this.renderLocation = new ResourceLocation("fancymenu_mp4_render_location_" + videoInstanceCount);
        videoInstanceCount++;
        this.renderFrame = MP4FrameImage.build(this.renderLocation, this.metaData.width, this.metaData.height);

        this.startFrameGrabSetupThread();

        this.startAudioDecodingThread();

    }

    protected void startFrameGrabSetupThread() {
        new Thread(this::prepareFrameGrab).start();
    }

    protected void prepareFrameGrab() {
        try {
            this.frameGrab = MP4FrameGrab.createMP4FrameGrab(NIOUtils.readableChannel(this.sourceFile));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void startAudioDecodingThread() {
        if (this.closed) return;
        new Thread(this::decodeAudio).start();
    }

    protected void decodeAudio() {
        try {
            this.tryCreateAndSetAudioClipBuffer();
            this.audioReady = true;
            if (this.closed) CloseableUtils.closeQuietly(this.audioClip);
        } catch (Exception ex) {
            this.audioReady = false;
            this.audioFailed = true;
            ex.printStackTrace();
        }
    }

    protected void tryCreateAndSetAudioClipBuffer() {
        AudioInputStream stream = null;
        ByteArrayInputStream byteIn = null;
        try {
            byteIn = new ByteArrayInputStream(MP4AudioDecoder.decode(this.sourceFile));
            stream = AudioSystem.getAudioInputStream(byteIn);
            this.audioSampleRate = stream.getFormat().getSampleRate();
            ByteBuffer byteBuffer = ALUtils.readStreamIntoBuffer(stream);
            ALAudioBuffer audioBuffer = new ALAudioBuffer(byteBuffer, stream.getFormat());
            this.audioClip.setStaticBuffer(audioBuffer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        CloseableUtils.closeQuietly(byteIn);
        CloseableUtils.closeQuietly(stream);
    }

    public void startVideoPlaybackThread() {

        if (this.closed) return;
        if (this.videoThreadRunning) return;

        this.videoThreadRunning = true;
        new Thread(this::videoPlayback).start();

    }

    @SuppressWarnings("all")
    protected void videoPlayback() {

        try {

            while (this.isPlaying() && !this.closed) {

                //If video not ready yet, sleep for 100ms and try again
                if (!this.isReady()) {
                    //TODO remove debug
                    LOGGER.info("############ NOT READY YET! Sleeping for 100ms!");
                    Thread.sleep(100L);
                    continue;
                }

                if (!this.isAudioWorking()) {
                    //TODO remove debug
                    LOGGER.info("############# AUDIO NOT WORKING (YET)! Sleeping for 100ms!");
                    Thread.sleep(100L);
                    continue;
                }

                if (!this.audioStartedAfterUnpauseOrPlay && !this.audioClip.isPlaying()) {
                    this.audioStartedAfterUnpauseOrPlay = true;
                    this.audioClip.setVolume(this.audioVolume);
                    this.audioClip.setSoundChannel(this.audioSoundSource);
                    this.audioClip.play();
                }

                //Set start time ONCE after starting the video
                if (this.startTime == -1) this.startTime = System.currentTimeMillis();

                long millis = System.currentTimeMillis() - this.startTime;
                int frame = (int) (millis / 1000D * this.metaData.videoFps);

                if (this.currentFrame != frame) {
                    MP4Frame next = this.frameGrab.nextFrame();
                    if (next != null) { // set next frame
                        this.currentFrame = frame;
                        this.queuedFrameForUpload = next.getBufferedImage();
                    } else { // end of video
                        this.stop();
                        break;
                    }
                }

                //Sleep 10ms after each tick to not kill the CPU
                Thread.sleep(10L);

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!this.closed) {
            if (this.isPaused()) {
                try {
                    if (this.isAudioWorking()) this.audioClip.pause();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                this.startTime = -1;
                this.currentFrame = -1;
                this.stop();
                this.resetVideo();
                this.resetAudio();
            }
        }

        this.videoThreadRunning = false;

    }

    protected void parseMetaData() {

        FileChannelWrapper wrapper = null;
        MP4Demuxer demuxer = null;
        DemuxerTrackMeta videoMeta;
        MovieBox box;

        try {

            wrapper = NIOUtils.readableChannel(this.sourceFile);
            demuxer = MP4Demuxer.createMP4Demuxer(wrapper);
            videoMeta = demuxer.getVideoTrack().getMeta();
            box = demuxer.getMovie();

            this.metaData.width = Math.max(1, box.getDisplaySize().getWidth());
            this.metaData.height = Math.max(1, box.getDisplaySize().getHeight());
            this.metaData.pureRef = box.isPureRefMovie();
            this.metaData.timescale = Math.max(1, box.getTimescale());
            if (videoMeta != null) {
                this.metaData.totalVideoDuration = Math.max(1.0D, videoMeta.getTotalDuration());
                this.metaData.totalVideoFrames = Math.max(1, videoMeta.getTotalFrames());
            }
            this.metaData.videoFps = Math.max(1.0D, (double)this.metaData.totalVideoFrames / this.metaData.totalVideoDuration);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CloseableUtils.closeQuietly(demuxer);
        CloseableUtils.closeQuietly(wrapper);

    }

    protected void resetVideo() {
        try {
            if (this.isReady()) {
                this.frameGrab.seekToFramePrecise(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void resetAudio() {
        try {
            if (this.isReady()) {
                if (this.isAudioWorking()) this.audioClip.stop();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isReady() {
        if (this.closed) return false;
        if (this.frameGrab == null) return false;
        if (this.audioReady || this.audioFailed) return true;
        return false;
    }

    public boolean isAudioWorking() {
        return (this.audioReady && !this.audioFailed && this.audioClip.isValidOpenAlSource());
    }

    /**
     * If the video is stopped, this will start the video.<br>
     * If the video is paused, this will resume the video.<br>
     * If the video is already playing, this will do nothing.
     */
    public void play() {
        if (this.closed) return;
        if (this.isPlaying()) return;
        this.audioStartedAfterUnpauseOrPlay = false;
        this.playState = PlayState.PLAYING;
        this.startVideoPlaybackThread();
    }

    public boolean isPlaying() {
        return (this.playState == PlayState.PLAYING);
    }

    /**
     * Pauses the video if it is currently playing.<br>
     * Use {@link MP4VideoClip#play()} or {@link MP4VideoClip#resume()} to resume the video.
     */
    public void pause() {
        if (!this.isPlaying()) return;
        this.playState = PlayState.PAUSED;
    }

    public boolean isPaused() {
        return (this.playState == PlayState.PAUSED);
    }

    /**
     * Stops the video.<br>
     * This resets the video, so it will start playing from the beginning the next time it gets started via {@link MP4VideoClip#play()}.
     */
    public void stop() {
        this.playState = PlayState.STOPPED;
    }

    public boolean isStopped() {
        return (this.playState == PlayState.STOPPED);
    }

    /**
     * Will resume the video if it is paused.<br>
     * Does nothing if the video is stopped or already playing.
     */
    public void resume() {
        if (!this.isPaused()) return;
        this.play();
    }

    /**
     * Value between 0.0F and 1.0F
     */
    public float getVolume() {
        return this.audioVolume;
    }

    /**
     * Value between 0.0F and 1.0F
     */
    public void setVolume(float volume) {
        this.audioVolume = volume;
        try {
            if (this.isAudioWorking()) this.audioClip.setVolume(volume);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @NotNull
    public SoundSource getSoundSource() {
        return this.audioSoundSource;
    }

    public void setSoundSource(@NotNull SoundSource soundSource) {
        this.audioSoundSource = Objects.requireNonNull(soundSource);
        try {
            if (this.isAudioWorking()) this.audioClip.setSoundChannel(soundSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getAudioPlayedMillis() {
        if (this.isReady() && this.isAudioWorking()) {
            try {
                float sampleOffset = AL11.alGetSourcef(this.audioSource, AL11.AL_SAMPLE_OFFSET);
                ALErrorHandler.checkOpenAlError();
                return (long) ((sampleOffset / this.audioSampleRate) * 1000.0F);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return 0L;
    }

    @NotNull
    public PlayState getPlayState() {
        return this.playState;
    }

    public double getFps() {
        return this.metaData.videoFps;
    }

    public File getSourceFile() {
        return this.sourceFile;
    }

    /**
     * Returns the current video frame or -1 if the video didn't start yet.
     */
    public int getCurrentFrame() {
        return this.currentFrame;
    }

    @NotNull
    public MP4MetaData getMetaData() {
        return this.metaData;
    }

    @Nullable
    public ResourceLocation getRenderResourceLocation() {

        if (this.closed) return null;
        if (!this.isReady()) return null;

        //Upload new frame if queue not empty
        BufferedImage queued = this.queuedFrameForUpload;
        this.queuedFrameForUpload = null;
        if (queued != null) {
            this.renderFrame.upload(queued);
            this.firstFrameUploaded = true;
        }

        if (!this.firstFrameUploaded) return null;

        return this.renderLocation;

    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        this.playState = PlayState.STOPPED;
        this.audioClip.closeQuietly();
        CloseableUtils.closeQuietly(this.renderFrame);
        try {
            Minecraft.getInstance().getTextureManager().release(this.renderLocation);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.audioReady = false;
        this.audioFailed = false;
        this.frameGrab = null;
        this.queuedFrameForUpload = null;
    }

    public boolean isClosed() {
        return this.closed;
    }

    protected static int getOpenAlAudioSource(@NotNull ALAudioClip clip) {
        try {
            Field f = clip.getClass().getDeclaredField("source");
            f.setAccessible(true);
            return (int) f.get(clip);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public enum PlayState {
        STOPPED,
        PLAYING,
        PAUSED
    }

}