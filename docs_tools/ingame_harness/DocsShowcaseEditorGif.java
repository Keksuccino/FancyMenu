package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.google.gson.JsonElement;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.AbstractFileBrowserWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import java.io.File;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;
import de.keksuccino.fancymenu.customization.listener.gui.ChooseListenerTypeScreen;
import de.keksuccino.fancymenu.customization.listener.gui.ManageListenersWindowBody;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.ui.BuildRequirementWindowBody;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsWindowBody;
import de.keksuccino.fancymenu.customization.action.ui.ChooseActionWindowBody;
import de.keksuccino.fancymenu.customization.customgui.CustomGui;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.customgui.BuildCustomGuiScreen;
import de.keksuccino.fancymenu.customization.customgui.ManageCustomGuisScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElement;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer.LayerLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.customization.scheduler.gui.ManageSchedulerSettingsScreen;
import de.keksuccino.fancymenu.customization.scheduler.gui.ManageSchedulersWindowBody;
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
import de.keksuccino.fancymenu.util.rendering.ui.screen.NumberPickerWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import de.keksuccino.fancymenu.util.AbstractOptions;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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
    /**
     * The schedulers of the instance before the config's {@code schedulers} replaced them. They come back when the showcase ends.
     */
    @Nullable
    private List<SchedulerInstance> savedSchedulers;
    /**
     * The Custom GUIs of the instance before the config's {@code custom_guis} replaced them. They come back when the showcase ends.
     */
    @Nullable
    private List<CustomGui> savedCustomGuis;
    /**
     * The layouts of the config's {@code custom_guis}. They only live in memory and get removed when the showcase ends.
     */
    private final List<Layout> customGuiLayouts = new ArrayList<>();
    /**
     * Put the FancyMenu options of the config's {@code fancymenu_options} back to what they were before the showcase.
     */
    private final List<Runnable> optionRestorers = new ArrayList<>();
    /**
     * The layout the showcase got built with, for scripts that save it, like {@code write_profile}.
     */
    @Nullable
    private Layout builtLayout;

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
                // Config window_height: a taller window for stills of windows whose parts are too small at 720, like the description of
                // Manage Schedulers. Only for stills: compose.py and encode.sh expect 1280x720 frames.
                // window_width/window_height/gui_scale: other window sizes, for stills that check how a layout fits them. GIFs need 1280x720.
                window.setWindowed(this.config.has("window_width") ? this.config.get("window_width").getAsInt() : 1280, this.config.has("window_height") ? this.config.get("window_height").getAsInt() : 720);
                minecraft.options.guiScale().set(Math.round((this.config.has("gui_scale") ? this.config.get("gui_scale").getAsInt() : 2) * window.getPixelDensity()));
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
                    // The Layers widget grows over the element at big UI scales and isn't part of what the showcase is about,
                    // unless the config wants it (show_widgets), like for an overview of the editor
                    boolean showWidgets = this.config.has("show_widgets") && this.config.get("show_widgets").getAsBoolean();
                    if (!showWidgets) ((LayoutEditorScreen) ScreenUtils.getScreen()).layoutEditorWidgets.forEach(widget -> widget.setVisible(false));
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
        if (this.config.has("fancymenu_options")) this.setUpFancyMenuOptions(this.config.getAsJsonObject("fancymenu_options"));
        if (this.config.has("variables")) setUpVariables(this.config.getAsJsonObject("variables"));
        if (this.config.has("shuffle_seed")) seedListShuffle(this.config.get("shuffle_seed").getAsLong());
        if (this.config.has("listeners")) this.setUpListeners(this.config.getAsJsonArray("listeners"));
        // After the variables and the shuffle seed, because running schedulers can change variables right away
        if (this.config.has("schedulers")) this.setUpSchedulers(this.config.getAsJsonArray("schedulers"));
        if (this.config.has("custom_guis")) this.setUpCustomGuis(this.config.getAsJsonArray("custom_guis"));
        Layout layout = Layout.buildForScreen(screen);
        this.builtLayout = layout;
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
        // elements_behind_vanilla: the layout's Render Custom Elements Behind Vanilla, like for full-screen image layers
        if (this.config.has("elements_behind_vanilla")) layout.renderElementsBehindVanilla = this.config.get("elements_behind_vanilla").getAsBoolean();
        // A seed makes overlays build the same random setup every run, like in scenes
        if (this.config.has("seed")) {
            PixelRandom.setSeedOverride(this.config.get("seed").getAsInt());
            this.optionRestorers.add(() -> PixelRandom.setSeedOverride(null));
        }
        if (this.config.has("overlays")) DocsShowcaseElements.addOverlaysTo(layout, this.config.getAsJsonArray("overlays"));
        // customization: turns Current Screen Customization of the screen on or off until the showcase ends, like for making a first layout
        if (this.config.has("customization")) {
            boolean old = ScreenCustomization.isCustomizationEnabledForScreen(screen);
            ScreenCustomization.setCustomizationForScreenEnabled(screen, this.config.get("customization").getAsBoolean());
            this.optionRestorers.add(() -> ScreenCustomization.setCustomizationForScreenEnabled(screen, old));
        }
        if (this.startsOnScreen()) {
            boolean menuBar = this.config.has("menu_bar") && this.config.get("menu_bar").getAsBoolean();
            if (!menuBar) this.menuBarControllerId = CustomizationOverlay.registerOverlayVisibilityController(s -> false);
            this.startScreen = screen;
            // no_layout: the screen as it is without layouts, like for Global Customizations, which work without one
            boolean noLayout = this.config.has("no_layout") && this.config.get("no_layout").getAsBoolean();
            if (!noLayout) LayoutHandler.setPreviewLayout(layout);
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
            case "manage_schedulers" -> this.buildManageSchedulersScript(minecraft);
            case "schedulers_still" -> this.buildSchedulersStillScript(minecraft);
            case "click_through" -> this.buildClickThroughScript(minecraft);
            case "manage_custom_guis" -> this.buildManageCustomGuisScript(minecraft);
            case "press_keys" -> this.buildPressKeysScript(minecraft);
            case "add_element" -> this.buildAddElementScript(minecraft);
            case "add_placeholder" -> this.buildAddPlaceholderScript(minecraft);
            case "manage_requirements" -> this.buildManageRequirementsScript(minecraft);
            case "hover_elements" -> this.buildHoverElementsScript(minecraft);
            case "editor_basics" -> this.buildEditorBasicsScript(minecraft);
            case "layers_widget" -> this.buildLayersWidgetScript(minecraft);
            case "rotate_and_tilt" -> this.buildRotateAndTiltScript(minecraft);
            case "context_entry_still" -> this.buildContextEntryStillScript(minecraft);
            case "screen_tour" -> this.buildScreenTourScript(minecraft);
            case "first_layout" -> this.buildFirstLayoutScript(minecraft);
            case "menu_background" -> this.buildMenuBackgroundScript(minecraft);
            case "panorama_background" -> this.buildPanoramaBackgroundScript(minecraft);
            case "slideshow_background" -> this.buildSlideshowBackgroundScript(minecraft);
            case "add_image" -> this.buildAddImageScript(minecraft);
            case "save_layout" -> this.buildSaveLayoutScript(minecraft);
            case "button_textures" -> this.buildButtonTexturesScript(minecraft);
            case "write_profile" -> this.buildWriteProfileScript(minecraft);
            case "element_settings" -> this.buildElementSettingsScript(minecraft);
            case "menu_path_still" -> this.buildMenuPathStillScript(minecraft);
            case "game_intro" -> this.buildGameIntroScript(minecraft);
            default -> throw new IllegalArgumentException("Unknown script " + script);
        }
    }

    /**
     * Sets the FancyMenu options of the config's {@code fancymenu_options} until the showcase ends, by their key in FancyMenu's
     * {@code options.txt}, like {@code "global_button_label_hover_color": "#FFD35C"} or {@code "global_button_background_nine_slice": true}.
     * The Global Customizations are such options.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setUpFancyMenuOptions(@NotNull JsonObject options) {
        Map<String, AbstractOptions.Option> byKey = new HashMap<>();
        try {
            for (Field field : FancyMenu.getOptions().getClass().getFields()) {
                if (AbstractOptions.Option.class.isAssignableFrom(field.getType())) {
                    AbstractOptions.Option option = (AbstractOptions.Option) field.get(FancyMenu.getOptions());
                    byKey.put(option.getKey(), option);
                }
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Can't read FancyMenu's options", ex);
        }
        for (Map.Entry<String, JsonElement> entry : options.entrySet()) {
            AbstractOptions.Option option = byKey.get(entry.getKey());
            if (option == null) throw new IllegalStateException("No FancyMenu option " + entry.getKey());
            Object old = option.getValue();
            Object defaultValue = option.getDefaultValue();
            JsonElement value = entry.getValue();
            Object parsed;
            if (defaultValue instanceof Boolean) parsed = value.getAsBoolean();
            else if (defaultValue instanceof Integer) parsed = value.getAsInt();
            else if (defaultValue instanceof Float) parsed = value.getAsFloat();
            else if (defaultValue instanceof Double) parsed = value.getAsDouble();
            else if (defaultValue instanceof Long) parsed = value.getAsLong();
            else parsed = value.getAsString();
            option.setValue(parsed);
            this.optionRestorers.add(() -> option.setValue(old));
            this.pass("option " + entry.getKey(), String.valueOf(parsed));
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
            if (data.has("script")) DocsShowcaseElements.addScriptEntries(script, data.getAsJsonArray("script"));
            // Setting the script gives every part of it the $$ values of the listener
            instance.setActionScript(script);
            instances.add(instance);
        }
        ListenerHandler.replaceInstances(instances);
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
     * Replaces the schedulers of the instance with the config's {@code schedulers} until the showcase ends. An empty array leaves no schedulers.<br>
     * Keys of a scheduler: {@code id}, {@code start_delay}, {@code tick_delay}, {@code ticks_to_run} (all optional, in ms and runs),
     * {@code start_on_launch} (optional), {@code script} (actions and IF statements, like the {@code script} of {@link #setUpListeners}) and
     * {@code running} (true starts it right away).
     */
    private void setUpSchedulers(@NotNull Iterable<JsonElement> schedulers) {
        if (this.savedSchedulers == null) this.savedSchedulers = SchedulerHandler.getInstances();
        SchedulerHandler.stopAllSchedulers();
        List<SchedulerInstance> instances = new ArrayList<>();
        List<String> running = new ArrayList<>();
        for (JsonElement entry : schedulers) {
            JsonObject data = entry.getAsJsonObject();
            SchedulerInstance instance = new SchedulerInstance(data.get("id").getAsString());
            if (data.has("start_delay")) instance.setStartDelayMs(data.get("start_delay").getAsLong());
            if (data.has("tick_delay")) instance.setTickDelayMs(data.get("tick_delay").getAsLong());
            if (data.has("ticks_to_run")) instance.setTicksToRun(data.get("ticks_to_run").getAsLong());
            if (data.has("start_on_launch")) instance.setStartOnLaunch(data.get("start_on_launch").getAsBoolean());
            GenericExecutableBlock script = new GenericExecutableBlock();
            if (data.has("script")) DocsShowcaseElements.addScriptEntries(script, data.getAsJsonArray("script"));
            instance.setActionScript(script);
            instances.add(instance);
            if (data.has("running") && data.get("running").getAsBoolean()) running.add(instance.getIdentifier());
        }
        SchedulerHandler.applyChanges(instances);
        running.forEach(SchedulerHandler::startScheduler);
    }

    /**
     * Outside the editor: makes a scheduler in Manage Schedulers, in two parts (config {@code part}), so each GIF stays short.<br>
     * {@code add} (with the menu bar: config {@code start: "screen"}, {@code menu_bar: true}, {@code schedulers: []}): opens
     * Customization -> Manage Schedulers, clicks Add Scheduler, gives the new scheduler an action like {@link #buildActionScriptScript} does
     * (config {@code search}, {@code action}, {@code fields}), clicks Done and selects the new scheduler.<br>
     * {@code settings} (config {@code schedulers} with the scheduler, {@code select}: its ID): opens Manage Schedulers, selects the scheduler,
     * fills in its Edit Settings window (config {@code settings_fields}, same format as {@code fields}), clicks Start Now and Done, then rests
     * the pointer at {@code rest_point} ([x, y]) to watch the scheduler work.<br>
     * Both end with {@code hold_ticks} (default 100).
     */
    private void buildManageSchedulersScript(@NotNull Minecraft minecraft) {
        boolean addPart = this.config.get("part").getAsString().equals("add");
        if (addPart) {
            this.waitTicks(12);
            // Customization -> Manage Schedulers
            this.moveTo(() -> menuBarEntry("screen"), 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
            this.waitTicks(10);
            this.moveTo(() -> new float[] {contextMenuEntry("manage_schedulers")[0] + 20, 200}, 12);
            this.waitTicks(4);
            this.scrollMenuUntilVisible(minecraft, "manage_schedulers", 230);
            this.waitTicks(8);
            this.clickMenuEntry(minecraft, "manage_schedulers");
            this.waitUntil("Manage Schedulers", () -> findWindow(ManageSchedulersWindowBody.class) != null, 60);
            this.waitTicks(14);
            // Add Scheduler opens the action script editor of the new scheduler right away
            this.moveTo(() -> windowButtonCenter(requireWindow(ManageSchedulersWindowBody.class), "fancymenu.schedulers.manage.add"), 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.openActionChooserAndAdd(minecraft, this.config.get("action").getAsString());
            this.waitTicks(12);
            this.moveTo(() -> windowWidgetCenter(requireWindow(ActionScriptEditorWindowBody.class), "doneButton"), 18);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("action script editor closed", () -> findWindow(ActionScriptEditorWindowBody.class) == null, 60);
            this.waitUntil("Manage Schedulers shown again", () -> requireWindow(ManageSchedulersWindowBody.class).isVisible(), 60);
            this.waitTicks(10);
            // Select the new scheduler, which got a random ID
            this.moveTo(() -> schedulerCell(null), 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(6);
            // Rest the pointer on an empty spot, so the random ID shows without a hover
            this.moveTo(DocsShowcaseEditorGif::schedulerListFreeSpot, 10);
        } else {
            String select = this.config.get("select").getAsString();
            this.waitTicks(10);
            this.steps.addLast(() -> {
                ManageSchedulersWindowBody.openInWindow(new ManageSchedulersWindowBody(accepted -> {}));
                return true;
            });
            this.waitUntil("Manage Schedulers", () -> findWindow(ManageSchedulersWindowBody.class) != null, 60);
            this.waitTicks(14);
            this.moveTo(() -> schedulerCell(select), 12);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(10);
            // Edit Settings
            this.moveTo(() -> windowButtonCenter(requireWindow(ManageSchedulersWindowBody.class), "fancymenu.schedulers.manage.settings"), 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Scheduler Settings", () -> findWindow(ManageSchedulerSettingsScreen.class) != null, 60);
            this.waitTicks(14);
            this.fillValueFields(minecraft, this.config.getAsJsonArray("settings_fields"));
            this.waitTicks(6);
            this.moveTo(() -> windowWidgetCenter(requireWindow(ManageSchedulerSettingsScreen.class), "doneButton"), 16);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Scheduler Settings closed", () -> findWindow(ManageSchedulerSettingsScreen.class) == null, 60);
            this.waitUntil("Manage Schedulers shown again", () -> requireWindow(ManageSchedulersWindowBody.class).isVisible(), 60);
            this.waitTicks(16);
            // Start Now turns into Stop Now while the scheduler runs
            this.moveTo(() -> windowButtonCenter(requireWindow(ManageSchedulersWindowBody.class), "fancymenu.schedulers.manage.start_now"), 14);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(24);
            this.moveTo(() -> windowWidgetCenter(requireWindow(ManageSchedulersWindowBody.class), "doneButton"), 16);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Manage Schedulers closed", () -> findWindow(ManageSchedulersWindowBody.class) == null, 60);
            this.waitTicks(4);
            if (this.config.has("rest_point")) {
                float x = this.config.getAsJsonArray("rest_point").get(0).getAsFloat();
                float y = this.config.getAsJsonArray("rest_point").get(1).getAsFloat();
                this.moveTo(() -> new float[] {x, y}, 16);
            }
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 100);
    }

    /**
     * Still of Manage Schedulers (config {@code start: "screen"}, {@code schedulers}): opens Manage Schedulers and selects a scheduler
     * (config {@code select}: its ID). Marks the frame of the still with a {@code mark} event ({@code manage}).
     */
    private void buildSchedulersStillScript(@NotNull Minecraft minecraft) {
        String select = this.config.get("select").getAsString();
        this.waitTicks(10);
        this.steps.addLast(() -> {
            ManageSchedulersWindowBody.openInWindow(new ManageSchedulersWindowBody(accepted -> {}));
            return true;
        });
        this.waitUntil("Manage Schedulers", () -> findWindow(ManageSchedulersWindowBody.class) != null, 60);
        this.waitTicks(14);
        this.moveTo(() -> schedulerCell(select), 10);
        this.waitTicks(3);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        // The description area is small at big UI scales, so scroll it down to the action script (config description_wheel_turns)
        if (this.config.has("description_wheel_turns")) {
            this.moveTo(() -> descriptionAreaCenter(requireWindow(ManageSchedulersWindowBody.class)), 8);
            for (int i = 0; i < this.config.get("description_wheel_turns").getAsInt(); i++) {
                this.wheel(minecraft, -1.0);
                this.waitTicks(2);
            }
        }
        // Rest the pointer on an empty spot, so no row shows a hover
        this.moveTo(() -> schedulerListFreeSpot(), 8);
        this.waitTicks(24);
        this.mark("manage");
        this.waitTicks(4);
    }

    /**
     * Replaces the Custom GUIs of the instance until the showcase ends (config {@code custom_guis}), like {@code listeners}. Each entry:
     * {@code identifier}, optional {@code title}, {@code allow_esc}, {@code pause_game}, {@code world_background},
     * {@code world_background_overlay}, {@code popup_mode} and {@code popup_mode_background_overlay}, and a layout with the same keys as the
     * config's own ({@code elements}, {@code vanilla}, {@code background_image}/{@code background_color}). These layouts only live in memory.
     * Only with {@code screen: "title"}, because the default start screen is the showcase's own Custom GUI.
     */
    private void setUpCustomGuis(@NotNull Iterable<JsonElement> guis) {
        if (this.savedCustomGuis == null) this.savedCustomGuis = CustomGuiHandler.getGuis();
        for (String identifier : CustomGuiHandler.getGuiIdentifiers()) CustomGuiHandler.removeGui(identifier);
        for (JsonElement entry : guis) {
            JsonObject data = entry.getAsJsonObject();
            CustomGui gui = new CustomGui();
            gui.identifier = data.get("identifier").getAsString();
            if (data.has("title")) gui.title = data.get("title").getAsString();
            if (data.has("allow_esc")) gui.allowEsc = data.get("allow_esc").getAsBoolean();
            if (data.has("pause_game")) gui.pauseGame = data.get("pause_game").getAsBoolean();
            if (data.has("world_background")) gui.worldBackground = data.get("world_background").getAsBoolean();
            if (data.has("world_background_overlay")) gui.worldBackgroundOverlay = data.get("world_background_overlay").getAsBoolean();
            if (data.has("popup_mode")) gui.popupMode = data.get("popup_mode").getAsBoolean();
            if (data.has("popup_mode_background_overlay")) gui.popupModeBackgroundOverlay = data.get("popup_mode_background_overlay").getAsBoolean();
            CustomGuiHandler.addGui(gui);
            if (data.has("elements") || data.has("vanilla") || data.has("background_image") || data.has("background_color")) {
                // After addGui, so the layout gets the identifier of the Custom GUI and no fixed screen identifier
                Layout layout = Layout.buildForScreen(gui.identifier);
                if (data.has("elements")) DocsShowcaseElements.addTo(layout, data.getAsJsonArray("elements"));
                if (data.has("vanilla")) DocsShowcaseElements.addVanillaTo(layout, data.getAsJsonArray("vanilla"));
                DocsShowcaseElements.addBackgroundTo(layout, data);
                LayoutHandler.addLayout(layout, false);
                this.customGuiLayouts.add(layout);
            }
        }
    }

    /**
     * Outside the editor: clicks elements one after another, like a player who opens a Custom GUI with a button and leaves it with
     * another (config {@code start: "screen"}). Config {@code clicks}: each with {@code target} (an element id, found on the screen that is
     * open when the pointer starts moving) and {@code wait_ticks} after the click (default 40). Then the pointer rests at the optional
     * {@code rest_point} ([x, y]) for {@code hold_ticks} (default 60). {@code start_wait_ticks} waits before the first move (default 20).
     */
    private void buildClickThroughScript(@NotNull Minecraft minecraft) {
        this.waitTicks(this.config.has("start_wait_ticks") ? this.config.get("start_wait_ticks").getAsInt() : 20);
        for (JsonElement entry : this.config.getAsJsonArray("clicks")) {
            JsonObject data = entry.getAsJsonObject();
            String target = data.get("target").getAsString();
            this.moveTo(() -> center(screenBox(target)), 20);
            this.waitTicks(10);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(data.has("wait_ticks") ? data.get("wait_ticks").getAsInt() : 40);
        }
        if (this.config.has("rest_point")) {
            float x = this.config.getAsJsonArray("rest_point").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("rest_point").get(1).getAsFloat();
            this.moveTo(() -> new float[] {x, y}, 16);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * Outside the editor (config {@code start: "screen"}, {@code screen: "title"}, {@code menu_bar: true}, {@code custom_guis: []}): opens
     * Customization -> Custom GUIs -> Manage Custom GUIs from the menu bar, clicks New GUI, types the {@code identifier} (and the optional
     * {@code title}), clicks the cycle buttons of {@code cycles} (their label keys, like {@code fancymenu.custom_guis.build.popup_mode}) once
     * each and clicks Done. Then it selects the new Custom GUI, clicks Open GUI and Done, and rests the pointer at {@code rest_point}
     * ([x, y]) for {@code hold_ticks} (default 80).
     */
    private void buildManageCustomGuisScript(@NotNull Minecraft minecraft) {
        String identifier = this.config.get("identifier").getAsString();
        this.waitTicks(12);
        // Customization -> Custom GUIs -> Manage Custom GUIs
        this.moveTo(() -> menuBarEntry("screen"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("custom_guis")[0] + 20, 200}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "custom_guis", 230);
        this.waitTicks(6);
        // Clicking Custom GUIs opens its sub-menu. Click its right part: hovering the info icon after its name shows a tooltip, which keeps the
        // sub-menu closed
        this.moveTo(() -> menuEntryRightPart("custom_guis"), 12);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Custom GUIs menu", () -> subMenu("custom_guis").isOpen(), 40);
        this.waitTicks(8);
        // Go into the sub-menu sideways first, so the pointer doesn't pass over other entries of the Customization menu
        this.moveTo(() -> new float[] {menuEntry("custom_guis", "manage_custom_guis")[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "custom_guis", "manage_custom_guis");
        this.waitUntil("Manage Custom GUIs", () -> findWindow(ManageCustomGuisScreen.class) != null, 60);
        this.waitTicks(14);
        // New GUI opens the settings window of the new Custom GUI
        this.moveTo(() -> windowButtonCenter(requireWindow(ManageCustomGuisScreen.class), "fancymenu.custom_guis.manage.add"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Build or Edit Custom GUI", () -> findWindow(BuildCustomGuiScreen.class) != null, 60);
        this.waitTicks(16);
        this.moveTo(() -> cellInputAfterLabel("fancymenu.custom_guis.build.identifier"), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(6);
        this.type(minecraft, identifier);
        this.waitTicks(12);
        if (this.config.has("title")) {
            this.moveTo(() -> cellInputAfterLabel("fancymenu.custom_guis.build.title"), 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(6);
            this.type(minecraft, this.config.get("title").getAsString());
            this.waitTicks(12);
        }
        if (this.config.has("cycles")) {
            for (JsonElement cycle : this.config.getAsJsonArray("cycles")) {
                String labelKey = cycle.getAsString();
                this.moveTo(() -> cellWidgetWithLabel(labelKey), 14);
                this.waitTicks(6);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(16);
            }
        }
        this.moveTo(() -> windowWidgetCenter(requireWindow(BuildCustomGuiScreen.class), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Build or Edit Custom GUI closed", () -> findWindow(BuildCustomGuiScreen.class) == null, 60);
        this.waitUntil("Manage Custom GUIs shown again", () -> requireWindow(ManageCustomGuisScreen.class).isVisible(), 60);
        this.waitTicks(12);
        this.moveTo(() -> customGuiCell(identifier), 12);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(10);
        // Open GUI saves the Custom GUIs and opens the selected one behind the window
        this.moveTo(() -> windowButtonCenter(requireWindow(ManageCustomGuisScreen.class), "fancymenu.custom_guis.manage.open"), 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Custom GUI opened", () -> ScreenUtils.getScreen() instanceof CustomGuiBaseScreen, 60);
        this.waitTicks(20);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ManageCustomGuisScreen.class), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Manage Custom GUIs closed", () -> findWindow(ManageCustomGuisScreen.class) == null, 60);
        this.waitTicks(4);
        if (this.config.has("rest_point")) {
            float x = this.config.getAsJsonArray("rest_point").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("rest_point").get(1).getAsFloat();
            this.moveTo(() -> new float[] {x, y}, 16);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 80);
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
        float widenBy = this.config.has("widen_by") ? this.config.get("widen_by").getAsFloat() : 0.0F;
        float heightenBy = this.config.has("heighten_by") ? this.config.get("heighten_by").getAsFloat() : 0.0F;
        boolean resizeFirst = this.config.has("resize_first") && this.config.get("resize_first").getAsBoolean();
        String labelEntry = this.config.has("label_entry") ? this.config.get("label_entry").getAsString() : null;
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
        // Its id, since it doesn't stay the last element when it moves to other layers
        String[] addedId = {null};
        this.steps.addLast(() -> {
            addedId[0] = requireLayoutEditor().normalEditorElements.get(requireLayoutEditor().normalEditorElements.size() - 1).element.getInstanceIdentifier();
            return true;
        });
        this.waitTicks(16);
        // Drag it to its place
        Runnable dragToPlace = () -> {
            this.moveTo(() -> center(newestElementBox()), 12);
            this.waitTicks(4);
            this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            if (this.config.has("exact_drag") && this.config.get("exact_drag").getAsBoolean()) {
                this.dragPastCrumpleZone(minecraft, () -> new float[] {dragX, dragY}, 22);
            } else {
                this.moveTo(() -> new float[] {dragX, dragY}, 22);
            }
            this.waitTicks(4);
            this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(12);
        };
        // Widen it at its right edge, and make it taller or lower at its bottom edge
        boolean resizeCorner = this.config.has("resize_corner") && this.config.get("resize_corner").getAsBoolean();
        Runnable resize = () -> {
            if (resizeCorner) {
                // Both at once, at the bottom-right corner
                this.moveTo(() -> {
                    float[] box = newestElementBox();
                    return new float[] {box[0] + box[2], box[1] + box[3]};
                }, 12);
                this.waitTicks(8);
                this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.moveTo(() -> new float[] {this.pointerX + widenBy, this.pointerY + heightenBy}, 18);
                this.waitTicks(4);
                this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(12);
                return;
            }
            if (widenBy != 0.0F) {
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
            }
            if (heightenBy != 0.0F) {
                this.moveTo(() -> {
                    float[] box = newestElementBox();
                    return new float[] {box[0] + box[2] / 2.0F, box[1] + box[3]};
                }, 12);
                this.waitTicks(8);
                this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.moveTo(() -> new float[] {this.pointerX, this.pointerY + heightenBy}, 16);
                this.waitTicks(4);
                this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(12);
            }
        };
        if (resizeFirst) {
            resize.run();
            dragToPlace.run();
        } else {
            dragToPlace.run();
            resize.run();
        }
        if (this.config.has("layer_down")) this.moveNewestElementLayerDown(minecraft, this.config.get("layer_down").getAsInt(), freeX, freeY);
        if (this.config.has("anchor")) this.anchorByHovering(minecraft, this.config.getAsJsonObject("anchor"), addedId);
        if (labelEntry != null) this.giveNewestElementLabel(minecraft, labelEntry);
        // Click the empty background, so the element shows without its selection frame
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 70);
    }

    /**
     * Anchors another element to the element that got added last, the way users do it: drag the element ({@code target}) at {@code grab}
     * ([x, y]) straight up or down until the pointer is over the new element, hold it there until the anchor charge bar is full
     * ({@code hover_ticks}), and drag it back to where it was. Fails if the element isn't anchored to the new one after that.
     */
    private void anchorByHovering(@NotNull Minecraft minecraft, @NotNull JsonObject anchor, @NotNull String[] addedId) {
        String target = anchor.get("target").getAsString();
        float grabX = anchor.getAsJsonArray("grab").get(0).getAsFloat();
        float grabY = anchor.getAsJsonArray("grab").get(1).getAsFloat();
        int hoverTicks = anchor.has("hover_ticks") ? anchor.get("hover_ticks").getAsInt() : 56;
        this.moveTo(() -> new float[] {grabX, grabY}, 16);
        this.waitTicks(6);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(4);
        if (anchor.has("out")) {
            // The new element is under the pointer from the start, which the editor ignores until the pointer left it once:
            // drag the element out of it by "out" and back, then hold
            float outX = anchor.getAsJsonArray("out").get(0).getAsFloat();
            float outY = anchor.getAsJsonArray("out").get(1).getAsFloat();
            float[] shift = this.crumpleShift(outX, outY);
            this.moveTo(() -> new float[] {grabX + shift[0], grabY + shift[1]}, 1);
            this.moveTo(() -> new float[] {grabX + outX, grabY + outY}, 14);
            this.waitTicks(6);
            // Back to the same pointer position as right after the crumple zone, so the element ends where it was
            this.moveTo(() -> new float[] {grabX + shift[0], grabY + shift[1]}, 14);
            this.waitTicks(hoverTicks);
        } else {
            this.moveTo(() -> {
                float[] box = editorBox(addedId[0]);
                return new float[] {grabX, box[1] + box[3] / 2.0F};
            }, 18);
            this.waitTicks(hoverTicks);
            this.moveTo(() -> new float[] {grabX, grabY}, 18);
            this.waitTicks(4);
        }
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        this.steps.addLast(() -> {
            LayoutEditorScreen editor = requireLayoutEditor();
            AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(target);
            if (element == null) throw new IllegalStateException("No element " + target + " in the editor");
            if (!addedId[0].equals(element.element.getAnchorPointElementIdentifier())) {
                throw new IllegalStateException(target + " didn't get anchored to " + addedId[0] + ", it is anchored to " + element.element.getAnchorPointElementIdentifier());
            }
            return true;
        });
    }

    /**
     * Drags a pressed element to the target, so that it moves by exactly the distance from the press to the target. The editor only starts
     * moving elements once the pointer left a crumple zone around the press, and counts from where it left it, so the pointer jumps out of
     * the zone in one tick first (a few pixels towards the target) and the target gets shifted by the same few pixels.
     */
    private void dragPastCrumpleZone(@NotNull Minecraft minecraft, @NotNull Supplier<float[]> target, int ticks) {
        float[] shift = new float[2];
        this.moveTo(() -> {
            float[] to = target.get();
            float[] s = this.crumpleShift(to[0] - this.pointerX, to[1] - this.pointerY);
            shift[0] = s[0];
            shift[1] = s[1];
            return new float[] {this.pointerX + s[0], this.pointerY + s[1]};
        }, 1);
        this.moveTo(() -> {
            float[] to = target.get();
            return new float[] {to[0] + shift[0], to[1] + shift[1]};
        }, ticks);
    }

    /** A step of 6 GUI pixels along the main axis of the direction, just past the editor's crumple zone of 5 pixels. */
    @NotNull
    private float[] crumpleShift(float dx, float dy) {
        return (Math.abs(dx) >= Math.abs(dy)) ? new float[] {Math.signum(dx) * 6, 0} : new float[] {0, Math.signum(dy) * 6};
    }

    /**
     * Right-clicks the element that got added last and clicks Move One Layer Down {@code times} times in its right-click menu, which stays
     * open. Then clicks the free spot twice: once to close the menu, once to unselect the element, since selected elements show on top.
     */
    private void moveNewestElementLayerDown(@NotNull Minecraft minecraft, int times, float freeX, float freeY) {
        this.moveTo(() -> center(newestElementBox()), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("element menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {contextMenuEntry("move_down_element")[0] + 20, Math.min(this.pointerY + 40, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "move_down_element");
        this.waitTicks(4);
        for (int i = 0; i < times; i++) {
            if (i > 0) this.waitTicks(10);
            this.clickMenuEntry(minecraft, "move_down_element");
        }
        this.waitTicks(8);
        for (int i = 0; i < 2; i++) {
            if (i == 0) this.moveTo(() -> new float[] {freeX, freeY}, 14);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(6);
        }
        this.waitTicks(6);
    }

    private void giveNewestElementLabel(@NotNull Minecraft minecraft, @NotNull String labelEntry) {
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
        if (this.config.has("fields")) this.fillValueFields(minecraft, this.config.getAsJsonArray("fields"));
        this.moveTo(() -> windowWidgetCenter(requireValueWindow(), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("value window closed", () -> findValueWindow() == null, 60);
        this.waitUntil("action script editor shown again", () -> requireWindow(ActionScriptEditorWindowBody.class).isVisible(), 60);
    }

    /**
     * Fills in the fields of the open value window: {@code {"label", "text"}} clicks the text field after the label (by its localization
     * key), selects its text and types, {@code {"cycle", "clicks"}} clicks a cycle button (by the localization key of its label) that often.
     */
    private void fillValueFields(@NotNull Minecraft minecraft, @NotNull Iterable<JsonElement> fields) {
        for (JsonElement entry : fields) {
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

    /**
     * In the editor: optionally copy the ID of an element through its right-click menu (config {@code copy_id}: its id), open the Loading
     * Requirements of the {@code target} element, run the config's {@code requirement_steps} in Manage Requirements, click Done and click the
     * empty background at {@code free_spot} ([x, y]). Then wait {@code hold_ticks} (default 60). {@code open_directly: true} opens Manage
     * Requirements of the target without its right-click menu, for GIFs that are about the window itself. {@code close: false} keeps it open at
     * the end, so the GIF ends on its list. Steps:
     * <ul>
     *     <li>{@code {"add": <requirement identifier>, "search": <typed into the chooser>}}: adds a requirement. {@code "via": "menu"} right-clicks
     *     the list and clicks Add Requirement, otherwise it presses A. Its value: {@code "value"} gets typed into the one-line text editor,
     *     {@code "paste": true} pastes the clipboard there, {@code "fields"} fills a value window with own fields (see {@link #fillValueFields}).</li>
     *     <li>{@code {"add_group": "menu"}} or {@code {"add_group": "key"}}: adds a group through the right-click menu or with G.</li>
     *     <li>{@code {"group_mode": <group identifier>}}: right-clicks the group and clicks Group Mode, which switches it between And and Or.</li>
     *     <li>{@code {"open_group": <group identifier>}}: right-clicks the group and clicks Edit Group Requirements. With {@code "from_open_menu": true}, it
     *     clicks it in the right-click menu that is still open, like after {@code group_mode}.</li>
     *     <li>{@code {"done": true}}: clicks Done in the window of the open group.</li>
     *     <li>{@code {"rest": <ticks>}}: rests the pointer on the free space of the list.</li>
     * </ul>
     */
    private void buildManageRequirementsScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        boolean close = !this.config.has("close") || this.config.get("close").getAsBoolean();
        int hold = this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60;
        this.waitTicks(16);
        if (this.config.has("open_directly") && this.config.get("open_directly").getAsBoolean()) {
            this.steps.addLast(() -> {
                AbstractElement element = requireEditorElement(target).element;
                ManageRequirementsWindowBody.openInWindow(new ManageRequirementsWindowBody(element.requirementContainer.copy(false), call -> {
                    if (call != null) element.requirementContainer = call;
                }));
                return true;
            });
        } else {
            this.openLoadingRequirements(minecraft, target);
        }
        this.waitUntil("Manage Requirements", () -> findRequirementsWindow() != null, 60);
        this.waitTicks(14);
        for (JsonElement entry : this.config.getAsJsonArray("requirement_steps")) {
            JsonObject step = entry.getAsJsonObject();
            if (step.has("add")) {
                this.addRequirement(minecraft, step);
            } else if (step.has("add_group")) {
                if (step.get("add_group").getAsString().equals("menu")) {
                    this.rightClickAndPick(minecraft, DocsShowcaseEditorGif::requirementsListFreeSpot, "add_group");
                } else {
                    this.key(minecraft, "g", "G");
                }
                this.waitTicks(16);
            } else if (step.has("group_mode")) {
                String group = step.get("group_mode").getAsString();
                this.rightClickAndPick(minecraft, () -> requirementRow(group), "cycle_group_mode");
                // The right-click menu opens again and shows the new mode
                this.waitTicks(18);
            } else if (step.has("open_group")) {
                String group = step.get("open_group").getAsString();
                // The script gets built before it runs, so the config says if the right-click menu of the group is still open by then
                if (step.has("from_open_menu") && step.get("from_open_menu").getAsBoolean()) {
                    this.clickMenuEntry(minecraft, "edit_group_requirements");
                } else {
                    this.rightClickAndPick(minecraft, () -> requirementRow(group), "edit_group_requirements");
                }
                this.waitUntil("group window", () -> isGroupWindow(findRequirementsWindow()), 60);
                this.waitTicks(14);
            } else if (step.has("done")) {
                this.moveTo(() -> windowWidgetCenter(requireRequirementsWindow(), "doneButton"), 14);
                this.waitTicks(5);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitUntil("main Manage Requirements shown again", () -> (findRequirementsWindow() != null) && !isGroupWindow(findRequirementsWindow()), 60);
                this.waitTicks(12);
            } else if (step.has("rest")) {
                this.moveTo(DocsShowcaseEditorGif::requirementsListFreeSpot, 12);
                this.waitTicks(step.get("rest").getAsInt());
            }
        }
        if (close) {
            float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
            float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
            this.moveTo(() -> windowWidgetCenter(requireRequirementsWindow(), "doneButton"), 16);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Manage Requirements closed", () -> findWindow(ManageRequirementsWindowBody.class) == null, 60);
            this.waitTicks(8);
            // Click the empty background, so the element shows without its selection frame
            this.moveTo(() -> new float[] {freeX, freeY}, 16);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        }
        this.waitTicks(hold);
    }

    /**
     * Copies the ID of the config's {@code copy_id} element through its right-click menu, if the config has one, then opens Loading
     * Requirements through the right-click menu of the target element.
     */
    private void openLoadingRequirements(@NotNull Minecraft minecraft, @NotNull String target) {
        if (this.config.has("copy_id")) {
            String source = this.config.get("copy_id").getAsString();
            this.moveTo(() -> center(editorBox(source)), 18);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
            this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
            this.waitTicks(10);
            this.clickMenuEntry(minecraft, "copy_id");
            this.waitTicks(12);
        }
        this.moveTo(() -> center(editorBox(target)), 18);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry("loading_requirements")[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, "loading_requirements");
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, "loading_requirements");
    }

    /**
     * Adds a requirement in the visible Manage Requirements window, see {@link #buildManageRequirementsScript}.
     */
    private void addRequirement(@NotNull Minecraft minecraft, @NotNull JsonObject step) {
        String requirement = step.get("add").getAsString();
        if (step.has("via") && step.get("via").getAsString().equals("menu")) {
            this.rightClickAndPick(minecraft, DocsShowcaseEditorGif::requirementsListFreeSpot, "add_requirement");
        } else {
            this.key(minecraft, "a", "A");
        }
        this.waitUntil("requirement chooser", () -> findWindow(BuildRequirementWindowBody.class) != null, 60);
        this.waitTicks(12);
        this.type(minecraft, step.get("search").getAsString());
        this.waitTicks(10);
        this.moveTo(() -> requirementChooserEntry(requirement), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        // Time to read the description
        this.waitTicks(step.has("read_ticks") ? step.get("read_ticks").getAsInt() : 16);
        Requirement type = RequirementRegistry.getRequirement(requirement);
        if (type == null) throw new IllegalStateException("No requirement " + requirement);
        // Requirements without a value have Done instead of Next Step
        String buttonKey = type.hasValue() ? "fancymenu.ui.generic.next_step" : "fancymenu.common_components.done";
        this.moveTo(() -> windowButtonCenter(requireWindow(BuildRequirementWindowBody.class), buttonKey), 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        if (step.has("fields")) {
            this.waitUntil("value window", () -> findValueWindow() != null, 60);
            this.waitTicks(12);
            this.fillValueFields(minecraft, step.getAsJsonArray("fields"));
            this.moveTo(() -> windowWidgetCenter(requireValueWindow(), "doneButton"), 14);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("value window closed", () -> findValueWindow() == null, 60);
        } else if (type.hasValue()) {
            // Requirements with a simple value edit it in the one-line text editor, which starts with an example value
            this.waitUntil("text editor", () -> findEditor() != null, 60);
            this.waitTicks(12);
            this.key(minecraft, "shortcut+a", "Ctrl + A");
            this.waitTicks(6);
            if (step.has("paste") && step.get("paste").getAsBoolean()) {
                this.key(minecraft, "shortcut+v", "Ctrl + V");
            } else {
                this.type(minecraft, step.get("value").getAsString());
            }
            this.waitTicks(12);
            this.moveTo(DocsShowcaseEditorGif::doneButton, 14);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("text editor closed", () -> findEditor() == null, 60);
        }
        // With its value set, or without a value, the requirement chooser closes on its own
        this.waitUntil("requirement chooser closed", () -> findWindow(BuildRequirementWindowBody.class) == null, 60);
        this.waitUntil("Manage Requirements shown again", () -> findRequirementsWindow() != null, 60);
        this.waitTicks(12);
    }

    /**
     * Outside the editor: moves the pointer over elements and rests on each, like a player trying hover effects (config {@code start: "screen"}).
     * Config: {@code hovers}, each with {@code target} (an element id, also of Vanilla elements) or {@code point} ([x, y]), {@code ticks}
     * (how long the pointer rests, default 40) and {@code move_ticks} (default 16). Optional {@code pointer} ([x, y], the start) and
     * {@code hold_ticks} (after the last hover, default 20).
     */
    /**
     * In the editor: right-clicks the {@code target} element, scrolls its right-click menu to the {@code entry} (an entry identifier, like
     * {@code entity_pose} of Player Entities) and clicks it, waits for what it opens, rests the pointer at {@code rest_point} ([x, y]) and marks
     * the frame of the still with a {@code mark} event ({@code still}).
     */
    private void buildContextEntryStillScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        String entry = this.config.get("entry").getAsString();
        this.waitTicks(16);
        this.moveTo(() -> center(editorBox(target)), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {contextMenuEntry(entry)[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 10);
        this.waitTicks(4);
        this.scrollMenuUntilVisible(minecraft, entry);
        this.waitTicks(6);
        this.moveTo(() -> contextMenuEntry(entry), 8);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        // Windows fade in
        this.waitTicks(30);
        if (this.config.has("rest_point")) {
            float x = this.config.getAsJsonArray("rest_point").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("rest_point").get(1).getAsFloat();
            this.moveTo(() -> new float[] {x, y}, 10);
        }
        this.waitTicks(16);
        this.mark("still");
        this.waitTicks(4);
    }

    private void buildHoverElementsScript(@NotNull Minecraft minecraft) {
        if (this.config.has("pointer")) {
            float x = this.config.getAsJsonArray("pointer").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("pointer").get(1).getAsFloat();
            this.steps.addLast(() -> {
                this.movePointer(minecraft, x, y);
                return true;
            });
        }
        this.waitTicks(16);
        for (JsonElement entry : this.config.getAsJsonArray("hovers")) {
            JsonObject hover = entry.getAsJsonObject();
            int moveTicks = hover.has("move_ticks") ? hover.get("move_ticks").getAsInt() : 16;
            if (hover.has("target")) {
                String target = hover.get("target").getAsString();
                this.moveTo(() -> center(screenBox(target)), moveTicks);
            } else {
                float x = hover.getAsJsonArray("point").get(0).getAsFloat();
                float y = hover.getAsJsonArray("point").get(1).getAsFloat();
                this.moveTo(() -> new float[] {x, y}, moveTicks);
            }
            this.waitTicks(hover.has("ticks") ? hover.get("ticks").getAsInt() : 40);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 20);
    }

    /**
     * Outside the editor: hover an element, click it and wait, like a player clicking a button. Config: {@code target} (the element's id),
     * {@code hold_ticks} (how long the result stays, default 100), {@code clicks} (default 1), {@code click_gap_ticks} (between clicks, default 40)
     * and {@code start_wait_ticks} (before the pointer moves, default 16, longer for things that show up after the screen opened).
     */
    /**
     * Outside the editor: walks through vanilla screens like a player, by the labels of their buttons and sliders, for showcases of what
     * changes every screen, like Global Customizations (config {@code start: "screen"}, usually {@code no_layout: true}).<br>
     * Config {@code tour}: steps, each one of {@code hover} (a label key, like {@code menu.singleplayer}), {@code click} (a label key, waits for
     * the next screen when {@code opens: true}), {@code drag} (a slider by its label key, from {@code from} to {@code to}, as parts of its width),
     * {@code point} ([x, y]) and {@code wait}. All but {@code wait} take {@code ticks} (how long the pointer rests after them, default 30).
     * Labels match from their start, so {@code options.fov} finds the slider {@code FOV: 70}. Optional {@code pointer} ([x, y], the start)
     * and {@code hold_ticks} (after the last step, default 60).
     */
    private void buildScreenTourScript(@NotNull Minecraft minecraft) {
        if (this.config.has("pointer")) {
            float x = this.config.getAsJsonArray("pointer").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("pointer").get(1).getAsFloat();
            this.steps.addLast(() -> {
                this.movePointer(minecraft, x, y);
                return true;
            });
        }
        this.waitTicks(16);
        for (JsonElement entry : this.config.getAsJsonArray("tour")) {
            JsonObject step = entry.getAsJsonObject();
            int rest = step.has("ticks") ? step.get("ticks").getAsInt() : 30;
            int moveTicks = step.has("move_ticks") ? step.get("move_ticks").getAsInt() : 18;
            if (step.has("hover")) {
                String label = step.get("hover").getAsString();
                this.moveTo(() -> center(vanillaWidgetBox(label)), moveTicks);
                this.waitTicks(rest);
            } else if (step.has("click")) {
                String label = step.get("click").getAsString();
                boolean opens = step.has("opens") && step.get("opens").getAsBoolean();
                this.moveTo(() -> center(vanillaWidgetBox(label)), moveTicks);
                this.waitTicks(8);
                Screen[] before = new Screen[1];
                this.steps.addLast(() -> {
                    before[0] = ScreenUtils.getScreen();
                    return true;
                });
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                if (opens) this.waitUntil("the screen of " + label, () -> (ScreenUtils.getScreen() != null) && (ScreenUtils.getScreen() != before[0]), 100);
                this.waitTicks(rest);
            } else if (step.has("drag")) {
                String label = step.get("drag").getAsString();
                float from = step.has("from") ? step.get("from").getAsFloat() : 0.3F;
                float to = step.has("to") ? step.get("to").getAsFloat() : 0.7F;
                int dragTicks = step.has("drag_ticks") ? step.get("drag_ticks").getAsInt() : 26;
                this.moveTo(() -> pointInWidget(vanillaWidgetBox(label), from), moveTicks);
                this.waitTicks(8);
                this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(4);
                // The label changes with the value, so the target is the box the slider had when the drag started
                float[][] box = new float[1][];
                this.steps.addLast(() -> {
                    box[0] = vanillaWidgetBox(label);
                    return true;
                });
                this.moveTo(() -> pointInWidget(box[0], to), dragTicks);
                this.waitTicks(4);
                this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(rest);
            } else if (step.has("grab")) {
                // Drags a custom element of the screen, like a Dragger, at a point of its box ("at": parts of its width and height) by "by"
                String target = step.get("grab").getAsString();
                float atX = step.has("at") ? step.getAsJsonArray("at").get(0).getAsFloat() : 0.5F;
                float atY = step.has("at") ? step.getAsJsonArray("at").get(1).getAsFloat() : 0.5F;
                float byX = step.getAsJsonArray("by").get(0).getAsFloat();
                float byY = step.getAsJsonArray("by").get(1).getAsFloat();
                int dragTicks = step.has("drag_ticks") ? step.get("drag_ticks").getAsInt() : 40;
                this.moveTo(() -> {
                    float[] box = screenBox(target);
                    return new float[] {box[0] + box[2] * atX, box[1] + box[3] * atY};
                }, moveTicks);
                this.waitTicks(8);
                this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(4);
                this.moveTo(() -> new float[] {this.pointerX + byX, this.pointerY + byY}, dragTicks);
                this.waitTicks(4);
                this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(rest);
            } else if (step.has("point")) {
                float x = step.getAsJsonArray("point").get(0).getAsFloat();
                float y = step.getAsJsonArray("point").get(1).getAsFloat();
                this.moveTo(() -> new float[] {x, y}, moveTicks);
                this.waitTicks(rest);
            } else if (step.has("wait")) {
                this.waitTicks(step.get("wait").getAsInt());
            } else {
                throw new IllegalArgumentException("Unknown tour step " + step);
            }
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    @NotNull
    private static float[] pointInWidget(@NotNull float[] box, float part) {
        return new float[] {box[0] + box[2] * part, box[1] + box[3] / 2.0F};
    }

    /**
     * X, Y, width and height of the visible widget of the open screen whose label is the text of the label key, or starts with it (like a
     * slider's {@code FOV: 70} for {@code options.fov}). Finds widgets in lists too, like the sliders of Music & Sounds.
     */
    @NotNull
    private static float[] vanillaWidgetBox(@NotNull String labelKey) {
        Screen screen = ScreenUtils.getScreen();
        if (screen == null) throw new IllegalStateException("No screen is open");
        String label = Component.translatable(labelKey).getString();
        List<AbstractWidget> widgets = new ArrayList<>();
        collectWidgets(screen, widgets);
        AbstractWidget match = null;
        for (AbstractWidget widget : widgets) {
            String message = widget.getMessage().getString();
            if (message.equals(label)) {
                match = widget;
                break;
            }
            if ((match == null) && message.startsWith(label)) match = widget;
        }
        if (match == null) throw new IllegalStateException("No widget labeled " + label + " on " + screen.getClass().getSimpleName());
        return new float[] {match.getX(), match.getY(), match.getWidth(), match.getHeight()};
    }

    private static void collectWidgets(@NotNull ContainerEventHandler container, @NotNull List<AbstractWidget> widgets) {
        for (GuiEventListener child : container.children()) {
            if ((child instanceof AbstractWidget widget) && widget.visible) widgets.add(widget);
            if (child instanceof ContainerEventHandler nested) collectWidgets(nested, widgets);
        }
    }

    private void buildClickElementScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        int hold = this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 100;
        int clicks = this.config.has("clicks") ? this.config.get("clicks").getAsInt() : 1;
        int gap = this.config.has("click_gap_ticks") ? this.config.get("click_gap_ticks").getAsInt() : 40;
        this.waitTicks(this.config.has("start_wait_ticks") ? this.config.get("start_wait_ticks").getAsInt() : 16);
        this.moveTo(() -> center(screenBox(target)), 20);
        this.waitTicks(10);
        for (int i = 0; i < clicks; i++) {
            if (i > 0) this.waitTicks(gap);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        }
        this.waitTicks(hold);
    }

    /**
     * The basics of the layout editor: select and drag an element, widen it at its right edge, nudge it with the arrow keys, select
     * buttons with a selection rectangle, copy and paste them, drag the copies away with Ctrl, undo twice and click the background.<br>
     * Config: {@code target} (the element to move and widen), {@code move} ([x, y] it gets dragged by), {@code widen_by},
     * {@code nudge} (how often Arrow Right gets pressed), {@code buttons} (the ids of the first and last button, in screen order),
     * {@code copies_move} ([x, y] the copies get dragged by) and {@code free_spot} ([x, y], clicked at the end).
     */
    private void buildEditorBasicsScript(@NotNull Minecraft minecraft) {
        String target = this.config.get("target").getAsString();
        float moveX = this.config.getAsJsonArray("move").get(0).getAsFloat();
        float moveY = this.config.getAsJsonArray("move").get(1).getAsFloat();
        float widenBy = this.config.get("widen_by").getAsFloat();
        int nudge = this.config.get("nudge").getAsInt();
        String firstButton = this.config.getAsJsonArray("buttons").get(0).getAsString();
        String lastButton = this.config.getAsJsonArray("buttons").get(1).getAsString();
        float copiesX = this.config.getAsJsonArray("copies_move").get(0).getAsFloat();
        float copiesY = this.config.getAsJsonArray("copies_move").get(1).getAsFloat();
        float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
        float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
        this.waitTicks(20);
        // Clicking the element selects it, dragging moves it. The targets get resolved when a move starts, before the element moves.
        this.moveTo(() -> center(editorBox(target)), 16);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> new float[] {this.pointerX + moveX, this.pointerY + moveY}, 22);
        this.waitTicks(6);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Widen it at the middle of its right edge
        this.moveTo(() -> {
            float[] box = editorBox(target);
            return new float[] {box[0] + box[2], box[1] + box[3] / 2.0F};
        }, 14);
        this.waitTicks(6);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> new float[] {this.pointerX + widenBy, this.pointerY}, 18);
        this.waitTicks(6);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Nudge it pixel by pixel
        for (int i = 0; i < nudge; i++) {
            this.key(minecraft, "right", "Arrow Right");
            this.waitTicks(6);
        }
        this.waitTicks(10);
        // Select the buttons with a selection rectangle, from a free spot above the first one to below the last one
        this.moveTo(() -> {
            float[] box = editorBox(firstButton);
            return new float[] {box[0] - 14, box[1] - 10};
        }, 16);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> {
            float[] box = editorBox(lastButton);
            return new float[] {box[0] + box[2] + 14, box[1] + box[3] + 10};
        }, 18);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Copy and paste them. The copies land on top of the buttons and get selected.
        this.key(minecraft, "shortcut+c", "Ctrl + C");
        this.waitTicks(14);
        this.key(minecraft, "shortcut+v", "Ctrl + V");
        this.waitTicks(14);
        // Drag the copies away. Ctrl keeps both of them selected.
        this.moveTo(() -> center(editorBox(firstButton)), 12);
        this.waitTicks(4);
        this.badge("Ctrl + Drag");
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
        this.moveTo(() -> new float[] {this.pointerX + copiesX, this.pointerY + copiesY}, 22);
        this.waitTicks(6);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(20);
        // Undo the move, then the paste
        this.key(minecraft, "shortcut+z", "Ctrl + Z");
        this.waitTicks(20);
        this.key(minecraft, "shortcut+z", "Ctrl + Z");
        this.waitTicks(16);
        // Click the empty background, so nothing shows a selection frame
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(50);
    }

    /**
     * Rotates and tilts elements with their controls in the editor. Config {@code steps}: each with {@code target} (an element id) and one of
     * {@code rotate_to}, {@code vertical_tilt} or {@code horizontal_tilt} (degrees), plus optional {@code ticks} (how long the drag takes,
     * default 30) and {@code rest_ticks} (after it, default 16). A step first selects its target, unless the step before had the same one.
     * Rotation steps start where the element is, which is {@code rotate_from} (default 0), because steps get built before they run.
     * Then it clicks {@code free_spot} and holds for {@code hold_ticks} (default 60).
     */
    private void buildRotateAndTiltScript(@NotNull Minecraft minecraft) {
        float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
        float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
        this.waitTicks(20);
        String selected = null;
        for (JsonElement entry : this.config.getAsJsonArray("steps")) {
            JsonObject step = entry.getAsJsonObject();
            String target = step.get("target").getAsString();
            int ticks = step.has("ticks") ? step.get("ticks").getAsInt() : 30;
            if (!target.equals(selected)) {
                this.moveTo(() -> center(editorBox(target)), 16);
                this.waitTicks(4);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(14);
                selected = target;
            }
            // The grabber positions follow the formulas of the controls in AbstractEditorElement. Rotation drags follow the circle,
            // which rotates the element by the angle the pointer travels. Tilt drags set the tilt by where on its line the pointer is.
            Function<Float, float[]> grabber;
            float from;
            float to;
            if (step.has("rotate_to")) {
                from = step.has("rotate_from") ? step.get("rotate_from").getAsFloat() : 0.0F;
                to = step.get("rotate_to").getAsFloat();
                grabber = degrees -> rotationGrabberPoint(target, degrees);
            } else if (step.has("vertical_tilt")) {
                from = 0.0F;
                to = step.get("vertical_tilt").getAsFloat();
                grabber = degrees -> {
                    float[] box = editorBox(target);
                    return new float[] {box[0] + box[2] + 10, (box[1] - 20) + (box[3] + 40) * ((degrees + 60.0F) / 120.0F)};
                };
            } else {
                from = 0.0F;
                to = step.get("horizontal_tilt").getAsFloat();
                grabber = degrees -> {
                    float[] box = editorBox(target);
                    return new float[] {(box[0] - 20) + (box[2] + 40) * ((degrees + 60.0F) / 120.0F), box[1] + box[3] + 10};
                };
            }
            this.moveTo(() -> grabber.apply(from), 14);
            this.waitTicks(6);
            this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            int[] tick = {0};
            this.steps.addLast(() -> {
                tick[0]++;
                float p = Math.min(1.0F, tick[0] / (float) ticks);
                float eased = (p < 0.5F) ? (4 * p * p * p) : (1 - (float)Math.pow(-2 * p + 2, 3) / 2);
                float[] point = grabber.apply(from + (to - from) * eased);
                this.movePointer(minecraft, point[0], point[1]);
                return p >= 1.0F;
            });
            this.waitTicks(6);
            this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(step.has("rest_ticks") ? step.get("rest_ticks").getAsInt() : 16);
        }
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /** The GUI position of the rotation grabber of an element at the given rotation: on a circle a bit bigger than its diagonal, at the top for 0 degrees. */
    @NotNull
    private static float[] rotationGrabberPoint(@NotNull String instanceIdentifier, float degrees) {
        float[] box = editorBox(instanceIdentifier);
        float halfWidth = box[2] / 2.0F;
        float halfHeight = box[3] / 2.0F;
        float radius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight) + 8;
        double angle = Math.toRadians(degrees - 90.0F);
        return new float[] {(float) (box[0] + halfWidth + radius * Math.cos(angle)), (float) (box[1] + halfHeight + radius * Math.sin(angle))};
    }

    /**
     * Works in the Layers widget (needs {@code show_widgets: true}): drags the layer of {@code cover} (an element that hides {@code covered})
     * below the layer of {@code covered} and clicks {@code free_spot} to unselect it, adds a group through the right-click menu of Layers,
     * selects both elements with a selection rectangle in the editor, drags them onto the group, then hides and shows the group with its
     * eye and folds it up with its arrow.
     */
    private void buildLayersWidgetScript(@NotNull Minecraft minecraft) {
        String cover = this.config.get("cover").getAsString();
        String covered = this.config.get("covered").getAsString();
        float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
        float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
        this.waitTicks(24);
        // Select the cover by its layer, then drag the layer onto the lower half of the covered one, which puts it right below it
        this.moveTo(() -> layerRow(cover, 0.5F), 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(10);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> layerRow(covered, 0.8F), 18);
        this.waitTicks(10);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("cover below covered", () -> {
            List<AbstractEditorElement<?, ?>> order = requireLayoutEditor().normalEditorElements;
            return order.indexOf(requireEditorElement(cover)) < order.indexOf(requireEditorElement(covered));
        }, 40);
        // Selected elements always show on top in the editor, so the cover still hides the text until it gets unselected
        this.waitTicks(16);
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(30);
        // Add a group through the right-click menu of an empty spot of Layers
        this.moveTo(DocsShowcaseEditorGif::layersFreeSpot, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("Layers menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.clickMenuEntry(minecraft, "add_group");
        this.waitUntil("group added", () -> !requireLayoutEditor().layout.layerGroups.isEmpty(), 40);
        this.waitTicks(16);
        // Select both elements with a selection rectangle in the editor
        this.moveTo(() -> {
            float[] box = editorBox(cover);
            return new float[] {box[0] - 12, box[1] - 10};
        }, 18);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> {
            float[] box = editorBox(cover);
            return new float[] {box[0] + box[2] + 12, box[1] + box[3] + 10};
        }, 16);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        // Drag one of the selected layers onto the group, which takes the other one along
        this.moveTo(() -> layerRow(covered, 0.5F), 16);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> layerGroupRow(0.5F), 18);
        this.waitTicks(10);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("layers in the group", () -> requireLayoutEditor().getLayerGroupForElement(requireEditorElement(cover)) != null
                && requireLayoutEditor().getLayerGroupForElement(requireEditorElement(covered)) != null, 40);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(20);
        // Hide the group with its eye and show it again
        this.moveTo(() -> layerGroupButton("eyeButton"), 16);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(30);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(20);
        // Fold the group up with its arrow
        this.moveTo(() -> layerGroupButton("collapseButton"), 10);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(14);
        this.moveTo(() -> new float[] {freeX, freeY}, 16);
        this.waitTicks(60);
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


    // ---- Parallax page ----

    /**
     * In the editor: selects the elements of {@code select} first, if the config has them (see {@link #selectElements}). Then it right-clicks
     * the element {@code target} (its id), or the one at {@code right_click} ([x, y], a spot where it is the topmost element), scrolls its
     * right-click menu and goes through {@code settings}, each either {@code toggle} (an entry identifier, like {@code enable_parallax}, clicked once) or
     * {@code number} (the entry of a number setting, like {@code menu_entry_parallax_intensity_x}) with {@code value}, typed into the number
     * picker the entry opens. The number picker closes the menu, so the setting after it right-clicks the element again. Config also
     * {@code rest_point} ([x, y], optional, where the pointer rests at the end) and {@code hold_ticks} (default 60).<br>
     * {@code save_and_view: true} saves the layout and closes the editor after the settings (see {@link #saveLayoutAndCloseEditor}), and the
     * pointer goes through the config's {@code hovers} on the screen, where settings like the Parallax Effect work. It needs
     * {@code customization: true}, and the saved layout stays in the instance: remove it after recording.
     */
    private void buildElementSettingsScript(@NotNull Minecraft minecraft) {
        Supplier<float[]> rightClickTarget;
        if (this.config.has("target")) {
            String target = this.config.get("target").getAsString();
            rightClickTarget = () -> center(editorBox(target));
        } else {
            float clickX = this.config.getAsJsonArray("right_click").get(0).getAsFloat();
            float clickY = this.config.getAsJsonArray("right_click").get(1).getAsFloat();
            rightClickTarget = () -> new float[] {clickX, clickY};
        }
        this.waitTicks(16);
        if (this.config.has("select")) {
            List<String> select = new ArrayList<>();
            for (JsonElement id : this.config.getAsJsonArray("select")) select.add(id.getAsString());
            this.selectElements(minecraft, select);
            this.waitTicks(10);
        }
        // Scripts get built before they run, so this tracks whether the menu will be open at this point of the script
        boolean menuOpen = false;
        for (JsonElement entry : this.config.getAsJsonArray("settings")) {
            JsonObject setting = entry.getAsJsonObject();
            boolean toggle = setting.has("toggle");
            String identifier = toggle ? setting.get("toggle").getAsString() : setting.get("number").getAsString();
            if (!menuOpen) {
                this.moveTo(rightClickTarget, 16);
                this.waitTicks(6);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
                this.waitUntil("right-click menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
                this.waitTicks(10);
                menuOpen = true;
            }
            this.moveTo(() -> new float[] {contextMenuEntry(identifier)[0] + 20, Math.min(this.pointerY + 50, MENU_SCROLL_TARGET_Y)}, 10);
            this.waitTicks(4);
            this.scrollMenuUntilVisible(minecraft, identifier);
            this.waitTicks(6);
            this.clickMenuEntry(minecraft, identifier);
            if (toggle) {
                this.waitTicks(22);
                continue;
            }
            this.waitUntil("number picker", () -> findWindow(NumberPickerWindowBody.class) != null, 60);
            this.waitTicks(16);
            this.key(minecraft, "shortcut+a", "Ctrl + A");
            this.waitTicks(6);
            this.type(minecraft, setting.get("value").getAsString());
            this.waitTicks(14);
            this.moveTo(() -> windowButtonCenter(requireWindow(NumberPickerWindowBody.class), "fancymenu.common_components.done"), 14);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("number picker closed", () -> findWindow(NumberPickerWindowBody.class) == null, 60);
            this.waitTicks(10);
            menuOpen = false;
        }
        if (this.config.has("save_and_view") && this.config.get("save_and_view").getAsBoolean()) {
            this.waitTicks(10);
            this.saveLayoutAndCloseEditor(minecraft);
            this.restOnHovers(minecraft);
        }
        if (this.config.has("rest_point")) {
            float x = this.config.getAsJsonArray("rest_point").get(0).getAsFloat();
            float y = this.config.getAsJsonArray("rest_point").get(1).getAsFloat();
            this.moveTo(() -> new float[] {x, y}, 14);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * In the editor: selects the elements of the ids, with a click on the first one and Ctrl + click on the others, which adds them to the
     * selection. The right-click menu of one of them then changes all of them at once, but only has the entries all of them have.
     */
    private void selectElements(@NotNull Minecraft minecraft, @NotNull List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            this.moveTo(() -> center(editorBox(id)), (i == 0) ? 16 : 10);
            this.waitTicks(4);
            if (i == 0) {
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            } else {
                this.badge("Ctrl + Click");
                this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
                this.waitTicks(1);
                this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            }
            this.waitTicks(6);
        }
    }

    /**
     * In the editor: right-clicks a free spot ({@code right_click}, [x, y]) and opens the sub-menus of {@code path} (entry identifiers, like
     * {@code menu_backgrounds} and {@code menu_background_image}), moving sideways into each one. The pointer rests on the entry
     * {@code rest_on} of the last sub-menu (optional), and the frame of the still gets a {@code mark} event ({@code still}).
     */
    private void buildMenuPathStillScript(@NotNull Minecraft minecraft) {
        float clickX = this.config.getAsJsonArray("right_click").get(0).getAsFloat();
        float clickY = this.config.getAsJsonArray("right_click").get(1).getAsFloat();
        List<String> path = new ArrayList<>();
        this.config.getAsJsonArray("path").forEach(element -> path.add(element.getAsString()));
        this.waitTicks(16);
        this.moveTo(() -> new float[] {clickX, clickY}, 16);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("right-click menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        for (int i = 0; i < path.size(); i++) {
            String[] entryPath = path.subList(0, i + 1).toArray(new String[0]);
            if (i > 0) this.moveTo(() -> new float[] {menuEntry(entryPath)[0], this.pointerY}, 10);
            this.clickMenuEntry(minecraft, entryPath);
            this.waitUntil("sub-menu " + entryPath[i], () -> subMenu(entryPath).isOpen(), 40);
            this.waitTicks(8);
        }
        if (this.config.has("rest_on")) {
            List<String> restPath = new ArrayList<>(path);
            restPath.add(this.config.get("rest_on").getAsString());
            String[] entryPath = restPath.toArray(new String[0]);
            this.moveTo(() -> new float[] {menuEntry(entryPath)[0], this.pointerY}, 10);
            this.moveTo(() -> menuEntry(entryPath), 10);
        }
        this.waitTicks(20);
        this.mark("still");
        this.waitTicks(4);
    }

    // ---- Game Intro page ----

    /**
     * Outside the editor, on a screen (config {@code start: "screen"}, {@code screen: "title"}, like with the intro's last scene as {@code background_image}).<br>
     * {@code part: "setup"} (default, needs {@code menu_bar: true}): opens Customization -> Global Customizations -> Game Intro and rests on its
     * settings for {@code menu_ticks} (default 40, with a {@code mark} event {@code menu} for a still), clicks Set Game Intro and picks
     * {@code file} (in the assets sub-folder {@code folder}, optional) through the resource chooser, then clicks Re-Trigger Game Intro. With
     * {@code skip_key} ({@code key} and {@code badge}), it presses that key {@code skip_after_ticks} (default 50) after the intro started.<br>
     * {@code part: "play"}: plays the intro of the config's {@code fancymenu_options} ({@code game_intro_animation_name}) the way Re-Trigger
     * Game Intro does, with the pointer out of the picture.<br>
     * Both mark the first frame of the intro ({@code intro}), wait until it closed and hold for {@code hold_ticks} (default 60).
     */
    private void buildGameIntroScript(@NotNull Minecraft minecraft) {
        String part = this.config.has("part") ? this.config.get("part").getAsString() : "setup";
        if (part.equals("play")) {
            this.steps.addLast(() -> {
                this.movePointer(minecraft, -40, -40);
                return true;
            });
            this.waitTicks(4);
            this.steps.addLast(() -> {
                if (GameIntroHandler.getIntro() == null) throw new IllegalStateException("No Game Intro, set game_intro_animation_name in fancymenu_options");
                // The same as Re-Trigger Game Intro
                GameIntroHandler.introPlayed = true;
                minecraft.gui.setOverlay(new GameIntroOverlay(ScreenUtils.getScreen(), GameIntroHandler.getIntro()));
                return true;
            });
            this.mark("intro");
        } else {
            this.waitTicks(20);
            this.moveTo(() -> menuBarEntry("screen"), 18);
            this.waitTicks(6);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
            this.waitTicks(10);
            this.clickMenuEntryPastIcon(minecraft, "global_customizations");
            this.waitUntil("Global Customizations menu", () -> subMenu("global_customizations").isOpen(), 40);
            this.waitTicks(8);
            // Into each sub-menu sideways, so the pointer doesn't pass over other entries
            this.moveTo(() -> new float[] {menuEntryPastIcon("global_customizations", "game_intro")[0], this.pointerY}, 10);
            this.clickMenuEntryPastIcon(minecraft, "global_customizations", "game_intro");
            this.waitUntil("Game Intro menu", () -> subMenu("global_customizations", "game_intro").isOpen(), 40);
            this.waitTicks(8);
            this.moveTo(() -> new float[] {menuEntryPastIcon("global_customizations", "game_intro", "set_game_intro")[0], this.pointerY}, 10);
            this.moveTo(() -> menuEntryPastIcon("global_customizations", "game_intro", "set_game_intro"), 8);
            this.waitTicks(8);
            this.mark("menu");
            this.waitTicks(this.config.has("menu_ticks") ? this.config.get("menu_ticks").getAsInt() : 40);
            // Set Game Intro opens the resource chooser right away when clicked
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            // Its chooser starts on Local already
            this.chooseLocalFile(minecraft, this.config.has("folder") ? this.config.get("folder").getAsString() : null, this.config.get("file").getAsString(), false);
            // Picking the file opens the menus again, down to Set Game Intro. The pointer is still where OK was, on Global Customizations,
            // which closes the Game Intro menu after a moment, so it goes back to Game Intro like a player would
            this.waitUntil("Set Game Intro menu again", () -> {
                try {
                    return subMenu("global_customizations", "game_intro", "set_game_intro").isOpen();
                } catch (IllegalStateException ex) {
                    return false;
                }
            }, 60);
            this.waitTicks(6);
            this.moveTo(() -> menuEntryPastIcon("global_customizations", "game_intro"), 12);
            this.waitUntil("Game Intro menu again", () -> subMenu("global_customizations", "game_intro").isOpen(), 40);
            this.waitTicks(14);
            this.moveTo(() -> new float[] {menuEntryPastIcon("global_customizations", "game_intro", "game_intro_retrigger")[0], this.pointerY}, 8);
            this.clickMenuEntryPastIcon(minecraft, "global_customizations", "game_intro", "game_intro_retrigger");
            this.waitUntil("intro playing", () -> minecraft.gui.overlay() instanceof GameIntroOverlay, 40);
            this.mark("intro");
            if (this.config.has("skip_key")) {
                JsonObject skip = this.config.getAsJsonObject("skip_key");
                this.waitTicks(this.config.has("skip_after_ticks") ? this.config.get("skip_after_ticks").getAsInt() : 50);
                this.key(minecraft, skip.get("key").getAsString(), skip.get("badge").getAsString());
            }
        }
        this.waitUntil("intro closed", () -> !(minecraft.gui.overlay() instanceof GameIntroOverlay), 400);
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * Like {@link #menuEntry}, but at 70% of the entry's width. At the colossal UI scale, 30% of a short entry can be its (i) tooltip icon,
     * and a hovered tooltip icon keeps the entry's sub-menu closed.
     */
    @NotNull
    private static float[] menuEntryPastIcon(@NotNull String... path) {
        ContextMenu menu = (path.length > 1) ? subMenu(Arrays.copyOf(path, path.length - 1)) : ContextMenuHandler.INSTANCE.getCurrent();
        if (menu == null) throw new IllegalStateException("No open context menu");
        ContextMenu.ContextMenuEntry<?> entry = menu.getEntry(path[path.length - 1]);
        if (entry == null) throw new IllegalStateException("No context menu entry " + String.join(" -> ", path));
        float scale = UIBase.calculateFixedRenderScale(menu.getScale());
        return new float[] {(readFloat(entry, "x") + readFloat(entry, "width") * 0.7F) * scale, (readFloat(entry, "y") + entry.getHeight() / 2.0F) * scale};
    }

    private void clickMenuEntryPastIcon(@NotNull Minecraft minecraft, @NotNull String... path) {
        this.moveTo(() -> menuEntryPastIcon(path), 12);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
    }

    // ---- Steps ----

    // ---- Getting Started page ----

    /**
     * Outside the editor, with the menu bar (config {@code start: "screen"}, {@code screen: "title"}, {@code menu_bar: true},
     * {@code no_layout: true}, {@code customization: false}): opens Customization, turns on Current Screen Customization and makes a layout
     * through Layouts -> New -> For Current Screen, which opens the layout editor. The frame with the open Customization menu has a
     * {@code mark} event ({@code menu}), for a still of the menu bar. Config {@code hold_ticks} (in the editor at the end, default 60).
     */
    private void buildFirstLayoutScript(@NotNull Minecraft minecraft) {
        this.waitTicks(24);
        this.moveTo(() -> menuBarEntry("screen"), 18);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Customization menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(12);
        this.mark("menu");
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, "toggle_current_customization");
        this.waitUntil("customization turned on", () -> ScreenCustomization.isCustomizationEnabledForScreen(ScreenUtils.getScreen()), 40);
        // Turning it on rebuilds the screen and its menu bar, which closes the menu, so open it again like a player would
        this.waitTicks(14);
        this.moveTo(() -> menuBarEntry("screen"), 12);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Customization menu again", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(16);
        // Layouts -> New -> For Current Screen, going into each sub-menu sideways
        this.clickMenuEntry(minecraft, "layouts");
        this.waitUntil("Layouts menu", () -> subMenu("layouts").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("layouts", "new_layout")[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "layouts", "new_layout");
        this.waitUntil("New menu", () -> subMenu("layouts", "new_layout").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("layouts", "new_layout", "new_layout_for_current")[0], this.pointerY}, 10);
        this.waitTicks(4);
        this.clickMenuEntry(minecraft, "layouts", "new_layout", "new_layout_for_current");
        this.waitUntil("layout editor", () -> ScreenUtils.getScreen() instanceof LayoutEditorScreen, 60);
        this.steps.addLast(() -> {
            // Like at the start of editor showcases: the editor widgets would cover the right part of the screen
            if (!(this.config.has("show_widgets") && this.config.get("show_widgets").getAsBoolean())) {
                requireLayoutEditor().layoutEditorWidgets.forEach(widget -> widget.setVisible(false));
            }
            return true;
        });
        this.waitTicks(10);
        this.moveTo(() -> new float[] {GUI_WIDTH * 0.5F, GUI_HEIGHT * 0.86F}, 20);
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * In the editor: right-clicks the background, opens Menu Backgrounds -> Image, turns on Show Background, clicks Choose Image and picks a
     * file of the assets folder (see {@link #chooseLocalFile}). Config: {@code right_click} ([x, y] of the free spot), {@code folder} (optional
     * sub-folder of the assets folder), {@code file}, {@code free_spot} ([x, y], clicked at the end) and {@code hold_ticks} (default 70).
     */
    private void buildMenuBackgroundScript(@NotNull Minecraft minecraft) {
        float clickX = this.config.getAsJsonArray("right_click").get(0).getAsFloat();
        float clickY = this.config.getAsJsonArray("right_click").get(1).getAsFloat();
        this.waitTicks(16);
        this.moveTo(() -> new float[] {clickX, clickY}, 18);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.clickMenuEntry(minecraft, "menu_backgrounds");
        this.waitUntil("Menu Backgrounds menu", () -> subMenu("menu_backgrounds").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("menu_backgrounds", "menu_background_image")[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "menu_backgrounds", "menu_background_image");
        this.waitUntil("Image background menu", () -> subMenu("menu_backgrounds", "menu_background_image").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("menu_backgrounds", "menu_background_image", "menu_entry_show_background")[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "menu_backgrounds", "menu_background_image", "menu_entry_show_background");
        this.waitTicks(16);
        this.clickMenuEntry(minecraft, "menu_backgrounds", "menu_background_image", "menu_entry_image_path");
        this.chooseLocalFile(minecraft, this.config.has("folder") ? this.config.get("folder").getAsString() : null, this.config.get("file").getAsString());
        this.waitTicks(16);
        this.clickFreeSpotAndHold(minecraft, 70);
    }

    /**
     * In the editor: right-clicks {@code right_click}, opens Menu Backgrounds -> Cubic Panorama, turns on Show Background, clicks
     * Panorama Name, picks the panorama {@code panorama} in Choose Panorama, rests on its preview for {@code preview_ticks} (default 50)
     * and clicks Done. Then it clicks {@code free_spot} and holds for {@code hold_ticks} (default 70).
     */
    private void buildPanoramaBackgroundScript(@NotNull Minecraft minecraft) {
        String panorama = this.config.get("panorama").getAsString();
        Class<?> chooser = de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen.class;
        this.buildChooserBackgroundScript(minecraft, "menu_background_panorama", "panorama_name", chooser, () -> chooserListEntry(chooser, "panoramaListScrollArea", entry -> (entry instanceof de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen.PanoramaScrollEntry panoramaEntry) && panoramaEntry.panorama.equals(panorama), panorama));
    }

    /**
     * In the editor: right-clicks {@code right_click}, opens Menu Backgrounds -> Slideshow, turns on Show Background, clicks
     * Slideshow Name, picks the slideshow {@code slideshow} in Choose Slideshow, rests on its preview for {@code preview_ticks}
     * (default 50) and clicks Done. Then it clicks {@code free_spot} and holds for {@code hold_ticks} (default 70).
     */
    private void buildSlideshowBackgroundScript(@NotNull Minecraft minecraft) {
        String slideshow = this.config.get("slideshow").getAsString();
        Class<?> chooser = de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen.class;
        this.buildChooserBackgroundScript(minecraft, "menu_background_slideshow", "slideshow_name", chooser, () -> chooserListEntry(chooser, "slideshowListScrollArea", entry -> (entry instanceof de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen.SlideshowScrollEntry slideshowEntry) && slideshowEntry.slideshow.equals(slideshow), slideshow));
    }

    /**
     * In the editor: right-clicks {@code right_click}, opens the sub-menu {@code background} of Menu Backgrounds, turns on Show
     * Background, clicks its {@code nameEntry}, picks the entry at {@code pickEntry} in the {@code chooser} window, rests on its
     * preview for {@code preview_ticks} (default 50) and clicks Done. Then it clicks {@code free_spot} and holds for
     * {@code hold_ticks} (default 70).
     */
    private void buildChooserBackgroundScript(@NotNull Minecraft minecraft, @NotNull String background, @NotNull String nameEntry, @NotNull Class<?> chooser, @NotNull Supplier<float[]> pickEntry) {
        float clickX = this.config.getAsJsonArray("right_click").get(0).getAsFloat();
        float clickY = this.config.getAsJsonArray("right_click").get(1).getAsFloat();
        this.waitTicks(16);
        this.moveTo(() -> new float[] {clickX, clickY}, 18);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("context menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.clickMenuEntry(minecraft, "menu_backgrounds");
        this.waitUntil("Menu Backgrounds menu", () -> subMenu("menu_backgrounds").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("menu_backgrounds", background)[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "menu_backgrounds", background);
        this.waitUntil(background + " menu", () -> subMenu("menu_backgrounds", background).isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("menu_backgrounds", background, "menu_entry_show_background")[0], this.pointerY}, 10);
        this.clickMenuEntry(minecraft, "menu_backgrounds", background, "menu_entry_show_background");
        this.waitTicks(16);
        this.clickMenuEntry(minecraft, "menu_backgrounds", background, nameEntry);
        this.waitUntil(chooser.getSimpleName(), () -> findWindow(chooser) != null, 60);
        this.waitTicks(16);
        this.moveTo(pickEntry, 14);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(this.config.has("preview_ticks") ? this.config.get("preview_ticks").getAsInt() : 50);
        this.moveTo(() -> windowWidgetCenter(requireWindow(chooser), "doneButton"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil(chooser.getSimpleName() + " closed", () -> findWindow(chooser) == null, 60);
        this.waitTicks(16);
        this.clickFreeSpotAndHold(minecraft, 70);
    }

    /**
     * The GUI position of the first entry that {@code matches} in the list (field {@code listField}) of the open {@code chooser}
     * window, a bit right of the start of its name. {@code name} only goes into the error when no entry matches.
     */
    @NotNull
    private static float[] chooserListEntry(@NotNull Class<?> chooser, @NotNull String listField, @NotNull java.util.function.Predicate<ScrollAreaEntry> matches, @NotNull String name) {
        PiPWindow window = requireWindow(chooser);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), listField);
        for (ScrollAreaEntry entry : list.getEntries()) {
            if (matches.test(entry)) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 2.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException(chooser.getSimpleName() + " lists no " + name);
    }

    /**
     * In the editor: removes vanilla elements (config {@code remove}: their ids, each clicked and deleted with the Delete key), adds an
     * Image element through Element -> New of the editor's menu bar, picks its image through Image Source (see {@link #chooseLocalFile}),
     * clicks Restore Aspect Ratio and drags it so its center is at {@code drag_to}. Config also: {@code folder}, {@code file},
     * {@code free_spot} and {@code hold_ticks} (default 70).
     */
    private void buildAddImageScript(@NotNull Minecraft minecraft) {
        float dragX = this.config.getAsJsonArray("drag_to").get(0).getAsFloat();
        float dragY = this.config.getAsJsonArray("drag_to").get(1).getAsFloat();
        String entry = newElementEntry("image");
        this.waitTicks(16);
        if (this.config.has("remove")) {
            for (JsonElement id : this.config.getAsJsonArray("remove")) {
                String target = id.getAsString();
                this.moveTo(() -> center(editorBox(target)), 14);
                this.waitTicks(5);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(10);
                this.key(minecraft, "delete", "Delete");
                this.waitTicks(14);
            }
        }
        int[] elementsBefore = {0};
        this.steps.addLast(() -> {
            elementsBefore[0] = requireLayoutEditor().normalEditorElements.size();
            return true;
        });
        // Element -> New -> Image in the editor's menu bar
        this.moveTo(() -> editorMenuBarEntry("element_tab"), 16);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Element menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 40);
        this.waitTicks(8);
        this.clickMenuEntry(minecraft, "new_element");
        this.waitUntil("New menu", () -> subMenu("new_element").isOpen(), 40);
        this.waitTicks(8);
        this.moveTo(() -> new float[] {menuEntry("new_element", entry)[0], this.pointerY}, 10);
        this.waitTicks(3);
        this.clickMenuEntry(minecraft, "new_element", entry);
        this.waitUntil("new element", () -> requireLayoutEditor().normalEditorElements.size() > elementsBefore[0], 40);
        this.waitTicks(14);
        // Image Source
        this.rightClickNewestAndPick(minecraft, "menu_entry_source");
        this.chooseLocalFile(minecraft, this.config.has("folder") ? this.config.get("folder").getAsString() : null, this.config.get("file").getAsString());
        this.waitTicks(20);
        // Restore Aspect Ratio. Its menu stays open, so close it with Esc before dragging, or the drag would click its entries.
        this.rightClickNewestAndPick(minecraft, "restore_aspect_ratio");
        this.waitTicks(14);
        this.key(minecraft, "escape", "Esc");
        this.waitTicks(10);
        // Drag it to its place
        this.moveTo(() -> center(newestElementBox()), 14);
        this.waitTicks(4);
        this.press(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.moveTo(() -> new float[] {dragX, dragY}, 24);
        this.waitTicks(4);
        this.release(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitTicks(12);
        this.clickFreeSpotAndHold(minecraft, 70);
    }

    /**
     * In the editor: saves the layout with Ctrl + S (the Save File window opens the first time, its file name stays), closes the editor with
     * the X of the menu bar and rests the pointer on the screen with the layout. Config {@code hovers} (like {@link #buildHoverElementsScript},
     * optional) and {@code hold_ticks} (default 60). The saved layout stays in the instance: remove it after recording.
     */
    private void buildSaveLayoutScript(@NotNull Minecraft minecraft) {
        this.waitTicks(20);
        this.saveLayoutAndCloseEditor(minecraft);
        this.restOnHovers(minecraft);
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * In the editor: saves the layout with Ctrl + S (the Save File window opens the first time, its file name stays) and closes the editor
     * with the X of the menu bar. The saved layout stays in the instance: remove it after recording.
     */
    private void saveLayoutAndCloseEditor(@NotNull Minecraft minecraft) {
        this.key(minecraft, "shortcut+s", "Ctrl + S");
        this.waitUntil("Save File", () -> findWindow(SaveFileWindowBody.class) != null, 60);
        this.waitTicks(24);
        this.moveTo(() -> windowWidgetCenter(requireWindow(SaveFileWindowBody.class), "confirmButton"), 16);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("Save File closed", () -> findWindow(SaveFileWindowBody.class) == null, 60);
        this.waitTicks(16);
        this.moveTo(() -> editorMenuBarEntry("close_editor"), 18);
        this.waitTicks(6);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("editor closed", () -> !(ScreenUtils.getScreen() instanceof LayoutEditorScreen), 60);
        // The X of the editor's menu bar is where the screen's menu bar has its door icon, so leave before its tooltip shows
        this.waitTicks(3);
    }

    /**
     * Outside the editor: rests the pointer on the config's {@code hovers}, if it has them: each a {@code label} (of a vanilla widget) or a
     * {@code point} ([x, y]), with {@code move_ticks} (default 18) and {@code ticks} (how long the pointer rests, default 30).
     */
    private void restOnHovers(@NotNull Minecraft minecraft) {
        if (this.config.has("hovers")) {
            for (JsonElement entry : this.config.getAsJsonArray("hovers")) {
                JsonObject hover = entry.getAsJsonObject();
                int moveTicks = hover.has("move_ticks") ? hover.get("move_ticks").getAsInt() : 18;
                if (hover.has("label")) {
                    String label = hover.get("label").getAsString();
                    this.moveTo(() -> center(vanillaWidgetBox(label)), moveTicks);
                } else {
                    float x = hover.getAsJsonArray("point").get(0).getAsFloat();
                    float y = hover.getAsJsonArray("point").get(1).getAsFloat();
                    this.moveTo(() -> new float[] {x, y}, moveTicks);
                }
                this.waitTicks(hover.has("ticks") ? hover.get("ticks").getAsInt() : 30);
            }
        }
    }

    /**
     * In the editor: selects the buttons of {@code select} (their ids: a click on the first one, Ctrl + click on the others), so the
     * right-click menu of the {@code target} button changes all of them at once. Only buttons: the menu of several elements only has the
     * entries all of them have, and some vanilla widgets aren't buttons, like the icons next to the Realms button. Then Button Textures -> Set -> Normal Background
     * ({@code normal}) and Hover Background ({@code hover}), both picked like {@link #chooseLocalFile} from the assets sub-folder
     * {@code folder}, and Button Textures -> Nine-Slice Custom Background, whose menu stays open, so Esc closes it. Config also
     * {@code free_spot} ([x, y], clicked at the end to unselect the buttons), {@code rest_on} (a vanilla element id the pointer rests on
     * at the end, optional) and {@code hold_ticks} (default 60).
     */
    private void buildButtonTexturesScript(@NotNull Minecraft minecraft) {
        List<String> select = new ArrayList<>();
        for (JsonElement id : this.config.getAsJsonArray("select")) select.add(id.getAsString());
        String target = this.config.get("target").getAsString();
        String folder = this.config.has("folder") ? this.config.get("folder").getAsString() : null;
        this.waitTicks(16);
        this.selectElements(minecraft, select);
        this.waitTicks(10);
        // Normal and hover textures for all of them
        this.openButtonTexturesMenu(minecraft, target);
        this.clickSubMenuEntrySideways(minecraft, "button_background", "set_background");
        this.waitUntil("Set menu", () -> subMenu("button_background", "set_background").isOpen(), 40);
        this.waitTicks(6);
        this.clickSubMenuEntrySideways(minecraft, "button_background", "set_background", "normal_background_texture");
        this.chooseLocalFile(minecraft, folder, this.config.get("normal").getAsString());
        this.waitTicks(20);
        this.openButtonTexturesMenu(minecraft, target);
        this.clickSubMenuEntrySideways(minecraft, "button_background", "set_background");
        this.waitUntil("Set menu", () -> subMenu("button_background", "set_background").isOpen(), 40);
        this.waitTicks(6);
        this.clickSubMenuEntrySideways(minecraft, "button_background", "set_background", "hover_background_texture");
        this.chooseLocalFile(minecraft, folder, this.config.get("hover").getAsString());
        this.waitTicks(20);
        // Nine-slicing keeps the corners of the textures sharp on every button width. Its menu stays open, so Esc closes it.
        this.openButtonTexturesMenu(minecraft, target);
        this.clickSubMenuEntrySideways(minecraft, "button_background", "nine_slice_background");
        this.waitTicks(18);
        this.key(minecraft, "escape", "Esc");
        this.waitTicks(12);
        if (this.config.has("free_spot")) {
            float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
            float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
            this.moveTo(() -> new float[] {freeX, freeY}, 16);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(10);
        }
        if (this.config.has("rest_on")) {
            String restOn = this.config.get("rest_on").getAsString();
            this.moveTo(() -> center(editorBox(restOn)), 16);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : 60);
    }

    /**
     * Saves the layout the config built (its {@code elements}, {@code vanilla}, background, {@code overlays} and so on) into the
     * {@code customization} folder of the profile directory {@code profile_dir} as {@code layout_name}.txt, then packs the profile
     * directory into the {@code .fmp} archive {@code archive} with FancyMenu's own profile archive writer and reads its metadata back,
     * like an import would. For downloadable example profiles, like the Wildwood title screen of the Getting Started page. Use
     * {@code start: "screen"} and {@code no_layout: true}, so nothing of the showcase layout touches the instance.
     */
    private void buildWriteProfileScript(@NotNull Minecraft minecraft) {
        Path profileDirectory = Path.of(this.config.get("profile_dir").getAsString());
        Path archive = Path.of(this.config.get("archive").getAsString());
        String layoutName = this.config.get("layout_name").getAsString();
        this.waitTicks(10);
        this.steps.addLast(() -> {
            Layout layout = this.builtLayout;
            if (layout == null) throw new IllegalStateException("No layout got built");
            try {
                Path layoutFile = profileDirectory.resolve("customization").resolve(layoutName + ".txt");
                Files.createDirectories(layoutFile.getParent());
                if (!LayoutHandler.saveLayoutToFile(layout, layoutFile.toAbsolutePath().toString())) throw new IllegalStateException("Saving the layout failed");
                this.pass("layout saved", layoutFile.toString());
                de.keksuccino.fancymenu.customization.profile.ProfileArchive.write(profileDirectory, path -> !de.keksuccino.fancymenu.customization.profile.ProfileStorage.isJunkFile(path), archive, new de.keksuccino.fancymenu.customization.profile.ProfileTaskProgress());
                var metadata = de.keksuccino.fancymenu.customization.profile.ProfileArchive.readMetadata(archive, "fallback");
                this.pass("profile archive", archive + " (" + Files.size(archive) + " bytes) id=" + metadata.getId() + " title=" + metadata.getTitle());
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Writing the profile failed", ex);
            }
            return true;
        });
        this.waitTicks(4);
    }

    /**
     * Right-clicks the (selected) element and opens Button Textures of its right-click menu, scrolling the menu first if needed.
     */
    private void openButtonTexturesMenu(@NotNull Minecraft minecraft, @NotNull String target) {
        this.moveTo(() -> center(editorBox(target)), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("button menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(8);
        this.scrollMenuUntilInside(minecraft, "button_background");
        this.waitTicks(4);
        this.clickMenuEntry(minecraft, "button_background");
        this.waitUntil("Button Textures menu", () -> subMenu("button_background").isOpen(), 40);
        this.waitTicks(6);
    }

    /**
     * Moves sideways into the sub-menu of the path first, so the pointer doesn't pass over other entries, then clicks the entry.
     */
    private void clickSubMenuEntrySideways(@NotNull Minecraft minecraft, @NotNull String... path) {
        this.moveTo(() -> new float[] {menuEntry(path)[0], this.pointerY}, 10);
        this.waitTicks(3);
        this.clickMenuEntry(minecraft, path);
    }

    /**
     * Right-clicks the element that got added last and clicks the entry of its right-click menu, scrolling the menu first if needed.
     */
    private void rightClickNewestAndPick(@NotNull Minecraft minecraft, @NotNull String entry) {
        this.moveTo(() -> center(newestElementBox()), 14);
        this.waitTicks(4);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_RIGHT);
        this.waitUntil("element menu", () -> (ContextMenuHandler.INSTANCE.getCurrent() != null) && ContextMenuHandler.INSTANCE.getCurrent().isOpen(), 60);
        this.waitTicks(10);
        this.moveTo(() -> new float[] {contextMenuEntry(entry)[0] + 20, Math.min(this.pointerY + 70, MENU_SCROLL_TARGET_Y)}, 12);
        this.waitTicks(4);
        this.scrollMenuUntilInside(minecraft, entry);
        this.waitTicks(6);
        this.clickMenuEntry(minecraft, entry);
    }

    /**
     * Turns the mouse wheel over the open context menu every second tick until the entry is fully inside the visible part of the menu.
     * Unlike {@link #scrollMenuUntilVisible(Minecraft, String, float)}, this works for menus of any height, like a menu opened near the
     * top of the screen, which ends higher up.
     */
    private void scrollMenuUntilInside(@NotNull Minecraft minecraft, @NotNull String identifier) {
        int[] cooldown = {0};
        int[] turns = {0};
        this.steps.addLast(() -> {
            if (cooldown[0]-- > 0) return false;
            ContextMenu menu = ContextMenuHandler.INSTANCE.getCurrent();
            if (menu == null) throw new IllegalStateException("No open context menu");
            ContextMenu.ContextMenuEntry<?> entry = menu.getEntry(identifier);
            if (entry == null) throw new IllegalStateException("No context menu entry " + identifier);
            if (!((boolean) readField(menu, "needsScrolling")) || isFullyInsideMenu(menu, entry)) {
                this.pass("scroll menu to " + identifier, turns[0] + " wheel turns");
                return true;
            }
            if (++turns[0] > 40) throw new IllegalStateException("Scrolling never reached " + identifier);
            // Two wheel steps per frame, because these menus are long
            minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0.0, -2.0);
            cooldown[0] = 1;
            return false;
        });
    }

    /**
     * If the entry is fully inside the visible part of the scrolling menu, between its scroll arrows (see ContextMenu#isEntryVisible).
     */
    private static boolean isFullyInsideMenu(@NotNull ContextMenu menu, @NotNull ContextMenu.ContextMenuEntry<?> entry) {
        float scale = UIBase.calculateFixedRenderScale(menu.getScale());
        float top = ((Number) invoke(menu, "getActualY")).floatValue() / scale + menu.getBorderThickness();
        float arrows = ((Number) readField(menu, "SCROLL_INDICATOR_HEIGHT")).floatValue();
        float visibleTop = top + arrows;
        float visibleBottom = top + readFloat(menu, "displayHeight") - arrows;
        float entryY = readFloat(entry, "y");
        return (entryY >= visibleTop) && ((entryY + entry.getHeight()) <= visibleBottom);
    }

    /**
     * Adds the steps the builder adds, but each of them only runs if {@code skip[0]} is false when it's its turn, which a step before
     * them decides at runtime, like for a folder that is already open.
     */
    private void unlessSkipped(@NotNull boolean[] skip, @NotNull Runnable builder) {
        int before = this.steps.size();
        builder.run();
        List<Step> added = new ArrayList<>();
        while (this.steps.size() > before) added.add(0, this.steps.pollLast());
        for (Step step : added) this.steps.addLast(() -> skip[0] || step.run());
    }

    /**
     * In the resource chooser that is opening: switches Source Type to Local, clicks Choose Local Resource File, opens the folder with a
     * double-click in the file chooser, clicks the file (its preview shows) and clicks OK, which closes both windows and sets the resource.
     */
    private void chooseLocalFile(@NotNull Minecraft minecraft, @Nullable String folder, @NotNull String fileName) {
        this.chooseLocalFile(minecraft, folder, fileName, true);
    }

    /**
     * Like {@link #chooseLocalFile(Minecraft, String, String)}. Without {@code switchToLocal}, it doesn't click Source Type first, for
     * choosers that start on Local already, like the one of Set Game Intro.
     */
    private void chooseLocalFile(@NotNull Minecraft minecraft, @Nullable String folder, @NotNull String fileName, boolean switchToLocal) {
        this.waitUntil("resource chooser", () -> findWindow(ResourceChooserWindowBody.class) != null, 60);
        this.waitTicks(12);
        if (switchToLocal) {
            this.moveTo(() -> cellWidgetWithLabel("fancymenu.resources.source_type"), 11);
            this.waitTicks(5);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
            this.waitTicks(10);
        }
        this.moveTo(() -> cellWidgetWithLabel("fancymenu.resources.chooser_screen.choose_local"), 11);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("file chooser", () -> findWindow(ChooseFileWindowBody.class) != null, 60);
        this.waitTicks(12);
        if (folder != null) {
            // The file chooser remembers the last folder of the game session, so a second chooser can already be in the folder
            BooleanSupplier inFolder = () -> folder.equals(((File) readField(requireWindow(ChooseFileWindowBody.class).getScreen(), "currentDir")).getName());
            boolean[] skip = {false};
            this.steps.addLast(() -> {
                skip[0] = inFolder.getAsBoolean();
                if (skip[0]) this.pass("file chooser already in " + folder, "no double-click");
                return true;
            });
            this.unlessSkipped(skip, () -> {
                this.moveTo(() -> fileEntry(folder), 11);
                this.waitTicks(5);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitTicks(2);
                this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
                this.waitUntil("folder " + folder, inFolder, 40);
                this.waitTicks(8);
            });
        }
        this.scrollFileListTo(minecraft, fileName);
        this.moveTo(() -> fileEntry(fileName), 11);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        // The preview of the image loads
        this.waitTicks(16);
        this.moveTo(() -> windowWidgetCenter(requireWindow(ChooseFileWindowBody.class), "confirmButton"), 12);
        this.waitTicks(5);
        this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        this.waitUntil("choosers closed", () -> (findWindow(ResourceChooserWindowBody.class) == null) && (findWindow(ChooseFileWindowBody.class) == null), 60);
    }

    private void clickFreeSpotAndHold(@NotNull Minecraft minecraft, int defaultHold) {
        if (this.config.has("free_spot")) {
            float freeX = this.config.getAsJsonArray("free_spot").get(0).getAsFloat();
            float freeY = this.config.getAsJsonArray("free_spot").get(1).getAsFloat();
            this.moveTo(() -> new float[] {freeX, freeY}, 16);
            this.waitTicks(4);
            this.click(minecraft, InputConstants.MOUSE_BUTTON_LEFT);
        }
        this.waitTicks(this.config.has("hold_ticks") ? this.config.get("hold_ticks").getAsInt() : defaultHold);
    }

    /**
     * Moves the pointer over the file list of the open file chooser and turns the mouse wheel every second tick until the file or folder
     * is fully inside the visible part of the list, like for files below the first few entries.
     */
    private void scrollFileListTo(@NotNull Minecraft minecraft, @NotNull String name) {
        this.moveTo(() -> {
            PiPWindow window = requireWindow(ChooseFileWindowBody.class);
            ScrollArea list = (ScrollArea) readField(window.getScreen(), "fileListScrollArea");
            return bodyToGui(window, list.getInnerX() + list.getInnerWidth() * 0.6F, list.getInnerY() + list.getInnerHeight() * 0.6F);
        }, 12);
        this.waitTicks(4);
        int[] cooldown = {0};
        int[] turns = {0};
        this.steps.addLast(() -> {
            if (cooldown[0]-- > 0) return false;
            ScrollArea list = (ScrollArea) readField(requireWindow(ChooseFileWindowBody.class).getScreen(), "fileListScrollArea");
            ScrollAreaEntry entry = findFileEntry(list, name);
            if ((entry.getY() >= list.getInnerY()) && ((entry.getY() + entry.getHeight()) <= (list.getInnerY() + list.getInnerHeight()))) {
                this.pass("scroll file list to " + name, turns[0] + " wheel turns");
                return true;
            }
            if (++turns[0] > 30) throw new IllegalStateException("Scrolling never reached " + name);
            minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0.0, -1.0);
            cooldown[0] = 1;
            return false;
        });
        this.waitTicks(6);
    }

    @NotNull
    private static ScrollAreaEntry findFileEntry(@NotNull ScrollArea list, @NotNull String name) {
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof AbstractFileBrowserWindowBody.AbstractFileScrollAreaEntry fileEntry) && fileEntry.file.getName().equals(name)) return entry;
        }
        throw new IllegalStateException("The file chooser lists no " + name);
    }

    /**
     * The GUI position of a file or folder of the open file chooser, a bit right of the start of its name.
     */
    @NotNull
    private static float[] fileEntry(@NotNull String name) {
        PiPWindow window = requireWindow(ChooseFileWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "fileListScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof AbstractFileBrowserWindowBody.AbstractFileScrollAreaEntry fileEntry) && fileEntry.file.getName().equals(name)) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 2.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The file chooser lists no " + name);
    }

    /**
     * The GUI position of the middle of an entry of the layout editor's menu bar, like {@code close_editor} (the X at its right).
     */
    @NotNull
    private static float[] editorMenuBarEntry(@NotNull String identifier) {
        MenuBar bar = requireLayoutEditor().getCurrentMenuBar();
        if (bar == null) throw new IllegalStateException("The editor has no menu bar");
        MenuBar.MenuBarEntry entry = bar.getEntry(identifier);
        if (entry == null) throw new IllegalStateException("No editor menu bar entry " + identifier);
        float scale = MenuBar.getRenderScale();
        float x = readFloat(entry, "x");
        int width = (int) invoke(entry, "getWidth");
        return new float[] {(x + width / 2.0F) * scale, (MenuBar.PIXEL_SIZE / 2.0F) * scale};
    }

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

    @NotNull
    private static LayerLayoutEditorWidget requireLayersWidget() {
        for (AbstractLayoutEditorWidget widget : requireLayoutEditor().layoutEditorWidgets) {
            if (widget instanceof LayerLayoutEditorWidget layers) return layers;
        }
        throw new IllegalStateException("The layout editor has no Layers widget");
    }

    /**
     * The GUI position of a point in the body of the Layers widget, which renders at the UI scale and has its own coordinates.
     */
    @NotNull
    private static float[] layersBodyToGui(@NotNull LayerLayoutEditorWidget widget, float bodyX, float bodyY) {
        float scale = UIBase.calculateFixedRenderScale(UIBase.getUIScale());
        return new float[] {(widget.getTranslatedX() + widget.getRealBodyX() + bodyX) * scale, (widget.getTranslatedY() + widget.getRealBodyY() + bodyY) * scale};
    }

    @NotNull
    private static List<ScrollAreaEntry> layersEntries(@NotNull LayerLayoutEditorWidget widget) {
        return ((ScrollArea) readField(widget, "scrollArea")).getEntries();
    }

    /**
     * The GUI position of the layer of an element in the Layers widget, a bit right of the start of its name. {@code relY} is where
     * in the height of the row, like {@code 0.8} for its lower part.
     */
    @NotNull
    private static float[] layerRow(@NotNull String instanceIdentifier, float relY) {
        LayerLayoutEditorWidget widget = requireLayersWidget();
        for (ScrollAreaEntry entry : layersEntries(widget)) {
            if (entry.getClass().getSimpleName().equals("LayerElementEntry")
                    && ((AbstractEditorElement<?, ?>) readField(entry, "element")).element.getInstanceIdentifier().equals(instanceIdentifier)) {
                return layersBodyToGui(widget, entry.getX() + Math.min(80.0F, entry.getWidth() / 3.0F), entry.getY() + entry.getHeight() * relY);
            }
        }
        throw new IllegalStateException("Layers shows no layer of " + instanceIdentifier);
    }

    /**
     * The GUI position of the first layer group in the Layers widget, a bit right of the start of its name.
     */
    @NotNull
    private static float[] layerGroupRow(float relY) {
        LayerLayoutEditorWidget widget = requireLayersWidget();
        ScrollAreaEntry entry = firstLayerGroupEntry(widget);
        return layersBodyToGui(widget, entry.getX() + Math.min(80.0F, entry.getWidth() / 3.0F), entry.getY() + entry.getHeight() * relY);
    }

    /**
     * The GUI position of a button of the first layer group in the Layers widget: {@code eyeButton} or {@code collapseButton}.
     */
    @NotNull
    private static float[] layerGroupButton(@NotNull String buttonField) {
        LayerLayoutEditorWidget widget = requireLayersWidget();
        UIIconButton button = (UIIconButton) readField(firstLayerGroupEntry(widget), buttonField);
        return layersBodyToGui(widget, button.getX() + button.getWidth() / 2.0F, button.getY() + button.getHeight() / 2.0F);
    }

    @NotNull
    private static ScrollAreaEntry firstLayerGroupEntry(@NotNull LayerLayoutEditorWidget widget) {
        for (ScrollAreaEntry entry : layersEntries(widget)) {
            if (entry.getClass().getSimpleName().equals("LayerGroupEntry")) return entry;
        }
        throw new IllegalStateException("Layers shows no group");
    }

    /**
     * The GUI position of an empty spot of the Layers widget, below its rows.
     */
    @NotNull
    private static float[] layersFreeSpot() {
        LayerLayoutEditorWidget widget = requireLayersWidget();
        List<ScrollAreaEntry> entries = layersEntries(widget);
        ScrollAreaEntry last = entries.get(entries.size() - 1);
        return layersBodyToGui(widget, widget.getBodyWidth() * 0.4F, Math.min(last.getY() + last.getHeight() + 40.0F, widget.getBodyHeight() - 12.0F));
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
     * The GUI position of the ID of a scheduler in the Manage Schedulers window. {@code null} finds the scheduler that got added in the
     * window, by the random ID new schedulers get ({@code scheduler_<UUID>}).
     */
    @NotNull
    private static float[] schedulerCell(@Nullable String identifier) {
        PiPWindow window = requireWindow(ManageSchedulersWindowBody.class);
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) readField(window.getScreen(), "allCells");
        for (Object cell : cells) {
            if (cell instanceof ManageSchedulersWindowBody.SchedulerInstanceCell schedulerCell) {
                String id = ((SchedulerInstance) readField(schedulerCell, "instance")).getIdentifier();
                if ((identifier == null) ? id.startsWith("scheduler_") : id.equals(identifier)) {
                    return bodyToGui(window, schedulerCell.getX() + Math.min(40.0F, schedulerCell.getWidth() / 2.0F), schedulerCell.getY() + schedulerCell.getHeight() / 2.0F);
                }
            }
        }
        throw new IllegalStateException("Manage Schedulers lists no " + ((identifier == null) ? "new scheduler" : identifier));
    }

    /**
     * The GUI position of the right part of an entry of the open context menu, away from the info icon after its name.
     */
    @NotNull
    private static float[] menuEntryRightPart(@NotNull String identifier) {
        ContextMenu menu = ContextMenuHandler.INSTANCE.getCurrent();
        if (menu == null) throw new IllegalStateException("No open context menu");
        ContextMenu.ContextMenuEntry<?> entry = menu.getEntry(identifier);
        if (entry == null) throw new IllegalStateException("No context menu entry " + identifier);
        float scale = UIBase.calculateFixedRenderScale(menu.getScale());
        return new float[] {(readFloat(entry, "x") + readFloat(entry, "width") * 0.75F) * scale, (readFloat(entry, "y") + entry.getHeight() / 2.0F) * scale};
    }

    /**
     * The GUI position of the identifier of a Custom GUI in the Manage Custom GUIs window.
     */
    @NotNull
    private static float[] customGuiCell(@NotNull String identifier) {
        PiPWindow window = requireWindow(ManageCustomGuisScreen.class);
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) readField(window.getScreen(), "allCells");
        for (Object cell : cells) {
            if ((cell instanceof ManageCustomGuisScreen.CustomGuiCell guiCell) && ((CustomGui) readField(guiCell, "gui")).identifier.equals(identifier)) {
                return bodyToGui(window, guiCell.getX() + Math.min(40.0F, guiCell.getWidth() / 2.0F), guiCell.getY() + guiCell.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("Manage Custom GUIs lists no " + identifier);
    }

    /**
     * The GUI position of an empty spot of the list of the Manage Schedulers window, below its rows.
     */
    @NotNull
    private static float[] schedulerListFreeSpot() {
        PiPWindow window = requireWindow(ManageSchedulersWindowBody.class);
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

    /**
     * The visible Manage Requirements window. The window of an open group is one too, and the window that opened it hides meanwhile.
     */
    @Nullable
    private static PiPWindow findRequirementsWindow() {
        for (PiPWindow window : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            if ((window.getScreen() instanceof ManageRequirementsWindowBody) && window.isVisible()) return window;
        }
        return null;
    }

    @NotNull
    private static PiPWindow requireRequirementsWindow() {
        PiPWindow window = findRequirementsWindow();
        if (window == null) throw new IllegalStateException("No visible Manage Requirements window");
        return window;
    }

    /**
     * If the Manage Requirements window shows the content of a group, which it does when it has a group path.
     */
    private static boolean isGroupWindow(@Nullable PiPWindow window) {
        return (window != null) && (readField(window.getScreen(), "groupPath") != null);
    }

    /**
     * The GUI position of a free spot of the list of the visible Manage Requirements window, below its rows.
     */
    @NotNull
    private static float[] requirementsListFreeSpot() {
        PiPWindow window = requireRequirementsWindow();
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "requirementsScrollArea");
        return bodyToGui(window, list.getXWithBorder() + list.getWidthWithBorder() * 0.45F, list.getYWithBorder() + list.getHeightWithBorder() * 0.72F);
    }

    /**
     * The GUI position of the row of a group (by its identifier) or a requirement (by its type identifier) in the visible Manage Requirements window.
     */
    @NotNull
    private static float[] requirementRow(@NotNull String groupOrRequirement) {
        PiPWindow window = requireRequirementsWindow();
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "requirementsScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            boolean group = (entry instanceof ManageRequirementsWindowBody.RequirementGroupEntry groupEntry) && groupEntry.group.identifier.equals(groupOrRequirement);
            boolean requirement = (entry instanceof ManageRequirementsWindowBody.RequirementInstanceEntry instanceEntry) && instanceEntry.instance.requirement.getIdentifier().equals(groupOrRequirement);
            if (group || requirement) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 3.0F), entry.getY() + Math.min(entry.getHeight() / 2.0F, 9.0F));
            }
        }
        throw new IllegalStateException("Manage Requirements lists no " + groupOrRequirement);
    }

    /**
     * The GUI position of the entry of a requirement in the list of the requirement chooser, a bit right of the start of its name.
     */
    @NotNull
    private static float[] requirementChooserEntry(@NotNull String requirementIdentifier) {
        PiPWindow window = requireWindow(BuildRequirementWindowBody.class);
        ScrollArea list = (ScrollArea) readField(window.getScreen(), "requirementsListScrollArea");
        for (ScrollAreaEntry entry : list.getEntries()) {
            if ((entry instanceof BuildRequirementWindowBody.RequirementScrollEntry requirementEntry) && (requirementEntry.requirement != null) && requirementEntry.requirement.getIdentifier().equals(requirementIdentifier)) {
                return bodyToGui(window, entry.getX() + Math.min(60.0F, entry.getWidth() / 2.0F), entry.getY() + entry.getHeight() / 2.0F);
            }
        }
        throw new IllegalStateException("The requirement chooser lists no " + requirementIdentifier);
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
        // Schedulers of the showcase would keep running and get saved for later recordings, like one that shuffles a list
        if (this.savedSchedulers != null) {
            SchedulerHandler.stopAllSchedulers();
            SchedulerHandler.applyChanges(this.savedSchedulers);
        }
        // Options of the showcase would stay in the docs instance and show up in later recordings, like global button textures
        for (int i = this.optionRestorers.size() - 1; i >= 0; i--) this.optionRestorers.get(i).run();
        // Custom GUIs of the showcase would stay in the docs instance, and their layouts in memory until the game closes
        this.customGuiLayouts.forEach(layout -> LayoutHandler.deleteLayout(layout, false));
        if (this.savedCustomGuis != null) {
            for (String identifier : CustomGuiHandler.getGuiIdentifiers()) CustomGuiHandler.removeGui(identifier);
            this.savedCustomGuis.forEach(CustomGuiHandler::addGui);
        }
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
