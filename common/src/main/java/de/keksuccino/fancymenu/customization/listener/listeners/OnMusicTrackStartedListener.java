package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.MusicTrackInfoHelper;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.MusicTrackInfoHelper.MusicTrackInfo;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnMusicTrackStartedListener extends AbstractListener {

    @Nullable
    private String cachedTrackResourceLocation;
    @Nullable
    private String cachedTrackDisplayName;
    @Nullable
    private String cachedTrackArtist;
    private long cachedTrackDurationMs;

    public OnMusicTrackStartedListener() {
        super("music_track_started");
    }

    public void onMusicTrackStarted(@Nullable String trackResourceLocation, @Nullable String trackEventLocation) {
        this.cachedTrackResourceLocation = (trackResourceLocation != null && !trackResourceLocation.isBlank()) ? trackResourceLocation : trackEventLocation;
        this.updateTrackInfoCache(trackResourceLocation, trackEventLocation);
        if ((trackResourceLocation != null && !trackResourceLocation.isBlank()) || (trackEventLocation != null && !trackEventLocation.isBlank())) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("track_resource_location", () -> this.cachedTrackResourceLocation != null ? this.cachedTrackResourceLocation : "ERROR"));
        list.add(new CustomVariable("track_display_name", () -> this.cachedTrackDisplayName != null ? this.cachedTrackDisplayName : "UNKNOWN"));
        list.add(new CustomVariable("track_artist", () -> this.cachedTrackArtist != null ? this.cachedTrackArtist : "UNKNOWN"));
        list.add(new CustomVariable("track_duration_ms", () -> Long.toString(Math.max(this.cachedTrackDurationMs, 0L))));
    }

    private void updateTrackInfoCache(@Nullable String trackResourceLocation, @Nullable String eventResourceLocation) {
        MusicTrackInfo info = MusicTrackInfoHelper.findTrackInfo(trackResourceLocation, eventResourceLocation);
        if (info != null) {
            this.cachedTrackDisplayName = info.getDisplayName();
            this.cachedTrackArtist = info.getArtist();
            this.cachedTrackDurationMs = info.getDurationMillis();
        } else {
            this.cachedTrackDisplayName = null;
            this.cachedTrackArtist = null;
            this.cachedTrackDurationMs = 0L;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_music_track_started");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_music_track_started.desc"));
    }
}
