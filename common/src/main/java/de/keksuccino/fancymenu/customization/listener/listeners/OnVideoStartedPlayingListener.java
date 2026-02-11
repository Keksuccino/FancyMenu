package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnVideoStartedPlayingListener extends AbstractListener {

    @Nullable
    private String cachedVideoSource;
    @Nullable
    private String cachedVideoSourceType;
    private boolean cachedWillRestart;

    public OnVideoStartedPlayingListener() {
        super("video_started_playing");
    }

    public void onVideoStartedPlaying(@Nullable String videoSource, @Nullable String videoSourceType, boolean willRestart) {
        this.cachedVideoSource = (videoSource != null && !videoSource.isBlank()) ? videoSource : null;
        this.cachedVideoSourceType = (videoSourceType != null && !videoSourceType.isBlank()) ? videoSourceType : null;
        this.cachedWillRestart = willRestart;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("video_source", () -> this.cachedVideoSource != null ? this.cachedVideoSource : "ERROR"));
        list.add(new CustomVariable("video_source_type", () -> this.cachedVideoSourceType != null ? this.cachedVideoSourceType : "UNKNOWN"));
        list.add(new CustomVariable("video_will_restart", () -> Boolean.toString(this.cachedWillRestart)));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_video_started_playing");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_video_started_playing.desc"));
    }
}
