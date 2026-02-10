package de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.IVideoMenuBackground;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NativeVideoMenuBackground extends MenuBackground<NativeVideoMenuBackground> implements IVideoMenuBackground {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Object VIDEO_REFERENCE_LOCK = new Object();
    private static final Map<IVideo, Integer> VIDEO_REFERENCE_COUNTS = new IdentityHashMap<>();
    private static final String MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU = "native_video_last_stopped_play_time_seconds";
    private static final String MEMORY_LAST_STOPPED_SOURCE_FANCYMENU = "native_video_last_stopped_source";

    public final Property<ResourceSupplier<IVideo>> videoSupplier = putProperty(Property.resourceSupplierProperty(IVideo.class, "source", null, "fancymenu.elements.video_mcef.set_source", true, true, true, null));
    public final Property<Boolean> loop = putProperty(Property.booleanProperty("loop", false, "fancymenu.elements.video_mcef.loop"));
    /** Value between 0.0 and 1.0 **/
    public final Property<Float> volume = putProperty(Property.floatProperty("volume", 1.0F, "fancymenu.elements.video_mcef.volume"))
            .setValueSetProcessor(value -> Math.max(0.0F, Math.min(1.0F, value)));
    public final Property.StringProperty soundSource = putProperty(Property.stringProperty("sound_source", SoundSource.MASTER.getName(), false, false, "fancymenu.elements.video_mcef.sound_channel"));
    public final Property<Boolean> parallaxEnabled = putProperty(Property.booleanProperty("parallax", false, "fancymenu.backgrounds.image.configure.parallax"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.FloatProperty parallaxIntensityXString = putProperty(Property.floatProperty("parallax_intensity_x", 0.02F, "fancymenu.backgrounds.image.configure.parallax_intensity_x"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.FloatProperty parallaxIntensityYString = putProperty(Property.floatProperty("parallax_intensity_y", 0.02F, "fancymenu.backgrounds.image.configure.parallax_intensity_y"));
    /** When TRUE, the parallax effect will move in the SAME direction as the mouse, otherwise it moves in the opposite direction **/
    public final Property<Boolean> invertParallax = putProperty(Property.booleanProperty("invert_parallax", false, "fancymenu.backgrounds.image.configure.invert_parallax"));
    public final Property<Boolean> playInEditor = putProperty(Property.booleanProperty("play_in_editor", true, "fancymenu.backgrounds.video.play_in_editor"));

    protected volatile boolean initialized = false;
    @Nullable
    protected IVideo video = null;
    protected float cachedActualVolume = -10000F;
    protected float lastCachedActualVolume = -11000F;
    protected Boolean lastLoop = null;
    protected Boolean lastPausedState = null;
    protected volatile long lastRenderTickTime = -1L;
    protected boolean pausedBySystem = false;
    protected final AtomicReference<Float> cachedDuration = new AtomicReference<>(0F);
    protected final AtomicReference<Float> cachedPlayTime = new AtomicReference<>(0F);
    @Nullable
    protected volatile String currentResolvedVideoSource = null;
    @Nullable
    protected volatile String activeVideoSource = null;
    // The field is currently unused, but the scheduler is used, so don't delete this
    protected final ScheduledFuture<?> garbageChecker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized && !this.shouldSkipWatchdogAutoClear() && (this.lastRenderTickTime != -1L) && ((this.lastRenderTickTime + 11000L) < System.currentTimeMillis())) {
            String sourceForLog = this.getConfiguredVideoSourceForLog();
            String videoTypeForLog = this.getVideoTypeForLog();
            boolean didStopPlayer = this.resetBackgroundAndReturnStopState();
            LOGGER.info("[FANCYMENU] Auto-clearing native video background after watchdog timeout. source: {}, videoType: {}, didStopPlayer: {}, backgroundInstance: {}",
                    sourceForLog,
                    videoTypeForLog,
                    didStopPlayer,
                    this.getInstanceIdentifier());
        }
    }, 0L, 100L, TimeUnit.MILLISECONDS);
    // The field is currently unused, but the scheduler is used, so don't delete this
    protected final ScheduledFuture<?> asyncTicker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized) {
            this.cachedDuration.set(this._getDuration());
            this.cachedPlayTime.set(this._getPlayTime());
        }
    }, 0L, 900L, TimeUnit.MILLISECONDS);

    public NativeVideoMenuBackground(MenuBackgroundBuilder<NativeVideoMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.videoSupplier.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> {
                    ResourceSupplier<IVideo> supplier = this.videoSupplier.get();
                    if (supplier == null) {
                        return UITooltip.of(Component.translatable("fancymenu.backgrounds.video.configure.no_video"));
                    }
                    return null;
                })
                .setIcon(MaterialIcons.MOVIE);

        menu.addSeparatorEntry("separator_after_video_source");

        this.loop.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.REPEAT);
        this.volume.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        List<SoundSource> soundSources = Arrays.asList(SoundSource.values());
        this.addCycleContextMenuEntryTo(menu, "sound_source", soundSources, NativeVideoMenuBackground.class, NativeVideoMenuBackground::getSoundSourceOrDefault, (background, source) -> {
            if (source != null) {
                background.soundSource.set(source.getName());
            }
        }, (menu1, entry, switcherValue) -> {
            Component name = Component.translatable("soundCategory." + switcherValue.getName())
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            return Component.translatable("fancymenu.elements.video_mcef.sound_channel", name);
        }).setIcon(MaterialIcons.SPEAKER);

        menu.addSeparatorEntry("separator_before_parallax");

        this.parallaxEnabled.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons._3D);
        this.parallaxIntensityXString.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.image.configure.parallax_intensity_x.desc")))
                .setIcon(MaterialIcons.SPLITSCREEN_LANDSCAPE);
        this.parallaxIntensityYString.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.image.configure.parallax_intensity_y.desc")))
                .setIcon(MaterialIcons.SPLITSCREEN_PORTRAIT);
        this.invertParallax.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.image.configure.invert_parallax.desc")))
                .setIcon(MaterialIcons.SWAP_HORIZ);

        menu.addSeparatorEntry("separator_before_play_in_editor");

        this.playInEditor.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.EDIT);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        float parallaxIntensityX = this.parallaxIntensityXString.getFloat();
        float parallaxIntensityY = this.parallaxIntensityYString.getFloat();

        this.lastRenderTickTime = System.currentTimeMillis();

        RenderSystem.enableBlend();

        float[] parallaxOffset = calculateParallaxOffset(mouseX, mouseY, parallaxIntensityX, parallaxIntensityY);
        int x = 0;
        int y = 0;
        int w = getScreenWidth();
        int h = getScreenHeight();

        if (this.parallaxEnabled.tryGetNonNull()) {
            // Reduce the expansion amount for parallax
            w = (int) (getScreenWidth() * (1.0F + parallaxIntensityX));
            h = (int) (getScreenHeight() * (1.0F + parallaxIntensityY));
            // Center the expanded area and apply parallax offset
            x = -((w - getScreenWidth()) / 2) + (int) parallaxOffset[0];
            y = -((h - getScreenHeight()) / 2) + (int) parallaxOffset[1];
        }

        // Always draw black background
        graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorIntWithAlpha(this.opacity));

        ResourceSupplier<IVideo> supplier = this.videoSupplier.get();
        this.currentResolvedVideoSource = this.getResolvedVideoSource(supplier);
        IVideo currentVideo = (supplier != null) ? supplier.get() : null;
        this.updateVideoReference(currentVideo);

        if (this.video == null) {
            RenderSystem.disableBlend();
            return;
        }

        this.updateVolume();
        if ((this.lastCachedActualVolume == -11000F) || (this.cachedActualVolume != this.lastCachedActualVolume)) {
            this.setVolume(this.volume.tryGetNonNull(), true);
        }
        this.lastCachedActualVolume = this.cachedActualVolume;

        boolean loop = this.loop.tryGetNonNull();
        if ((this.lastLoop == null) || !Objects.equals(loop, this.lastLoop)) {
            this.video.setLooping(loop);
        }
        this.lastLoop = loop;

        boolean pausedState = this._isPaused();
        if (pausedState) {
            if ((this.lastPausedState == null) || !Objects.equals(true, this.lastPausedState)) {
                this.video.pause();
            }
        } else {
            // Keep editor playback alive even when lifecycle hooks paused the player in between renders.
            if ((this.lastPausedState == null) || Objects.equals(true, this.lastPausedState) || this.video.isPaused()) {
                this.video.play();
            }
        }
        this.lastPausedState = pausedState;

        ResourceLocation resourceLocation = this.video.getResourceLocation();
        if (resourceLocation != null) {
            graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
            if (this.keepBackgroundAspectRatio) {
                this.renderKeepAspectRatio(graphics, resourceLocation, parallaxOffset, parallaxIntensityX, parallaxIntensityY);
            } else {
                this.renderFullScreen(graphics, resourceLocation, parallaxOffset, parallaxIntensityX, parallaxIntensityY);
            }
        }

        RenderingUtils.resetShaderColor(graphics);

        RenderSystem.disableBlend();

    }

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset, float parallaxIntensityX, float parallaxIntensityY) {
        AspectRatio ratio = this.video.getAspectRatio();
        boolean parallax = this.parallaxEnabled.tryGetNonNull();
        float parallaxScaleX = parallax ? (1.0F + parallaxIntensityX) : 1.0F;
        float parallaxScaleY = parallax ? (1.0F + parallaxIntensityY) : 1.0F;
        int[] baseSize = ratio.getAspectRatioSizeByMinimumSize(
                (int)(getScreenWidth() * parallaxScaleX),
                (int)(getScreenHeight() * parallaxScaleY)
        );

        int x = (getScreenWidth() - baseSize[0]) / 2 + (int)parallaxOffset[0];
        int y = (getScreenHeight() - baseSize[1]) / 2 + (int)parallaxOffset[1];

        graphics.blit(resourceLocation, x, y, 0.0F, 0.0F, baseSize[0], baseSize[1], baseSize[0], baseSize[1]);
    }

    protected void renderFullScreen(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset, float parallaxIntensityX, float parallaxIntensityY) {
        if (this.parallaxEnabled.tryGetNonNull()) {
            int expandedWidth = (int)(getScreenWidth() * (1.0F + parallaxIntensityX));
            int expandedHeight = (int)(getScreenHeight() * (1.0F + parallaxIntensityY));

            int x = -((expandedWidth - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            int y = -((expandedHeight - getScreenHeight()) / 2) + (int)parallaxOffset[1];

            graphics.blit(resourceLocation, x, y, 0.0F, 0.0F, expandedWidth, expandedHeight, expandedWidth, expandedHeight);
        } else {
            graphics.blit(resourceLocation, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }
    }

    protected void updateVideoReference(@Nullable IVideo newVideo) {
        if (this.video == newVideo) return;

        IVideo oldVideo = this.video;
        String oldVideoSource = this.activeVideoSource;
        if (oldVideo != null) {
            this.releaseVideoReference(oldVideo, oldVideoSource);
        }

        this.video = newVideo;
        if (newVideo != null) {
            this.acquireVideoReference(newVideo);
        }
        this.activeVideoSource = (newVideo != null) ? this.currentResolvedVideoSource : null;
        if (newVideo != null) {
            this.tryRestorePlaybackPositionFromMemory(newVideo, this.activeVideoSource);
        }
        this.initialized = (newVideo != null);
        this.lastLoop = null;
        this.cachedActualVolume = -10000F;
        this.lastCachedActualVolume = -11000F;
        this.lastPausedState = null;

        if (newVideo == null) {
            this.cachedDuration.set(0F);
            this.cachedPlayTime.set(0F);
        }
    }

    protected float[] calculateParallaxOffset(int mouseX, int mouseY, float parallaxIntensityX, float parallaxIntensityY) {

        if (!this.parallaxEnabled.tryGetNonNull()) {
            return new float[]{0, 0};
        }

        // Calculate mouse position as a percentage from the center of the screen
        float mouseXPercent = (2.0f * mouseX / getScreenWidth()) - 1.0f;
        float mouseYPercent = (2.0f * mouseY / getScreenHeight()) - 1.0f;

        // Apply inversion if enabled
        float directionMultiplier = this.invertParallax.tryGetNonNull() ? 1.0f : -1.0f;

        // Calculate offset based on screen dimensions and center-adjusted mouse position
        float xOffset = directionMultiplier * parallaxIntensityX * mouseXPercent * getScreenWidth() * 0.5f;
        float yOffset = directionMultiplier * parallaxIntensityY * mouseYPercent * getScreenHeight() * 0.5f;

        return new float[]{xOffset, yOffset};

    }

    @Override
    public void onOpenScreen() {
        super.onOpenScreen();
        if (this.initialized && (this.video != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.video.play();
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        if (this.initialized && (this.video != null)) {
            this.cachePlaybackPositionToMemory(this.video, this.activeVideoSource, true);
            this.pausedBySystem = true;
            this.video.pause();
        }
    }

    @Override
    public void onAfterEnable() {
        super.onAfterEnable();
        if (this.initialized && (this.video != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.video.play();
        }
    }

    @Override
    public void onDisableOrRemove() {
        super.onDisableOrRemove();
        if (this.initialized && (this.video != null)) {
            this.cachePlaybackPositionToMemory(this.video, this.activeVideoSource, true);
            this.pausedBySystem = true;
            this.video.pause();
        }
    }

    /**
     * @param volume Value between 0.0 and 1.0.
     */
    protected void setVolume(float volume, boolean updateVideo) {
        volume = Math.max(0.0F, Math.min(1.0F, volume));
        if (this.video != null) {
            float actualVolume = volume;
            float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            SoundSource resolvedSoundSource = this.getSoundSourceOrDefault();
            float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(resolvedSoundSource);
            if (resolvedSoundSource != SoundSource.MASTER) {
                soundSourceVolume *= masterVolume;
            }
            actualVolume *= soundSourceVolume;
            actualVolume *= this.getControllerVolume();
            this.cachedActualVolume = actualVolume;
            if (updateVideo) {
                this.video.setVolume(Math.min(1.0F, Math.max(0.0F, actualVolume)));
            }
        }
    }

    protected void updateVolume() {
        this.setVolume(this.volume.tryGetNonNull(), false);
    }

    /**
     * Returns the volume of this element that is set in the {@link VideoElementController}.<br>
     * The controller volume is set by actions and similar things that are user-controlled in most cases.
     */
    public float getControllerVolume() {
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
        VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(this.getInstanceIdentifier());
        if (meta != null) return Math.max(0.0F, Math.min(1.0F, meta.volume));
        return 1.0F;
    }

    /**
     * Returns the paused state of this element that is set in the {@link VideoElementController}.<br>
     * The controller paused state is set by actions and similar things that are user-controlled in most cases.
     */
    public boolean getControllerPausedState() {
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
        VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(this.getInstanceIdentifier());
        if (meta == null) return false;
        return meta.paused;
    }

    public void resetBackground() {
        this.resetBackgroundAndReturnStopState();
    }

    protected boolean resetBackgroundAndReturnStopState() {
        boolean didStopPlayer = false;
        IVideo oldVideo = this.video;
        String oldVideoSource = this.activeVideoSource;
        if (oldVideo != null) {
            didStopPlayer = this.releaseVideoReference(oldVideo, oldVideoSource);
        }
        this.initialized = false;
        this.video = null;
        this.activeVideoSource = null;
        this.cachedActualVolume = -10000F;
        this.lastCachedActualVolume = -11000F;
        this.lastLoop = null;
        this.lastPausedState = null;
        this.lastRenderTickTime = -1L;
        this.pausedBySystem = false;
        this.cachedDuration.set(0F);
        this.cachedPlayTime.set(0F);
        return didStopPlayer;
    }

    protected float _getDuration() {
        if (!this.initialized || (this.video == null)) return 0F;
        return Math.max(0F, this.video.getDuration());
    }

    protected float _getPlayTime() {
        if (!this.initialized || (this.video == null)) return 0F;
        return Math.max(0F, this.video.getPlayTime());
    }

    protected boolean _isPaused() {
        if (isEditor()) {
            if (!this.playInEditor.tryGetNonNull()) return true;
            return this.getControllerPausedState();
        }
        return (this.getControllerPausedState() || this.pausedBySystem);
    }

    @Override
    public float getDuration() {
        return this.cachedDuration.get();
    }

    @Override
    public float getPlayTime() {
        return this.cachedPlayTime.get();
    }

    @NotNull
    protected SoundSource getSoundSourceOrDefault() {
        String name = this.soundSource.get();
        if (name != null) {
            SoundSource source = getSoundSourceByName(name);
            if (source != null) return source;
        }
        return SoundSource.MASTER;
    }

    @Nullable
    protected static SoundSource getSoundSourceByName(@NotNull String name) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equals(name)) return source;
        }
        return null;
    }

    @NotNull
    protected String getConfiguredVideoSourceForLog() {
        ResourceSupplier<IVideo> supplier = this.videoSupplier.get();
        if (supplier == null) return "[null]";
        String source = supplier.getSourceWithPrefix();
        return source.isEmpty() ? "[empty]" : source;
    }

    @NotNull
    protected String getVideoTypeForLog() {
        IVideo cachedVideo = this.video;
        if (cachedVideo == null) return "[null]";
        return cachedVideo.getClass().getName();
    }

    protected boolean shouldSkipWatchdogAutoClear() {
        return isEditor() && this.playInEditor.tryGetNonNull();
    }

    protected void acquireVideoReference(@NotNull IVideo video) {
        synchronized (VIDEO_REFERENCE_LOCK) {
            VIDEO_REFERENCE_COUNTS.merge(video, 1, Integer::sum);
        }
    }

    protected boolean releaseVideoReference(@NotNull IVideo video, @Nullable String source) {
        boolean shouldStop = false;
        synchronized (VIDEO_REFERENCE_LOCK) {
            Integer amount = VIDEO_REFERENCE_COUNTS.get(video);
            if ((amount == null) || (amount <= 1)) {
                VIDEO_REFERENCE_COUNTS.remove(video);
                shouldStop = true;
            } else {
                VIDEO_REFERENCE_COUNTS.put(video, amount - 1);
            }
        }
        // Video resources are shared by source via ResourceHandler cache.
        // Only stop once this was the last known background reference, otherwise another background would get interrupted.
        if (shouldStop) {
            this.cachePlaybackPositionToMemory(video, source, !this.pausedBySystem);
            video.stop();
        }
        return shouldStop;
    }

    @Nullable
    protected String getResolvedVideoSource(@Nullable ResourceSupplier<IVideo> supplier) {
        if (supplier == null) return null;
        String source = PlaceholderParser.replacePlaceholders(supplier.getSourceWithPrefix());
        if (source.isEmpty()) return null;
        return source;
    }

    protected void cachePlaybackPositionToMemory(@NotNull IVideo video, @Nullable String source, boolean allowLowerOverwrite) {
        if ((source == null) || source.isEmpty()) return;
        float playTime = Math.max(0.0F, video.getPlayTime());
        if (playTime <= 0.05F) return;
        String cachedSource = this.getMemory().getStringProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU);
        Float cachedPlayTime = this.getMemory().getProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, Float.class);
        if (!allowLowerOverwrite && Objects.equals(source, cachedSource) && (cachedPlayTime != null) && (cachedPlayTime > playTime)) return;
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU, source);
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, playTime);
    }

    protected void tryRestorePlaybackPositionFromMemory(@NotNull IVideo video, @Nullable String source) {
        if ((source == null) || source.isEmpty()) return;
        String cachedSource = this.getMemory().getStringProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU);
        if (!Objects.equals(source, cachedSource)) return;
        Float cachedPlayTime = this.getMemory().getProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, Float.class);
        if ((cachedPlayTime == null) || (cachedPlayTime <= 0.0F)) return;
        float duration = video.getDuration();
        float seekTime = cachedPlayTime;
        if (duration > 0.0F) {
            seekTime = Math.min(seekTime, Math.max(0.0F, duration - 0.05F));
        }
        video.setPlayTime(Math.max(0.0F, seekTime));
    }

}
