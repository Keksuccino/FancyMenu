package de.keksuccino.fancymenu.util;

import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SerializationHelperSoundSourceTest {

    @Test
    void everyChannelGetsResolvedByItsName() {
        for (SoundSource channel : SoundSource.values()) {
            assertEquals(channel, SerializationHelper.INSTANCE.deserializeSoundSource(channel.getName()));
            assertEquals(channel, SerializationHelper.INSTANCE.deserializeSoundSource(SoundSource.MASTER, channel.getName()));
        }
    }

    @Test
    void namesAreMatchedIgnoringCase() {
        assertEquals(SoundSource.MUSIC, SerializationHelper.INSTANCE.deserializeSoundSource(SoundSource.MUSIC.getName().toUpperCase()));
        assertEquals(SoundSource.WEATHER, SerializationHelper.INSTANCE.deserializeSoundSource(SoundSource.MASTER, "Weather"));
    }

    @Test
    void unknownOrMissingNamesResolveToNullOrTheFallback() {
        assertNull(SerializationHelper.INSTANCE.deserializeSoundSource("not_a_channel"));
        assertNull(SerializationHelper.INSTANCE.deserializeSoundSource((String) null));
        assertEquals(SoundSource.AMBIENT, SerializationHelper.INSTANCE.deserializeSoundSource(SoundSource.AMBIENT, "not_a_channel"));
        assertEquals(SoundSource.AMBIENT, SerializationHelper.INSTANCE.deserializeSoundSource(SoundSource.AMBIENT, null));
    }

}
