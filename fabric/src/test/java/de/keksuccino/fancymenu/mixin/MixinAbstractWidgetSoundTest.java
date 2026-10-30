package de.keksuccino.fancymenu.mixin;

import de.keksuccino.fancymenu.mixin.mixins.common.client.MixinAbstractWidget;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinAbstractWidgetSoundTest {

    @Test
    void customClickSoundRestartsWithItsChannelAndVolumeAndReplacesTheVanillaSound() throws Exception {
        TestWidget widget = new TestWidget();
        RecordingAudio sound = new RecordingAudio();
        // Another user of the same shared audio resource left different settings behind.
        sound.channel = SoundSource.MUSIC;
        sound.volume = 1.0F;
        widget.setCustomClickSoundFancyMenu(sound);
        widget.setCustomClickSoundVolumeFancyMenu(0.3F);
        widget.setCustomClickSoundChannelFancyMenu(SoundSource.PLAYERS);

        CallbackInfo info = playDownSoundHook(widget);

        assertTrue(info.isCancelled());
        assertEquals(List.of("stop", "play"), sound.operations);
        assertEquals(SoundSource.PLAYERS, sound.playedChannel);
        assertEquals(0.3F, sound.playedVolume);
    }

    @Test
    void widgetsWithoutCustomClickSoundKeepTheVanillaSound() throws Exception {
        TestWidget widget = new TestWidget();
        widget.setCustomClickSoundVolumeFancyMenu(0.3F);
        widget.setCustomClickSoundChannelFancyMenu(SoundSource.PLAYERS);

        assertFalse(playDownSoundHook(widget).isCancelled());
        assertFalse(widget.playCustomClickSoundFancyMenu());
    }

    @Test
    void customClickSoundsPlayAtFullVolumeOnTheMasterChannelByDefault() {
        TestWidget widget = new TestWidget();
        RecordingAudio sound = new RecordingAudio();
        sound.channel = SoundSource.AMBIENT;
        sound.volume = 0.1F;
        widget.setCustomClickSoundFancyMenu(sound);

        assertTrue(widget.playCustomClickSoundFancyMenu());
        assertEquals(SoundSource.MASTER, sound.playedChannel);
        assertEquals(1.0F, sound.playedVolume);
    }

    @Test
    void volumeIsClampedToTheValidRange() {
        TestWidget widget = new TestWidget();

        widget.setCustomClickSoundVolumeFancyMenu(4.0F);
        assertEquals(1.0F, widget.getCustomClickSoundVolumeFancyMenu());
        widget.setCustomClickSoundVolumeFancyMenu(-2.0F);
        assertEquals(0.0F, widget.getCustomClickSoundVolumeFancyMenu());
        widget.setCustomClickSoundVolumeFancyMenu(Float.NaN);
        assertEquals(1.0F, widget.getCustomClickSoundVolumeFancyMenu());
    }

    @Test
    void hoverAndUnhoverSoundsRestartWithTheirOwnChannelAndVolume() throws Exception {
        TestWidget widget = new TestWidget();
        RecordingAudio hover = new RecordingAudio();
        RecordingAudio unhover = new RecordingAudio();
        widget.setHoverSoundFancyMenu(hover);
        widget.setHoverSoundVolumeFancyMenu(0.4F);
        widget.setHoverSoundChannelFancyMenu(SoundSource.AMBIENT);
        widget.setUnhoverSoundFancyMenu(unhover);
        widget.setUnhoverSoundVolumeFancyMenu(0.7F);
        widget.setUnhoverSoundChannelFancyMenu(SoundSource.WEATHER);

        invokeSoundHandler(widget, "handleHoverSoundFancyMenu");
        invokeSoundHandler(widget, "handleUnhoverSoundFancyMenu");

        assertEquals(List.of("stop", "play"), hover.operations);
        assertEquals(SoundSource.AMBIENT, hover.playedChannel);
        assertEquals(0.4F, hover.playedVolume);
        assertEquals(List.of("stop", "play"), unhover.operations);
        assertEquals(SoundSource.WEATHER, unhover.playedChannel);
        assertEquals(0.7F, unhover.playedVolume);
    }

    @Test
    void hoverAndUnhoverSoundsPlayAtFullVolumeOnTheMasterChannelByDefault() throws Exception {
        TestWidget widget = new TestWidget();
        RecordingAudio hover = new RecordingAudio();
        hover.channel = SoundSource.MUSIC;
        hover.volume = 0.1F;
        widget.setHoverSoundFancyMenu(hover);

        invokeSoundHandler(widget, "handleHoverSoundFancyMenu");
        invokeSoundHandler(widget, "handleUnhoverSoundFancyMenu");

        assertEquals(SoundSource.MASTER, hover.playedChannel);
        assertEquals(1.0F, hover.playedVolume);
    }

    @Test
    void hoverAndUnhoverVolumesAreClampedToTheValidRange() {
        TestWidget widget = new TestWidget();

        widget.setHoverSoundVolumeFancyMenu(2.0F);
        widget.setUnhoverSoundVolumeFancyMenu(-1.0F);
        assertEquals(1.0F, widget.getHoverSoundVolumeFancyMenu());
        assertEquals(0.0F, widget.getUnhoverSoundVolumeFancyMenu());
        widget.setHoverSoundVolumeFancyMenu(Float.NaN);
        assertEquals(1.0F, widget.getHoverSoundVolumeFancyMenu());
    }

    @Test
    void resettingWidgetCustomizationsRestoresTheDefaultSoundSettings() {
        TestWidget widget = new TestWidget();
        widget.setCustomClickSoundFancyMenu(new RecordingAudio());
        widget.setCustomClickSoundVolumeFancyMenu(0.2F);
        widget.setCustomClickSoundChannelFancyMenu(SoundSource.WEATHER);
        widget.setHoverSoundFancyMenu(new RecordingAudio());
        widget.setHoverSoundVolumeFancyMenu(0.3F);
        widget.setHoverSoundChannelFancyMenu(SoundSource.BLOCKS);
        widget.setUnhoverSoundFancyMenu(new RecordingAudio());
        widget.setUnhoverSoundVolumeFancyMenu(0.4F);
        widget.setUnhoverSoundChannelFancyMenu(SoundSource.HOSTILE);

        widget.resetWidgetCustomizationsFancyMenu();

        assertNull(widget.getCustomClickSoundFancyMenu());
        assertEquals(1.0F, widget.getCustomClickSoundVolumeFancyMenu());
        assertEquals(SoundSource.MASTER, widget.getCustomClickSoundChannelFancyMenu());
        assertNull(widget.getHoverSoundFancyMenu());
        assertEquals(1.0F, widget.getHoverSoundVolumeFancyMenu());
        assertEquals(SoundSource.MASTER, widget.getHoverSoundChannelFancyMenu());
        assertNull(widget.getUnhoverSoundFancyMenu());
        assertEquals(1.0F, widget.getUnhoverSoundVolumeFancyMenu());
        assertEquals(SoundSource.MASTER, widget.getUnhoverSoundChannelFancyMenu());
    }

    private static void invokeSoundHandler(TestWidget widget, String handlerName) throws Exception {
        // The hover state listeners that call these handlers only get registered on the first render, which needs a running client.
        Method handler = MixinAbstractWidget.class.getDeclaredMethod(handlerName);
        handler.setAccessible(true);
        handler.invoke(widget);
    }

    private static CallbackInfo playDownSoundHook(TestWidget widget) throws Exception {
        // Invoke the production hook directly: a unit-test JVM does not apply Minecraft mixins.
        Method hook = MixinAbstractWidget.class.getDeclaredMethod("before_playDownSound_FancyMenu", SoundManager.class, CallbackInfo.class);
        hook.setAccessible(true);
        CallbackInfo info = new CallbackInfo("playDownSound", true);
        hook.invoke(widget, null, info);
        return info;
    }

    private static final class TestWidget extends MixinAbstractWidget {

        private boolean focused;

        private TestWidget() {
            this.active = true;
            this.visible = true;
            this.width = 100;
            this.height = 20;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public boolean isHoveredOrFocused() {
            return this.isHovered || this.focused;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

    }

    private static final class RecordingAudio implements IAudio {

        private final List<String> operations = new ArrayList<>();
        private SoundSource channel = SoundSource.MASTER;
        private float volume = 1.0F;
        @Nullable
        private SoundSource playedChannel;
        private float playedVolume = Float.NaN;

        @Override
        public void play() {
            this.operations.add("play");
            this.playedChannel = this.channel;
            this.playedVolume = this.volume;
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public void pause() {
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public void stop() {
            this.operations.add("stop");
        }

        @Override
        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public float getVolume() {
            return this.volume;
        }

        @Override
        public void setSoundChannel(@NotNull SoundSource channel) {
            this.channel = channel;
        }

        @Override
        public @NotNull SoundSource getSoundChannel() {
            return this.channel;
        }

        @Override
        public float getDuration() {
            return 0.0F;
        }

        @Override
        public float getPlayTime() {
            return 0.0F;
        }

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isLoadingCompleted() {
            return true;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

    }

}
