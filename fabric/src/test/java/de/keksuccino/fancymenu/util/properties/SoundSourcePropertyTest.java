package de.keksuccino.fancymenu.util.properties;

import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundSourcePropertyTest {

    @Test
    void everyChannelSurvivesSerializationByItsName() {
        for (SoundSource channel : SoundSource.values()) {
            Property<SoundSource> property = Property.soundSourceProperty("channel", SoundSource.MASTER, "test.channel");
            property.set(channel);
            PropertyContainer container = new PropertyContainer("test");

            property.serialize(container);
            Property<SoundSource> loaded = Property.soundSourceProperty("channel", SoundSource.MASTER, "test.channel");
            loaded.deserialize(container);

            assertEquals(channel.getName(), container.getValue("channel"));
            assertEquals(channel, loaded.get());
        }
    }

    @Test
    void unknownOrMissingChannelsFallBackToTheDefault() {
        PropertyContainer unknown = new PropertyContainer("test");
        unknown.putProperty("channel", "not_a_channel");
        Property<SoundSource> fromUnknown = Property.soundSourceProperty("channel", SoundSource.MUSIC, SoundSource.AMBIENT, "test.channel");
        fromUnknown.deserialize(unknown);

        Property<SoundSource> fromMissing = Property.soundSourceProperty("channel", SoundSource.MUSIC, SoundSource.AMBIENT, "test.channel");
        fromMissing.deserialize(new PropertyContainer("test"));

        assertEquals(SoundSource.MUSIC, fromUnknown.get());
        assertEquals(SoundSource.MUSIC, fromMissing.get());
        assertTrue(fromMissing.isDefault());
    }

    @Test
    void currentValueStartsAsGivenAndDefaultStaysSeparate() {
        Property<SoundSource> property = Property.soundSourceProperty("channel", SoundSource.MASTER, SoundSource.PLAYERS, "test.channel");

        assertEquals(SoundSource.PLAYERS, property.get());
        assertEquals(SoundSource.MASTER, property.getDefault());
        assertFalse(property.isDefault());
    }

}
