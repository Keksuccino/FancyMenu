package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnAnimatedTextureFinishedPlayingListener extends AbstractListener {

    @Nullable
    private String cachedTextureSource;
    @Nullable
    private String cachedTextureSourceType;
    private boolean cachedWillRestart;

    public OnAnimatedTextureFinishedPlayingListener() {
        super("animated_texture_finished_playing");
    }

    public void onAnimatedTextureFinishedPlaying(@Nullable String textureSource, @Nullable String textureSourceType, boolean willRestart) {
        this.cachedTextureSource = (textureSource != null && !textureSource.isBlank()) ? textureSource : null;
        this.cachedTextureSourceType = (textureSourceType != null && !textureSourceType.isBlank()) ? textureSourceType : null;
        this.cachedWillRestart = willRestart;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("texture_source", () -> this.cachedTextureSource != null ? this.cachedTextureSource : "ERROR"));
        list.add(new CustomVariable("texture_source_type", () -> this.cachedTextureSourceType != null ? this.cachedTextureSourceType : "UNKNOWN"));
        list.add(new CustomVariable("texture_will_restart", () -> Boolean.toString(this.cachedWillRestart)));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_animated_texture_finished_playing");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_animated_texture_finished_playing.desc"));
    }
}
