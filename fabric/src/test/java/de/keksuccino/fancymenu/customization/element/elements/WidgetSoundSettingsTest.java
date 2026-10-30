package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.checkbox.CheckboxElement;
import de.keksuccino.fancymenu.customization.element.elements.checkbox.CheckboxElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElement;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElementBuilder;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Element properties get saved and loaded through the element's property map, which is what this test drives.
 * Sliders are not covered here, because building a slider element needs a running Minecraft client.
 */
class WidgetSoundSettingsTest {

    private static final List<String> SOUND_KEYS = List.of("click_sound", "hover_sound", "unhover_sound");

    @Test
    void buttonSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new ButtonElement(new ButtonElementBuilder()), WidgetSoundSettingsTest::soundsOf);
    }

    @Test
    void checkboxSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new CheckboxElement(new CheckboxElementBuilder()), element -> List.of(element.clickSoundSettings, element.hoverSoundSettings, element.unhoverSoundSettings));
    }

    @Test
    void inputFieldSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new InputFieldElement(new InputFieldElementBuilder()), element -> List.of(element.clickSoundSettings, element.hoverSoundSettings, element.unhoverSoundSettings));
    }

    @Test
    void everySoundOfAnElementHasItsOwnSettings() {
        ButtonElement element = new ButtonElement(new ButtonElementBuilder());
        element.hoverSoundSettings.volume.set(0.2F);
        element.unhoverSoundSettings.channel.set(SoundSource.AMBIENT);

        assertNotSame(element.clickSoundSettings.volume, element.hoverSoundSettings.volume);
        assertEquals(1.0F, element.clickSoundSettings.getVolume());
        assertEquals(0.2F, element.hoverSoundSettings.getVolume());
        assertEquals(1.0F, element.unhoverSoundSettings.getVolume());
        assertEquals(SoundSource.MASTER, element.clickSoundSettings.getChannel());
        assertEquals(SoundSource.MASTER, element.hoverSoundSettings.getChannel());
        assertEquals(SoundSource.AMBIENT, element.unhoverSoundSettings.getChannel());
    }

    @Test
    void elementsSavedWithoutSoundSettingsUseFullVolumeOnTheMasterChannel() {
        ButtonElement loaded = new ButtonElement(new ButtonElementBuilder());
        for (WidgetSoundSettings sound : soundsOf(loaded)) {
            sound.volume.set(0.1F);
            sound.channel.set(SoundSource.WEATHER);
        }

        loadProperties(loaded, new SerializedElement());

        for (WidgetSoundSettings sound : soundsOf(loaded)) {
            assertEquals(1.0F, sound.getVolume());
            assertEquals(SoundSource.MASTER, sound.getChannel());
        }
    }

    @Test
    void missingChannelValuesFallBackToTheMasterChannel() {
        ButtonElement element = new ButtonElement(new ButtonElementBuilder());
        element.hoverSoundSettings.channel.set(null);

        assertEquals(SoundSource.MASTER, element.hoverSoundSettings.getChannel());
    }

    @Test
    void vanillaWidgetCustomizationsStackSoundSettingsOfDifferentLayouts() {
        VanillaWidgetElement first = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        first.clickSound = ResourceSupplier.audio("config/fancymenu/assets/click.ogg");
        first.clickSoundSettings.volume.set(0.5F);
        first.hoverSoundSettings.channel.set(SoundSource.NEUTRAL);
        VanillaWidgetElement second = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        second.clickSoundSettings.channel.set(SoundSource.PLAYERS);
        second.unhoverSoundSettings.volume.set(0.3F);
        VanillaWidgetElement stack = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();

        VanillaWidgetElementBuilder.INSTANCE.stackElements(first, stack);
        VanillaWidgetElementBuilder.INSTANCE.stackElements(second, stack);

        assertSame(first.clickSound, stack.clickSound);
        assertEquals(0.5F, stack.clickSoundSettings.getVolume());
        assertEquals(SoundSource.PLAYERS, stack.clickSoundSettings.getChannel());
        assertEquals(SoundSource.NEUTRAL, stack.hoverSoundSettings.getChannel());
        assertEquals(1.0F, stack.hoverSoundSettings.getVolume());
        assertEquals(0.3F, stack.unhoverSoundSettings.getVolume());
        assertEquals(SoundSource.MASTER, stack.unhoverSoundSettings.getChannel());
    }

    private static <E extends AbstractElement> void assertRoundTrip(@NotNull Supplier<E> factory, @NotNull Function<E, List<WidgetSoundSettings>> sounds) {
        E element = factory.get();
        List<WidgetSoundSettings> original = sounds.apply(element);
        SoundSource[] channels = {SoundSource.HOSTILE, SoundSource.BLOCKS, SoundSource.RECORDS};
        for (int i = 0; i < original.size(); i++) {
            assertEquals(1.0F, original.get(i).getVolume());
            assertEquals(SoundSource.MASTER, original.get(i).getChannel());
            original.get(i).volume.set(0.25F + (0.25F * i));
            original.get(i).channel.set(channels[i]);
        }

        SerializedElement serialized = new SerializedElement();
        element.getPropertyMap().forEach((key, property) -> property.serialize(serialized));
        E loaded = factory.get();
        loadProperties(loaded, serialized);

        List<WidgetSoundSettings> reloaded = sounds.apply(loaded);
        for (int i = 0; i < reloaded.size(); i++) {
            assertEquals(channels[i].getName(), serialized.getValue(SOUND_KEYS.get(i) + "_channel"));
            assertEquals(0.25F + (0.25F * i), reloaded.get(i).getVolume());
            assertEquals(channels[i], reloaded.get(i).getChannel());
        }
    }

    private static List<WidgetSoundSettings> soundsOf(@NotNull ButtonElement element) {
        return List.of(element.clickSoundSettings, element.hoverSoundSettings, element.unhoverSoundSettings);
    }

    private static void loadProperties(@NotNull AbstractElement element, @NotNull SerializedElement serialized) {
        element.getPropertyMap().forEach((key, property) -> property.deserialize(serialized));
    }

}
