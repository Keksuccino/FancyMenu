package de.keksuccino.fancymenu.util.resources.audio;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class OggAudio implements IAudio {

    public final Library library = new Library();
    public final SoundEngineExecutor soundEngineExecutor = new SoundEngineExecutor();
    public final ChannelAccess channelAccess = new ChannelAccess(this.library, this.soundEngineExecutor);
    @Nullable
    public ChannelAccess.ChannelHandle channelHandle;
    @Nullable
    protected Channel channel;
    protected boolean muted = false;
    protected float volume = 0.0F;

    @NotNull
    public static OggAudio of(@NotNull InputStream in) {

        OggAudio audio = new OggAudio();

        try {

            String soundDevice = Minecraft.getInstance().options.soundDevice().get();
            audio.library.init("".equals(soundDevice) ? null : soundDevice, Minecraft.getInstance().options.directionalAudio().get());

            OggAudioStream stream = new OggAudioStream(in);
            ByteBuffer buffer = stream.readAll();
            SoundBuffer soundBuffer = new SoundBuffer(buffer, stream.getFormat());
            audio.channelHandle = audio.channelAccess.createHandle(Library.Pool.STATIC).join();
            audio.channelHandle.execute(channel1 -> {
                channel1.attachStaticBuffer(soundBuffer);
                audio.channel = channel1;
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return audio;

    }

    public void updateChannel() {
        if (this.channelHandle != null) this.channelHandle.execute(channel1 -> this.channel = channel1);
    }

    public void tryChannel(@NotNull Consumer<Channel> channelConsumer) {
        this.updateChannel();
        if (this.channel != null) channelConsumer.accept(this.channel);
    }

    @Override
    public void play() {
        this.tryChannel(Channel::play);
    }

    @Override
    public void pause() {
        this.tryChannel(Channel::pause);
    }

    @Override
    public void stop() {
        this.tryChannel(Channel::stop);
    }

    @Override
    public boolean isPlaying() {
        this.updateChannel();
        if (this.channel != null) return this.channel.playing();
        return false;
    }

    @Override
    public void setMuted(boolean muted) {
        //TODO make this work
        this.muted = muted;
    }

    @Override
    public boolean isMuted() {
        return this.muted;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        this.tryChannel(channel1 -> channel1.setVolume(volume));
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        return null;
    }

    @Override
    public boolean isReady() {
        return this.channelHandle != null;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

}
