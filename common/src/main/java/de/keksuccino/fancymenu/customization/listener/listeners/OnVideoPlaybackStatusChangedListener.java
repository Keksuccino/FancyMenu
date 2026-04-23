package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnVideoPlaybackStatusChangedListener extends AbstractListener {

    @Nullable
    private String cachedVideoSource;
    @Nullable
    private String cachedVideoSourceType;
    private boolean cachedLooping;
    @NotNull
    private VideoPlaybackStatus cachedStatus = VideoPlaybackStatus.STOPPED;

    public OnVideoPlaybackStatusChangedListener() {
        super("video_playback_status_changed");
    }

    public void onVideoPlaybackStatusChanged(@Nullable String videoSource, @Nullable String videoSourceType, boolean isLooping, @NotNull VideoPlaybackStatus newStatus) {
        this.cachedVideoSource = (videoSource != null && !videoSource.isBlank()) ? videoSource : null;
        this.cachedVideoSourceType = (videoSourceType != null && !videoSourceType.isBlank()) ? videoSourceType : null;
        this.cachedLooping = isLooping;
        this.cachedStatus = newStatus;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("video_source", () -> this.cachedVideoSource != null ? this.cachedVideoSource : "ERROR"));
        list.add(new CustomVariable("video_source_type", () -> this.cachedVideoSourceType != null ? this.cachedVideoSourceType : "UNKNOWN"));
        list.add(new CustomVariable("is_looping", () -> Boolean.toString(this.cachedLooping)));
        list.add(new CustomVariable("new_status", () -> this.cachedStatus.name()));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_video_playback_status_changed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_video_playback_status_changed.desc"));
    }

    public enum VideoPlaybackStatus {
        PLAYING,
        STOPPED,
        PAUSED,
        FINISHED
    }
}
