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
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Element properties get saved and loaded through the element's property map, which is what this test drives.
 * Sliders are not covered here, because building a slider element needs a running Minecraft client.
 */
class WidgetClickSoundSettingsTest {

    @Test
    void buttonClickSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new ButtonElement(new ButtonElementBuilder()), element -> element.clickSoundVolume, element -> element.clickSoundChannel);
    }

    @Test
    void checkboxClickSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new CheckboxElement(new CheckboxElementBuilder()), element -> element.clickSoundVolume, element -> element.clickSoundChannel);
    }

    @Test
    void inputFieldClickSoundSettingsSurviveSavingAndLoading() {
        assertRoundTrip(() -> new InputFieldElement(new InputFieldElementBuilder()), element -> element.clickSoundVolume, element -> element.clickSoundChannel);
    }

    @Test
    void elementsSavedWithoutClickSoundSettingsUseFullVolumeOnTheMasterChannel() {
        ButtonElement loaded = new ButtonElement(new ButtonElementBuilder());
        loaded.clickSoundVolume.set(0.1F);
        loaded.clickSoundChannel.set(SoundSource.WEATHER);

        loadProperties(loaded, new SerializedElement());

        assertEquals(1.0F, loaded.clickSoundVolume.getFloat());
        assertEquals(SoundSource.MASTER, loaded.clickSoundChannel.get());
    }

    @Test
    void vanillaWidgetCustomizationsStackClickSoundSettingsOfDifferentLayouts() {
        VanillaWidgetElement withSoundAndVolume = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        withSoundAndVolume.clickSound = ResourceSupplier.audio("config/fancymenu/assets/click.ogg");
        withSoundAndVolume.clickSoundVolume.set(0.5F);
        VanillaWidgetElement withChannel = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        withChannel.clickSoundChannel.set(SoundSource.PLAYERS);
        VanillaWidgetElement stack = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();

        VanillaWidgetElementBuilder.INSTANCE.stackElements(withSoundAndVolume, stack);
        VanillaWidgetElementBuilder.INSTANCE.stackElements(withChannel, stack);

        assertSame(withSoundAndVolume.clickSound, stack.clickSound);
        assertEquals(0.5F, stack.clickSoundVolume.getFloat());
        assertEquals(SoundSource.PLAYERS, stack.clickSoundChannel.get());
    }

    private static <E extends AbstractElement> void assertRoundTrip(@NotNull Supplier<E> factory, @NotNull Function<E, Property.FloatProperty> volume, @NotNull Function<E, Property<SoundSource>> channel) {
        E element = factory.get();
        assertEquals(1.0F, volume.apply(element).getFloat());
        assertEquals(SoundSource.MASTER, channel.apply(element).get());
        volume.apply(element).set(0.25F);
        channel.apply(element).set(SoundSource.HOSTILE);

        SerializedElement serialized = new SerializedElement();
        element.getPropertyMap().forEach((key, property) -> property.serialize(serialized));
        E loaded = factory.get();
        loadProperties(loaded, serialized);

        assertEquals("0.25", serialized.getValue("click_sound_volume"));
        assertEquals(SoundSource.HOSTILE.getName(), serialized.getValue("click_sound_channel"));
        assertEquals(0.25F, volume.apply(loaded).getFloat());
        assertEquals(SoundSource.HOSTILE, channel.apply(loaded).get());
    }

    private static void loadProperties(@NotNull AbstractElement element, @NotNull SerializedElement serialized) {
        element.getPropertyMap().forEach((key, property) -> property.deserialize(serialized));
    }

}
