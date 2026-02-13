package de.keksuccino.fancymenu.customization.element.elements.video.nativevideo;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.video.IVideoElement;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.fancymenu.util.resource.resources.video.NativeVideoReferenceTracker;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NativeVideoElement extends AbstractElement implements IVideoElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Object ELEMENT_INSTANCE_LOCK_FANCYMENU = new Object();
    private static final Set<NativeVideoElement> ELEMENT_INSTANCES_FANCYMENU = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String MEMORY_LAST_STOPPED_PLAY_TIME_SECONDS_FANCYMENU = "native_video_element_last_stopped_play_time_seconds";
    private static final String MEMORY_LAST_STOPPED_SOURCE_FANCYMENU = "native_video_element_last_stopped_source";
    private static final String MEMORY_LAST_ENDED_SOURCE_FANCYMENU = "native_video_element_last_ended_source";
    private static final ResourceLocation MISSING_TEXTURE_FANCYMENU = IVideo.MISSING_TEXTURE_LOCATION;
    private static final DrawableColor WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU = DrawableColor.of(180, 0, 0);
    private static final String WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia/files/all?page=1&pageSize=20&showAlphaFiles=show";
    private static final String WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia-binaries/files/all?page=1&pageSize=20&showAlphaFiles=show";

    public final Property<ResourceSupplier<IVideo>> videoSupplier = putProperty(Property.resourceSupplierProperty(IVideo.class, "source", null, "fancymenu.elements.video_mcef.set_source", true, true, true, null));
    public final Property.BooleanProperty loop = putProperty(Property.booleanProperty("loop", false, "fancymenu.elements.video_mcef.loop"));
    /** Value between 0.0 and 1.0 **/
    public final Property<Float> volume = putProperty(Property.floatProperty("volume", 1.0F, "fancymenu.elements.video_mcef.volume"))
            .setValueSetProcessor(value -> Math.max(0.0F, Math.min(1.0F, value)));
    public final Property.StringProperty soundSource = putProperty(Property.stringProperty("sound_source", SoundSource.MASTER.getName(), false, false, "fancymenu.elements.video_mcef.sound_channel"));
    public final Property.BooleanProperty preserveAspectRatio = putProperty(Property.booleanProperty("preserve_aspect_ratio", true, "fancymenu.elements.video_mcef.preserve_aspect_ratio"));
    public final Property.BooleanProperty playInEditor = putProperty(Property.booleanProperty("play_in_editor", true, "fancymenu.backgrounds.video.play_in_editor"));

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
    protected float watermediaDownloadX_FancyMenu = Float.NaN;
    protected float watermediaDownloadY_FancyMenu = Float.NaN;
    protected float watermediaDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaDownloadHeight_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadX_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadY_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    protected boolean watermediaLeftMouseWasDown_FancyMenu = false;
    protected volatile boolean destroyed_FancyMenu = false;
    // The field is currently unused, but the scheduler is used, so don't delete this
    protected final ScheduledFuture<?> garbageChecker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.destroyed_FancyMenu) return;
        if (this.initialized && !this.shouldSkipWatchdogAutoClear() && (this.lastRenderTickTime != -1L) && ((this.lastRenderTickTime + 11000L) < System.currentTimeMillis())) {
            String sourceForLog = this.getConfiguredVideoSourceForLog();
            String videoTypeForLog = this.getVideoTypeForLog();
            boolean didStopPlayer = this.resetElementAndReturnStopState();
            LOGGER.info("[FANCYMENU] Auto-clearing native video element after watchdog timeout. source: {}, videoType: {}, didStopPlayer: {}, elementInstance: {}",
                    sourceForLog,
                    videoTypeForLog,
                    didStopPlayer,
                    this.getInstanceIdentifier());
        }
    }, 0L, 100L, TimeUnit.MILLISECONDS);
    // The field is currently unused, but the scheduler is used, so don't delete this
    protected final ScheduledFuture<?> asyncTicker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.destroyed_FancyMenu) return;
        if (this.initialized) {
            this.cachedDuration.set(this._getDuration());
            this.cachedPlayTime.set(this._getPlayTime());
            IVideo cachedVideo = this.video;
            if (cachedVideo != null) {
                this.updateEndedStateMemory(cachedVideo, this.activeVideoSource);
            }
        }
    }, 0L, 900L, TimeUnit.MILLISECONDS);

    public NativeVideoElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
        synchronized (ELEMENT_INSTANCE_LOCK_FANCYMENU) {
            ELEMENT_INSTANCES_FANCYMENU.add(this);
        }
    }

    public static int forceReloadAllAfterSoundEngineReload_FancyMenu() {
        List<NativeVideoElement> elements;
        synchronized (ELEMENT_INSTANCE_LOCK_FANCYMENU) {
            elements = new ArrayList<>(ELEMENT_INSTANCES_FANCYMENU);
        }

        if (elements.isEmpty()) return 0;

        Set<IVideo> videosToRelease = Collections.newSetFromMap(new IdentityHashMap<>());
        int resetCount = 0;
        int stoppedCount = 0;

        for (NativeVideoElement element : elements) {
            if ((element == null) || element.destroyed_FancyMenu) continue;
            IVideo oldVideo = element.video;
            if (oldVideo != null) {
                videosToRelease.add(oldVideo);
            }
            if (element.resetElementAndReturnStopState()) {
                stoppedCount++;
            }
            resetCount++;
        }

        int releasedCount = 0;
        for (IVideo video : videosToRelease) {
            if (NativeVideoReferenceTracker.hasReferences(video)) continue;
            try {
                ResourceHandlers.getVideoHandler().release(video);
                releasedCount++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release cached native video resource after sound engine reload!", ex);
            }
        }

        LOGGER.info("[FANCYMENU] Forced native video element reload after sound engine reload. elementsReset: {}, stoppedPlayers: {}, videoResourcesReleased: {}",
                resetCount,
                stoppedCount,
                releasedCount);

        return resetCount;
    }

    @Override
    public void afterConstruction() {
        super.afterConstruction();
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) {
            VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
        }
        if (getSoundSourceByName(Objects.requireNonNullElse(this.soundSource.get(), SoundSource.MASTER.getName())) == null) {
            this.soundSource.set(SoundSource.MASTER.getName());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        this.lastRenderTickTime = System.currentTimeMillis();

        int x = this.getAbsoluteX();
        int y = this.getAbsoluteY();
        int w = this.getAbsoluteWidth();
        int h = this.getAbsoluteHeight();

        RenderSystem.enableBlend();
        graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorIntWithAlpha(this.opacity));

        ResourceSupplier<IVideo> supplier = this.videoSupplier.get();
        this.currentResolvedVideoSource = this.getResolvedVideoSource(supplier);
        IVideo currentVideo = (supplier != null) ? supplier.get() : null;
        this.updateVideoReference(currentVideo);
        double resolvedMouseX_FancyMenu = MouseUtil.getGuiScaledMouseX();
        double resolvedMouseY_FancyMenu = MouseUtil.getGuiScaledMouseY();
        boolean showWatermediaWarning = this.shouldRenderWatermediaMissingOverlay_FancyMenu(supplier);
        this.tickWatermediaMissingOverlayMouseClick_FancyMenu(showWatermediaWarning, resolvedMouseX_FancyMenu, resolvedMouseY_FancyMenu);

        if (showWatermediaWarning) {
            this.renderWatermediaMissingOverlay_FancyMenu(graphics, resolvedMouseX_FancyMenu, resolvedMouseY_FancyMenu, x, y, w, h);
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
            if (!this.shouldKeepNaturalEndedState(this.video) && shouldRecoverPlayback) {
                this.video.play();
            }
        }
        this.lastPausedState = pausedState;

        ResourceLocation resourceLocation = this.video.getResourceLocation();
        if ((resourceLocation != null) && !Objects.equals(resourceLocation, MISSING_TEXTURE_FANCYMENU)) {
            graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
            if (this.preserveAspectRatio.tryGetNonNull()) {
                this.renderKeepAspectRatio(graphics, resourceLocation, this.video.getAspectRatio());
            } else {
                this.renderFullArea(graphics, resourceLocation);
            }
        }

        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.MouseButton.fromGlfwButton(button) != MouseUtil.MouseButton.LEFT && !MouseUtil.isLeftMouseDown()) return false;
        if (!this.shouldRenderWatermediaMissingOverlay_FancyMenu(this.videoSupplier.get())) return false;
        double resolvedMouseX_FancyMenu = MouseUtil.getGuiScaledMouseX();
        double resolvedMouseY_FancyMenu = MouseUtil.getGuiScaledMouseY();
        boolean handled = this.handleWatermediaMissingOverlayClick_FancyMenu(resolvedMouseX_FancyMenu, resolvedMouseY_FancyMenu);
        if (handled) {
            this.watermediaLeftMouseWasDown_FancyMenu = true;
        }
        return handled;
    }

    protected boolean handleWatermediaMissingOverlayClick_FancyMenu(double mouseX, double mouseY) {
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

    protected void tickWatermediaMissingOverlayMouseClick_FancyMenu(boolean showWarning, double mouseX, double mouseY) {
        boolean leftDown = MouseUtil.isLeftMouseDown();
        if (showWarning && leftDown && !this.watermediaLeftMouseWasDown_FancyMenu) {
            this.handleWatermediaMissingOverlayClick_FancyMenu(mouseX, mouseY);
        }
        this.watermediaLeftMouseWasDown_FancyMenu = leftDown;
    }

    protected boolean shouldRenderWatermediaMissingOverlay_FancyMenu(@Nullable ResourceSupplier<IVideo> supplier) {
        return (supplier != null) && !WatermediaUtil.isWatermediaVideoPlaybackAvailable();
    }

    protected void renderWatermediaMissingOverlay_FancyMenu(@NotNull GuiGraphics graphics, double mouseX, double mouseY, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU.getColorIntWithAlpha(this.opacity));

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

        float infoX = (x + (w / 2.0F)) - (infoTextWidth / 2.0F);
        float infoY = (y + (h / 2.0F)) - (totalHeight / 2.0F);
        float downloadX = (x + (w / 2.0F)) - (downloadTextWidth / 2.0F);
        float downloadY = infoY + infoTextHeight + spacing;
        float downloadBinariesX = (x + (w / 2.0F)) - (downloadBinariesTextWidth / 2.0F);
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
        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredBinaries));

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

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, @NotNull AspectRatio ratio) {
        int[] size = ratio.getAspectRatioSizeByMaximumSize(this.getAbsoluteWidth(), this.getAbsoluteHeight());
        int x = this.getAbsoluteX() + ((this.getAbsoluteWidth() - size[0]) / 2);
        int y = this.getAbsoluteY() + ((this.getAbsoluteHeight() - size[1]) / 2);
        graphics.blit(resourceLocation, x, y, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
    }

    protected void renderFullArea(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation) {
        graphics.blit(resourceLocation, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
    }

    protected void updateVideoReference(@Nullable IVideo newVideo) {
        String newVideoSource = (newVideo != null) ? this.currentResolvedVideoSource : null;
        if ((this.video == newVideo) && Objects.equals(this.activeVideoSource, newVideoSource)) return;

        IVideo oldVideo = this.video;
        String oldVideoSource = this.activeVideoSource;
        if (oldVideo != null) {
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
        this.resetWatermediaDownloadLinkBounds_FancyMenu();

        if (newVideo == null) {
            this.cachedDuration.set(0F);
            this.cachedPlayTime.set(0F);
        }
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
    public void onBeforeResizeScreen() {
        super.onBeforeResizeScreen();
        if (this.initialized && (this.video != null)) {
            this.cachePlaybackPositionToMemory(this.video, this.activeVideoSource, true);
            this.pausedBySystem = true;
            this.video.pause();
        }
    }

    @Override
    public void onBecomeVisible() {
        super.onBecomeVisible();
        this.tryResumeFromSystemPauseIfNeeded();
    }

    @Override
    public void onBecomeInvisible() {
        super.onBecomeInvisible();
        if (this.initialized && (this.video != null)) {
            this.cachePlaybackPositionToMemory(this.video, this.activeVideoSource, true);
            this.pausedBySystem = true;
            this.video.pause();
        }
    }

    @Override
    public void onDestroyElement() {
        super.onDestroyElement();
        this.destroyed_FancyMenu = true;
        this.garbageChecker.cancel(true);
        this.asyncTicker.cancel(true);
        this.resetElement();
        synchronized (ELEMENT_INSTANCE_LOCK_FANCYMENU) {
            ELEMENT_INSTANCES_FANCYMENU.remove(this);
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

    public void resetElement() {
        this.resetElementAndReturnStopState();
    }

    protected boolean resetElementAndReturnStopState() {
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
            return !this.playInEditor.tryGetNonNull();
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
    public static SoundSource getSoundSourceByName(@NotNull String name) {
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
        NativeVideoReferenceTracker.acquire(video);
    }

    protected boolean releaseVideoReference(@NotNull IVideo video, @Nullable String source) {
        boolean shouldStop = NativeVideoReferenceTracker.release(video);
        // Video resources are shared by source via ResourceHandler cache.
        // Only stop once this was the last known reference, otherwise another element/background would get interrupted.
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
}
