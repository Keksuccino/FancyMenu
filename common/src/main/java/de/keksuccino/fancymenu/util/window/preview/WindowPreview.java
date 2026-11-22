package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.variables.ManageVariablesWindowBody;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.AccessorMixinMouseHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinKeyboardHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dev-only preview of one FancyMenu window for off-screen test clients, documented in TESTING_GUIDELINES.md in the project root.<br>
 * It waits for the game to load, sizes the hidden window, replaces the variables if asked to, opens the title screen and then the window
 * (the Manage Variables window, the value editor of an action or requirement, or whatever a dev-only opener method opens), simulates
 * mouse and keyboard input, takes screenshots at given times, logs {@code [CHECK]} lines and stops the game.
 *
 * <p>Input goes through the game's own input handlers, so it reaches windows exactly like real input. Only checks that read the real
 * key state from GLFW ({@code Screen#hasShiftDown()} and similar) don't see simulated modifiers, they only reach code through the
 * modifiers of the key events.</p>
 */
public final class WindowPreview {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TICKS_PER_SECOND = 20;
    private static final int LOADING_TIMEOUT_TICKS = 180 * TICKS_PER_SECOND;
    private static final int WINDOW_TIMEOUT_TICKS = 2 * TICKS_PER_SECOND;
    private static final int OPEN_TIMEOUT_TICKS = 10 * TICKS_PER_SECOND;
    private static final int WRITE_TIMEOUT_TICKS = 30 * TICKS_PER_SECOND;
    private static final int DRAG_MOVE_STEPS = 10;

    @Nullable
    private final WindowPreviewConfig config;
    @Nullable
    private final String configError;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu Window Preview Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    /** The steps of the input that runs right now. The next input only starts once all of them ran. */
    private final Deque<Step> pendingSteps = new ArrayDeque<>();
    @NotNull
    private Phase phase = Phase.WAIT_FOR_GAME;
    private int phaseTicks = 0;
    private int runTicks = 0;
    private int stepTicks = 0;
    private int nextShot = 0;
    private int nextInput = 0;
    @Nullable
    private String menuBarControllerId;
    /** Whether the game counted its window as active before the run started, NULL before that */
    @Nullable
    private Boolean windowActiveBeforeRun = null;

    private WindowPreview(@Nullable WindowPreviewConfig config, @Nullable String configError) {
        this.config = config;
        this.configError = configError;
    }

    /**
     * Starts the preview if its environment variable is set and this is an off-screen test client.
     */
    public static void startIfRequested() {
        if (!WindowPreviewConfig.isRequested(System::getenv)) return;
        if (!OffscreenTestClient.isEnabled()) {
            LOGGER.warn("[FANCYMENU] The window preview only runs in off-screen test clients (see TESTING_GUIDELINES.md), so it got skipped.");
            return;
        }
        WindowPreview preview;
        try {
            preview = new WindowPreview(WindowPreviewConfig.read(System::getenv), null);
        } catch (IllegalArgumentException ex) {
            // The game still has to stop, or the test run would never end
            preview = new WindowPreview(null, ex.getMessage());
        }
        LOGGER.info("[FANCYMENU] Window preview is active.");
        EventHandler.INSTANCE.registerListenersOf(preview);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            this.tick();
        } catch (Exception ex) {
            LOGGER.error("[CHECK] window preview: FAIL the preview crashed", ex);
            this.failures.incrementAndGet();
            // Stopping while screenshots still get written would lose them, so it waits for the writes first
            if ((this.phase != Phase.WRITE_SCREENSHOTS) && (this.phase != Phase.DONE)) {
                this.setPhase(Phase.WRITE_SCREENSHOTS);
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
                boolean loading = (minecraft.getOverlay() != null) || (ScreenUtils.getScreen() == null);
                if (loading && (this.phaseTicks < LOADING_TIMEOUT_TICKS)) return;
                if (loading) {
                    this.fail("window preview setup", "the game didn't finish loading");
                    this.finish();
                    return;
                }
                if (this.config == null) {
                    this.fail("window preview setup", String.valueOf(this.configError));
                    this.finish();
                    return;
                }
                Window window = minecraft.getWindow();
                // Framebuffer pixels per window coordinate, 2 on Retina displays
                float density = (window.getScreenWidth() > 0) ? (window.getWidth() / (float)window.getScreenWidth()) : 1.0F;
                window.setWindowed(Math.round(this.config.guiWidth() * this.config.guiScale() / density), Math.round(this.config.guiHeight() * this.config.guiScale() / density));
                this.setPhase(Phase.SIZE_WINDOW);
            }
            case SIZE_WINDOW -> {
                WindowPreviewConfig config = this.requireConfig();
                Window window = minecraft.getWindow();
                boolean sized = (window.getWidth() == config.guiWidth() * config.guiScale()) && (window.getHeight() == config.guiHeight() * config.guiScale());
                if (!sized && (this.phaseTicks < WINDOW_TIMEOUT_TICKS)) return;
                // The GUI scale option of the game decides the scale. offscreen_client.sh restores options.txt when the client stops,
                // so setting it leaves the game directory as it was.
                minecraft.options.guiScale().set(config.guiScale());
                minecraft.resizeDisplay();
                if (!this.openPreview(config)) {
                    this.finish();
                    return;
                }
                this.setPhase(Phase.WAIT_FOR_WINDOW);
            }
            case WAIT_FOR_WINDOW -> {
                if (PiPWindowHandler.INSTANCE.isAnyWindowOpen()) {
                    Window window = minecraft.getWindow();
                    this.pass("window opened", describeWindows() + " gui=" + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + " guiScale=" + window.getGuiScale()
                            + " framebuffer=" + window.getWidth() + "x" + window.getHeight());
                    // The hidden window never gets focus, so the game counts it as inactive and ignores pointer moves (see TESTING_GUIDELINES.md).
                    // This only sets the game's own flag and never touches the real window.
                    this.windowActiveBeforeRun = minecraft.isWindowActive();
                    minecraft.setWindowActive(true);
                    this.runTicks = 0;
                    this.setPhase(Phase.RUN);
                } else if (this.phaseTicks >= OPEN_TIMEOUT_TICKS) {
                    this.fail("window opened", "no window opened for " + this.requireConfig().target());
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

    private boolean openPreview(@NotNull WindowPreviewConfig config) {
        try {
            Files.createDirectories(config.outputDirectory());
        } catch (Exception ex) {
            this.fail("window preview setup", "can't create the output folder " + config.outputDirectory() + ": " + ex.getMessage());
            return false;
        }
        if ((config.variables() != null) && !this.replaceVariables(config.variables())) return false;
        // FancyMenu's menu bar would cover the top of the screen
        this.menuBarControllerId = CustomizationOverlay.registerOverlayVisibilityController(shownScreen -> false);
        ScreenUtils.setScreen(new TitleScreen());
        try {
            if (!this.openTarget(config)) return false;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] The window preview failed to open {}", config.target(), ex);
            this.fail("window preview setup", "opening " + config.target() + " failed: " + ex);
            return false;
        }
        this.pass("window preview setup", "target=" + config.target() + ((config.value() != null) ? (" value=" + config.value()) : ""));
        return true;
    }

    /**
     * Replaces all variables with the ones of the JSON object: arrays become list variables, everything else text variables.
     */
    private boolean replaceVariables(@NotNull String json) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            List<UserVariableSnapshot> snapshots = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonArray()) {
                    List<String> entries = VariableLists.parseText(value.toString());
                    snapshots.add(UserVariableSnapshot.ofList(entry.getKey(), (entries != null) ? entries : List.of(), false));
                } else {
                    snapshots.add(new UserVariableSnapshot(entry.getKey(), value.isJsonNull() ? "" : (value.isJsonPrimitive() ? value.getAsString() : value.toString()), false));
                }
            }
            VariableHandler.replaceVariables(snapshots);
            this.pass("variables", snapshots.size() + " variables: " + VariableHandler.getVariableSnapshots());
            return true;
        } catch (Exception ex) {
            this.fail("variables", WindowPreviewConfig.ENV_VARIABLES + " has to be a JSON object like {\"fruits\":[\"apple\"],\"name\":\"Steve\"}: " + ex.getMessage());
            return false;
        }
    }

    private boolean openTarget(@NotNull WindowPreviewConfig config) throws Exception {
        String target = config.target();
        if (target.equals("manage_variables")) {
            ManageVariablesWindowBody.openInWindow(new ManageVariablesWindowBody(variables -> this.pass("window result", variables.size() + " variables")));
            return true;
        }
        if (target.startsWith("action:")) {
            String identifier = target.substring("action:".length());
            Action action = ActionRegistry.getAction(identifier);
            if (action == null) {
                this.fail("window preview setup", "no action '" + identifier + "'");
                return false;
            }
            ActionInstance instance = new ActionInstance(action, config.value());
            action.editValueInternal(instance, (edited, oldValue, newValue) -> this.pass("editor result", "value=" + newValue), edited -> this.pass("editor result", "canceled"));
            return true;
        }
        if (target.startsWith("requirement:")) {
            String identifier = target.substring("requirement:".length());
            Requirement requirement = RequirementRegistry.getRequirement(identifier);
            if (requirement == null) {
                this.fail("window preview setup", "no requirement '" + identifier + "'");
                return false;
            }
            RequirementInstance instance = new RequirementInstance(requirement, config.value(), RequirementInstance.RequirementMode.IF, new RequirementContainer());
            requirement.editValueInternal(instance, (edited, oldValue, newValue) -> this.pass("editor result", "value=" + newValue), edited -> this.pass("editor result", "canceled"));
            return true;
        }
        if (target.startsWith("opener:")) {
            // Dev-only opener methods let tests open any window: public static methods without parameters
            String reference = target.substring("opener:".length());
            int separator = reference.indexOf('#');
            if (separator <= 0) {
                this.fail("window preview setup", "openers look like opener:<fully.qualified.Class>#<method>: " + target);
                return false;
            }
            Method method = Class.forName(reference.substring(0, separator)).getMethod(reference.substring(separator + 1));
            method.invoke(null);
            return true;
        }
        this.fail("window preview setup", "unknown target '" + target + "', use manage_variables, action:<identifier>, requirement:<identifier> or opener:<class>#<method>");
        return false;
    }

    private void tickRun(@NotNull Minecraft minecraft) {
        WindowPreviewConfig config = this.requireConfig();
        this.runTicks++;
        float elapsed = this.runTicks / (float)TICKS_PER_SECOND;
        this.runPendingSteps();
        List<WindowPreviewConfig.Input> inputs = config.inputs();
        while (this.pendingSteps.isEmpty() && (this.nextInput < inputs.size()) && (inputs.get(this.nextInput).seconds() <= elapsed + 0.0001F)) {
            this.queueInput(minecraft, inputs.get(this.nextInput++));
            this.runPendingSteps();
        }
        List<Float> shots = config.shotSeconds();
        while ((this.nextShot < shots.size()) && (shots.get(this.nextShot) <= elapsed + 0.0001F)) {
            this.takeScreenshot(minecraft, config, shots.get(this.nextShot++));
        }
        if ((this.nextShot >= shots.size()) && (this.nextInput >= inputs.size()) && this.pendingSteps.isEmpty()) {
            this.setPhase(Phase.WRITE_SCREENSHOTS);
        }
    }

    private void runPendingSteps() {
        while (!this.pendingSteps.isEmpty()) {
            Step step = this.pendingSteps.peekFirst();
            if (this.stepTicks < step.ticksBefore()) {
                this.stepTicks++;
                return;
            }
            this.pendingSteps.pollFirst();
            this.stepTicks = 0;
            step.action().run();
        }
    }

    private void queueInput(@NotNull Minecraft minecraft, @NotNull WindowPreviewConfig.Input input) {
        float[] numbers = input.numbers();
        String name = String.format(Locale.ROOT, "%s %s at %.2fs", input.action().getName(), input.argument().isEmpty() ? formatNumbers(numbers) : input.argument(), input.seconds());
        switch (input.action()) {
            case MOVE -> this.step(0, () -> this.movePointer(minecraft, numbers[0], numbers[1]));
            case CLICK -> this.queueClick(minecraft, numbers[0], numbers[1], InputConstants.MOUSE_BUTTON_LEFT, 1);
            case RIGHT_CLICK -> this.queueClick(minecraft, numbers[0], numbers[1], InputConstants.MOUSE_BUTTON_RIGHT, 1);
            case DOUBLE_CLICK -> this.queueClick(minecraft, numbers[0], numbers[1], InputConstants.MOUSE_BUTTON_LEFT, 2);
            case DRAG -> {
                this.step(0, () -> this.movePointer(minecraft, numbers[0], numbers[1]));
                this.step(1, () -> this.mouseButton(minecraft, InputConstants.MOUSE_BUTTON_LEFT, InputConstants.PRESS));
                for (int i = 1; i <= DRAG_MOVE_STEPS; i++) {
                    float progress = i / (float)DRAG_MOVE_STEPS;
                    this.step(1, () -> this.movePointer(minecraft, numbers[0] + (numbers[2] - numbers[0]) * progress, numbers[1] + (numbers[3] - numbers[1]) * progress));
                }
                this.step(1, () -> this.mouseButton(minecraft, InputConstants.MOUSE_BUTTON_LEFT, InputConstants.RELEASE));
            }
            case SCROLL -> {
                this.step(0, () -> this.movePointer(minecraft, numbers[0], numbers[1]));
                this.step(1, () -> ((AccessorMixinMouseHandler)minecraft.mouseHandler).invoke_onScroll_FancyMenu(minecraft.getWindow().getWindow(), 0.0, numbers[2]));
            }
            case KEY -> {
                Key key = parseKey(input.argument());
                if (key == null) {
                    this.fail(name, "unknown key, use letters, digits, enter, escape, tab, backspace, delete, space, up, down, left, right, home, end, page_up, page_down or f1 to f12, with shortcut+, ctrl+, shift+, alt+ or cmd+ in front");
                    return;
                }
                this.step(0, () -> minecraft.keyboardHandler.keyPress(minecraft.getWindow().getWindow(), key.key(), key.scancode(), GLFW.GLFW_PRESS, key.modifiers()));
                this.step(1, () -> minecraft.keyboardHandler.keyPress(minecraft.getWindow().getWindow(), key.key(), key.scancode(), GLFW.GLFW_RELEASE, key.modifiers()));
            }
            case TYPE -> this.step(0, () -> input.argument().codePoints().forEach(codepoint -> ((IMixinKeyboardHandler)minecraft.keyboardHandler).invoke_charTyped_FancyMenu(minecraft.getWindow().getWindow(), codepoint, 0)));
        }
        this.step(1, () -> this.pass(name, describeWindows() + " screen=" + ((ScreenUtils.getScreen() != null) ? ScreenUtils.getScreen().getClass().getSimpleName() : "none")));
    }

    /**
     * Moves the pointer a tick before pressing, because many UIs update what is hovered while they render, and their click handling reads that.
     */
    private void queueClick(@NotNull Minecraft minecraft, float x, float y, int button, int clicks) {
        this.step(0, () -> this.movePointer(minecraft, x, y));
        for (int i = 0; i < clicks; i++) {
            this.step(1, () -> this.mouseButton(minecraft, button, InputConstants.PRESS));
            this.step(1, () -> this.mouseButton(minecraft, button, InputConstants.RELEASE));
        }
    }

    private void step(int ticksBefore, @NotNull Runnable action) {
        this.pendingSteps.addLast(new Step(ticksBefore, action));
    }

    private void movePointer(@NotNull Minecraft minecraft, float guiX, float guiY) {
        Window window = minecraft.getWindow();
        // The middle of the GUI pixel, in window coordinates. The first move after a screen change only sets the position, so move twice.
        double x = (guiX + 0.5) * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = (guiY + 0.5) * window.getScreenHeight() / window.getGuiScaledHeight();
        AccessorMixinMouseHandler mouse = (AccessorMixinMouseHandler)minecraft.mouseHandler;
        mouse.invoke_onMove_FancyMenu(window.getWindow(), x, y);
        mouse.invoke_onMove_FancyMenu(window.getWindow(), x, y);
    }

    private void mouseButton(@NotNull Minecraft minecraft, int button, int action) {
        ((AccessorMixinMouseHandler)minecraft.mouseHandler).invoke_onPress_FancyMenu(minecraft.getWindow().getWindow(), button, action, 0);
    }

    /**
     * Reads keys like {@code enter}, {@code a} or {@code shortcut+z}. {@code shortcut} is Cmd on macOS and Ctrl elsewhere, like in FancyMenu's edit shortcuts.
     */
    @Nullable
    static Key parseKey(@NotNull String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).split("\\+");
        int modifiers = 0;
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i].trim()) {
                case "shortcut" -> modifiers |= Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
                case "ctrl" -> modifiers |= GLFW.GLFW_MOD_CONTROL;
                case "shift" -> modifiers |= GLFW.GLFW_MOD_SHIFT;
                case "alt" -> modifiers |= GLFW.GLFW_MOD_ALT;
                case "cmd" -> modifiers |= GLFW.GLFW_MOD_SUPER;
                default -> {
                    return null;
                }
            }
        }
        String key = parts[parts.length - 1].trim();
        int code;
        int layoutCode = (key.length() == 1) ? findKeyOfLayout(key) : -1;
        if (layoutCode >= 0) {
            code = layoutCode;
        } else if ((key.length() == 1) && (key.charAt(0) >= 'a') && (key.charAt(0) <= 'z')) {
            code = GLFW.GLFW_KEY_A + (key.charAt(0) - 'a');
        } else if ((key.length() == 1) && (key.charAt(0) >= '0') && (key.charAt(0) <= '9')) {
            code = GLFW.GLFW_KEY_0 + (key.charAt(0) - '0');
        } else if (key.matches("f([1-9]|1[0-2])")) {
            code = GLFW.GLFW_KEY_F1 + (Integer.parseInt(key.substring(1)) - 1);
        } else {
            code = switch (key) {
                case "enter" -> GLFW.GLFW_KEY_ENTER;
                case "escape" -> GLFW.GLFW_KEY_ESCAPE;
                case "tab" -> GLFW.GLFW_KEY_TAB;
                case "backspace" -> GLFW.GLFW_KEY_BACKSPACE;
                case "delete" -> GLFW.GLFW_KEY_DELETE;
                case "space" -> GLFW.GLFW_KEY_SPACE;
                case "up" -> GLFW.GLFW_KEY_UP;
                case "down" -> GLFW.GLFW_KEY_DOWN;
                case "left" -> GLFW.GLFW_KEY_LEFT;
                case "right" -> GLFW.GLFW_KEY_RIGHT;
                case "home" -> GLFW.GLFW_KEY_HOME;
                case "end" -> GLFW.GLFW_KEY_END;
                case "page_up" -> GLFW.GLFW_KEY_PAGE_UP;
                case "page_down" -> GLFW.GLFW_KEY_PAGE_DOWN;
                default -> -1;
            };
        }
        if (code < 0) return null;
        return new Key(code, GLFW.glfwGetKeyScancode(code), modifiers);
    }

    /**
     * Finds the key that types the character with the keyboard layout of the system, because UIs match letter shortcuts by the name GLFW
     * gives the key in that layout. Key codes are US positions, so on a German layout the key code of Z types "y".
     *
     * @return The key code, or -1 if no printable key types the character.
     */
    private static int findKeyOfLayout(@NotNull String character) {
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_WORLD_2; code++) {
            // Not every number in the range is a key, and asking GLFW for the name of one that isn't logs an "Invalid scancode" error
            if (GLFW.glfwGetKeyScancode(code) < 0) continue;
            if (character.equals(GLFW.glfwGetKeyName(code, 0))) return code;
        }
        return -1;
    }

    private void takeScreenshot(@NotNull Minecraft minecraft, @NotNull WindowPreviewConfig config, float seconds) {
        String checkName = String.format(Locale.ROOT, "screenshot %.2fs", seconds);
        Path file = config.outputDirectory().resolve(String.format(Locale.ROOT, "%s_%06.2fs.png", config.namePrefix(), seconds));
        String details = " " + describeWindows();
        // The readback of the last frame happens right here, the file gets written on the writer thread
        NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
        this.pendingWrites.incrementAndGet();
        this.writer.execute(() -> this.writeScreenshot(image, file, checkName, details));
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

    private void finish() {
        if (this.phase == Phase.DONE) return;
        this.phase = Phase.DONE;
        if (this.menuBarControllerId != null) CustomizationOverlay.unregisterOverlayVisibilityController(this.menuBarControllerId);
        if (this.windowActiveBeforeRun != null) Minecraft.getInstance().setWindowActive(this.windowActiveBeforeRun);
        this.writer.shutdown();
        EventHandler.INSTANCE.unregisterListenersOf(this);
        int failed = this.failures.get();
        if (failed == 0) {
            LOGGER.info("[CHECK] window preview finished: PASS");
        } else {
            LOGGER.info("[CHECK] window preview finished: FAIL {} failed checks", failed);
        }
        Minecraft.getInstance().stop();
    }

    private void setPhase(@NotNull Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    @NotNull
    private WindowPreviewConfig requireConfig() {
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

    /**
     * Lists the titles of the open windows, front to back as the handler keeps them, with a {@code *} at the focused one.
     */
    @NotNull
    private static String describeWindows() {
        List<String> titles = new ArrayList<>();
        for (PiPWindow window : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            titles.add("'" + window.getTitle().getString() + "'" + (PiPWindowHandler.INSTANCE.isWindowFocused(window) ? "*" : ""));
        }
        return "windows=" + titles;
    }

    @NotNull
    private static String formatNumbers(float @NotNull [] numbers) {
        List<String> parts = new ArrayList<>();
        for (float number : numbers) parts.add(String.format(Locale.ROOT, "%.0f", number));
        return String.join(",", parts);
    }

    private enum Phase {
        WAIT_FOR_GAME,
        SIZE_WINDOW,
        WAIT_FOR_WINDOW,
        RUN,
        WRITE_SCREENSHOTS,
        DONE
    }

    private record Step(int ticksBefore, @NotNull Runnable action) {
    }

    /**
     * A key press as the keyboard handler of the game gets it from GLFW.
     */
    record Key(int key, int scancode, int modifiers) {
    }

}
