package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.MusicTrackInfoHelper;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnMusicTrackStoppedListener extends AbstractListener {

    @Nullable
    private String cachedTrackResourceLocation;
    @Nullable
    private String cachedDisplayNameString;
    @Nullable
    private String cachedDisplayNameComponent;

    public OnMusicTrackStoppedListener() {
        super("music_track_stopped");
    }

    public void onMusicTrackStopped(@Nullable String trackResourceLocation, @Nullable String trackEventLocation) {
        this.cachedTrackResourceLocation = trackResourceLocation;
        this.updateDisplayNameCache(trackResourceLocation, trackEventLocation);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("track_resource_location", () -> this.cachedTrackResourceLocation != null ? this.cachedTrackResourceLocation : "ERROR"));
        list.add(new CustomVariable("display_name_string", () -> this.cachedDisplayNameString != null ? this.cachedDisplayNameString : "NONE"));
        list.add(new CustomVariable("display_name_component", () -> this.cachedDisplayNameComponent != null ? this.cachedDisplayNameComponent : "NONE"));
    }

    private void updateDisplayNameCache(@Nullable String trackResourceLocation, @Nullable String eventResourceLocation) {
        Component displayName = MusicTrackInfoHelper.resolveDisplayName(trackResourceLocation, eventResourceLocation);
        if (displayName != null) {
            this.cachedDisplayNameString = displayName.getString();
            this.cachedDisplayNameComponent = MusicTrackInfoHelper.serializeComponent(displayName);
        } else {
            this.cachedDisplayNameString = null;
            this.cachedDisplayNameComponent = null;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_music_track_stopped");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_music_track_stopped.desc"));
    }
}
