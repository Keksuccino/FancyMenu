package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LayoutScreenAudioSettingsTest {

    @Test
    void newLayoutsPlayScreenAudiosAtFullVolumeOnTheMasterChannel() {
        Layout layout = new Layout("test-screen");

        assertEquals(1.0F, layout.openAudioVolume);
        assertEquals(SoundSource.MASTER, layout.openAudioSoundChannel);
        assertEquals(1.0F, layout.closeAudioVolume);
        assertEquals(SoundSource.MASTER, layout.closeAudioSoundChannel);
    }

    @Test
    void volumeAndChannelSurviveSavingAndLoading() {
        Layout layout = new Layout("test-screen");
        layout.openAudio = ResourceSupplier.audio("config/fancymenu/assets/open.ogg");
        layout.openAudioVolume = 0.35F;
        layout.openAudioSoundChannel = SoundSource.MUSIC;
        layout.closeAudio = ResourceSupplier.audio("config/fancymenu/assets/close.ogg");
        layout.closeAudioVolume = 0.0F;
        layout.closeAudioSoundChannel = SoundSource.PLAYERS;

        Layout reloaded = Layout.deserialize(layout.serialize(), null);

        assertNotNull(reloaded);
        assertEquals(0.35F, reloaded.openAudioVolume);
        assertEquals(SoundSource.MUSIC, reloaded.openAudioSoundChannel);
        assertEquals(0.0F, reloaded.closeAudioVolume);
        assertEquals(SoundSource.PLAYERS, reloaded.closeAudioSoundChannel);
    }

    @Test
    void settingsWithoutAnAudioAreKeptForWhenAnAudioGetsChosenLater() {
        Layout layout = new Layout("test-screen");
        layout.openAudioVolume = 0.6F;
        layout.closeAudioSoundChannel = SoundSource.AMBIENT;

        Layout copy = layout.copy();

        assertEquals(0.6F, copy.openAudioVolume);
        assertEquals(SoundSource.AMBIENT, copy.closeAudioSoundChannel);
    }

    @Test
    void layoutsSavedWithoutAudioSettingsKeepTheDefaults() {
        PropertyContainerSet legacy = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");
        meta.putProperty("identifier", "test-screen");
        legacy.putContainer(meta);

        Layout loaded = Layout.deserialize(legacy, null);

        assertNotNull(loaded);
        assertEquals(1.0F, loaded.openAudioVolume);
        assertEquals(SoundSource.MASTER, loaded.openAudioSoundChannel);
        assertEquals(1.0F, loaded.closeAudioVolume);
        assertEquals(SoundSource.MASTER, loaded.closeAudioSoundChannel);
    }

    @Test
    void brokenHandEditedValuesFallBackToSafeValues() {
        Layout loaded = Layout.deserialize(layoutWithAudioSettings("2.5", "not_a_channel", "NaN", ""), null);
        assertNotNull(loaded);
        assertEquals(1.0F, loaded.openAudioVolume);
        assertEquals(SoundSource.MASTER, loaded.openAudioSoundChannel);
        assertEquals(1.0F, loaded.closeAudioVolume);
        assertEquals(SoundSource.MASTER, loaded.closeAudioSoundChannel);

        Layout negative = Layout.deserialize(layoutWithAudioSettings("-1", "weather", "nope", "hostile"), null);
        assertNotNull(negative);
        assertEquals(0.0F, negative.openAudioVolume);
        assertEquals(SoundSource.WEATHER, negative.openAudioSoundChannel);
        assertEquals(1.0F, negative.closeAudioVolume);
        assertEquals(SoundSource.HOSTILE, negative.closeAudioSoundChannel);
    }

    @Test
    void stackingTakesVolumeAndChannelFromTheLayoutThatProvidesTheAudio() {
        ResourceSupplier<IAudio> openAudio = ResourceSupplier.audio("config/fancymenu/assets/open.ogg");
        ResourceSupplier<IAudio> closeAudio = ResourceSupplier.audio("config/fancymenu/assets/close.ogg");
        LayoutBase withAudios = new LayoutBase();
        withAudios.openAudio = openAudio;
        withAudios.openAudioVolume = 0.4F;
        withAudios.openAudioSoundChannel = SoundSource.MUSIC;
        withAudios.closeAudio = closeAudio;
        withAudios.closeAudioVolume = 0.2F;
        withAudios.closeAudioSoundChannel = SoundSource.BLOCKS;
        // A later layout without audios must not change the settings of audios it does not provide.
        LayoutBase withoutAudios = new LayoutBase();
        withoutAudios.openAudioVolume = 0.9F;
        withoutAudios.openAudioSoundChannel = SoundSource.VOICE;
        withoutAudios.closeAudioVolume = 0.8F;
        withoutAudios.closeAudioSoundChannel = SoundSource.NEUTRAL;

        LayoutBase stacked = LayoutBase.stackLayoutBases(withAudios, withoutAudios);

        assertSame(openAudio, stacked.openAudio);
        assertEquals(0.4F, stacked.openAudioVolume);
        assertEquals(SoundSource.MUSIC, stacked.openAudioSoundChannel);
        assertSame(closeAudio, stacked.closeAudio);
        assertEquals(0.2F, stacked.closeAudioVolume);
        assertEquals(SoundSource.BLOCKS, stacked.closeAudioSoundChannel);
    }

    @Test
    void stackingUsesTheSettingsOfTheLastLayoutWithAnAudio() {
        LayoutBase first = new LayoutBase();
        first.openAudio = ResourceSupplier.audio("config/fancymenu/assets/first.ogg");
        first.openAudioVolume = 0.4F;
        first.openAudioSoundChannel = SoundSource.MUSIC;
        LayoutBase last = new LayoutBase();
        last.openAudio = ResourceSupplier.audio("config/fancymenu/assets/last.ogg");

        LayoutBase stacked = LayoutBase.stackLayoutBases(first, last);

        assertSame(last.openAudio, stacked.openAudio);
        assertEquals(1.0F, stacked.openAudioVolume);
        assertEquals(SoundSource.MASTER, stacked.openAudioSoundChannel);
    }

    private static PropertyContainerSet layoutWithAudioSettings(String openVolume, String openChannel, String closeVolume, String closeChannel) {
        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");
        meta.putProperty("identifier", "test-screen");
        set.putContainer(meta);
        PropertyContainer settings = new PropertyContainer("screen_audio_settings");
        settings.putProperty("open_audio_volume", openVolume);
        settings.putProperty("open_audio_sound_channel", openChannel);
        settings.putProperty("close_audio_volume", closeVolume);
        settings.putProperty("close_audio_sound_channel", closeChannel);
        set.putContainer(settings);
        return set;
    }

}
