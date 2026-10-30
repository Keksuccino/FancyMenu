package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

/**
 * The volume and sound channel of one sound of a widget element, like its click, hover or unhover sound.<br>
 * Both settings are properties of the element, so they get saved, loaded and stacked like all other element properties.
 */
public final class WidgetSoundSettings {

    @NotNull
    public final Property.FloatProperty volume;
    @NotNull
    public final Property<SoundSource> channel;

    /**
     * @param element The element that owns the sound.
     * @param soundKey The base of the property and localization keys, like {@code click_sound}. Changing it breaks saved layouts.
     */
    public WidgetSoundSettings(@NotNull PropertyHolder element, @NotNull String soundKey) {
        String localizationKeyBase = "fancymenu.elements.widgets." + soundKey;
        this.volume = element.putProperty(Property.floatProperty(soundKey + "_volume", 1.0F, localizationKeyBase + "_volume", Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 1.0F).build()));
        this.channel = element.putProperty(Property.soundSourceProperty(soundKey + "_channel", SoundSource.MASTER, localizationKeyBase + "_channel"));
    }

    public float getVolume() {
        return this.volume.getFloat();
    }

    @NotNull
    public SoundSource getChannel() {
        return this.channel.tryGetNonNullElse(SoundSource.MASTER);
    }

    /**
     * Adds the volume and sound channel entries to the context menu of the element's editor element.
     */
    public <H extends PropertyHolder> void addContextMenuEntriesTo(@NotNull ContextMenu menu, @NotNull ContextMenuBuilder<H> builder) {
        this.volume.buildContextMenuEntryAndAddTo(menu, builder)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(this.volume.getContextMenuEntryLocalizationKeyBase() + ".desc")))
                .setIcon(MaterialIcons.VOLUME_UP);
        this.channel.buildContextMenuEntryAndAddTo(menu, builder)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(this.channel.getContextMenuEntryLocalizationKeyBase() + ".desc")))
                .setIcon(MaterialIcons.SPEAKER);
    }

}
