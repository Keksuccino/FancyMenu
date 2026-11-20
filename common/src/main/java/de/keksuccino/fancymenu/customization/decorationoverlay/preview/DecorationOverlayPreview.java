package de.keksuccino.fancymenu.customization.decorationoverlay.preview;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.customization.background.backgrounds.MenuBackgrounds;
import de.keksuccino.fancymenu.customization.background.backgrounds.color.ColorMenuBackground;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayPass;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.AccessorMixinMouseHandler;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelTextures;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dev-only preview of one decoration overlay for off-screen test clients, documented in TESTING_GUIDELINES.md in the project root.<br>
 * It waits for the game to load, sizes the hidden window, opens the title screen with a temporary in-memory layout that shows only the
 * overlay (with the given properties and Layers setting), simulates pointer input, takes screenshots at given times, logs
 * {@code [CHECK]} lines and stops the game. The layout never gets saved, and the title screen only counts as customizable while the
 * preview runs (see {@link LayoutHandler#setPreviewLayout}), so the game directory stays as it was.
 */
public final class DecorationOverlayPreview {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TICKS_PER_SECOND = 20;
    private static final int LOADING_TIMEOUT_TICKS = 180 * TICKS_PER_SECOND;
    private static final int WINDOW_TIMEOUT_TICKS = 2 * TICKS_PER_SECOND;
    private static final int START_TIMEOUT_TICKS = 30 * TICKS_PER_SECOND;
    private static final int WRITE_TIMEOUT_TICKS = 30 * TICKS_PER_SECOND;
    /** Ticks between pressing and releasing a simulated click, and between the release and checking what the click did. */
    private static final int CLICK_STEP_TICKS = 2;

    @Nullable
    private final DecorationOverlayPreviewConfig config;
    @Nullable
    private final String configError;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu Overlay Preview Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    @NotNull
    private Phase phase = Phase.WAIT_FOR_GAME;
    private int phaseTicks = 0;
    private int runTicks = 0;
    private int nextShot = 0;
    private int nextInput = 0;
    @Nullable
    private AbstractDecorationOverlay<?> overlay;
    @Nullable
    private Screen previewScreen;
    @Nullable
    private PendingClick pendingClick;
    @Nullable
    private String menuBarControllerId;
    private long framesRendered = 0L;
    private long lastCountedFrame = Long.MIN_VALUE;
    private long startUpdates = 0L;
    private long startBackgroundPasses = 0L;
    private long startForegroundPasses = 0L;
    /** Ticks the preview screen was open since the run started, for the frame rate */
    private int previewScreenTicks = 0;

    private DecorationOverlayPreview(@Nullable DecorationOverlayPreviewConfig config, @Nullable String configError) {
        this.config = config;
        this.configError = configError;
    }

    /**
     * Starts the preview if its environment variable is set and this is an off-screen test client.
     */
    public static void startIfRequested() {
        if (!DecorationOverlayPreviewConfig.isRequested(System::getenv)) return;
        if (!OffscreenTestClient.isEnabled()) {
            LOGGER.warn("[FANCYMENU] The decoration overlay preview only runs in off-screen test clients (see TESTING_GUIDELINES.md), so it got skipped.");
            return;
        }
        DecorationOverlayPreview preview;
        try {
            preview = new DecorationOverlayPreview(DecorationOverlayPreviewConfig.read(System::getenv), null);
        } catch (IllegalArgumentException ex) {
            // The game still has to stop, or the test run would never end
            preview = new DecorationOverlayPreview(null, ex.getMessage());
        }
        LOGGER.info("[FANCYMENU] Decoration overlay preview is active.");
        EventHandler.INSTANCE.registerListenersOf(preview);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            this.tick();
        } catch (Exception ex) {
            LOGGER.error("[CHECK] overlay preview: FAIL the preview crashed", ex);
            this.failures.incrementAndGet();
            // Stopping while a screenshot readback is pending would crash the game, so it waits for the writes first
            if ((this.phase != Phase.WRITE_SCREENSHOTS) && (this.phase != Phase.DONE)) {
                this.setPhase(Phase.WRITE_SCREENSHOTS);
            } else {
                this.finish();
            }
        }
    }

    @EventListener
    public void onRenderScreenPost(RenderScreenEvent.Post e) {
        if ((this.phase != Phase.RUN) || (e.getScreen() != this.previewScreen)) return;
        long frame = RenderFrameCounter.getFrame();
        if (frame == this.lastCountedFrame) return;
        this.lastCountedFrame = frame;
        this.framesRendered++;
    }

    private void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        this.phaseTicks++;
        switch (this.phase) {
            case WAIT_FOR_GAME -> {
                boolean loading = (minecraft.getOverlay() != null) || (ScreenUtils.getScreen() == null);
                if (loading && (this.phaseTicks < LOADING_TIMEOUT_TICKS)) return;
                if (loading) {
                    this.fail("overlay preview setup", "the game didn't finish loading");
                    this.finish();
                    return;
                }
                if (this.config == null) {
                    this.fail("overlay preview setup", String.valueOf(this.configError));
                    this.finish();
                    return;
                }
                Window window = minecraft.getWindow();
                // Framebuffer pixels per window unit (2 on Retina screens)
                float density = (window.getScreenWidth() > 0) ? Math.max(1.0F, window.getWidth() / (float)window.getScreenWidth()) : 1.0F;
                int framebufferWidth = this.config.guiWidth() * this.config.guiScale();
                int framebufferHeight = this.config.guiHeight() * this.config.guiScale();
                window.setWindowed(Math.round(framebufferWidth / density), Math.round(framebufferHeight / density));
                this.setPhase(Phase.SIZE_WINDOW);
            }
            case SIZE_WINDOW -> {
                DecorationOverlayPreviewConfig config = this.requireConfig();
                Window window = minecraft.getWindow();
                boolean sized = (window.getWidth() == config.guiWidth() * config.guiScale()) && (window.getHeight() == config.guiHeight() * config.guiScale());
                if (!sized && (this.phaseTicks < WINDOW_TIMEOUT_TICKS)) return;
                if (!this.openPreview(config)) {
                    this.finish();
                    return;
                }
                this.setPhase(Phase.WAIT_FOR_FIRST_FRAME);
            }
            case WAIT_FOR_FIRST_FRAME -> {
                AbstractDecorationOverlay<?> shown = this.overlay;
                if ((shown != null) && (shown._getPassCount(DecorationOverlayPass.FOREGROUND) > 0)) {
                    this.startRun(minecraft, shown);
                    return;
                }
                if (this.phaseTicks >= START_TIMEOUT_TICKS) {
                    this.fail("overlay preview setup", "the overlay never rendered (screen: " + describeScreen(ScreenUtils.getScreen()) + ")");
                    this.finish();
                }
            }
            case RUN -> this.tickRun(minecraft);
            case WRITE_SCREENSHOTS -> {
                if ((this.pendingWrites.get() > 0) && (this.phaseTicks < WRITE_TIMEOUT_TICKS)) return;
                if (this.pendingWrites.get() > 0) this.fail("screenshots", this.pendingWrites.get() + " screenshots didn't get written in time");
                this.finish();
            }
            case DONE -> {
            }
        }
    }

    private boolean openPreview(@NotNull DecorationOverlayPreviewConfig config) {
        AbstractDecorationOverlayBuilder<?> builder = DecorationOverlayRegistry.getByIdentifier(config.overlayType());
        if (builder == null) {
            List<String> known = DecorationOverlayRegistry.getAll().stream().map(AbstractDecorationOverlayBuilder::getIdentifier).toList();
            this.fail("overlay preview setup", "no overlay type '" + config.overlayType() + "', known types: " + known);
            return false;
        }
        try {
            Files.createDirectories(config.outputDirectory());
        } catch (Exception ex) {
            this.fail("overlay preview setup", "can't create the output folder " + config.outputDirectory() + ": " + ex.getMessage());
            return false;
        }
        PixelRandom.setSeedOverride(config.seed());
        AbstractDecorationOverlay<?> created = createOverlay(builder, config);
        TitleScreen screen = new TitleScreen();
        Layout layout = Layout.buildForScreen(screen);
        layout.forcedScale = config.guiScale();
        if (config.backgroundColor() != null) {
            ColorMenuBackground background = MenuBackgrounds.COLOR.buildDefaultInstance();
            background.color.set(config.backgroundColor());
            background.showBackground.set(true);
            layout.menuBackgrounds.add(background);
        }
        layout.decorationOverlays.add(Pair.of(builder, created));
        LayoutHandler.setPreviewLayout(layout);
        // FancyMenu's menu bar would cover the top of the screen
        this.menuBarControllerId = CustomizationOverlay.registerOverlayVisibilityController(shownScreen -> false);
        this.overlay = created;
        this.previewScreen = screen;
        ScreenUtils.setScreen(screen);
        this.pass("overlay preview setup", "overlay=" + config.overlayType() + " layers=" + created.getLayers().getName() + " properties=" + config.properties());
        return true;
    }

    /**
     * Builds the overlay the way a layout file loads it, so the properties go through the overlay's real deserialization.
     */
    @NotNull
    private static AbstractDecorationOverlay<?> createOverlay(@NotNull AbstractDecorationOverlayBuilder<?> builder, @NotNull DecorationOverlayPreviewConfig config) {
        PropertyContainer serialized = builder._serialize(builder.buildDefaultInstance());
        // Serialized overlays refuse to overwrite their values, but the preview has to replace the defaults
        serialized.setInvulnerableProperties(false);
        serialized.putProperty("show_decoration_overlay", "true");
        if (config.layers() != null) serialized.putProperty("decoration_overlay_layers", config.layers());
        config.properties().forEach(serialized::putProperty);
        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        set.putContainer(serialized);
        return builder._deserializeAll(set).getFirst();
    }

    private void startRun(@NotNull Minecraft minecraft, @NotNull AbstractDecorationOverlay<?> shown) {
        this.startUpdates = shown._getUpdateCount();
        this.startBackgroundPasses = shown._getPassCount(DecorationOverlayPass.BACKGROUND);
        this.startForegroundPasses = shown._getPassCount(DecorationOverlayPass.FOREGROUND);
        this.framesRendered = 0L;
        this.lastCountedFrame = RenderFrameCounter.getFrame();
        this.previewScreenTicks = 0;
        this.runTicks = 0;
        Window window = minecraft.getWindow();
        DecorationOverlayPreviewConfig config = this.requireConfig();
        String details = describeScreen(ScreenUtils.getScreen()) + " gui=" + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + " guiScale=" + WindowHandler.getGuiScale()
                + " framebuffer=" + window.getWidth() + "x" + window.getHeight();
        if ((window.getGuiScaledWidth() == config.guiWidth()) && (window.getGuiScaledHeight() == config.guiHeight())) {
            this.pass("preview screen", details);
        } else {
            this.fail("preview screen", details + ", but " + config.guiWidth() + "x" + config.guiHeight() + " GUI pixels were asked for");
        }
        this.setPhase(Phase.RUN);
    }

    private void tickRun(@NotNull Minecraft minecraft) {
        DecorationOverlayPreviewConfig config = this.requireConfig();
        this.runTicks++;
        if (ScreenUtils.getScreen() == this.previewScreen) this.previewScreenTicks++;
        float elapsed = this.runTicks / (float)TICKS_PER_SECOND;
        if (this.pendingClick != null) this.tickClick(minecraft);
        List<DecorationOverlayPreviewConfig.Input> inputs = config.inputs();
        while ((this.pendingClick == null) && (this.nextInput < inputs.size()) && (inputs.get(this.nextInput).seconds() <= elapsed + 0.0001F)) {
            this.runInput(minecraft, inputs.get(this.nextInput++));
        }
        List<Float> shots = config.shotSeconds();
        while ((this.nextShot < shots.size()) && (shots.get(this.nextShot) <= elapsed + 0.0001F)) {
            this.takeScreenshot(minecraft, config, shots.get(this.nextShot++));
        }
        if ((this.nextShot >= shots.size()) && (this.nextInput >= inputs.size()) && (this.pendingClick == null)) {
            this.logFrameChecks();
            this.setPhase(Phase.WRITE_SCREENSHOTS);
        }
    }

    private void runInput(@NotNull Minecraft minecraft, @NotNull DecorationOverlayPreviewConfig.Input input) {
        switch (input.action()) {
            case MOVE -> {
                this.movePointer(minecraft, input.x(), input.y());
                LOGGER.info("[FANCYMENU] Overlay preview moved the pointer to {},{} at {}s.", input.x(), input.y(), input.seconds());
            }
            case CLICK -> this.pressAt(minecraft, input.x(), input.y(), "click at " + formatPosition(input.x(), input.y()), false);
            case CLICK_WIDGET -> {
                AbstractWidget widget = findWidget(ScreenUtils.getScreen(), input.argument());
                if (widget == null) {
                    this.fail("click_widget " + input.argument(), "no widget with that identifier or label on " + describeScreen(ScreenUtils.getScreen()) + ", widgets: " + describeWidgets(ScreenUtils.getScreen()));
                    return;
                }
                float x = widget.getX() + widget.getWidth() / 2.0F;
                float y = widget.getY() + widget.getHeight() / 2.0F;
                this.pressAt(minecraft, x, y, "click_widget " + input.argument(), true);
            }
            case TITLE -> {
                TitleScreen screen = new TitleScreen();
                this.previewScreen = screen;
                ScreenUtils.setScreen(screen);
                LOGGER.info("[FANCYMENU] Overlay preview opened the title screen again at {}s.", input.seconds());
            }
            case SET -> this.setProperty(input.argument());
        }
    }

    /**
     * Sets an overlay property the way a layout file loads it, while the overlay runs.
     */
    private void setProperty(@NotNull String assignment) {
        String key = assignment.substring(0, assignment.indexOf('=')).trim();
        String value = assignment.substring(assignment.indexOf('=') + 1).trim();
        AbstractDecorationOverlay<?> shown = this.overlay;
        Property<?> property = (shown != null) ? shown.getProperty(key) : null;
        if (property == null) {
            this.fail("set " + assignment, "the overlay has no property '" + key + "'");
            return;
        }
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty(key, value);
        property.deserialize(serialized);
        this.pass("set " + assignment, key + " is " + property.get());
    }

    private void movePointer(@NotNull Minecraft minecraft, float guiX, float guiY) {
        Window window = minecraft.getWindow();
        // The middle of the GUI pixel, in window coordinates. The first move after a screen change only sets the position, so move twice.
        double x = (guiX + 0.5) * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = (guiY + 0.5) * window.getScreenHeight() / window.getGuiScaledHeight();
        AccessorMixinMouseHandler mouse = (AccessorMixinMouseHandler)minecraft.mouseHandler;
        mouse.invoke_onMove_FancyMenu(window.handle(), x, y);
        mouse.invoke_onMove_FancyMenu(window.handle(), x, y);
    }

    /**
     * @param expectScreenChange true if the click targets a widget that opens another screen, which shows that the click reached it
     */
    private void pressAt(@NotNull Minecraft minecraft, float guiX, float guiY, @NotNull String checkName, boolean expectScreenChange) {
        this.movePointer(minecraft, guiX, guiY);
        Screen screen = ScreenUtils.getScreen();
        AbstractWidget target = findWidgetAt(screen, guiX + 0.5, guiY + 0.5);
        ((AccessorMixinMouseHandler)minecraft.mouseHandler).invoke_onButton_FancyMenu(minecraft.getWindow().handle(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS);
        this.pendingClick = new PendingClick(checkName, screen, (target != null) ? target.getMessage().getString() : null, expectScreenChange);
    }

    private void tickClick(@NotNull Minecraft minecraft) {
        PendingClick click = this.pendingClick;
        if (click == null) return;
        click.ticks++;
        if (click.ticks == CLICK_STEP_TICKS) {
            ((AccessorMixinMouseHandler)minecraft.mouseHandler).invoke_onButton_FancyMenu(minecraft.getWindow().handle(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE);
            return;
        }
        if (click.ticks < CLICK_STEP_TICKS * 2) return;
        this.pendingClick = null;
        Screen after = ScreenUtils.getScreen();
        String widget = (click.widgetLabel != null) ? ("'" + click.widgetLabel + "'") : "no widget";
        String result = (after != click.screenBefore) ? ("opened " + describeScreen(after)) : ("the screen stayed " + describeScreen(after));
        if (!click.expectScreenChange || (after != click.screenBefore)) {
            this.pass(click.name, widget + " under the pointer, " + result);
        } else {
            this.fail(click.name, widget + " under the pointer, but " + result + ", so the click didn't reach it (or it doesn't open a screen)");
        }
    }

    private void takeScreenshot(@NotNull Minecraft minecraft, @NotNull DecorationOverlayPreviewConfig config, float seconds) {
        RenderTarget target = minecraft.getMainRenderTarget();
        // Averaging blocks of guiScale x guiScale pixels gives one image pixel per GUI pixel, which only works for whole-number scales
        double guiScale = WindowHandler.getGuiScale();
        int factor = (config.fullResolutionShots() || (guiScale != Math.rint(guiScale))) ? 1 : (int)guiScale;
        if ((factor < 1) || (target.width % factor != 0) || (target.height % factor != 0)) factor = 1;
        String checkName = String.format(Locale.ROOT, "screenshot %.2fs", seconds);
        Path file = config.outputDirectory().resolve(String.format(Locale.ROOT, "%s_%06.2fs.png", config.namePrefix(), seconds));
        AbstractDecorationOverlay<?> shown = this.overlay;
        String quads = (shown instanceof PixelDecorationOverlay<?> pixelOverlay) ? (" quads/frame=" + pixelOverlay.getLastFrameQuadCount()) : "";
        String details = " " + target.width / factor + "x" + target.height / factor + " screen=" + describeScreen(ScreenUtils.getScreen()) + quads + " textures=" + PixelTextures.getLiveTextureCount();
        this.pendingWrites.incrementAndGet();
        // The readback finishes in a later frame. Stopping the game while one is pending crashes it, so the end waits for all writes.
        Screenshot.takeScreenshot(target, factor, image -> this.writer.execute(() -> this.writeScreenshot(image, file, checkName, details)));
    }

    private void writeScreenshot(@NotNull NativeImage image, @NotNull Path file, @NotNull String checkName, @NotNull String details) {
        try (image) {
            image.writeToFile(file);
            this.pass(checkName, file + details);
        } catch (Exception ex) {
            this.fail(checkName, "can't write " + file + ": " + ex.getMessage());
        } finally {
            this.pendingWrites.decrementAndGet();
        }
    }

    private void logFrameChecks() {
        AbstractDecorationOverlay<?> shown = this.overlay;
        if (shown == null) return;
        long updates = shown._getUpdateCount() - this.startUpdates;
        long backgroundPasses = shown._getPassCount(DecorationOverlayPass.BACKGROUND) - this.startBackgroundPasses;
        long foregroundPasses = shown._getPassCount(DecorationOverlayPass.FOREGROUND) - this.startForegroundPasses;
        float seconds = this.previewScreenTicks / (float)TICKS_PER_SECOND;
        String counts = "updates=" + updates + " frames=" + this.framesRendered + " backgroundPasses=" + backgroundPasses + " foregroundPasses=" + foregroundPasses
                + String.format(Locale.ROOT, " fps=%.1f", (seconds > 0.0F) ? this.framesRendered / seconds : 0.0F);
        // The counts can differ by one at the edges of the measured time
        if (Math.abs(updates - this.framesRendered) <= 1) {
            this.pass("update once per frame", counts);
        } else {
            this.fail("update once per frame", counts);
        }
        if (Math.abs(backgroundPasses - foregroundPasses) <= 1) {
            this.pass("background and foreground pass per frame", counts);
        } else {
            this.fail("background and foreground pass per frame", counts);
        }
    }

    private void finish() {
        if (this.phase == Phase.DONE) return;
        this.phase = Phase.DONE;
        LayoutHandler.setPreviewLayout(null);
        PixelRandom.setSeedOverride(null);
        if (this.menuBarControllerId != null) CustomizationOverlay.unregisterOverlayVisibilityController(this.menuBarControllerId);
        this.writer.shutdown();
        EventHandler.INSTANCE.unregisterListenersOf(this);
        int failed = this.failures.get();
        if (failed == 0) {
            LOGGER.info("[CHECK] overlay preview finished: PASS");
        } else {
            LOGGER.info("[CHECK] overlay preview finished: FAIL {} failed checks", failed);
        }
        Minecraft.getInstance().stop();
    }

    private void setPhase(@NotNull Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    @NotNull
    private DecorationOverlayPreviewConfig requireConfig() {
        if (this.config == null) throw new IllegalStateException("The preview has no settings");
        return this.config;
    }

    private void pass(@NotNull String name, @NotNull String details) {
        LOGGER.info("[CHECK] {}: PASS {}", name, details);
    }

    private void fail(@NotNull String name, @NotNull String details) {
        this.failures.incrementAndGet();
        LOGGER.info("[CHECK] {}: FAIL {}", name, details);
    }

    @Nullable
    private static AbstractWidget findWidget(@Nullable Screen screen, @NotNull String identifierOrLabel) {
        if (screen == null) return null;
        // FancyMenu knows vanilla widgets by the identifiers of their widget metas
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
        if (layer != null) {
            for (WidgetMeta meta : layer.cachedScreenWidgetMetas) {
                if (identifierOrLabel.equals(meta.getIdentifier()) && meta.getWidget().visible) return meta.getWidget();
            }
        }
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.visible) continue;
            String identifier = (widget instanceof UniqueWidget unique) ? unique.getWidgetIdentifierFancyMenu() : null;
            if (identifierOrLabel.equals(identifier) || identifierOrLabel.equalsIgnoreCase(widget.getMessage().getString())) return widget;
        }
        return null;
    }

    @NotNull
    private static List<String> describeWidgets(@Nullable Screen screen) {
        List<String> widgets = new ArrayList<>();
        if (screen == null) return widgets;
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
        if (layer != null) {
            for (WidgetMeta meta : layer.cachedScreenWidgetMetas) widgets.add(meta.getIdentifier() + " '" + meta.getWidget().getMessage().getString() + "'");
        }
        return widgets;
    }

    @Nullable
    private static AbstractWidget findWidgetAt(@Nullable Screen screen, double x, double y) {
        if (screen == null) return null;
        for (GuiEventListener child : screen.children()) {
            if ((child instanceof AbstractWidget widget) && widget.visible && widget.active && widget.isMouseOver(x, y)) return widget;
        }
        return null;
    }

    @NotNull
    private static String describeScreen(@Nullable Screen screen) {
        return (screen != null) ? screen.getClass().getSimpleName() : "no screen";
    }

    @NotNull
    private static String formatPosition(float x, float y) {
        return String.format(Locale.ROOT, "%.0f,%.0f", x, y);
    }

    private enum Phase {
        WAIT_FOR_GAME,
        SIZE_WINDOW,
        WAIT_FOR_FIRST_FRAME,
        RUN,
        WRITE_SCREENSHOTS,
        DONE
    }

    private static final class PendingClick {

        @NotNull
        private final String name;
        @Nullable
        private final Screen screenBefore;
        @Nullable
        private final String widgetLabel;
        private final boolean expectScreenChange;
        private int ticks = 0;

        private PendingClick(@NotNull String name, @Nullable Screen screenBefore, @Nullable String widgetLabel, boolean expectScreenChange) {
            this.name = name;
            this.screenBefore = screenBefore;
            this.widgetLabel = widgetLabel;
            this.expectScreenChange = expectScreenChange;
        }

    }

}
