package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.customization.customgui.CustomGui;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dev-only docs tool that renders docs screenshots in an off-screen test client. Not part of the mod: it lives in the docs_tools folder
 * of the agent root and only gets copied into a workspace while recording (see docs_tools/README.md). Never commit it.<br>
 * {@code FANCYMENU_DOCS_SHOWCASE=scenes}, {@code FANCYMENU_DOCS_SHOWCASE_OUT=<folder>}, {@code FANCYMENU_DOCS_SHOWCASE_CONFIG=<json file>}.
 */
public final class DocsShowcase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CUSTOM_GUI_IDENTIFIER = "docs_showcase";
    private static final int TICKS_PER_SECOND = 20;
    private static final int LOADING_TIMEOUT_TICKS = 180 * TICKS_PER_SECOND;
    private static final int WRITE_TIMEOUT_TICKS = 30 * TICKS_PER_SECOND;

    private final Path outputDirectory;
    private final JsonObject config;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu Docs Showcase Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    @NotNull
    private Phase phase = Phase.WAIT_FOR_GAME;
    private int phaseTicks = 0;
    private int sceneIndex = 0;
    @Nullable
    private String menuBarControllerId;

    private DocsShowcase(@NotNull Path outputDirectory, @NotNull JsonObject config) {
        this.outputDirectory = outputDirectory;
        this.config = config;
    }

    public static void startIfRequested() {
        String mode = System.getenv("FANCYMENU_DOCS_SHOWCASE");
        if ((mode == null) || mode.isBlank()) return;
        if (!OffscreenTestClient.isEnabled()) {
            LOGGER.warn("[FANCYMENU] The docs showcase only runs in off-screen test clients, so it got skipped.");
            return;
        }
        try {
            Path out = Path.of(System.getenv("FANCYMENU_DOCS_SHOWCASE_OUT"));
            Files.createDirectories(out);
            JsonObject config = JsonParser.parseString(Files.readString(Path.of(System.getenv("FANCYMENU_DOCS_SHOWCASE_CONFIG")))).getAsJsonObject();
            if (mode.equals("editor")) {
                EventHandler.INSTANCE.registerListenersOf(new DocsShowcaseEditorGif(out, config));
            } else {
                EventHandler.INSTANCE.registerListenersOf(new DocsShowcase(out, config));
            }
            LOGGER.info("[FANCYMENU] Docs showcase is active.");
        } catch (Exception ex) {
            LOGGER.error("[CHECK] docs showcase setup: FAIL", ex);
            Minecraft.getInstance().stop();
        }
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            this.tick();
        } catch (Exception ex) {
            LOGGER.error("[CHECK] docs showcase: FAIL the showcase crashed", ex);
            this.failures.incrementAndGet();
            if ((this.phase != Phase.WRITE) && (this.phase != Phase.DONE)) {
                this.setPhase(Phase.WRITE);
            } else {
                this.finish();
            }
        }
    }

    private void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        this.phaseTicks++;
        switch (this.phase) {
            case WAIT_FOR_GAME -> {
                boolean loading = (minecraft.gui.overlay() != null) || (ScreenUtils.getScreen() == null);
                if (loading && (this.phaseTicks < LOADING_TIMEOUT_TICKS)) return;
                Window window = minecraft.getWindow();
                float density = window.getPixelDensity();
                window.setWindowed(Math.round(this.guiWidth() * this.guiScale() / density), Math.round(this.guiHeight() * this.guiScale() / density));
                this.menuBarControllerId = CustomizationOverlay.registerOverlayVisibilityController(screen -> false);
                if (!CustomGuiHandler.guiExists(CUSTOM_GUI_IDENTIFIER)) {
                    CustomGui gui = new CustomGui();
                    gui.identifier = CUSTOM_GUI_IDENTIFIER;
                    gui.title = "";
                    CustomGuiHandler.addGui(gui);
                }
                this.setPhase(Phase.SCENE_OPEN);
            }
            case SCENE_OPEN -> {
                if (this.phaseTicks < 10) return;
                JsonArray scenes = this.config.getAsJsonArray("scenes");
                if (this.sceneIndex >= scenes.size()) {
                    this.setPhase(Phase.WRITE);
                    return;
                }
                this.openScene(scenes.get(this.sceneIndex).getAsJsonObject());
                this.setPhase(Phase.SCENE_WAIT);
            }
            case SCENE_WAIT -> {
                JsonObject scene = this.config.getAsJsonArray("scenes").get(this.sceneIndex).getAsJsonObject();
                float wait = scene.has("wait") ? scene.get("wait").getAsFloat() : 3.0F;
                if (this.phaseTicks < wait * TICKS_PER_SECOND) return;
                this.takeScreenshot(minecraft, scene.get("name").getAsString());
                this.sceneIndex++;
                this.setPhase(Phase.SCENE_OPEN);
            }
            case WRITE -> {
                if ((this.pendingWrites.get() > 0) && (this.phaseTicks < WRITE_TIMEOUT_TICKS)) return;
                this.finish();
            }
            case DONE -> {
            }
        }
    }

    private void openScene(@NotNull JsonObject scene) {
        CustomGui gui = CustomGuiHandler.getGui(CUSTOM_GUI_IDENTIFIER);
        if (gui == null) throw new IllegalStateException("The showcase GUI is missing");
        var screen = CustomGuiHandler.constructInstance(gui, null, null);
        Layout layout = Layout.buildForScreen(screen);
        layout.forcedScale = this.guiScale();
        DocsShowcaseElements.addTo(layout, scene.getAsJsonArray("elements"));
        LayoutHandler.setPreviewLayout(layout);
        ScreenUtils.setScreen(screen);
        this.pass("scene " + scene.get("name").getAsString(), "elements=" + scene.getAsJsonArray("elements").size());
    }

    private void takeScreenshot(@NotNull Minecraft minecraft, @NotNull String name) {
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        Path file = this.outputDirectory.resolve(name + ".png");
        String details = target.width + "x" + target.height + " gui=" + minecraft.getWindow().getGuiScaledWidth() + "x" + minecraft.getWindow().getGuiScaledHeight();
        this.pendingWrites.incrementAndGet();
        // The readback finishes in a later frame. Stopping the game while one is pending crashes it, so the end waits for all writes.
        Screenshot.takeScreenshot(target, 1, image -> this.writer.execute(() -> this.writeScreenshot(image, file, details)));
    }

    private void writeScreenshot(@NotNull NativeImage image, @NotNull Path file, @NotNull String details) {
        try (image) {
            image.writeToFile(file);
            this.pass("screenshot", file + " " + details);
        } catch (Exception ex) {
            this.fail("screenshot", "can't write " + file + ": " + ex.getMessage());
        } finally {
            this.pendingWrites.decrementAndGet();
        }
    }

    private int guiWidth() {
        return this.config.has("gui_width") ? this.config.get("gui_width").getAsInt() : 960;
    }

    private int guiHeight() {
        return this.config.has("gui_height") ? this.config.get("gui_height").getAsInt() : 540;
    }

    private int guiScale() {
        return this.config.has("gui_scale") ? this.config.get("gui_scale").getAsInt() : 2;
    }

    private void finish() {
        if (this.phase == Phase.DONE) return;
        this.phase = Phase.DONE;
        LayoutHandler.setPreviewLayout(null);
        if (this.menuBarControllerId != null) CustomizationOverlay.unregisterOverlayVisibilityController(this.menuBarControllerId);
        this.writer.shutdown();
        EventHandler.INSTANCE.unregisterListenersOf(this);
        LOGGER.info("[CHECK] docs showcase finished: {}", (this.failures.get() == 0) ? "PASS" : ("FAIL " + this.failures.get()));
        Minecraft.getInstance().stop();
    }

    private void setPhase(@NotNull Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    private void pass(@NotNull String name, @NotNull String details) {
        LOGGER.info("[CHECK] {}: PASS {}", name, details);
    }

    private void fail(@NotNull String name, @NotNull String details) {
        this.failures.incrementAndGet();
        LOGGER.info("[CHECK] {}: FAIL {}", name, details);
    }

    private enum Phase {
        WAIT_FOR_GAME,
        SCENE_OPEN,
        SCENE_WAIT,
        WRITE,
        DONE
    }

}
