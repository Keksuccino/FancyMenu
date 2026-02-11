package de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.IVideoMenuBackground;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
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
    private static final Object BACKGROUND_INSTANCE_LOCK_FANCYMENU = new Object();
    private static final Set<NativeVideoMenuBackground> BACKGROUND_INSTANCES_FANCYMENU = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU = "native_video_last_stopped_play_time_seconds";
    private static final String MEMORY_LAST_STOPPED_SOURCE_FANCYMENU = "native_video_last_stopped_source";
    private static final String MEMORY_LAST_ENDED_SOURCE_FANCYMENU = "native_video_last_ended_source";
    private static final ResourceLocation MISSING_TEXTURE_FANCYMENU = IVideo.MISSING_TEXTURE_LOCATION;
    private static final File VIDEO_THUMBNAIL_DIR_FANCYMENU = FileUtils.createDirectory(new File(FancyMenu.INSTANCE_DATA_DIR, "video_thumbnails"));
    private static final DrawableColor WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU = DrawableColor.of(180, 0, 0);
    private static final String WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia/files/all?page=1&pageSize=20&showAlphaFiles=show";
    private static final String WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia-binaries/files/all?page=1&pageSize=20&showAlphaFiles=show";

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
    @Nullable
    protected ResourceSupplier<ITexture> pausedThumbnailSupplier = null;
    @Nullable
    protected String pausedThumbnailSource = null;
    protected float watermediaDownloadX_FancyMenu = Float.NaN;
    protected float watermediaDownloadY_FancyMenu = Float.NaN;
    protected float watermediaDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaDownloadHeight_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadX_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadY_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
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
            IVideo cachedVideo = this.video;
            if (cachedVideo != null) {
                this.updateEndedStateMemory(cachedVideo, this.activeVideoSource);
            }
        }
    }, 0L, 900L, TimeUnit.MILLISECONDS);

    public NativeVideoMenuBackground(MenuBackgroundBuilder<NativeVideoMenuBackground> builder) {
        super(builder);
        synchronized (BACKGROUND_INSTANCE_LOCK_FANCYMENU) {
            BACKGROUND_INSTANCES_FANCYMENU.add(this);
        }
    }

    public static int forceReloadAllAfterSoundEngineReload_FancyMenu() {
        List<NativeVideoMenuBackground> backgrounds;
        synchronized (BACKGROUND_INSTANCE_LOCK_FANCYMENU) {
            backgrounds = new ArrayList<>(BACKGROUND_INSTANCES_FANCYMENU);
        }

        if (backgrounds.isEmpty()) return 0;

        Set<IVideo> videosToRelease = Collections.newSetFromMap(new IdentityHashMap<>());
        int resetCount = 0;
        int stoppedCount = 0;

        for (NativeVideoMenuBackground background : backgrounds) {
            if (background == null) continue;
            IVideo oldVideo = background.video;
            if (oldVideo != null) {
                videosToRelease.add(oldVideo);
            }
            if (background.resetBackgroundAndReturnStopState()) {
                stoppedCount++;
            }
            resetCount++;
        }

        int releasedCount = 0;
        for (IVideo video : videosToRelease) {
            try {
                ResourceHandlers.getVideoHandler().release(video);
                releasedCount++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release cached native video resource after sound engine reload!", ex);
            }
        }

        LOGGER.info("[FANCYMENU] Forced native video background reload after sound engine reload. backgroundsReset: {}, stoppedPlayers: {}, videoResourcesReleased: {}",
                resetCount,
                stoppedCount,
                releasedCount);

        return resetCount;
    }

    public static void savePauseThumbnailForBackgroundIdentifier_FancyMenu(@Nullable String identifier) {
        if ((identifier == null) || identifier.isBlank()) return;
        List<NativeVideoMenuBackground> backgrounds;
        synchronized (BACKGROUND_INSTANCE_LOCK_FANCYMENU) {
            backgrounds = new ArrayList<>(BACKGROUND_INSTANCES_FANCYMENU);
        }
        for (NativeVideoMenuBackground background : backgrounds) {
            if (background == null) continue;
            if (!identifier.equals(background.getInstanceIdentifier())) continue;
            background.trySaveCurrentFrameThumbnail_FancyMenu();
        }
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

        if (this.shouldRenderWatermediaMissingOverlay_FancyMenu(supplier)) {
            this.renderWatermediaMissingOverlay_FancyMenu(graphics, mouseX, mouseY);
            RenderingUtils.resetShaderColor(graphics);
            RenderSystem.disableBlend();
            return;
        }
        this.resetWatermediaDownloadLinkBounds_FancyMenu();

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
        this.updateEndedStateMemory(this.video, this.activeVideoSource);

        boolean pausedState = this._isPaused();
        if (pausedState) {
            if ((this.lastPausedState == null) || !Objects.equals(true, this.lastPausedState)) {
                this.video.pause();
            }
        } else {
            boolean shouldRecoverPlayback =
                    (this.lastPausedState == null)
                    || Objects.equals(true, this.lastPausedState)
                    || this.video.isPaused()
                    || !this.video.isPlaying()
                    || (loop && this.video.isEnded());
            // Keep editor playback alive even when lifecycle hooks paused the player in between renders.
            if (!this.shouldKeepNaturalEndedState(this.video) && shouldRecoverPlayback) {
                this.video.play();
            }
        }
        this.lastPausedState = pausedState;

        ResourceLocation resourceLocation = this.video.getResourceLocation();
        boolean missingPausedFrame = pausedState && ((resourceLocation == null) || Objects.equals(resourceLocation, MISSING_TEXTURE_FANCYMENU));
        if (missingPausedFrame) {
            this.renderPausedThumbnailFallback_FancyMenu(graphics, parallaxOffset, parallaxIntensityX, parallaxIntensityY);
        } else if (resourceLocation != null) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!this.showBackground.tryGetNonNull()) return false;
        if (!this.shouldRenderWatermediaMissingOverlay_FancyMenu(this.videoSupplier.get())) return false;
        if (this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        if (this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        return false;
    }

    protected boolean shouldRenderWatermediaMissingOverlay_FancyMenu(@Nullable ResourceSupplier<IVideo> supplier) {
        return (supplier != null) && !WatermediaUtil.isWatermediaLoaded();
    }

    protected void renderWatermediaMissingOverlay_FancyMenu(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        int width = getScreenWidth();
        int height = getScreenHeight();
        graphics.fill(0, 0, width, height, WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU.getColorIntWithAlpha(this.opacity));

        Component infoText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.info");
        Component downloadText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download");
        Component downloadBinariesText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download_binaries");

        float normalTextSize = UIBase.getUITextSizeNormal();
        float largeTextSize = UIBase.getUITextSizeLarge();
        float infoTextWidth = UIBase.getUITextWidth(infoText, normalTextSize);
        float infoTextHeight = UIBase.getUITextHeight(normalTextSize);
        float downloadTextWidth = UIBase.getUITextWidth(downloadText, largeTextSize);
        float downloadTextHeight = UIBase.getUITextHeight(largeTextSize);
        float downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, largeTextSize);
        float downloadBinariesTextHeight = UIBase.getUITextHeight(largeTextSize);
        float spacing = Math.max(4.0F, UIBase.getUITextHeightSmall());
        float totalHeight = infoTextHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;

        float infoX = (width / 2.0F) - (infoTextWidth / 2.0F);
        float infoY = (height / 2.0F) - (totalHeight / 2.0F);
        float downloadX = (width / 2.0F) - (downloadTextWidth / 2.0F);
        float downloadY = infoY + infoTextHeight + spacing;
        float downloadBinariesX = (width / 2.0F) - (downloadBinariesTextWidth / 2.0F);
        float downloadBinariesY = downloadY + downloadTextHeight + spacing;

        this.watermediaDownloadX_FancyMenu = downloadX;
        this.watermediaDownloadY_FancyMenu = downloadY;
        this.watermediaDownloadWidth_FancyMenu = downloadTextWidth;
        this.watermediaDownloadHeight_FancyMenu = downloadTextHeight;
        this.watermediaBinariesDownloadX_FancyMenu = downloadBinariesX;
        this.watermediaBinariesDownloadY_FancyMenu = downloadBinariesY;
        this.watermediaBinariesDownloadWidth_FancyMenu = downloadBinariesTextWidth;
        this.watermediaBinariesDownloadHeight_FancyMenu = downloadBinariesTextHeight;

        boolean hoveredMain = this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY);
        boolean hoveredBinaries = this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY);
        if (hoveredMain || hoveredBinaries) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }
        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withUnderlined(hoveredBinaries));

        int textColor = DrawableColor.WHITE.getColorIntWithAlpha(this.opacity);
        UIBase.renderText(graphics, infoText, infoX, infoY, textColor, normalTextSize);
        UIBase.renderText(graphics, renderedDownloadText, downloadX, downloadY, textColor, largeTextSize);
        UIBase.renderText(graphics, renderedDownloadBinariesText, downloadBinariesX, downloadBinariesY, textColor, largeTextSize);
    }

    protected boolean isMouseOverWatermediaDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaDownloadX_FancyMenu + this.watermediaDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaDownloadY_FancyMenu + this.watermediaDownloadHeight_FancyMenu));
    }

    protected boolean isMouseOverWatermediaBinariesDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaBinariesDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaBinariesDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaBinariesDownloadX_FancyMenu + this.watermediaBinariesDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaBinariesDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaBinariesDownloadY_FancyMenu + this.watermediaBinariesDownloadHeight_FancyMenu));
    }

    protected void resetWatermediaDownloadLinkBounds_FancyMenu() {
        this.watermediaDownloadX_FancyMenu = Float.NaN;
        this.watermediaDownloadY_FancyMenu = Float.NaN;
        this.watermediaDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaDownloadHeight_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadX_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadY_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    }

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset, float parallaxIntensityX, float parallaxIntensityY) {
        this.renderKeepAspectRatioWithAspectRatio_FancyMenu(graphics, resourceLocation, this.video.getAspectRatio(), parallaxOffset, parallaxIntensityX, parallaxIntensityY);
    }

    protected void renderKeepAspectRatioWithAspectRatio_FancyMenu(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, @NotNull AspectRatio ratio, float[] parallaxOffset, float parallaxIntensityX, float parallaxIntensityY) {
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

    protected boolean renderPausedThumbnailFallback_FancyMenu(@NotNull GuiGraphics graphics, float[] parallaxOffset, float parallaxIntensityX, float parallaxIntensityY) {
        ITexture thumbnail = this.getPausedThumbnailTexture_FancyMenu();
        if (thumbnail == null) return false;
        ResourceLocation thumbnailLocation = thumbnail.getResourceLocation();
        if ((thumbnailLocation == null) || Objects.equals(thumbnailLocation, ITexture.MISSING_TEXTURE_LOCATION)) return false;
        graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
        if (this.keepBackgroundAspectRatio) {
            this.renderKeepAspectRatioWithAspectRatio_FancyMenu(graphics, thumbnailLocation, thumbnail.getAspectRatio(), parallaxOffset, parallaxIntensityX, parallaxIntensityY);
        } else {
            this.renderFullScreen(graphics, thumbnailLocation, parallaxOffset, parallaxIntensityX, parallaxIntensityY);
        }
        return true;
    }

    @Nullable
    protected ITexture getPausedThumbnailTexture_FancyMenu() {
        File thumbnailFile = getThumbnailFileForIdentifier_FancyMenu(this.getInstanceIdentifier());
        if (!thumbnailFile.isFile()) return null;
        String thumbnailSource = toLocalThumbnailSource_FancyMenu(thumbnailFile);
        if ((this.pausedThumbnailSupplier == null) || !Objects.equals(this.pausedThumbnailSource, thumbnailSource)) {
            this.pausedThumbnailSupplier = ResourceSupplier.image(thumbnailSource);
            this.pausedThumbnailSource = thumbnailSource;
        }
        ITexture thumbnail = this.pausedThumbnailSupplier.get();
        if ((thumbnail == null) || thumbnail.isLoadingFailed()) return null;
        return thumbnail;
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
        String newVideoSource = (newVideo != null) ? this.currentResolvedVideoSource : null;
        if ((this.video == newVideo) && Objects.equals(this.activeVideoSource, newVideoSource)) return;

        IVideo oldVideo = this.video;
        String oldVideoSource = this.activeVideoSource;
        boolean sourceChanged = !Objects.equals(oldVideoSource, newVideoSource);
        if (oldVideo != null) {
            if (sourceChanged && isEditor()) {
                oldVideo.stop();
            }
            this.releaseVideoReference(oldVideo, oldVideoSource);
        }

        this.video = newVideo;
        if (newVideo != null) {
            this.acquireVideoReference(newVideo);
        }
        this.activeVideoSource = newVideoSource;
        if (newVideo != null) {
            this.tryRestorePlaybackPositionFromMemory(newVideo, this.activeVideoSource);
        }
        this.initialized = (newVideo != null);
        this.lastLoop = null;
        this.cachedActualVolume = -10000F;
        this.lastCachedActualVolume = -11000F;
        this.lastPausedState = null;
        this.pausedThumbnailSupplier = null;
        this.pausedThumbnailSource = null;
        this.resetWatermediaDownloadLinkBounds_FancyMenu();

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
        this.tryResumeFromSystemPauseIfNeeded();
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
        this.tryResumeFromSystemPauseIfNeeded();
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
        this.pausedThumbnailSupplier = null;
        this.pausedThumbnailSource = null;
        this.resetWatermediaDownloadLinkBounds_FancyMenu();
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
            return false;
        }
        return (this.getControllerPausedState() || this.pausedBySystem);
    }

    protected void tryResumeFromSystemPauseIfNeeded() {
        if (!this.initialized || (this.video == null) || !this.pausedBySystem) return;
        this.pausedBySystem = false;
        if (this._isPaused()) {
            this.video.pause();
            return;
        }
        if (!this.shouldKeepNaturalEndedState(this.video)) {
            this.video.play();
        }
    }

    protected void trySaveCurrentFrameThumbnail_FancyMenu() {
        if (!this.initialized || (this.video == null)) return;
        ResourceLocation resourceLocation = this.video.getResourceLocation();
        if ((resourceLocation == null) || Objects.equals(resourceLocation, MISSING_TEXTURE_FANCYMENU)) return;
        int width = Math.max(1, this.video.getWidth());
        int height = Math.max(1, this.video.getHeight());
        String identifier = this.getInstanceIdentifier();
        Runnable captureTask = () -> this.saveCurrentFrameThumbnailOnRenderThread_FancyMenu(identifier, resourceLocation, width, height);
        if (RenderSystem.isOnRenderThreadOrInit()) {
            captureTask.run();
        } else {
            RenderSystem.recordRenderCall(captureTask::run);
        }
    }

    protected void saveCurrentFrameThumbnailOnRenderThread_FancyMenu(@NotNull String identifier, @NotNull ResourceLocation resourceLocation, int width, int height) {
        NativeImage image = null;
        try {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
            if (texture == null) return;
            int textureId = texture.getId();
            if (textureId <= 0) return;
            GlStateManager._bindTexture(textureId);
            image = new NativeImage(width, height, false);
            image.downloadTexture(0, false);
            File thumbnailFile = getThumbnailFileForIdentifier_FancyMenu(identifier);
            FileUtils.createDirectory(thumbnailFile.getParentFile());
            NativeImage capturedImage = image;
            image = null;
            Util.ioPool().execute(() -> {
                try {
                    capturedImage.writeToFile(thumbnailFile);
                    releaseCachedThumbnailTexture_FancyMenu(thumbnailFile);
                } catch (Exception ex) {
                    LOGGER.warn("[FANCYMENU] Failed to save paused native video thumbnail. backgroundId: {}", identifier, ex);
                } finally {
                    capturedImage.close();
                }
            });
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to save paused native video thumbnail. backgroundId: {}", identifier, ex);
        } finally {
            if (image != null) {
                image.close();
            }
        }
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
        boolean ended = this.isNonLoopingVideoNaturallyEnded(video);
        this.setEndedStateInMemory(source, ended);
        float playTime = Math.max(0.0F, video.getPlayTime());
        if (ended) {
            float duration = Math.max(Math.max(0.0F, video.getDuration()), this.cachedDuration.get());
            if (duration > 0.05F) {
                playTime = Math.max(playTime, duration - 0.05F);
            }
        }
        if (playTime <= 0.05F) return;
        String cachedSource = this.getMemory().getStringProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU);
        Float cachedPlayTime = this.getMemory().getProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, Float.class);
        if (!allowLowerOverwrite && Objects.equals(source, cachedSource) && (cachedPlayTime != null) && (cachedPlayTime > playTime)) return;
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU, source);
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, playTime);
    }

    protected void tryRestorePlaybackPositionFromMemory(@NotNull IVideo video, @Nullable String source) {
        if ((source == null) || source.isEmpty()) return;
        boolean restoreEndedState = this.shouldRestoreEndedStateFromMemory(source) && !this.loop.tryGetNonNull();
        String cachedSource = this.getMemory().getStringProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU);
        if (!restoreEndedState && !Objects.equals(source, cachedSource)) return;
        Float cachedPlayTime = this.getMemory().getProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, Float.class);
        if (((cachedPlayTime == null) || (cachedPlayTime <= 0.0F)) && !restoreEndedState) return;
        float duration = Math.max(Math.max(0.0F, video.getDuration()), this.cachedDuration.get());
        float seekTime = Math.max(0.0F, Objects.requireNonNullElse(cachedPlayTime, 0.0F));
        if (restoreEndedState && (duration > 0.0F)) {
            seekTime = Math.max(seekTime, Math.max(0.0F, duration - 0.05F));
        }
        if (seekTime <= 0.0F) return;
        if (duration > 0.0F) {
            seekTime = Math.min(seekTime, Math.max(0.0F, duration - 0.05F));
        }
        video.setPlayTime(Math.max(0.0F, seekTime));
    }

    protected void updateEndedStateMemory(@NotNull IVideo video, @Nullable String source) {
        if ((source == null) || source.isEmpty()) return;
        boolean ended = this.isNonLoopingVideoNaturallyEnded(video);
        this.setEndedStateInMemory(source, ended);
        if (!ended) return;
        float duration = Math.max(Math.max(0.0F, video.getDuration()), this.cachedDuration.get());
        float playTime = Math.max(Math.max(0.0F, video.getPlayTime()), this.cachedPlayTime.get());
        float targetTime = playTime;
        if (duration > 0.05F) {
            targetTime = Math.max(targetTime, duration - 0.05F);
        }
        if (targetTime <= 0.05F) return;
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_SOURCE_FANCYMENU, source);
        this.getMemory().putProperty(MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU, targetTime);
    }

    protected boolean isNonLoopingVideoNaturallyEnded(@NotNull IVideo video) {
        if (this.loop.tryGetNonNull()) return false;
        if (video.isLooping()) return false;
        return video.isEnded();
    }

    protected boolean shouldKeepNaturalEndedState(@NotNull IVideo video) {
        if (this.loop.tryGetNonNull()) return false;
        if (video.isLooping()) return false;
        return video.isEnded();
    }

    protected void setEndedStateInMemory(@Nullable String source, boolean ended) {
        if ((source == null) || source.isEmpty()) return;
        if (ended) {
            this.getMemory().putProperty(MEMORY_LAST_ENDED_SOURCE_FANCYMENU, source);
            return;
        }
        String endedSource = this.getMemory().getStringProperty(MEMORY_LAST_ENDED_SOURCE_FANCYMENU);
        if (Objects.equals(source, endedSource)) {
            this.getMemory().removeProperty(MEMORY_LAST_ENDED_SOURCE_FANCYMENU);
        }
    }

    protected boolean shouldRestoreEndedStateFromMemory(@Nullable String source) {
        if ((source == null) || source.isEmpty()) return false;
        String endedSource = this.getMemory().getStringProperty(MEMORY_LAST_ENDED_SOURCE_FANCYMENU);
        return Objects.equals(source, endedSource);
    }

    @NotNull
    protected static File getThumbnailFileForIdentifier_FancyMenu(@NotNull String identifier) {
        return new File(VIDEO_THUMBNAIL_DIR_FANCYMENU, sanitizeThumbnailFileName_FancyMenu(identifier) + ".png");
    }

    @NotNull
    protected static String sanitizeThumbnailFileName_FancyMenu(@NotNull String identifier) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            boolean allowed = ((c >= 'a') && (c <= 'z'))
                    || ((c >= 'A') && (c <= 'Z'))
                    || ((c >= '0') && (c <= '9'))
                    || (c == '-')
                    || (c == '_')
                    || (c == '.');
            builder.append(allowed ? c : '_');
        }
        if (builder.isEmpty()) return "video_background";
        return builder.toString();
    }

    @NotNull
    protected static String toLocalThumbnailSource_FancyMenu(@NotNull File thumbnailFile) {
        return ResourceSource.of(thumbnailFile.getAbsolutePath().replace("\\", "/"), ResourceSourceType.LOCAL).getSourceWithPrefix();
    }

    protected static void releaseCachedThumbnailTexture_FancyMenu(@NotNull File thumbnailFile) {
        try {
            ResourceHandlers.getImageHandler().release(toLocalThumbnailSource_FancyMenu(thumbnailFile), true);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to release cached native video thumbnail texture.", ex);
        }
    }

}
