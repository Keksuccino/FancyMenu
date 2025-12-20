package de.keksuccino.fancymenu.customization.global;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.elements.musiccontroller.MusicControllerHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMusicManager;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GlobalCustomizationHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_SEPARATOR = "%!source_end!%";
    private static final int MENU_MUSIC_MIN_DELAY_TICKS = 20;
    private static final int MENU_MUSIC_MAX_DELAY_TICKS = 600;
    private static final int MENU_MUSIC_START_DELAY_TICKS = 100;
    private static final RandomSource MENU_MUSIC_RANDOM = RandomSource.create();

    private static final CachedSupplier<ITexture> BUTTON_BACKGROUND_NORMAL = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> BUTTON_BACKGROUND_HOVER = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> BUTTON_BACKGROUND_INACTIVE = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> SLIDER_BACKGROUND = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> SLIDER_HANDLE_NORMAL = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> SLIDER_HANDLE_HOVER = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> SLIDER_HANDLE_INACTIVE = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<ITexture> MENU_BACKGROUND = new CachedSupplier<>(ResourceSupplier::image);
    private static final CachedSupplier<IAudio> BUTTON_CLICK_SOUND = new CachedSupplier<>(ResourceSupplier::audio);

    @Nullable
    private static String cachedMenuMusicSerialized;
    @NotNull
    private static List<String> cachedMenuMusicTracks = Collections.emptyList();
    private static int nextMenuMusicDelayTicks = MENU_MUSIC_START_DELAY_TICKS;
    private static int lastMenuMusicIndex = -1;
    @Nullable
    private static IAudio currentMenuMusic;
    private static boolean currentMenuMusicStarted = false;

    private GlobalCustomizationHandler() {
    }

    @Nullable
    public static RenderableResource getCustomButtonBackgroundNormal() {
        return getTextureResource(BUTTON_BACKGROUND_NORMAL, FancyMenu.getOptions().globalButtonBackgroundNormal.getValue());
    }

    @Nullable
    public static RenderableResource getCustomButtonBackgroundHover() {
        return getTextureResource(BUTTON_BACKGROUND_HOVER, FancyMenu.getOptions().globalButtonBackgroundHover.getValue());
    }

    @Nullable
    public static RenderableResource getCustomButtonBackgroundInactive() {
        return getTextureResource(BUTTON_BACKGROUND_INACTIVE, FancyMenu.getOptions().globalButtonBackgroundInactive.getValue());
    }

    @Nullable
    public static RenderableResource getCustomSliderBackground() {
        return getTextureResource(SLIDER_BACKGROUND, FancyMenu.getOptions().globalSliderBackground.getValue());
    }

    @Nullable
    public static RenderableResource getCustomSliderHandleNormal() {
        return getTextureResource(SLIDER_HANDLE_NORMAL, FancyMenu.getOptions().globalSliderHandleNormal.getValue());
    }

    @Nullable
    public static RenderableResource getCustomSliderHandleHover() {
        return getTextureResource(SLIDER_HANDLE_HOVER, FancyMenu.getOptions().globalSliderHandleHover.getValue());
    }

    @Nullable
    public static RenderableResource getCustomSliderHandleInactive() {
        return getTextureResource(SLIDER_HANDLE_INACTIVE, FancyMenu.getOptions().globalSliderHandleInactive.getValue());
    }

    @Nullable
    public static RenderableResource getCustomMenuBackgroundTexture() {
        return getTextureResource(MENU_BACKGROUND, FancyMenu.getOptions().globalMenuBackgroundTexture.getValue());
    }

    @Nullable
    public static IAudio getCustomButtonClickSound() {
        ResourceSupplier<IAudio> supplier = BUTTON_CLICK_SOUND.get(FancyMenu.getOptions().globalButtonClickSound.getValue());
        return (supplier != null) ? supplier.get() : null;
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomButtonBackgroundNormalSupplier() {
        return BUTTON_BACKGROUND_NORMAL.get(FancyMenu.getOptions().globalButtonBackgroundNormal.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomButtonBackgroundHoverSupplier() {
        return BUTTON_BACKGROUND_HOVER.get(FancyMenu.getOptions().globalButtonBackgroundHover.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomButtonBackgroundInactiveSupplier() {
        return BUTTON_BACKGROUND_INACTIVE.get(FancyMenu.getOptions().globalButtonBackgroundInactive.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomSliderBackgroundSupplier() {
        return SLIDER_BACKGROUND.get(FancyMenu.getOptions().globalSliderBackground.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomSliderHandleNormalSupplier() {
        return SLIDER_HANDLE_NORMAL.get(FancyMenu.getOptions().globalSliderHandleNormal.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomSliderHandleHoverSupplier() {
        return SLIDER_HANDLE_HOVER.get(FancyMenu.getOptions().globalSliderHandleHover.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomSliderHandleInactiveSupplier() {
        return SLIDER_HANDLE_INACTIVE.get(FancyMenu.getOptions().globalSliderHandleInactive.getValue());
    }

    @Nullable
    public static ResourceSupplier<ITexture> getCustomMenuBackgroundSupplier() {
        return MENU_BACKGROUND.get(FancyMenu.getOptions().globalMenuBackgroundTexture.getValue());
    }

    @Nullable
    public static ResourceSupplier<IAudio> getCustomButtonClickSoundSupplier() {
        return BUTTON_CLICK_SOUND.get(FancyMenu.getOptions().globalButtonClickSound.getValue());
    }

    @Nullable
    public static LocalTexturePanoramaRenderer getCustomBackgroundPanorama() {
        String panoramaName = FancyMenu.getOptions().globalBackgroundPanorama.getValue();
        if ((panoramaName == null) || panoramaName.trim().isEmpty()) return null;
        if (!PanoramaHandler.panoramaExists(panoramaName)) return null;
        return PanoramaHandler.getPanorama(panoramaName);
    }

    public static void tickMenuMusic() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        if (minecraft.level != null) {
            stopCurrentMenuMusic();
            return;
        }
        if (!MusicControllerHandler.shouldPlayMenuMusic()) {
            stopCurrentMenuMusic();
            return;
        }
        List<String> tracks = getMenuMusicTracksInternal();
        if (tracks.isEmpty()) {
            stopCurrentMenuMusic();
            return;
        }
        if (((IMixinMusicManager)minecraft.getMusicManager()).getCurrentMusic_FancyMenu() != null) {
            minecraft.getMusicManager().stopPlaying();
        }

        if (currentMenuMusic != null) {
            if (currentMenuMusic.isLoadingFailed()) {
                stopCurrentMenuMusic(false);
                nextMenuMusicDelayTicks = randomMenuMusicDelayTicks();
                return;
            }
            if (currentMenuMusic.isReady() && currentMenuMusicStarted && !currentMenuMusic.isPlaying() && !currentMenuMusic.isPaused()) {
                stopCurrentMenuMusic(false);
                nextMenuMusicDelayTicks = randomMenuMusicDelayTicks();
            }
        }

        nextMenuMusicDelayTicks = Math.min(nextMenuMusicDelayTicks, MENU_MUSIC_MAX_DELAY_TICKS);
        if (currentMenuMusic == null && nextMenuMusicDelayTicks-- <= 0) {
            startNextMenuMusicTrack(tracks);
        }
    }

    public static boolean hasCustomMenuMusicTracks() {
        return !getMenuMusicTracksInternal().isEmpty();
    }

    @NotNull
    public static List<String> getCustomMenuMusicTracks() {
        return new ArrayList<>(getMenuMusicTracksInternal());
    }

    public static void saveCustomMenuMusicTracks(@NotNull List<String> tracks) {
        String serialized = serializeMenuMusicTracks(tracks);
        FancyMenu.getOptions().globalMenuMusicTracks.setValue(serialized);
        invalidateMenuMusicCache();
    }

    public static void invalidateMenuMusicCache() {
        cachedMenuMusicSerialized = null;
        cachedMenuMusicTracks = Collections.emptyList();
        stopCurrentMenuMusic();
    }

    @NotNull
    public static List<String> parseMenuMusicTracks(@Nullable String serialized) {
        if (serialized == null) return Collections.emptyList();
        String trimmed = serialized.trim();
        if (trimmed.isEmpty()) return Collections.emptyList();
        List<String> entries = new ArrayList<>();
        if (!trimmed.contains(SOURCE_SEPARATOR)) {
            entries.add(trimmed);
            return entries;
        }
        for (String entry : trimmed.split(SOURCE_SEPARATOR)) {
            String cleaned = entry.trim();
            if (!cleaned.isEmpty()) entries.add(cleaned);
        }
        return entries;
    }

    @NotNull
    public static String serializeMenuMusicTracks(@NotNull List<String> tracks) {
        StringBuilder builder = new StringBuilder();
        for (String track : tracks) {
            if (track == null) continue;
            String cleaned = track.trim();
            if (!cleaned.isEmpty()) {
                builder.append(cleaned).append(SOURCE_SEPARATOR);
            }
        }
        return builder.toString();
    }

    @Nullable
    private static RenderableResource getTextureResource(@NotNull CachedSupplier<ITexture> cache, @Nullable String source) {
        ResourceSupplier<ITexture> supplier = cache.get(source);
        if (supplier == null) return null;
        return supplier.get();
    }

    @NotNull
    private static List<String> getMenuMusicTracksInternal() {
        String serialized = FancyMenu.getOptions().globalMenuMusicTracks.getValue();
        if (!Objects.equals(serialized, cachedMenuMusicSerialized)) {
            cachedMenuMusicSerialized = serialized;
            cachedMenuMusicTracks = parseMenuMusicTracks(serialized);
            stopCurrentMenuMusic();
            nextMenuMusicDelayTicks = MENU_MUSIC_START_DELAY_TICKS;
        }
        return cachedMenuMusicTracks;
    }

    private static void startNextMenuMusicTrack(@NotNull List<String> tracks) {
        if (tracks.isEmpty()) return;
        int index = pickNextMenuMusicIndex(tracks.size());
        String source = tracks.get(index);
        ResourceSupplier<IAudio> supplier = ResourceSupplier.audio(source);
        IAudio audio = supplier.get();
        if (audio == null) {
            LOGGER.warn("[FANCYMENU] Failed to load custom menu music track: {}", source);
            nextMenuMusicDelayTicks = randomMenuMusicDelayTicks();
            return;
        }
        audio.setSoundChannel(SoundSource.MUSIC);
        audio.play();
        currentMenuMusic = audio;
        currentMenuMusicStarted = true;
        lastMenuMusicIndex = index;
        nextMenuMusicDelayTicks = Integer.MAX_VALUE;
    }

    private static int pickNextMenuMusicIndex(int size) {
        if (size <= 1) return 0;
        int index = Mth.nextInt(MENU_MUSIC_RANDOM, 0, size - 1);
        if (index == lastMenuMusicIndex) {
            index = (index + 1) % size;
        }
        return index;
    }

    private static int randomMenuMusicDelayTicks() {
        return Mth.nextInt(MENU_MUSIC_RANDOM, MENU_MUSIC_MIN_DELAY_TICKS, MENU_MUSIC_MAX_DELAY_TICKS);
    }

    private static void stopCurrentMenuMusic() {
        stopCurrentMenuMusic(true);
    }

    private static void stopCurrentMenuMusic(boolean resetDelay) {
        if (currentMenuMusic != null) {
            currentMenuMusic.stop();
        }
        currentMenuMusic = null;
        currentMenuMusicStarted = false;
        if (resetDelay) {
            nextMenuMusicDelayTicks = MENU_MUSIC_START_DELAY_TICKS;
        }
    }

    private static final class CachedSupplier<R extends de.keksuccino.fancymenu.util.resource.Resource> {
        @Nullable
        private String lastSource;
        @Nullable
        private ResourceSupplier<R> supplier;
        @NotNull
        private final java.util.function.Function<String, ResourceSupplier<R>> builder;

        private CachedSupplier(@NotNull java.util.function.Function<String, ResourceSupplier<R>> builder) {
            this.builder = builder;
        }

        @Nullable
        private ResourceSupplier<R> get(@Nullable String source) {
            if (source == null || source.trim().isEmpty()) {
                this.lastSource = null;
                this.supplier = null;
                return null;
            }
            if (!source.equals(this.lastSource)) {
                this.lastSource = source;
                this.supplier = this.builder.apply(source);
            }
            return this.supplier;
        }
    }
}
