package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.google.gson.JsonElement;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;
import de.keksuccino.fancymenu.customization.listener.gui.ChooseListenerTypeScreen;
import de.keksuccino.fancymenu.customization.listener.gui.ManageListenersWindowBody;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.action.ui.ChooseActionWindowBody;
import de.keksuccino.fancymenu.customization.customgui.CustomGui;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.variables.ListVariableEditorWindowBody;
import de.keksuccino.fancymenu.customization.variables.ManageVariablesWindowBody;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLMouse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Dev-only docs tool that records a showcase GIF of the layout editor in an off-screen test client. Not part of the mod: it lives in the
 * docs_tools folder of the agent root and only gets copied into a workspace while recording (see docs_tools/README.md). Never commit it.<br>
 * {@link #buildScript} picks the showcase (editing a Text element, or filling a Scrollable Area), everything else is reusable.<br>
 * The config's {@code elements} (see {@link DocsShowcaseElements}) are the layout the editor opens with.<br>
 * Records every second tick as a PNG plus one JSON line per frame with the pointer state, see TESTING_GUIDELINES.md (Recording showcase GIFs).
 */
public final class DocsShowcaseEditorGif {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CUSTOM_GUI_IDENTIFIER = "docs_showcase";
    private static final int TICKS_PER_SECOND = 20;
    private static final int GUI_WIDTH = 640;
    private static final int GUI_HEIGHT = 360;
    private static final int OUT_WIDTH = 1280;
    private static final int ELEMENT_X = 170;
    private static final int ELEMENT_Y = 70;
    private static final int ELEMENT_W = 300;
    private static final int ELEMENT_H = 90;
    private static final float MENU_SCROLL_TARGET_Y = 290;

    private final Path framesDirectory;
    private final JsonObject config;
    private final ExecutorService writers = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu Docs GIF Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<String> frameLines = new ArrayList<>();
    private final List<String> pendingEvents = new ArrayList<>();
    @NotNull
    private Phase phase = Phase.WAIT_FOR_GAME;
    private int phaseTicks = 0;
    private int runTicks = 0;
    private int frameIndex = 0;
    private float pointerX = 560;
    private float pointerY = 300;
    private boolean leftDown = false;
    private boolean rightDown = false;
    @Nullable
    private Screen startScreen;
    @Nullable
    private String menuBarControllerId;
    /**
     * The listeners of the instance before the config's {@code listeners} replaced them. They come back when the showcase ends.
     */
    @Nullable
    private List<ListenerInstance> savedListeners;

    DocsShowcaseEditorGif(@NotNull Path framesDirectory, @NotNull JsonObject config) {
        this.framesDirectory = framesDirectory;
        this.config = config;
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            this.tick();
        } catch (Exception ex) {
            LOGGER.error("[CHECK] docs gif: FAIL the recorder crashed", ex);
            this.failures.incrementAndGet();
            if ((this.phase != Phase.WRITE) && (this.phase != Phase.DONE)) {
                this.setPhase(Phase.WRITE);
            } else {
                this.finish();
            }
        }
    }

    private void tick() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        this.phaseTicks++;
        switch (this.phase) {
            case WAIT_FOR_GAME -> {
                boolean loading = (minecraft.gui.overlay() != null) || (ScreenUtils.getScreen() == null);
                if (loading && (this.phaseTicks < 180 * TICKS_PER_SECOND)) return;
                Window window = minecraft.getWindow();
                window.setWindowed(1280, 720);
                minecraft.options.guiScale().set(Math.round(2 * window.getPixelDensity()));
                minecraft.resizeGui();
                // The dense editor grid would cover the whole showcase (only the separate docs instance keeps this option)
                FancyMenu.getOptions().showLayoutEditorGrid.setValue(false);
                if (!CustomGuiHandler.guiExists(CUSTOM_GUI_IDENTIFIER)) {
                    CustomGui gui = new CustomGui();
                    gui.identifier = CUSTOM_GUI_IDENTIFIER;
                    gui.title = "";
                    CustomGuiHandler.addGui(gui);
                }
                this.setPhase(Phase.OPEN_EDITOR);
            }
            case OPEN_EDITOR -> {
                if (this.phaseTicks < 20) return;
                Window window = minecraft.getWindow();
                this.pass("window", window.getWidth() + "x" + window.getHeight() + " gui=" + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight());
                this.openStart();
                this.setPhase(Phase.WAIT_FOR_EDITOR);
            }
            case WAIT_FOR_EDITOR -> {
                if (this.startsOnScreen()) {
                    if ((ScreenUtils.getScreen() != this.startScreen) || (this.phaseTicks < 30)) {
                        if (this.phaseTicks > 200) throw new IllegalStateException("The screen never opened");
                        return;
                    }
                } else {
                    if (!(ScreenUtils.getScreen() instanceof LayoutEditorScreen) || (this.phaseTicks < 30)) {
                        if (this.phaseTicks > 200) throw new IllegalStateException("The layout editor never opened");
                        return;
                    }
                    // The Layers widget grows over the element at big UI scales and isn't part of what the showcase is about
                    ((LayoutEditorScreen) ScreenUtils.getScreen()).layoutEditorWidgets.forEach(widget -> widget.setVisible(false));
                }
                this.movePointer(minecraft, this.pointerX, this.pointerY);
                this.buildScript(minecraft);
                this.setPhase(Phase.RUN);
            }
            case RUN -> {
                this.runTicks++;
                // The main render target still holds the last frame, which shows the pointer state of the end of the previous tick
                if ((this.runTicks % 2) == 0) this.captureFrame(minecraft);
                this.runSteps();
                if (this.steps.isEmpty()) this.setPhase(Phase.WRITE);
            }
            case WRITE -> {
                if ((this.pendingWrites.get() > 0) && (this.phaseTicks < 60 * TICKS_PER_SECOND)) return;
                Files.write(this.framesDirectory.resolve("frames.jsonl"), this.frameLines);
                this.finish();
            }
            case DONE -> {
            }
        }
    }

    /**
     * Config {@code start}: {@code editor} (default) opens the layout editor, {@code screen} opens the screen itself with the layout, like a
     * player sees it. Only {@code screen} shows the menu bar of FancyMenu, and only with {@code menu_bar: true}.
     */
    private boolean startsOnScreen() {
        return this.config.has("start") && this.config.get("start").getAsString().equals("screen");
    }

    /**
     * Opens the layout editor or the screen (see {@link #startsOnScreen()}). Config {@code screen}: {@code title} for the title screen, otherwise
     * an empty Custom GUI. The layout gets the config's {@code elements}, {@code vanilla} and {@code background_image}/{@code background_color}
     * (see {@link DocsShowcaseElements}).
     */
    private void openStart() {
        Screen screen;
        if (this.config.has("screen") && this.config.get("screen").getAsString().equals("title")) {
            screen = new TitleScreen();
        } else {
            CustomGui gui = CustomGuiHandler.getGui(CUSTOM_GUI_IDENTIFIER);
            if (gui == null) throw new IllegalStateException("The showcase GUI is missing");
            screen = CustomGuiHandler.constructInstance(gui, null, null);
        }
        if (this.config.has("variables")) setUpVariables(this.config.getAsJsonObject("variables"));
        if (this.config.has("shuffle_seed")) seedListShuffle(this.config.get("shuffle_seed").getAsLong());
        if (this.config.has("listeners")) this.setUpListeners(this.config.getAsJsonArray("listeners"));
        Layout layout = Layout.buildForScreen(screen);
        if (this.config.has("elements")) {
            DocsShowcaseElements.addTo(layout, this.config.getAsJsonArray("elements"));
        } else {
            ElementBuilder<?, ?> builder = ElementRegistry.getBuilder("text_v2");
            if (builder == null) throw new IllegalStateException("No Text element builder");
            TextElement element = (TextElement) builder.buildDefaultInstance();
            element.setSource(TextElement.SourceMode.DIRECT, this.config.get("initial_text").getAsString());
            element.markdownSettings.applyLook(MarkdownLook.PIXEL);
            element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
            element.posOffsetX = ELEMENT_X;
            element.posOffsetY = ELEMENT_Y;
            element.baseWidth = ELEMENT_W;
            element.baseHeight = ELEMENT_H;
            layout.serializedElements.add(builder.serializeElementInternal(element));
        }
        if (this.config.has("vanilla")) DocsShowcaseElements.addVanillaTo(layout, this.config.getAsJsonArray("vanilla"));
        DocsShowcaseElements.addBackgroundTo(layout, this.config);
        if (this.startsOnScreen()) {
            boolean menuBar = this.config.has("menu_bar") && this.config.get("menu_bar").getAsBoolean();
            if (!menuBar) this.menuBarControllerId = CustomizationOverlay.registerOverlayVisibilityController(s -> false);
            this.startScreen = screen;
            LayoutHandler.setPreviewLayout(layout);
            ScreenUtils.setScreen(screen);
        } else {
            LayoutHandler.openLayoutEditor(layout, screen);
        }
    }

    /**
     * Picks the showcase by the {@code script} of the config: {@code text_editor} (default) or {@code scroll_area}.
     */
    private void buildScript(@NotNull Minecraft minecraft) {
        String script = this.config.has("script") ? this.config.get("script").getAsString() : "text_editor";
        switch (script) {
            case "text_editor" -> this.buildTextEditorScript(minecraft);
            case "scroll_area" -> this.buildScrollAreaScript(minecraft);
            case "select_and_scroll" -> this.buildSelectAndScrollScript(minecraft);
            case "action_script" -> this.buildActionScriptScript(minecraft);
            case "click_element" -> this.buildClickElementScript(minecraft);
            case "manage_variables" -> this.buildManageVariablesScript(minecraft);
            case "manage_listeners" -> this.buildManageListenersScript(minecraft);
            case "listeners_still" -> this.buildListenersStillScript(minecraft);
            case "press_keys" -> this.buildPressKeysScript(minecraft);
            case "add_element" -> this.buildAddElementScript(minecraft);
            case "add_placeholder" -> this.buildAddPlaceholderScript(minecraft);
            default -> throw new IllegalArgumentException("Unknown script " + script);
        }
    }

    /**
     * Replaces the listeners of the instance with the config's {@code listeners} until the showcase ends. An empty array leaves no listeners.<br>
     * Keys of a listener: {@code type} (the listener identifier, like {@code keyboard_key_pressed}), {@code name} (its display name, optional)
     * and {@code script}: actions ({@code action} identifier and {@code value}) and IF statements ({@code if}: requirements with
     * {@code requirement} identifier and {@code value}, {@code actions}: the script inside the IF, with the same keys).
     */
    private void setUpListeners(@NotNull Iterable<JsonElement> listeners) {
        if (this.savedListeners == null) this.savedListeners = ListenerHandler.getInstances();
        List<ListenerInstance> instances = new ArrayList<>();
        for (JsonElement entry : listeners) {
            JsonObject data = entry.getAsJsonObject();
            AbstractListener type = ListenerRegistry.getListener(data.get("type").getAsString());
            if (type == null) throw new IllegalStateException("No listener " + data.get("type").getAsString());
            ListenerInstance instance = type.createFreshInstance();
            if (data.has("name")) instance.setDisplayName(data.get("name").getAsString());
            GenericExecutableBlock script = new GenericExecutableBlock();
            if (data.has("script")) addScriptEntries(script, data.getAsJsonArray("script"));
            // Setting the script gives every part of it the $$ values of the listener
            instance.setActionScript(script);
            instances.add(instance);
        }
        ListenerHandler.replaceInstances(instances);
    }

    private static void addScriptEntries(@NotNull AbstractExecutableBlock block, @NotNull Iterable<JsonElement> entries) {
        for (JsonElement entry : entries) {
            JsonObject data = entry.getAsJsonObject();
            if (data.has("if")) {
                RequirementContainer condition = new RequirementContainer();
                for (JsonElement requirementEntry : data.getAsJsonArray("if")) {
                    JsonObject requirementData = requirementEntry.getAsJsonObject();
                    Requirement requirement = RequirementRegistry.getRequirement(requirementData.get("requirement").getAsString());
                    if (requirement == null) throw new IllegalStateException("No requirement " + requirementData.get("requirement").getAsString());
                    String value = requirementData.has("value") ? requirementData.get("value").getAsString() : null;
                    condition.addInstance(new RequirementInstance(requirement, value, RequirementInstance.RequirementMode.IF, condition));
                }
                IfExecutableBlock ifBlock = new IfExecutableBlock(condition);
                addScriptEntries(ifBlock, data.getAsJsonArray("actions"));
                block.addExecutable(ifBlock);
            } else {
                Action action = ActionRegistry.getAction(data.get("action").getAsString());
                if (action == null) throw new IllegalStateException("No action " + data.get("action").getAsString());
                block.addExecutable(new ActionInstance(action, data.has("value") ? data.get("value").getAsString() : null));
            }
        }
    }

    /**
     * Outside the editor, with the menu bar (config {@code start: "screen"}, {@code menu_bar: true}, {@code listeners: []}): open
     * Customization -> Manage Listeners, click Add Listener, search and pick a listener, give it an action like
     * {@link #buildActionScriptScript} does, select the new listener so its description shows, and click Done. Then press keys to try it.<br>
     * Config: {@code listener} (the identifier to pick), {@code listener_search} (typed into the search bar of the listener chooser),
     * {@code search}, {@code action} and {@code fields} (see {@link #buildActionScriptScript}), {@code keys} (see {@link #buildPressKeysScript}).
     */
    private void buildManageListenersScript(@NotNull Minecraft minecraft) {
        String listener = this.config.get("listener").getAsString();
        this.waitTicks(12);
        // Customization -> Manage Listeners
        this.moveTo(() -> menuBarEntry("screen"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("manage_listeners")[0] + 20, 200}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "manage_listeners", 230);
        this.waitTicks(8);
        this.clickMenuEntry(minecraft, "manage_listeners");
        this.waitUntil("Manage Listeners", () -> findWindow(ManageListenersWindowBody.class) != null, 60);
        this.waitTicks(14);
        // Add Listener, then search and pick the listener
        this.moveTo(() -> windowButtonCenter(requireWindow(ManageListenersWindowBody.class), "fancymenu.listeners.manage.add"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("listener chooser", () -> findWindow(ChooseListenerTypeScreen.class) != null, 60);
        this.waitTicks(12);
        this.type(minecraft, this.config.get("listener_search").getAsString());
        this.waitTicks(12);
        this.moveTo(() -> listenerChooserEntry(listener), 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        // Time to read the description with the $$ values
        this.waitTicks(32);
        this.moveTo(() -> windowButtonCenter(requireWindow(ChooseListenerTypeScreen.class), "fancymenu.common_components.done"), 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        // The action script editor of the new listener opens
        this.openActionChooserAndAdd(minecraft, this.config.get("action").getAsString());
        this.waitTicks(12);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ActionScriptEditorWindowBody.class), "doneButton"), 18);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("action script editor closed", () -> findWindow(ActionScriptEditorWindowBody.class) == null, 60);
        this.waitUntil("Manage Listeners shown again", () -> requireWindow(ManageListenersWindowBody.class).isVisible(), 60);
        this.waitTicks(10);
        // Select the new listener, so its type and action script show on the right
        this.moveTo(() -> listenerCell(listener), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(26);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ManageListenersWindowBody.class), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Manage Listeners closed", () -> findWindow(ManageListenersWindowBody.class) == null, 60);
        this.waitTicks(10);
        this.addKeyPresses(minecraft);
    }

    /**
     * Stills of the listener windows (config {@code start: "screen"}, {@code listeners}): opens Manage Listeners, selects a listener
     * (config {@code select}: its display name or type identifier), then opens Add Listener, searches and selects a listener type (config
     * {@code listener_search}, {@code listener}). Marks the frame of each still with a {@code mark} event ({@code manage}, {@code choose}).
     */
    private void buildListenersStillScript(@NotNull Minecraft minecraft) {
        String select = this.config.get("select").getAsString();
        String listener = this.config.get("listener").getAsString();
        this.waitTicks(10);
        this.steps.addLast(() -> {
            ManageListenersWindowBody.openInWindow(new ManageListenersWindowBody(accepted -> {}));
            return true;
        });
        this.waitUntil("Manage Listeners", () -> findWindow(ManageListenersWindowBody.class) != null, 60);
        this.waitTicks(14);
        this.moveTo(() -> listenerCell(select), 10);
        this.waitTicks(3);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        // The description area is small at big UI scales, so scroll it down to the action script of the listener (config description_wheel_turns)
        if (this.config.has("description_wheel_turns")) {
            this.moveTo(() -> descriptionAreaCenter(requireWindow(ManageListenersWindowBody.class)), 8);
            for (int i = 0; i < this.config.get("description_wheel_turns").getAsInt(); i++) {
                this.wheel(minecraft, -1.0);
                this.waitTicks(2);
            }
        }
        // Rest the pointer on an empty spot, so no row shows a hover
        this.moveTo(() -> listenerListFreeSpot(), 8);
        this.waitTicks(24);
        this.mark("manage");
        this.waitTicks(4);
        this.moveTo(() -> windowButtonCenter(requireWindow(ManageListenersWindowBody.class), "fancymenu.listeners.manage.add"), 10);
        this.waitTicks(3);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("listener chooser", () -> findWindow(ChooseListenerTypeScreen.class) != null, 60);
        this.waitTicks(12);
        this.typeQuickly(minecraft, this.config.get("listener_search").getAsString());
        this.waitTicks(8);
        this.moveTo(() -> listenerChooserEntry(listener), 10);
        this.waitTicks(3);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(4);
        this.moveTo(() -> listenerChooserFreeSpot(), 8);
        this.waitTicks(24);
        this.mark("choose");
        this.waitTicks(4);
    }

    /**
     * Outside the editor: presses keys one after another and waits, like a player trying a listener (config {@code start: "screen"},
     * {@code listeners}). Config: {@code keys} (each with {@code key}, like {@code h}, and {@code badge}, like {@code H}),
     * {@code key_gap_ticks} (between keys, default 40) and {@code hold_ticks} (after the last key, default 100). Optional {@code pointer}: [x, y].
     */
    private void buildPressKeysScript(@NotNull Minecraft minecraft) {
        if (this.config.has("pointer")) {
            float x = this.config.getAsJsonArray("pointer").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("pointer").get(1).getAsFloat();
            this.steps.addLast(() -> {
                this.movePointer(minecraft, x, y);
                return true;
            });
        }
        this.waitTicks(20);
        this.addKeyPresses(minecraft);
    }

    /**
     * Presses the config's {@code keys}, see {@link #buildPressKeysScript}. Does nothing without them.
     */
    private void addKeyPresses(@NotNull Minecraft minecraft) {
        if (!this.config.has("keys")) return;
        int gap = this.config.has("key_gap_ticks") ? this.config.get("key_gap_ticks").getAsInt() : 40;
        int hold = this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 100;
        boolean first = true;
        for (JsonElement entry : this.config.getAsJsonArray("keys")) {
            if (!first) this.waitTicks(gap);
            first = false;
            JsonObject key = entry.getAsJsonObject();
            this.key(minecraft, key.get("key").getAsString(), key.get("badge").getAsString());
        }
        this.waitTicks(hold);
    }

    /**
     * Marks the next frame with a {@code mark} event, to find the frame of a still in frames.jsonl.
     */
    private void mark(@NotNull String name) {
        this.steps.addLast(() -> {
            this.pendingEvents.add("{\"type\":\"mark\",\"name\":\"" + name + "\"}");
            return true;
        });
    }

    /**
     * Sets up FancyMenu's variables before the showcase starts, from the config's {@code variables}: every other variable gets deleted,
     * then a text value creates a text variable and an array of texts a list variable. An empty object leaves no variables.
     */
    private static void setUpVariables(@NotNull JsonObject variables) {
        VariableHandler.clearVariables();
        for (Map.Entry<String, JsonElement> variable : variables.entrySet()) {
            if (variable.getValue().isJsonArray()) {
                List<String> entries = new ArrayList<>();
                variable.getValue().getAsJsonArray().forEach(entry -> entries.add(entry.getAsString()));
                VariableHandler.setListIfAbsent(variable.getKey(), entries);
            } else {
                VariableHandler.setVariable(variable.getKey(), variable.getValue().getAsString());
            }
        }
    }

    /**
     * Seeds the randomness of the Shuffle of Arrange List, so a GIF that shuffles a list gets the same order every run
     * (config {@code shuffle_seed}). Pick a seed that shows a change on every click.
     */
    private static void seedListShuffle(long seed) {
        try {
            Field field = VariableHandler.class.getDeclaredField("LIST_SHUFFLE_RANDOM");
            field.setAccessible(true);
            ((java.util.Random) field.get(null)).setSeed(seed);
        } catch (Exception ex) {
            throw new IllegalStateException("Can't seed the list shuffle", ex);
        }
    }

    /**
     * Outside the editor, with the menu bar (config {@code start: "screen"}, {@code menu_bar: true}, {@code variables: {}}): open
     * Customization -> Manage Variables, add a text variable and set its value, then add a list variable and type its entries in the list editor.<br>
     * Config: {@code text_variable} ({@code name}, {@code value}) and {@code list_variable} ({@code name}, {@code entries}).
     */
    private void buildManageVariablesScript(@NotNull Minecraft minecraft) {
        JsonObject textVariable = this.config.getAsJsonObject("text_variable");
        JsonObject listVariable = this.config.getAsJsonObject("list_variable");
        String textName = textVariable.get("name").getAsString();
        String listName = listVariable.get("name").getAsString();
        this.waitTicks(12);
        // Customization -> Manage Variables
        this.moveTo(() -> menuBarEntry("screen"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(10);
        // Manage Variables sits below the visible part of the long menu, so scroll the menu like a player would
        this.moveTo(() -> new float[] {contextMenuEntry("variables")[0] + 20, 200}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "variables", 230);
        this.waitTicks(8);
        this.clickMenuEntry(minecraft, "variables");
        this.waitUntil("Manage Variables", () -> findWindow(ManageVariablesWindowBody.class) != null, 60);
        this.waitTicks(12);
        // Add a text variable
        this.rightClickAndPick(minecraft, () -> variablesListFreeSpot(), "add_text_variable");
        this.typeIntoTextDialog(minecraft, textName);
        this.waitUntil("text variable added", () -> VariableHandler.variableExists(textName), 40);
        this.waitTicks(6);
        // Set its value
        this.rightClickAndPick(minecraft, () -> variableRow(textName), "set_value");
        this.typeIntoTextDialog(minecraft, textVariable.get("value").getAsString());
        this.waitTicks(10);
        // Add a list variable
        this.rightClickAndPick(minecraft, () -> variablesListFreeSpot(), "add_list_variable");
        this.typeIntoTextDialog(minecraft, listName);
        this.waitUntil("list variable added", () -> VariableHandler.variableExists(listName), 40);
        this.waitTicks(6);
        // Type its entries in the list editor. Enter saves an entry and starts the next one, Enter on the empty one stops.
        this.rightClickAndPick(minecraft, () -> variableRow(listName), "edit_list");
        this.waitUntil("list editor", () -> findWindow(ListVariableEditorWindowBody.class) != null, 60);
        this.waitTicks(12);
        this.moveTo(DocsShowcaseEditorGif::addEntryRow, 12);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        for (JsonElement entry : listVariable.getAsJsonArray("entries")) {
            this.typeQuickly(minecraft, entry.getAsString());
            this.waitTicks(5);
            this.key(minecraft, "enter", null);
            this.waitTicks(6);
        }
        this.key(minecraft, "enter", null);
        this.waitTicks(12);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ListVariableEditorWindowBody.class), "doneButton"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("list editor closed", () -> findWindow(ListVariableEditorWindowBody.class) == null, 60);
        this.waitTicks(8);
        // Rest the pointer on the free space of the list, so no row shows a hover
        this.moveTo(() -> variablesListFreeSpot(), 12);
        this.waitTicks(56);
    }

    /**
     * Right-clicks the target in the Manage Variables window and clicks the entry of its right-click menu.
     */
    private void rightClickAndPick(@NotNull Minecraft minecraft, @NotNull Supplier<float[]> target, @NotNull String entry) {
        this.moveTo(target, 12);
        this.waitTicks(3);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("right-click menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, entry);
    }

    /**
     * Types into the text dialog that just opened (name and value dialogs) and clicks its Done button.
     */
    private void typeIntoTextDialog(@NotNull Minecraft minecraft, @NotNull String text) {
        this.waitUntil("text dialog", () -> findWindow(TextInputWindowBody.class) != null, 60);
        this.waitTicks(8);
        this.type(minecraft, text);
        this.waitTicks(6);
        this.moveTo(() -> windowButtonCenter(requireWindow(TextInputWindowBody.class), "fancymenu.common_components.done"), 12);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("text dialog closed", () -> findWindow(TextInputWindowBody.class) == null, 60);
    }

    /**
     * Right-click a free spot of the editor, open New Element and pick an element type, drag the new element to its place, widen it at its
     * right edge and give it a label through its right-click menu. Config: {@code right_click} ([x, y] of the free spot), {@code element_type}
     * (its identifier, like {@code custom_button}), {@code drag_to} ([x, y] of the element's center after the drag), {@code widen_by} (GUI
     * pixels the right edge moves), {@code label_entry} (the right-click menu entry of the label, like {@code edit_label}), {@code label}
     * (the typed label) and {@code free_spot} ([x, y] of the background click at the end, which unselects the element).
     */
    private void buildAddElementScript(@NotNull Minecraft minecraft) {
        float clickX = this.config.getAsJsonArray("right_click").get(0).getAsFloat();
        float clickY = this.config.getAsJsonArray("right_click").get(1).getAsFloat();
        float dragX = this.config.getAsJsonArray("drag_to").get(0).getAsFloat();
        float dragY = this.config.getAsJsonArray("drag_to").get(1).getAsFloat();
        float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
        float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
        float widenBy = this.config.get("widen_by").getAsFloat();
        String labelEntry = this.config.get("label_entry").getAsString();
        String entry = newElementEntry(this.config.get("element_type").getAsString());
        int[] elementsBefore = {0};
        this.waitTicks(16);
        this.moveTo(() -> new float[] {clickX, clickY}, 18);
        this.waitTicks(6);
        this.steps.addLast(() -> {
            elementsBefore[0] = requireLayoutEditor().normalEditorElements.size();
            return true;
        });
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("add_element")[0] + 20, Math.min(this.pointerY + 40, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        // New Element is the last entry of the long menu, which ends below MENU_SCROLL_TARGET_Y at big UI scales
        this.scrollMenuUntilVisible(minecraft, "add_element", 345);
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, "add_element");
        this.waitUntil("New Element menu", () -> subMenu("add_element").isOpen(), 40);
        this.waitTicks(10);
        // Go into the sub-menu sideways first, so the pointer doesn't pass over other entries of the right-click menu
        this.moveTo(() -> new float[] {menuEntry("add_element", entry)[0], this.pointerY}, 10);
        this.waitTicks(4);
        this.clickMenuEntry(minecraft, "add_element", entry);
        this.waitUntil("new element", () -> requireLayoutEditor().normalEditorElements.size() > elementsBefore[0], 40);
        this.waitTicks(16);
        // Drag it to its place
        this.moveTo(() -> center(newestElementBox()), 12);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> new float[] {dragX, dragY}, 22);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Widen it at its right edge
        this.moveTo(() -> {
            float[] box = newestElementBox();
            return new float[] {box[0] + box[2], box[1] + box[3] / 2.0F};
        }, 12);
        this.waitTicks(8);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> new float[] {this.pointerX + widenBy, this.pointerY}, 16);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Give it a label through its right-click menu
        this.moveTo(() -> center(newestElementBox()), 12);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("element menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry(labelEntry)[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, labelEntry);
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, labelEntry);
        this.waitUntil("text editor", () -> findEditor() != null, 60);
        this.waitTicks(12);
        this.key(minecraft, "shortcut+a", "Ctrl + A");
        this.waitTicks(6);
        this.type(minecraft, this.config.get("label").getAsString());
        this.waitTicks(10);
        this.moveTo(DocsShowcaseEditorGif::doneButton, 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("text editor closed", () -> findEditor() == null, 60);
        this.waitTicks(8);
        // Click the empty background, so the element shows without its selection frame
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(70);
    }

    /**
     * The identifier of the entry of an element type in the New Element menu of the layout editor. The menu lists the types that aren't
     * deprecated by name, as {@code element_<index>} (see LayoutEditorUI#buildElementContextMenu).
     */
    @NotNull
    private static String newElementEntry(@NotNull String elementType) {
        LayoutEditorScreen editor = requireLayoutEditor();
        List<ElementBuilder<?, ?>> builders = new ArrayList<>();
        for (ElementBuilder<?, ?> builder : ElementRegistry.getBuilders()) {
            if (builder.shouldShowUpInEditorElementMenu(editor) && !builder.isDeprecated()) builders.add(builder);
        }
        builders.sort(Comparator
                .comparing((ElementBuilder<?, ?> builder) -> builder.getDisplayName(null).getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(builder -> builder.getDisplayName(null).getString())
                .thenComparing(ElementBuilder::getIdentifier));
        for (int i = 0; i < builders.size(); i++) {
            if (builders.get(i).getIdentifier().equals(elementType)) return "element_" + i;
        }
        throw new IllegalStateException("No New Element entry for " + elementType);
    }

    @NotNull
    private static LayoutEditorScreen requireLayoutEditor() {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        if (editor == null) throw new IllegalStateException("The layout editor is not open");
        return editor;
    }

    /**
     * X, Y, width and height of the element that got added last in the layout editor, in GUI pixels.
     */
    @NotNull
    private static float[] newestElementBox() {
        List<AbstractEditorElement<?, ?>> elements = requireLayoutEditor().normalEditorElements;
        if (elements.isEmpty()) throw new IllegalStateException("The layout has no elements");
        AbstractEditorElement<?, ?> element = elements.get(elements.size() - 1);
        return new float[] {element.getX(), element.getY(), element.getWidth(), element.getHeight()};
    }

    /**
     * Right-click an element with an action script (like a button), open its action script editor, press A, search an action in the action
     * chooser, pick it, fill in the text fields of its value window and click Done in both windows.<br>
     * Config: {@code target} (the element's id), {@code search} (typed into the chooser's search bar), {@code action} (the identifier of the action to pick),
     * {@code fields} (text fields of the value window: {@code label} is the localization key of the label above the field, {@code text} what gets typed in).
     */
    private void buildActionScriptScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        String action = this.config.get("action").getAsString();
        this.waitTicks(16);
        this.moveTo(() -> center(editorBox(target)), 18);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("manage_actions")[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "manage_actions");
        this.waitTicks(6);
        this.moveTo(() -> contextMenuEntry("manage_actions"), 10);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.openActionChooserAndAdd(minecraft, action);
        this.waitTicks(30);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ActionScriptEditorWindowBody.class), "doneButton"), 18);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("action script editor closed", () -> findWindow(ActionScriptEditorWindowBody.class) == null, 60);
        this.waitTicks(6);
        // Click the empty background, so the element shows without its selection frame
        this.moveTo(() -> {
            float[] box = editorBox(target);
            return new float[] {box[0] + box[2] + 60, box[1] + box[3] + 50};
        }, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(50);
    }

    /**
     * In an open action script editor: press A, search the action, pick it, click Next Step and fill in the fields of the value window
     * (config {@code search} and {@code fields}, see {@link #buildActionScriptScript}), then click its Done button.
     */
    private void openActionChooserAndAdd(@NotNull Minecraft minecraft, @NotNull String action) {
        this.waitUntil("action script editor", () -> findWindow(ActionScriptEditorWindowBody.class) != null, 60);
        this.waitTicks(20);
        this.key(minecraft, "a", "A");
        this.waitUntil("action chooser", () -> findWindow(ChooseActionWindowBody.class) != null, 60);
        this.waitTicks(12);
        this.type(minecraft, this.config.get("search").getAsString());
        this.waitTicks(12);
        this.moveTo(() -> chooserEntry(action), 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(20);
        this.moveTo(() -> windowButtonCenter(requireWindow(ChooseActionWindowBody.class), "fancymenu.ui.generic.next_step"), 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("value window", () -> findValueWindow() != null, 60);
        this.waitTicks(14);
        if (this.config.has("fields")) {
            for (JsonElement entry : this.config.getAsJsonArray("fields")) {
                if (entry.getAsJsonObject().has("cycle")) {
                    // A cycle button, like the Arrangement of Arrange List: click it until it shows the wanted value
                    String cycle = entry.getAsJsonObject().get("cycle").getAsString();
                    int clicks = entry.getAsJsonObject().get("clicks").getAsInt();
                    this.moveTo(() -> cellWidgetWithLabel(cycle), 14);
                    this.waitTicks(6);
                    for (int i = 0; i < clicks; i++) {
                        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                        this.waitTicks(9);
                    }
                    this.waitTicks(10);
                    continue;
                }
                String label = entry.getAsJsonObject().get("label").getAsString();
                String text = entry.getAsJsonObject().get("text").getAsString();
                this.moveTo(() -> cellInputAfterLabel(label), 14);
                this.waitTicks(4);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(6);
                this.key(minecraft, "shortcut+a", "Ctrl + A");
                this.waitTicks(6);
                this.type(minecraft, text);
                this.waitTicks(10);
            }
        }
        this.moveTo(() -> windowWidgetCenter(requireValueWindow(), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("value window closed", () -> findValueWindow() == null, 60);
        this.waitUntil("action script editor shown again", () -> requireWindow(ActionScriptEditorWindowBody.class).isVisible(), 60);
    }

    /**
     * Outside the editor: hover an element, click it and wait, like a player clicking a button. Config: {@code target} (the element's id),
     * {@code hold_ticks} (how long the result stays, default 100), {@code clicks} (default 1) and {@code click_gap_ticks} (between clicks, default 40).
     */
    private void buildClickElementScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        int hold = this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 100;
        int clicks = this.config.has("clicks") ? this.config.get("clicks").getAsInt() : 1;
        int gap = this.config.has("click_gap_ticks") ? this.config.get("click_gap_ticks").getAsInt() : 40;
        this.waitTicks(16);
        this.moveTo(() -> center(screenBox(target)), 20);
        this.waitTicks(10);
        for (int i = 0; i < clicks; i++) {
            if (i > 0) this.waitTicks(gap);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        }
        this.waitTicks(hold);
    }

    /**
     * For stills of the editor: select an element by clicking it, then turn the mouse wheel over it. The last frame is the still.
     * Config: {@code target} (its id), {@code click} (X and Y relative to the element, on a free spot of it), {@code wheel_turns} (negative scrolls down).
     */
    private void buildSelectAndScrollScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        float clickX = this.config.getAsJsonArray("click").get(0).getAsFloat();
        float clickY = this.config.getAsJsonArray("click").get(1).getAsFloat();
        int turns = this.config.get("wheel_turns").getAsInt();
        this.waitTicks(20);
        this.moveTo(() -> {
            float[] box = editorBox(target);
            return new float[] {box[0] + clickX, box[1] + clickY};
        }, 10);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(10);
        for (int i = 0; i < Math.abs(turns); i++) {
            this.wheel(minecraft, Math.signum(turns));
            this.waitTicks(4);
        }
        // Elements under the pointer get a hover frame, which doesn't belong in the still
        this.moveTo(() -> {
            float[] box = editorBox(target);
            return new float[] {box[0] - 80, box[1] + box[3] + 80};
        }, 8);
        this.waitTicks(30);
    }

    /**
     * Select the buttons next to a Scrollable Area that only holds a title, drag them in, switch the area to Stack, scroll it with
     * the mouse wheel and drag the last button in front of the first one. Config: {@code area} (its id), {@code buttons} (their ids, in screen order).
     */
    private void buildScrollAreaScript(@NotNull Minecraft minecraft) {
        String area = this.config.get("area").getAsString();
        List<String> buttons = new ArrayList<>();
        this.config.getAsJsonArray("buttons").forEach(id -> buttons.add(id.getAsString()));
        String first = buttons.get(0);
        String last = buttons.get(buttons.size() - 1);
        this.waitTicks(24);
        // Select all buttons with a selection rectangle
        this.moveTo(() -> {
            float[] box = editorBox(first);
            return new float[] {box[0] - 12, box[1] - 14};
        }, 16);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> {
            float[] box = editorBox(last);
            return new float[] {box[0] + box[2] + 12, box[1] + box[3] + 12};
        }, 16);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(8);
        // Drag them into the area, below its title: Ctrl keeps all of them selected, so the others come along with the first one
        this.moveTo(() -> center(editorBox(first)), 14);
        this.waitTicks(4);
        this.badge("Ctrl + Drag");
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
        this.moveTo(() -> {
            float[] target = editorBox(area);
            float[] grabbed = editorBox(first);
            return new float[] {target[0] + 10 + grabbed[2] / 2.0F, target[1] + 26 + grabbed[3] / 2.0F};
        }, 22);
        this.waitTicks(10);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("buttons in the area", () -> requireEditorElement(last).element.getParentContainer() != null, 40);
        this.waitTicks(14);
        // Right-click a free spot of the area and switch it to Stack
        this.moveTo(() -> {
            float[] box = editorBox(area);
            return new float[] {box[0] + box[2] * 0.5F, box[1] + box[3] - 7};
        }, 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("arrangement")[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "arrangement");
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, "arrangement");
        this.waitUntil("Arrangement menu", () -> subMenu("arrangement").isOpen(), 40);
        this.waitTicks(8);
        this.clickMenuEntry(minecraft, "arrangement", "layout_mode");
        this.waitUntil("Mode menu", () -> subMenu("arrangement", "layout_mode").isOpen(), 40);
        this.waitTicks(8);
        this.clickMenuEntry(minecraft, "arrangement", "layout_mode", "layout_mode_stack");
        this.waitUntil("context menu closed", () -> (ContextMenuHandler.INSTANCE.getCurrent() == null) || !ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(20);
        // Scroll down with the mouse wheel
        this.moveTo(() -> center(editorBox(area)), 14);
        this.waitTicks(6);
        for (int i = 0; i < 3; i++) {
            this.wheel(minecraft, -1.0);
            this.waitTicks(6);
        }
        this.waitTicks(10);
        // Drag the last button in front of the first one. The area scrolls back up while the button is close to its top edge.
        this.moveTo(() -> center(editorBox(last)), 14);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> {
            float[] box = editorBox(area);
            return new float[] {box[0] + box[2] / 2.0F, box[1] + 6};
        }, 20);
        this.waitTicks(14);
        this.moveTo(() -> {
            float[] box = editorBox(first);
            return new float[] {box[0] + box[2] / 2.0F, box[1] + 4};
        }, 8);
        this.waitTicks(12);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(10);
        // Click the empty background, so the area shows without its selection frame
        this.moveTo(() -> {
            float[] box = editorBox(area);
            return new float[] {box[0] + box[2] + 90, box[1] + box[3] + 40};
        }, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(60);
    }

    /**
     * Right-click the element, open its text, bold a word, center the headline, type a patch notes list and click Done.
     */
    private void buildTextEditorScript(@NotNull Minecraft minecraft) {
        this.openTextContentEditor(minecraft);
        // Select a word with the mouse and make it bold
        String word = this.config.get("bold_word").getAsString();
        this.moveTo(() -> wordEdge(word, false), 16);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(2);
        this.dragSelectWord(minecraft, word, 10);
        this.waitTicks(2);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(8);
        this.key(minecraft, "shortcut+b", "Ctrl + B");
        this.waitTicks(18);
        // Center the headline
        this.moveTo(() -> lineEnd(0), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        this.key(minecraft, "shortcut+e", "Ctrl + E");
        this.waitTicks(18);
        // Type a patch notes list below the text
        this.moveTo(() -> lineEnd(-1), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        this.key(minecraft, "enter", null);
        this.waitTicks(2);
        this.key(minecraft, "enter", null);
        this.waitTicks(4);
        List<String> lines = new ArrayList<>();
        this.config.getAsJsonArray("typed_lines").forEach(line -> lines.add(line.getAsString()));
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                this.key(minecraft, "enter", null);
                this.waitTicks(3);
            }
            this.type(minecraft, lines.get(i));
            this.waitTicks(4);
        }
        this.waitTicks(14);
        this.moveTo(DocsShowcaseEditorGif::doneButton, 18);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("text editor closed", () -> findEditor() == null, 60);
        this.waitTicks(6);
        // Click the empty background, so the element shows without its selection frame
        this.moveTo(() -> new float[] {ELEMENT_X + ELEMENT_W + 60, ELEMENT_Y + ELEMENT_H + 70}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(60);
    }

    /**
     * Right-click the element, open its text, click behind the last line, open the placeholder menu of the text editor, open a category,
     * rest on a placeholder so its description shows, add it, type a bit behind it and click Done.<br>
     * Config: {@code initial_text}, {@code category} (the label of the category entry, like {@code Player}), {@code placeholder} (the label of
     * the placeholder entry, like {@code Player Name}) and {@code typed_after} (typed behind the placeholder, optional).
     */
    private void buildAddPlaceholderScript(@NotNull Minecraft minecraft) {
        // All text editors share the open state of the placeholder menu, so a recording before this one could have left it open
        writeStaticField(TextEditorWindowBody.class, "extendedPlaceholderMenu", false);
        this.openTextContentEditor(minecraft);
        this.moveTo(() -> lineEnd(-1), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(10);
        this.moveTo(() -> windowWidgetCenter(requireEditor(), "placeholderButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(16);
        String category = this.config.get("category").getAsString();
        this.moveTo(() -> placeholderMenuEntry(category), 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(14);
        String placeholder = this.config.get("placeholder").getAsString();
        this.moveTo(() -> placeholderMenuEntry(placeholder), 12);
        // Rest on the entry, so its description shows as tooltip
        this.waitTicks(36);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(18);
        if (this.config.has("typed_after")) {
            this.type(minecraft, this.config.get("typed_after").getAsString());
            this.waitTicks(16);
        }
        this.moveTo(DocsShowcaseEditorGif::doneButton, 18);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("text editor closed", () -> findEditor() == null, 60);
        this.waitTicks(6);
        // Click the empty background, so the element shows without its selection frame
        this.moveTo(() -> new float[] {ELEMENT_X + ELEMENT_W + 60, ELEMENT_Y + ELEMENT_H + 70}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(70);
    }

    /**
     * Right-click the element of the showcase and click Text Content in its right-click menu, which opens the text editor.
     */
    private void openTextContentEditor(@NotNull Minecraft minecraft) {
        this.waitTicks(16);
        this.moveTo(() -> new float[] {ELEMENT_X + 120, ELEMENT_Y + 34}, 18);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        // The entry sits below the visible part of the long menu, so scroll the menu like a player would
        this.moveTo(() -> new float[] {contextMenuEntry("set_text_content")[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "set_text_content");
        this.waitTicks(6);
        this.moveTo(() -> contextMenuEntry("set_text_content"), 10);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("text editor", () -> findEditor() != null, 60);
        this.waitTicks(14);
    }


    // ---- Steps ----

    private void runSteps() {
        while (!this.steps.isEmpty()) {
            Step step = this.steps.peekFirst();
            if (!step.run()) return;
            this.steps.pollFirst();
        }
    }

    private void waitTicks(int ticks) {
        int[] left = {ticks};
        this.steps.addLast(() -> left[0]-- <= 0);
    }

    private void waitUntil(@NotNull String name, @NotNull BooleanSupplier condition, int timeoutTicks) {
        int[] ticks = {0};
        this.steps.addLast(() -> {
            if (condition.getAsBoolean()) {
                this.pass("wait for " + name, "after " + ticks[0] + " ticks");
                return true;
            }
            if (++ticks[0] > timeoutTicks) throw new IllegalStateException("Timed out waiting for " + name);
            return false;
        });
    }

    /**
     * Moves the pointer along an eased, slightly curved path. The target gets resolved when the move starts.
     */
    private void moveTo(@NotNull Supplier<float[]> target, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        float[] state = new float[5]; // startX, startY, targetX, targetY, tick
        boolean[] started = {false};
        this.steps.addLast(() -> {
            if (!started[0]) {
                float[] resolved = target.get();
                state[0] = this.pointerX;
                state[1] = this.pointerY;
                state[2] = resolved[0];
                state[3] = resolved[1];
                started[0] = true;
            }
            state[4]++;
            float p = Math.min(1.0F, state[4] / ticks);
            float eased = (p < 0.5F) ? (4 * p * p * p) : (1 - (float)Math.pow(-2 * p + 2, 3) / 2);
            float dx = state[2] - state[0];
            float dy = state[3] - state[1];
            float bend = (float)Math.sin(Math.PI * eased) * 0.08F;
            float x = state[0] + dx * eased - dy * bend;
            float y = state[1] + dy * eased + dx * bend;
            this.movePointer(minecraft, x, y);
            return p >= 1.0F;
        });
    }

    private void press(@NotNull Minecraft minecraft, int button) {
        this.press(minecraft, button, 0);
    }

    /**
     * Presses the button with modifier keys held, like {@link InputQuirks#EDIT_SHORTCUT_KEY_MODIFIER} for Ctrl (Cmd on macOS).
     * Only the press carries them, so this is for what the editor decides when the button goes down, like dragging several elements.
     */
    private void press(@NotNull Minecraft minecraft, int button, int modifiers) {
        this.steps.addLast(() -> {
            minecraft.mouseHandler.onButton(minecraft.getWindow().handle(), new MouseButtonInfo(button, modifiers), InputConstants.PRESS);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) this.leftDown = true; else this.rightDown = true;
            this.pendingEvents.add(String.format(Locale.ROOT, "{\"type\":\"press\",\"button\":\"%s\",\"x\":%.2f,\"y\":%.2f}", (button == InputConstants.MOUSE_BUTTON_LEFT) ? "left" : "right", this.outX(this.pointerX), this.outY(this.pointerY)));
            return true;
        });
    }

    private void release(@NotNull Minecraft minecraft, int button) {
        this.steps.addLast(() -> {
            minecraft.mouseHandler.onButton(minecraft.getWindow().handle(), new MouseButtonInfo(button, 0), InputConstants.RELEASE);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) this.leftDown = false; else this.rightDown = false;
            return true;
        });
    }

    private void click(@NotNull Minecraft minecraft, int button) {
        this.press(minecraft, button);
        this.waitTicks(1);
        this.release(minecraft, button);
    }

    /**
     * Drags the pointer over the word while the left button is held, and selects the part of the word the pointer passed.<br>
     * The text editor checks the real mouse button state of SDL while it selects by dragging, which simulated input can't set,
     * so this applies the same selection a real drag would make through the editor's own selection method.
     */
    private void dragSelectWord(@NotNull Minecraft minecraft, @NotNull String word, int ticks) {
        float[] state = new float[5]; // startX, startY, endX, endY, tick
        int[] offsets = {-1, -1};
        this.steps.addLast(() -> {
            PiPWindow window = requireEditor();
            TextEditorWindowBody body = (TextEditorWindowBody) window.getScreen();
            if (offsets[0] < 0) {
                float[] start = wordEdge(word, false);
                float[] end = wordEdge(word, true);
                System.arraycopy(start, 0, state, 0, 2);
                System.arraycopy(end, 0, state, 2, 2);
                int offset = 0;
                for (int i = 0; i < body.getLineCount(); i++) {
                    int index = body.getLine(i).getValue().indexOf(word);
                    if (index >= 0) {
                        offsets[0] = offset + index;
                        break;
                    }
                    offset += body.getLine(i).getValue().length() + 1;
                }
                offsets[1] = offsets[0] + word.length();
            }
            state[4]++;
            float p = Math.min(1.0F, state[4] / ticks);
            float eased = 1 - (1 - p) * (1 - p);
            this.movePointer(minecraft, state[0] + (state[2] - state[0]) * eased, state[1] + (state[3] - state[1]) * eased);
            int selectedEnd = offsets[0] + Math.round(word.length() * eased);
            invoke(body, "selectBetweenOffsets", new Class<?>[] {int.class, int.class}, offsets[0], selectedEnd);
            return p >= 1.0F;
        });
    }

    /**
     * Turns the mouse wheel over the open context menu every second tick (once per frame) until the entry is above {@link #MENU_SCROLL_TARGET_Y}.
     */
    private void scrollMenuUntilVisible(@NotNull Minecraft minecraft, @NotNull String identifier) {
        this.scrollMenuUntilVisible(minecraft, identifier, MENU_SCROLL_TARGET_Y);
    }

    /**
     * Like {@link #scrollMenuUntilVisible(Minecraft, String)}, until the entry is above the given GUI Y. Menus that end above
     * {@link #MENU_SCROLL_TARGET_Y}, like the dropdowns of the menu bar, need a smaller one.
     */
    private void scrollMenuUntilVisible(@NotNull Minecraft minecraft, @NotNull String identifier, float targetY) {
        int[] cooldown = {0};
        int[] turns = {0};
        this.steps.addLast(() -> {
            if (cooldown[0]-- > 0) return false;
            if (contextMenuEntry(identifier)[1] <= targetY) {
                this.pass("scroll menu to " + identifier, turns[0] + " wheel turns");
                return true;
            }
            if (++turns[0] > 40) throw new IllegalStateException("Scrolling never reached " + identifier);
            minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0.0, -1.0);
            cooldown[0] = 1;
            return false;
        });
    }

    /**
     * Turns the mouse wheel once at the pointer. Negative turns scroll down.
     */
    private void wheel(@NotNull Minecraft minecraft, double turns) {
        this.steps.addLast(() -> {
            minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0.0, turns);
            return true;
        });
    }

    /**
     * Shows a key badge from the next frame on, for input that is no key press of its own, like a modifier held during a drag.
     */
    private void badge(@NotNull String text) {
        this.steps.addLast(() -> {
            this.pendingEvents.add("{\"type\":\"badge\",\"text\":\"" + text + "\"}");
            return true;
        });
    }

    private void key(@NotNull Minecraft minecraft, @NotNull String keySpec, @Nullable String badge) {
        KeyEvent event = WindowPreview.parseKey(keySpec);
        if (event == null) throw new IllegalArgumentException("Unknown key " + keySpec);
        this.steps.addLast(() -> {
            minecraft.keyboardHandler.keyPress(minecraft.getWindow().handle(), InputConstants.PRESS, event);
            if (badge != null) this.pendingEvents.add("{\"type\":\"badge\",\"text\":\"" + badge + "\"}");
            return true;
        });
        this.waitTicks(1);
        this.steps.addLast(() -> {
            minecraft.keyboardHandler.keyPress(minecraft.getWindow().handle(), InputConstants.RELEASE, event);
            return true;
        });
    }

    /**
     * Types one character per one or two ticks, which reads like quick typing at 10 frames per second.
     */
    private void type(@NotNull Minecraft minecraft, @NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            this.steps.addLast(() -> {
                minecraft.keyboardHandler.textInput(minecraft.getWindow().handle(), character);
                return true;
            });
            // A wait of one tick ends the tick, so every character gets its own tick
            this.waitTicks((i % 3 == 2) ? 2 : 1);
        }
    }

    /**
     * Types three characters per two ticks, for longer texts where {@link #type(Minecraft, String)} would take too long.
     */
    private void typeQuickly(@NotNull Minecraft minecraft, @NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            this.steps.addLast(() -> {
                minecraft.keyboardHandler.textInput(minecraft.getWindow().handle(), character);
                return true;
            });
            if ((i % 3) != 1) this.waitTicks(1);
        }
    }

    // ---- Targets ----

    @NotNull
    private static float[] contextMenuEntry(@NotNull String identifier) {
        ContextMenu menu = ContextMenuHandler.INSTANCE.getCurrent();
        if (menu == null) throw new IllegalStateException("No open context menu");
        ContextMenu.ContextMenuEntry<?> entry = menu.getEntry(identifier);
        if (entry == null) throw new IllegalStateException("No context menu entry " + identifier);
        float scale = UIBase.calculateFixedRenderScale(menu.getScale());
        float x = readFloat(entry, "x");
        float y = readFloat(entry, "y");
        float width = readFloat(entry, "width");
        return new float[] {(x + width * 0.3F) * scale, (y + entry.getHeight() / 2.0F) * scale};
    }

    /**
     * The GUI position of an entry of the open context menu or one of its sub-menus. The path holds the identifiers of the
     * sub-menu entries that lead to the entry, then the entry itself.
     */
    @NotNull
    private static float[] menuEntry(@NotNull String... path) {
        ContextMenu menu = (path.length > 1) ? subMenu(Arrays.copyOf(path, path.length - 1)) : ContextMenuHandler.INSTANCE.getCurrent();
        if (menu == null) throw new IllegalStateException("No open context menu");
        ContextMenu.ContextMenuEntry<?> entry = menu.getEntry(path[path.length - 1]);
        if (entry == null) throw new IllegalStateException("No context menu entry " + String.join(" -> ", path));
        float scale = UIBase.calculateFixedRenderScale(menu.getScale());
        return new float[] {(readFloat(entry, "x") + readFloat(entry, "width") * 0.3F) * scale, (readFloat(entry, "y") + entry.getHeight() / 2.0F) * scale};
    }

    /**
     * The sub-menu the given path of sub-menu entries of the open context menu leads to.
     */
    @NotNull
    private static ContextMenu subMenu(@NotNull String... path) {
        ContextMenu menu = ContextMenuHandler.INSTANCE.getCurrent();
        if (menu == null) throw new IllegalStateException("No open context menu");
        for (String identifier : path) {
            if (!(menu.getEntry(identifier) instanceof ContextMenu.SubMenuContextMenuEntry entry)) throw new IllegalStateException("No sub-menu entry " + identifier);
            menu = entry.getSubContextMenu();
        }
        return menu;
    }

    private void clickMenuEntry(@NotNull Minecraft minecraft, @NotNull String... path) {
        this.moveTo(() -> menuEntry(path), 12);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
    }

    @NotNull
    private static AbstractEditorElement<?, ?> requireEditorElement(@NotNull String instanceIdentifier) {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        if (editor == null) throw new IllegalStateException("The layout editor is not open");
        AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(instanceIdentifier);
        if (element == null) throw new IllegalStateException("No element " + instanceIdentifier);
        return element;
    }

    /**
     * X, Y, width and height of the element in the layout editor, in GUI pixels.
     */
    @NotNull
    private static float[] editorBox(@NotNull String instanceIdentifier) {
        AbstractEditorElement<?, ?> element = requireEditorElement(instanceIdentifier);
        return new float[] {element.getX(), element.getY(), element.getWidth(), element.getHeight()};
    }

    @NotNull
    private static float[] center(@NotNull float[] box) {
        return new float[] {box[0] + box[2] / 2.0F, box[1] + box[3] / 2.0F};
    }

    /**
     * X, Y, width and height of the element on the open screen outside the editor, in GUI pixels.
     */
    @NotNull
    private static float[] screenBox(@NotNull String instanceIdentifier) {
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (layer == null) throw new IllegalStateException("The screen has no customization layer");
        AbstractElement element = layer.getElementByInstanceIdentifier(instanceIdentifier);
        if (element == null) throw new IllegalStateException("No element " + instanceIdentifier + " on the screen");
        return new float[] {element.getAbsoluteX(), element.getAbsoluteY(), element.getAbsoluteWidth(), element.getAbsoluteHeight()};
    }

    @Nullable
    private static PiPWindow findEditor() {
        return findWindow(TextEditorWindowBody.class);
    }

    /**
     * The open window whose body is of the type, like {@code ActionScriptEditorWindowBody.class}. Hidden windows count too: windows hide
     * while a window they opened is on top of them.
     */
    @Nullable
    private static PiPWindow findWindow(@NotNull Class<?> bodyType) {
        for (PiPWindow window : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            if (bodyType.isInstance(window.getScreen())) return window;
        }
        return null;
    }

    @NotNull
    private static PiPWindow requireWindow(@NotNull Class<?> bodyType) {
        PiPWindow window = findWindow(bodyType);
        if (window == null) throw new IllegalStateException("No open window with a " + bodyType.getSimpleName());
        return window;
    }

    /**
     * The visible cell window, like the value window of an action. Windows that opened it are cell windows too sometimes, like Manage Listeners,
     * but they hide while it's open.
     */
    @Nullable
    private static PiPWindow findValueWindow() {
        for (PiPWindow window : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            if ((window.getScreen() instanceof PiPCellWindowBody) && window.isVisible()) return window;
        }
        return null;
    }

    @NotNull
    private static PiPWindow requireValueWindow() {
        PiPWindow window = findValueWindow();
        if (window == null) throw new IllegalStateException("No visible cell window");
        return window;
    }

    /**
     * The GUI position of the center of a widget the window body keeps in a field, like {@code doneButton}.
     */
    @NotNull
    private static float[] windowWidgetCenter(@NotNull PiPWindow window, @NotNull String fieldName) {
        AbstractWidget widget = (AbstractWidget) readField(window.getScreen(), fieldName);
        return bodyToGui(window, widget.getX() + widget.getWidth() / 2.0F, widget.getY() + widget.getHeight() / 2.0F);
    }

    /**
     * The GUI position of the center of the window body's button with the label of the localization key, for buttons the body keeps in no field.
     */
    @NotNull
    private static float[] windowButtonCenter(@NotNull PiPWindow window, @NotNull String labelKey) {
        String label = Component.translatable(labelKey).getString();
        for (GuiEventListener child : window.getScreen().children()) {
            if ((child instanceof AbstractWidget widget) && widget.getMessage().getString().equals(label)) {
                return bodyToGui(window, widget.getX() + widget.getWidth() / 2.0F, widget.getY() + widget.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("No button " + label + " in " + window.getScreen().getClass().getSimpleName());
    }

    /**
     * The GUI position of the entry of an action in the list of the action chooser, a bit right of the start of its name.
     */
    @NotNull
    private static float[] chooserEntry(@NotNull String actionIdentifier) {
        PiPWindow window = requireWindow(ChooseActionWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "actionsListScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof ChooseActionWindowBody.ActionScrollEntry actionEntry) && actionEntry.action.getIdentifier().equals(actionIdentifier)) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 2.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The action chooser lists no " + actionIdentifier);
    }

    /**
     * The GUI position of the text field that follows the label of the localization key in the open cell window, like the value window of an action.
     */
    @NotNull
    private static float[] cellInputAfterLabel(@NotNull String labelKey) {
        PiPWindow window = requireValueWindow();
        String label = Component.translatable(labelKey).getString();
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) readField(window.getScreen(), "allCells");
        boolean labelFound = false;
        for (Object cell : cells) {
            if ((cell instanceof CellScreen.LabelCell labelCell) && ((Component) readField(labelCell, "text")).getString().equals(label)) {
                labelFound = true;
            } else if (labelFound && (cell instanceof CellScreen.TextInputCell inputCell)) {
                ExtendedEditBox box = inputCell.editBox;
                return bodyToGui(window, box.getX() + Math.min(40.0F, box.getWidth() / 2.0F), box.getY() + box.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("No text field after the label " + label);
    }

    /**
     * The GUI position of the widget of the open cell window whose label starts with the text of the localization key, like the cycle
     * button "Arrangement: %s" (the part before its value counts).
     */
    @NotNull
    private static float[] cellWidgetWithLabel(@NotNull String labelKey) {
        PiPWindow window = requireValueWindow();
        String prefix = Component.translatable(labelKey, "").getString();
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) readField(window.getScreen(), "allCells");
        for (Object cell : cells) {
            if ((cell instanceof CellScreen.WidgetCell widgetCell) && widgetCell.widget.getMessage().getString().startsWith(prefix)) {
                AbstractWidget widget = widgetCell.widget;
                return bodyToGui(window, widget.getX() + widget.getWidth() / 2.0F, widget.getY() + widget.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("No widget with the label " + prefix);
    }

    /**
     * The GUI position of the middle of an entry of FancyMenu's menu bar (outside the editor), like {@code screen} for Customization.
     */
    @NotNull
    private static float[] menuBarEntry(@NotNull String identifier) {
        MenuBar bar = CustomizationOverlay.getCurrentMenuBarInstance();
        if (bar == null) throw new IllegalStateException("There is no menu bar");
        MenuBar.MenuBarEntry entry = bar.getEntry(identifier);
        if (entry == null) throw new IllegalStateException("No menu bar entry " + identifier);
        float scale = MenuBar.getRenderScale();
        float x = readFloat(entry, "x");
        int width = (int) invoke(entry, "getWidth");
        return new float[] {(x + width / 2.0F) * scale, (MenuBar.PIXEL_SIZE / 2.0F) * scale};
    }

    /**
     * The GUI position of the row of a variable in the Manage Variables window.
     */
    @NotNull
    private static float[] variableRow(@NotNull String name) {
        PiPWindow window = requireWindow(ManageVariablesWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "variableListScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof ManageVariablesWindowBody.VariableScrollEntry row) && row.getVariableName().equals(name)) {
                return bodyToGui(window, entry.getX() + Math.min(70.0F, entry.getWidth() / 3.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("Manage Variables lists no " + name);
    }

    /**
     * The GUI position of the entry of a listener type in the list of the listener chooser, a bit right of the start of its name.
     */
    @NotNull
    private static float[] listenerChooserEntry(@NotNull String listenerIdentifier) {
        PiPWindow window = requireWindow(ChooseListenerTypeScreen.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "listenersScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof ChooseListenerTypeScreen.ListenerScrollEntry listenerEntry) && (listenerEntry.listener != null) && listenerEntry.listener.getIdentifier().equals(listenerIdentifier)) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 2.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The listener chooser lists no " + listenerIdentifier);
    }

    /**
     * The GUI position of an empty spot of the list of the listener chooser, below its entries.
     */
    @NotNull
    private static float[] listenerChooserFreeSpot() {
        PiPWindow window = requireWindow(ChooseListenerTypeScreen.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "listenersScrollArea");
        return bodyToGui(window, list.getXWithBorder() + list.getWidthWithBorder() * 0.5F, list.getYWithBorder() + list.getHeightWithBorder() * 0.85F);
    }

    /**
     * The GUI position of the name of a listener in the Manage Listeners window, found by its display name or its type identifier.
     */
    @NotNull
    private static float[] listenerCell(@NotNull String nameOrType) {
        PiPWindow window = requireWindow(ManageListenersWindowBody.class);
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) readField(window.getScreen(), "allCells");
        for (Object cell : cells) {
            if (cell instanceof ManageListenersWindowBody.ListenerInstanceCell listenerCell) {
                ListenerInstance instance = (ListenerInstance) readField(listenerCell, "instance");
                if (nameOrType.equals(instance.getDisplayName()) || ((instance.getDisplayName() == null) && instance.parent.getIdentifier().equals(nameOrType))) {
                    return bodyToGui(window, listenerCell.getX() + Math.min(40.0F, listenerCell.getWidth() / 2.0F), listenerCell.getY() + listenerCell.getHeight() / 2.0F);
                }
            }
        }
        throw new IllegalStateException("Manage Listeners lists no " + nameOrType);
    }

    /**
     * The GUI position of the middle of the description area of a cell window, like Manage Listeners.
     */
    @NotNull
    private static float[] descriptionAreaCenter(@NotNull PiPWindow window) {
        ScrollArea area = (ScrollArea) readField(window.getScreen(), "descriptionScrollArea");
        return bodyToGui(window, area.getXWithBorder() + area.getWidthWithBorder() / 2.0F, area.getYWithBorder() + area.getHeightWithBorder() / 2.0F);
    }

    /**
     * The GUI position of an empty spot of the list of the Manage Listeners window, below its rows.
     */
    @NotNull
    private static float[] listenerListFreeSpot() {
        PiPWindow window = requireWindow(ManageListenersWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "scrollArea");
        return bodyToGui(window, list.getXWithBorder() + list.getWidthWithBorder() * 0.5F, list.getYWithBorder() + list.getHeightWithBorder() * 0.8F);
    }

    /**
     * The GUI position of a free spot of the list of the Manage Variables window, below its rows.
     */
    @NotNull
    private static float[] variablesListFreeSpot() {
        PiPWindow window = requireWindow(ManageVariablesWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "variableListScrollArea");
        return bodyToGui(window, list.getXWithBorder() + list.getWidthWithBorder() * 0.45F, list.getYWithBorder() + list.getHeightWithBorder() * 0.62F);
    }

    /**
     * The GUI position of the Add Entry row at the end of the list editor.
     */
    @NotNull
    private static float[] addEntryRow() {
        PiPWindow window = requireWindow(ListVariableEditorWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "entriesScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if (entry.getClass().getSimpleName().equals("AddEntryRow")) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 3.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The list editor has no Add Entry row");
    }

    @NotNull
    private static float[] bodyToGui(@NotNull PiPWindow window, float localX, float localY) {
        double scale = (double) invoke(window, "getScreenRenderScaleFactor");
        return new float[] {(float)(window.getBodyX() + localX * scale), (float)(window.getBodyY() + localY * scale)};
    }

    /**
     * The GUI position of the start or end of the first occurrence of the word in the editor.
     */
    @NotNull
    private static float[] wordEdge(@NotNull String word, boolean end) {
        PiPWindow window = requireEditor();
        TextEditorWindowBody body = (TextEditorWindowBody) window.getScreen();
        for (int i = 0; i < body.getLineCount(); i++) {
            int index = body.getLine(i).getValue().indexOf(word);
            if (index >= 0) return charPosition(window, body.getLine(i), end ? (index + word.length()) : index);
        }
        throw new IllegalStateException("The editor has no word " + word);
    }

    /**
     * The GUI position a bit behind the end of a line. Negative indexes count from the last line.
     */
    @NotNull
    private static float[] lineEnd(int lineIndex) {
        PiPWindow window = requireEditor();
        TextEditorWindowBody body = (TextEditorWindowBody) window.getScreen();
        TextEditorLine line = body.getLine((lineIndex < 0) ? (body.getLineCount() + lineIndex) : lineIndex);
        float[] position = charPosition(window, line, line.getValue().length());
        position[0] += 6;
        return position;
    }

    @NotNull
    private static float[] charPosition(@NotNull PiPWindow window, @NotNull TextEditorLine line, int charIndex) {
        int displayPos = Math.max(0, line.getAsAccessor().getDisplayPosFancyMenu());
        float textX = line.getAsAccessor().getBorderedFancyMenu() ? (line.getX() + 4.0F) : (line.getX() + 1.0F);
        String before = line.getValue().substring(Math.min(displayPos, charIndex), charIndex);
        float localX = textX + UIBase.getUITextWidthNormal(Component.literal(before));
        float localY = line.getY() + line.getHeight() / 2.0F;
        return bodyToGui(window, localX, localY);
    }

    @NotNull
    private static float[] doneButton() {
        PiPWindow window = requireEditor();
        AbstractWidget button = (AbstractWidget) readField(window.getScreen(), "doneButton");
        return bodyToGui(window, button.getX() + button.getWidth() / 2.0F, button.getY() + button.getHeight() / 2.0F);
    }

    /**
     * The GUI position of an entry of the placeholder menu of the open text editor, a bit right of the start of its label: a category
     * (like {@code Player}), a placeholder (like {@code Player Name}) or BACK.
     */
    @NotNull
    private static float[] placeholderMenuEntry(@NotNull String label) {
        PiPWindow window = requireEditor();
        @SuppressWarnings("unchecked")
        List<TextEditorWindowBody.PlaceholderMenuEntry> entries = (List<TextEditorWindowBody.PlaceholderMenuEntry>) readField(window.getScreen(), "placeholderMenuEntries");
        for (TextEditorWindowBody.PlaceholderMenuEntry entry : entries) {
            if (entry.label.getString().equals(label)) {
                return bodyToGui(window, entry.x + Math.min(40.0F, entry.getWidth() / 2.0F), entry.y + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The placeholder menu lists no " + label);
    }

    private static void writeStaticField(@NotNull Class<?> type, @NotNull String name, @Nullable Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception ex) {
            throw new IllegalStateException("Can't write " + type.getSimpleName() + "." + name, ex);
        }
    }

    @NotNull
    private static PiPWindow requireEditor() {
        PiPWindow window = findEditor();
        if (window == null) throw new IllegalStateException("The text editor is not open");
        return window;
    }

    // ---- Input and frames ----

    private void movePointer(@NotNull Minecraft minecraft, float guiX, float guiY) {
        this.pointerX = guiX;
        this.pointerY = guiY;
        Window window = minecraft.getWindow();
        double x = guiX * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = guiY * window.getScreenHeight() / window.getGuiScaledHeight();
        minecraft.mouseHandler.onMove(window.handle(), x, y, 0.0, 0.0);
    }

    private void captureFrame(@NotNull Minecraft minecraft) {
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        int factor = Math.max(1, target.width / OUT_WIDTH);
        int index = this.frameIndex++;
        Path file = this.framesDirectory.resolve(String.format(Locale.ROOT, "f_%05d.png", index));
        int shape = CursorHandler.getActiveStandardCursorShape();
        String cursor = (shape == SDLMouse.SDL_SYSTEM_CURSOR_TEXT) ? "text" : ((shape == SDLMouse.SDL_SYSTEM_CURSOR_POINTER) ? "hand" : "arrow");
        this.frameLines.add(String.format(Locale.ROOT, "{\"frame\":%d,\"x\":%.2f,\"y\":%.2f,\"cursor\":\"%s\",\"left\":%b,\"right\":%b,\"events\":[%s]}",
                index, this.outX(this.pointerX), this.outY(this.pointerY), cursor, this.leftDown, this.rightDown, String.join(",", this.pendingEvents)));
        this.pendingEvents.clear();
        this.pendingWrites.incrementAndGet();
        Screenshot.takeScreenshot(target, factor, image -> this.writers.execute(() -> this.writeFrame(image, file)));
    }

    private void writeFrame(@NotNull NativeImage image, @NotNull Path file) {
        try (image) {
            image.writeToFile(file);
        } catch (Exception ex) {
            this.fail("frame", "can't write " + file + ": " + ex.getMessage());
        } finally {
            this.pendingWrites.decrementAndGet();
        }
    }

    private float outX(float guiX) {
        return guiX * OUT_WIDTH / GUI_WIDTH;
    }

    private float outY(float guiY) {
        return guiY * OUT_WIDTH / GUI_WIDTH;
    }

    // ---- Reflection ----

    private static float readFloat(@NotNull Object object, @NotNull String name) {
        return ((Number) readField(object, name)).floatValue();
    }

    @NotNull
    private static Object readField(@NotNull Object object, @NotNull String name) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No field " + name + " in " + object.getClass());
    }

    @NotNull
    private static Object invoke(@NotNull Object object, @NotNull String name) {
        return invoke(object, name, new Class<?>[0]);
    }

    private static Object invoke(@NotNull Object object, @NotNull String name, @NotNull Class<?>[] parameterTypes, Object... arguments) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method.invoke(object, arguments);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No method " + name + " in " + object.getClass());
    }

    // ---- Lifecycle ----

    private void finish() {
        if (this.phase == Phase.DONE) return;
        this.phase = Phase.DONE;
        LayoutHandler.setPreviewLayout(null);
        if (this.menuBarControllerId != null) CustomizationOverlay.unregisterOverlayVisibilityController(this.menuBarControllerId);
        // Listeners of the showcase would run in later recordings of the instance, like a toast on every key press
        if (this.savedListeners != null) ListenerHandler.replaceInstances(this.savedListeners);
        this.writers.shutdown();
        EventHandler.INSTANCE.unregisterListenersOf(this);
        LOGGER.info("[CHECK] docs gif finished: {} frames={}", (this.failures.get() == 0) ? "PASS" : ("FAIL " + this.failures.get()), this.frameIndex);
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

    @FunctionalInterface
    private interface Step {
        boolean run();
    }

    private enum Phase {
        WAIT_FOR_GAME,
        OPEN_EDITOR,
        WAIT_FOR_EDITOR,
        RUN,
        WRITE,
        DONE
    }

}
