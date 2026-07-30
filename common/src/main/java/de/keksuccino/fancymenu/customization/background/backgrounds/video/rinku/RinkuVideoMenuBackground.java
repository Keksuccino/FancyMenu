package de.keksuccino.fancymenu.customization.background.backgrounds.video.rinku;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.IVideoMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.VideoBackgroundTaskController;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.rinku.RinkuVideoSourceResolver;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.video.rinku.RinkuVideoManager;
import de.keksuccino.fancymenu.util.rendering.video.rinku.RinkuVideoPlayer;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class RinkuVideoMenuBackground extends MenuBackground<RinkuVideoMenuBackground> implements IVideoMenuBackground {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = FancyMenuExecutors.newSingleThreadScheduledExecutor("FancyMenu-RinkuVideoMenuBackground");
    private static final DrawableColor MISSING_RINKU_COLOR = DrawableColor.of(Color.RED);

    @SuppressWarnings("unchecked")
    public final Property<ResourceSource> rawVideoUrlSource = putProperty(Property.resourceSourceProperty("source", null, "fancymenu.elements.video_rinku.set_source", true, true, true, null, (FileTypeGroup<FileType<Resource>>)(FileTypeGroup<?>)FileTypeGroups.VIDEO_TYPES, FileMediaType.VIDEO));
    public final Property<Boolean> loop = putProperty(Property.booleanProperty("loop", false, "fancymenu.elements.video_rinku.loop"));
    /** Value between 0.0 and 1.0 **/
    public final Property<Float> volume = putProperty(Property.floatProperty("volume", 1.0F, "fancymenu.elements.video_rinku.volume"))
            .setValueSetProcessor(value -> Math.max(0.0F, Math.min(1.0F, value)));
    public final Property.StringProperty soundSource = putProperty(Property.stringProperty("sound_source", SoundSource.MASTER.getName(), false, false, "fancymenu.elements.video_rinku.sound_channel"));
    public final Property<Boolean> parallaxEnabled = putProperty(Property.booleanProperty("parallax", false, "fancymenu.backgrounds.image.configure.parallax"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.FloatProperty parallaxIntensityXString = putProperty(Property.floatProperty("parallax_intensity_x", 0.02F, "fancymenu.backgrounds.image.configure.parallax_intensity_x"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.FloatProperty parallaxIntensityYString = putProperty(Property.floatProperty("parallax_intensity_y", 0.02F, "fancymenu.backgrounds.image.configure.parallax_intensity_y"));
    /** When TRUE, the parallax effect will move in the SAME direction as the mouse, otherwise it moves in the opposite direction **/
    public final Property<Boolean> invertParallax = putProperty(Property.booleanProperty("invert_parallax", false, "fancymenu.backgrounds.image.configure.invert_parallax"));

    protected volatile boolean initialized = false;
    @Nullable
    protected RinkuVideoManager videoManager = null;
    protected RinkuVideoPlayer videoPlayer = null;
    protected String playerId = null;
    protected String lastFinalUrl = null;
    protected int lastAbsoluteWidth = -10000;
    protected int lastAbsoluteHeight = -10000;
    protected int lastAbsoluteX = -10000;
    protected int lastAbsoluteY = -10000;
    protected Boolean lastLoop = null;
    protected float cachedActualVolume = -10000F;
    protected float lastCachedActualVolume = -11000F;
    protected Boolean lastPausedState = null;
    protected volatile long lastRenderTickTime = -1L;
    protected boolean pausedBySystem = false;
    protected final AtomicReference<Float> cachedDuration = new AtomicReference<>(0F);
    protected final AtomicReference<Float> cachedPlayTime = new AtomicReference<>(0F);
    protected final VideoBackgroundTaskController taskController;

    public RinkuVideoMenuBackground(MenuBackgroundBuilder<RinkuVideoMenuBackground> builder) {
        super(builder);
        this.taskController = new VideoBackgroundTaskController(EXECUTOR, this::runGarbageCheck, 100L, this::updateCachedPlaybackState, 900L);
        if (RinkuUtil.isRinkuLoaded() && RinkuUtil.rinku_initialized) this.videoManager = RinkuVideoManager.getInstance();
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.rawVideoUrlSource.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> {
                    if (this.rawVideoUrlSource.get() == null) {
                        return UITooltip.of(Component.translatable("fancymenu.backgrounds.video_rinku.configure.no_video"));
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
        this.addCycleContextMenuEntryTo(menu, "sound_source", soundSources, RinkuVideoMenuBackground.class, RinkuVideoMenuBackground::getSoundSourceOrDefault, (background, source) -> {
            if (source != null) {
                background.soundSource.set(source.getName());
            }
        }, (menu1, entry, switcherValue) -> {
            Component name = Component.translatable("soundCategory." + switcherValue.getName())
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            return Component.translatable("fancymenu.elements.video_rinku.sound_channel", name);
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

    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.taskController.isClosed()) return;

        float parallaxIntensityX = this.parallaxIntensityXString.getFloat();
        float parallaxIntensityY = this.parallaxIntensityYString.getFloat();

        if (!RinkuUtil.isRinkuLoaded() || !RinkuUtil.rinku_initialized) {
            graphics.fill(0, 0, getScreenWidth(), getScreenHeight(), MISSING_RINKU_COLOR.getColorInt());
            graphics.centeredText(Minecraft.getInstance().font, "§lRinku IS NOT INSTALLED! PLEASE DOWNLOAD FROM CURSEFORGE!", getScreenWidth() / 2, getScreenHeight() / 2, -1);
            return;
        }

        this.lastRenderTickTime = System.currentTimeMillis();

        float[] parallaxOffset = calculateParallaxOffset(mouseX, mouseY, parallaxIntensityX, parallaxIntensityY);
        int x = 0;
        int y = 0;
        int w = getScreenWidth();
        int h = getScreenHeight();

        if (this.parallaxEnabled.tryGetNonNull()) {
            // Reduce the expansion amount for parallax
            w = (int)(getScreenWidth() * (1.0F + parallaxIntensityX));
            h = (int)(getScreenHeight() * (1.0F + parallaxIntensityY));
            // Center the expanded area and apply parallax offset
            x = -((w - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            y = -((h - getScreenHeight()) / 2) + (int)parallaxOffset[1];
        }

        // Always draw black background
        graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorIntWithAlpha(this.opacity));

        if (!this.ensureVideoManagerReady()) {
            return;
        }
        if (!this.taskController.start()) return;

        if (!this.initialized) {
            this.initialized = true;
            playerId = videoManager.createPlayer(x, y, w, h);
            if (playerId != null) {
                videoPlayer = videoManager.getPlayer(playerId);
                if (videoPlayer != null) {
                    videoPlayer.setFillScreen(true); // Enable fill screen by default
                }
            }
        }

        if (this.videoPlayer == null) return;

        this.updateVolume();
        if ((this.lastCachedActualVolume == -11000F) || (this.cachedActualVolume != this.lastCachedActualVolume)) {
            this.setVolume(this.volume.tryGetNonNull(), true);
        }
        this.lastCachedActualVolume = this.cachedActualVolume;

        boolean loop = this.loop.tryGetNonNull();
        if ((this.lastLoop == null) || !Objects.equals(loop, this.lastLoop)) {
            this.videoPlayer.setLooping(loop);
        }
        this.lastLoop = loop;

        // Update size and position of player if needed
        if ((this.lastAbsoluteX != x) || (this.lastAbsoluteY != y) || (this.lastAbsoluteWidth != w) || (this.lastAbsoluteHeight != h)) {
            this.videoPlayer.setPosition(x, y);
            this.videoPlayer.setSize(w, h);
        }
        this.lastAbsoluteX = x;
        this.lastAbsoluteY = y;
        this.lastAbsoluteWidth = w;
        this.lastAbsoluteHeight = h;

        boolean pausedState = this._isPaused();

        String finalVideoUrl = null;
        ResourceSource rawSource = this.rawVideoUrlSource.get();
        if (rawSource != null) {
            String placeholderExpandedSource = PlaceholderParser.replacePlaceholders(rawSource.getSourceWithoutPrefix());
            finalVideoUrl = RinkuVideoSourceResolver.resolve(rawSource, placeholderExpandedSource);
        }
        // Check if the video URL has changed since last time
        boolean videoUrlChanged = !Objects.equals(finalVideoUrl, this.lastFinalUrl);
        this.lastFinalUrl = finalVideoUrl;
        if (videoUrlChanged && (finalVideoUrl != null)) {
            // Stop any existing video before loading new one
            this.videoPlayer.stop();
            try {
                this.videoPlayer.loadVideo(finalVideoUrl);
                if (!pausedState) this.videoPlayer.play();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Error processing video URL", e);
            }
        }

        this.tryApplyPendingControllerSeekTimeMs(finalVideoUrl != null);
        if ((this.lastPausedState == null) || !Objects.equals(pausedState, this.lastPausedState)) {
            if (pausedState) {
                this.videoPlayer.pause();
            } else {
                this.videoPlayer.play();
            }
        }
        this.lastPausedState = pausedState;

        this.videoPlayer.setOpacity(this.opacity);

        if (finalVideoUrl != null) {
            this.videoPlayer.extractRenderState(graphics, mouseX, mouseY, partial);
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
        if (this.initialized && (this.videoPlayer != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.videoPlayer.play();
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        if (this.initialized && (this.videoPlayer != null)) {
            this.pausedBySystem = true;
            this.videoPlayer.pause();
        }
    }

    @Override
    public void onAfterEnable() {
        super.onAfterEnable();
        if (this.initialized && (this.videoPlayer != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.videoPlayer.play();
        }
    }

    @Override
    public void onDisableOrRemove() {
        super.onDisableOrRemove();
        if (this.initialized && (this.videoPlayer != null)) {
            this.pausedBySystem = true;
            this.videoPlayer.pause();
        }
    }

    /**
     * @param volume Value between 0.0 and 1.0.
     */
    protected void setVolume(float volume, boolean updatePlayer) {
        volume = Math.max(0.0F, Math.min(1.0F, volume));
        if ((this.videoPlayer != null)) {
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
            if (updatePlayer) {
                this.videoPlayer.setVolume(Math.min(1.0F, Math.max(0.0F, actualVolume)));
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

    protected void tryApplyPendingControllerSeekTimeMs(boolean hasVideoSource) {
        if (!this.initialized || (this.videoPlayer == null) || !hasVideoSource) return;
        if (this.cachedDuration.get() <= 0.0F) return;
        Long pendingSeekTimeMs = VideoElementController.pollPendingSeekTimeMs(this.getInstanceIdentifier());
        if (pendingSeekTimeMs == null) return;
        try {
            this.videoPlayer.setCurrentTimeMillis(Math.max(0L, pendingSeekTimeMs));
            this.cachedPlayTime.set(Math.max(0.0F, pendingSeekTimeMs / 1000.0F));
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to apply pending seek time to Rinku video background. backgroundId: {}, seekTimeMs: {}", this.getInstanceIdentifier(), pendingSeekTimeMs, ex);
        }
    }

    public void disposePlayer() {
        if ((this.videoManager != null) && (this.playerId != null)) {
            this.videoManager.removePlayer(this.playerId);
        } else if (this.videoPlayer != null) {
            this.videoPlayer.dispose();
        }
    }

    public void resetBackground() {
        this.taskController.stop();
        this.resetPlayerState();
    }

    @Override
    public void onDestroyBackground() {
        super.onDestroyBackground();
        this.taskController.close();
        this.resetPlayerState();
    }

    protected void resetPlayerState() {
        this.initialized = false;
        this.disposePlayer();
        this.videoPlayer = null;
        this.playerId = null;
        this.lastFinalUrl = null;
        this.lastAbsoluteWidth = -10000;
        this.lastAbsoluteHeight = -10000;
        this.lastAbsoluteX = -10000;
        this.lastAbsoluteY = -10000;
        this.lastLoop = null;
        this.cachedActualVolume = -10000F;
        this.lastCachedActualVolume = -11000F;
        this.lastPausedState = null;
        this.lastRenderTickTime = -1L;
        this.pausedBySystem = false;
        this.cachedDuration.set(0F);
        this.cachedPlayTime.set(0F);
    }

    protected float _getDuration() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getDuration();
    }

    protected float _getPlayTime() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getCurrentTime();
    }

    protected boolean _isPaused() {
        if (isEditor()) return false;
        return (this.getControllerPausedState() || this.pausedBySystem);
    }

    protected void runGarbageCheck() {
        if (this.initialized && (this.lastRenderTickTime != -1L) && ((this.lastRenderTickTime + 11000L) < System.currentTimeMillis())) this.resetBackground();
    }

    protected void updateCachedPlaybackState() {
        if (!this.initialized) return;
        this.cachedDuration.set(this._getDuration());
        this.cachedPlayTime.set(this._getPlayTime());
    }

    @Override
    public float getDuration() {
        return this.cachedDuration.get();
    }

    @Override
    public float getPlayTime() {
        return this.cachedPlayTime.get();
    }

    protected boolean ensureVideoManagerReady() {
        if (this.videoManager != null) return true;
        if (!RinkuUtil.isRinkuLoaded() || !RinkuUtil.rinku_initialized) return false;
        if (!RinkuVideoManager.initialized) return false;
        this.videoManager = RinkuVideoManager.getInstance();
        return (this.videoManager != null);
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

}
