package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import net.minecraft.client.CloudStatus;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dev-only docs tool: makes a fresh flat world, builds a scene in it with commands and takes Minecraft's own panorama screenshot of it.
 * Not part of the mod: it lives in docs_tools/world_harness of the agent root and only gets copied into a workspace while recording
 * (see docs_tools/README.md). Never commit it.<br>
 * {@code FANCYMENU_DOCS_WORLD=<config json>}, {@code FANCYMENU_DOCS_WORLD_OUT=<folder>}.<br>
 * Config: {@code commands_file} (one command per line, without the slash, {@code #} for comments), {@code eye} ({@code [x, y, z]} of the
 * camera), {@code yaw} (where the front image looks), {@code shots} (each with {@code name} and the {@code time} of day: one panorama
 * each, in {@code <out>/<name>/screenshots/}), {@code render_distance}, {@code settle_ticks} (wait after building, for chunks, light and
 * smoke) and {@code shot_settle_ticks} (wait after changing the time).
 */
public final class DocsWorldPanorama {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final double SPECTATOR_EYE_HEIGHT = 1.62D;
    private static final int COMMANDS_PER_BATCH = 250;

    private final Path out;
    private final JsonObject config;
    private final List<String> commands = new ArrayList<>();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger commandFailures = new AtomicInteger();
    private Phase phase = Phase.WAIT_FOR_GAME;
    private int phaseTicks = 0;
    private int nextCommand = 0;
    private int shotIndex = 0;
    private CompletableFuture<Void> lastBatch = CompletableFuture.completedFuture(null);

    private DocsWorldPanorama(@NotNull Path out, @NotNull JsonObject config) throws Exception {
        this.out = out;
        this.config = config;
        for (String line : Files.readAllLines(Path.of(config.get("commands_file").getAsString()))) {
            String command = line.strip();
            if (command.isEmpty() || command.startsWith("#")) continue;
            this.commands.add(command);
        }
    }

    public static void startIfRequested() {
        String configPath = System.getenv("FANCYMENU_DOCS_WORLD");
        if ((configPath == null) || configPath.isBlank()) return;
        if (!OffscreenTestClient.isEnabled()) {
            LOGGER.warn("[FANCYMENU] The docs world panorama only runs in off-screen test clients, so it got skipped.");
            return;
        }
        try {
            Path out = Path.of(System.getenv("FANCYMENU_DOCS_WORLD_OUT"));
            Files.createDirectories(out);
            JsonObject config = JsonParser.parseString(Files.readString(Path.of(configPath))).getAsJsonObject();
            EventHandler.INSTANCE.registerListenersOf(new DocsWorldPanorama(out, config));
            LOGGER.info("[FANCYMENU] Docs world panorama is active.");
        } catch (Exception ex) {
            LOGGER.error("[CHECK] docs world panorama setup: FAIL", ex);
            Minecraft.getInstance().stop();
        }
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            this.tick();
        } catch (Exception ex) {
            LOGGER.error("[CHECK] docs world panorama: FAIL it crashed in phase " + this.phase, ex);
            this.failures.incrementAndGet();
            this.finish();
        }
    }

    private void tick() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        this.phaseTicks++;
        switch (this.phase) {
            case WAIT_FOR_GAME -> {
                if ((minecraft.gui.overlay() != null) || (ScreenUtils.getScreen() == null)) return;
                if (this.phaseTicks < 40) return;
                this.setUpOptions(minecraft);
                String levelId = "docs_panorama_" + System.currentTimeMillis();
                LevelSettings settings = new LevelSettings(levelId, GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                WorldOptions options = new WorldOptions(this.config.has("seed") ? this.config.get("seed").getAsLong() : 1L, false, false);
                minecraft.createWorldOpenFlows().createFreshLevel(levelId, settings, options,
                        registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        ScreenUtils.getScreen());
                LOGGER.info("[CHECK] world created: PASS " + levelId);
                this.setPhase(Phase.WAIT_FOR_WORLD);
            }
            case WAIT_FOR_WORLD -> {
                if ((minecraft.level == null) || (minecraft.player == null) || (ScreenUtils.getScreen() != null) || (minecraft.getSingleplayerServer() == null)) {
                    if (this.phaseTicks > 2400) throw new IllegalStateException("The world never loaded");
                    return;
                }
                if (this.phaseTicks < 60) return;
                if (!minecraft.gui.hud.isHidden()) minecraft.gui.hud.toggle();
                this.setPhase(Phase.BUILD);
            }
            case BUILD -> {
                if (!this.lastBatch.isDone()) return;
                if (this.nextCommand >= this.commands.size()) {
                    LOGGER.info("[CHECK] built the scene: " + ((this.commandFailures.get() == 0) ? "PASS " : "FAIL ") + this.commands.size() + " commands, "
                            + this.commandFailures.get() + " failed");
                    this.placeCamera(minecraft);
                    this.setPhase(Phase.SETTLE);
                    return;
                }
                IntegratedServer server = minecraft.getSingleplayerServer();
                int end = Math.min(this.commands.size(), this.nextCommand + COMMANDS_PER_BATCH);
                List<String> batch = this.commands.subList(this.nextCommand, end);
                this.nextCommand = end;
                this.lastBatch = server.submit(() -> {
                    for (String command : batch) this.runCommand(server, command);
                });
            }
            case SETTLE -> {
                if (this.phaseTicks == 20) this.placeCamera(minecraft);
                if (this.phaseTicks == 40) {
                    // A square window, so every side of the cube is a square with a 90 degree field of view
                    int size = this.intConfig("capture_size", 2048);
                    // Framebuffer pixels per window unit (2 on Retina screens)
                    float density = Math.max(1.0F, (float) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getScreenWidth()));
                    minecraft.getWindow().setWindowed(Math.round(size / density), Math.round(size / density));
                    minecraft.gameRenderer.mainCamera().enablePanoramicMode();
                }
                if (this.phaseTicks < this.intConfig("settle_ticks", 300)) return;
                RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
                LOGGER.info("[CHECK] capture size: " + target.width + "x" + target.height);
                this.setPhase(Phase.SHOT);
            }
            case SHOT -> {
                JsonArray shots = this.config.getAsJsonArray("shots");
                if (this.shotIndex >= shots.size()) {
                    this.setPhase(Phase.WRITE);
                    return;
                }
                JsonObject shot = shots.get(this.shotIndex).getAsJsonObject();
                if (this.phaseTicks == 1) {
                    IntegratedServer server = minecraft.getSingleplayerServer();
                    String time = shot.get("time").getAsString();
                    server.execute(() -> this.runCommand(server, "time set " + time));
                    if (shot.has("commands")) {
                        for (JsonElement command : shot.getAsJsonArray("commands")) {
                            String c = command.getAsString();
                            server.execute(() -> this.runCommand(server, c));
                        }
                    }
                    this.placeCamera(minecraft, shot);
                    return;
                }
                int settle = this.intConfig("shot_settle_ticks", 60);
                int faceTicks = this.intConfig("face_ticks", 12);
                if (this.phaseTicks < settle) return;
                // One normal frame per side of the cube: Minecraft's own panorama grab renders outside of the frame and fails
                int step = this.phaseTicks - settle;
                int face = step / faceTicks;
                if (face >= 6) {
                    LOGGER.info("[CHECK] panorama " + shot.get("name").getAsString() + ": PASS");
                    this.shotIndex++;
                    this.phaseTicks = 0;
                    return;
                }
                if ((step % faceTicks) == 0) {
                    // Only turn on the client: a teleport from the server would reset the rotation a few ticks later
                    this.turnCamera(minecraft, shot, face);
                } else if ((step % faceTicks) == (faceTicks - 1)) {
                    File folder = this.out.resolve(shot.get("name").getAsString()).toFile();
                    folder.mkdirs();
                    int factor = this.intConfig("downscale", 2);
                    Screenshot.grab(folder, "panorama_" + face + ".png", minecraft.gameRenderer.mainRenderTarget(), factor, message -> {
                        if (!message.getString().contains("panorama_")) {
                            this.failures.incrementAndGet();
                            LOGGER.error("[CHECK] screenshot failed: " + message.getString());
                        }
                    });
                }
            }
            case WRITE -> {
                // Screenshots get written on a background thread
                if (this.phaseTicks < 100) return;
                this.finish();
            }
            case DONE -> {
            }
        }
    }

    private void setUpOptions(@NotNull Minecraft minecraft) {
        minecraft.options.renderDistance().set(this.intConfig("render_distance", 16));
        minecraft.options.simulationDistance().set(Math.min(12, this.intConfig("render_distance", 16)));
        minecraft.options.cloudStatus().set(CloudStatus.FANCY);
        minecraft.options.particles().set(ParticleStatus.ALL);
        minecraft.options.ambientOcclusion().set(true);
        minecraft.options.graphicsPreset().set(net.minecraft.client.GraphicsPreset.FANCY);
        minecraft.options.bobView().set(false);
        minecraft.options.biomeBlendRadius().set(this.intConfig("biome_blend", 5));
        if (this.config.has("gamma")) minecraft.options.gamma().set(this.config.get("gamma").getAsDouble());
    }

    private void placeCamera(@NotNull Minecraft minecraft) {
        this.placeCamera(minecraft, null);
    }

    /**
     * Puts the camera at the eye position of the shot, or of the config when the shot has none.
     */
    private void placeCamera(@NotNull Minecraft minecraft, @Nullable JsonObject shot) {
        JsonArray eye = ((shot != null) && shot.has("eye")) ? shot.getAsJsonArray("eye") : this.config.getAsJsonArray("eye");
        double x = eye.get(0).getAsDouble();
        double y = eye.get(1).getAsDouble() - SPECTATOR_EYE_HEIGHT;
        double z = eye.get(2).getAsDouble();
        float yaw = ((shot != null) && shot.has("yaw")) ? shot.get("yaw").getAsFloat() : (this.config.has("yaw") ? this.config.get("yaw").getAsFloat() : 180.0F);
        IntegratedServer server = minecraft.getSingleplayerServer();
        server.execute(() -> this.runCommand(server, String.format(java.util.Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f 0", x, y, z, yaw)));
        if (minecraft.player != null) {
            minecraft.player.snapTo(x, y, z, yaw, 0.0F);
        }
    }

    /**
     * Turns the camera like Minecraft's own panorama: 0 to 3 turn right around in steps of 90 degrees, 4 looks up, 5 looks down.
     */
    private void turnCamera(@NotNull Minecraft minecraft, @NotNull JsonObject shot, int face) {
        float yaw = shot.has("yaw") ? shot.get("yaw").getAsFloat() : (this.config.has("yaw") ? this.config.get("yaw").getAsFloat() : 180.0F);
        float[][] rotations = {{yaw, 0}, {yaw + 90, 0}, {yaw + 180, 0}, {yaw - 90, 0}, {yaw, -90}, {yaw, 90}};
        if (minecraft.player == null) return;
        minecraft.player.setYRot(rotations[face][0]);
        minecraft.player.setXRot(rotations[face][1]);
        minecraft.player.yRotO = rotations[face][0];
        minecraft.player.xRotO = rotations[face][1];
    }

    private void runCommand(@NotNull IntegratedServer server, @NotNull String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withCallback((success, result) -> {
            if (!success) {
                this.commandFailures.incrementAndGet();
                LOGGER.error("[CHECK] command failed: " + command);
            }
        });
        server.getCommands().performPrefixedCommand(source, command);
    }

    private int intConfig(@NotNull String key, int fallback) {
        return this.config.has(key) ? this.config.get(key).getAsInt() : fallback;
    }

    private void setPhase(@NotNull Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    private void finish() {
        if (this.phase == Phase.DONE) return;
        this.phase = Phase.DONE;
        boolean pass = this.failures.get() == 0;
        LOGGER.info("[CHECK] docs world panorama finished: " + (pass ? "PASS" : "FAIL"));
        Minecraft.getInstance().stop();
    }

    private enum Phase {
        WAIT_FOR_GAME, WAIT_FOR_WORLD, BUILD, SETTLE, SHOT, WRITE, DONE
    }

}
