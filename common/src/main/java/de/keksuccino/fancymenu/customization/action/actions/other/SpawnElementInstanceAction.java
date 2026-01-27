package de.keksuccino.fancymenu.customization.action.actions.other;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class SpawnElementInstanceAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";
    private static final String RAW_PLACEHOLDER_ESCAPE = "$fancymenu_raw_placeholder$";

    public SpawnElementInstanceAction() {
        super("spawn_element_instance");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        try {
            Config config = Config.parse(value);
            if (config == null) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: No configuration provided!");
                return;
            }

            String elementType = config.elementType;
            if (elementType == null || elementType.isBlank()) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: Element type is missing!");
                return;
            }

            ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(elementType);
            if (builder == null) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: Unknown element type '{}'", elementType);
                return;
            }

            String elementBody = config.elementBody;
            if (elementBody == null || elementBody.isBlank()) {
                elementBody = buildDefaultBodyFor(elementType);
            }
            if (elementBody == null || elementBody.isBlank()) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: Element body is missing!");
                return;
            }

            elementBody = normalizeBodyForParsing(elementBody, config.replacePlaceholdersInBody);
            if (config.replacePlaceholdersInBody) {
                elementBody = PlaceholderParser.replacePlaceholders(elementBody);
            }

            SerializedElement serialized = deserializeBody(elementBody);
            if (serialized == null) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: Failed to deserialize element body!");
                return;
            }

            String identifier = config.elementIdentifier;
            if (identifier != null && !identifier.isBlank()) {
                identifier = PlaceholderParser.replacePlaceholders(identifier);
            }
            if (identifier == null || identifier.isBlank()) {
                identifier = ScreenCustomization.generateUniqueIdentifier();
            }

            serialized.putProperty("instance_identifier", identifier);
            serialized.putProperty("element_type", elementType);

            AbstractElement element = builder.deserializeElementInternal(serialized);
            if (element == null) {
                LOGGER.error("[FANCYMENU] SpawnElementInstanceAction: Failed to construct element instance for type '{}'", elementType);
                return;
            }

            if (element instanceof VanillaWidgetElement) {
                LOGGER.warn("[FANCYMENU] SpawnElementInstanceAction: Vanilla widget elements cannot be spawned via this action.");
                return;
            }

            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) {
                return;
            }

            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
            if (layer == null) {
                return;
            }

            int insertIndex = layer.normalElements.backgroundElements.size() + layer.normalElements.foregroundElements.size();
            layer.normalElements.foregroundElements.add(element);
            if (insertIndex < 0 || insertIndex > layer.allElements.size()) {
                insertIndex = layer.allElements.size();
            }
            layer.allElements.add(insertIndex, element);

            List<GuiEventListener> widgetsToRegister = element.getWidgetsToRegister();
            if (widgetsToRegister != null) {
                widgetsToRegister = Lists.reverse(widgetsToRegister);
                for (GuiEventListener w : widgetsToRegister) {
                    if (((IMixinScreen) screen).getChildrenFancyMenu().contains(w)) {
                        continue;
                    }
                    ((IMixinScreen) screen).getChildrenFancyMenu().addFirst(w);
                    if (screen instanceof CustomizableScreen c) {
                        c.removeOnInitChildrenFancyMenu().add(w);
                    }
                }
            }

            if (element instanceof ButtonElement buttonElement) {
                buttonElement.updateWidget();
            }

            if (!((IMixinScreen) screen).getChildrenFancyMenu().contains(element)) {
                ((IMixinScreen) screen).getChildrenFancyMenu().addFirst(element);
                if (screen instanceof CustomizableScreen c) {
                    c.removeOnInitChildrenFancyMenu().add(element);
                }
            }

            element._onOpenScreen();

            Listeners.ON_ELEMENT_SPAWNED.onElementSpawned(elementType, element.getInstanceIdentifier(), layer.getScreenIdentifier());

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute SpawnElementInstanceAction!", ex);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.spawn_element_instance");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.spawn_element_instance.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable String getValuePreset() {
        Config config = Config.createDefault();
        return config.serialize();
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        SpawnElementInstanceActionValueScreen s = new SpawnElementInstanceActionValueScreen(
                Objects.requireNonNullElse(instance.value, this.getValuePreset()),
                value -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (value != null) {
                        instance.value = value;
                        onEditingCompleted.accept(instance, oldValue, value);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                    PiPWindow window = windowHolder[0];
                    if (window != null) {
                        window.close();
                    }
                }
        );
        PiPWindow window = new PiPWindow(s.getTitle())
                .setScreen(s)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        windowHolder[0] = window;
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        window.addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    @Nullable
    private static SerializedElement deserializeBody(@NotNull String body) {
        String serializedString = body;
        String trimmed = body.trim();
        if (!trimmed.startsWith("type=") && !trimmed.startsWith("type =")) {
            serializedString = "type = element\n\n" + body;
        }
        PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(serializedString);
        if (set == null) {
            return null;
        }
        PropertyContainer container = set.getFirstContainerOfType("element");
        if (container == null && !set.getContainers().isEmpty()) {
            container = set.getContainers().getFirst();
        }
        if (container == null) {
            return null;
        }
        SerializedElement serialized = new SerializedElement();
        for (Map.Entry<String, String> entry : container.getProperties().entrySet()) {
            serialized.putProperty(entry.getKey(), entry.getValue());
        }
        return serialized;
    }

    @NotNull
    private static String normalizeBodyForParsing(@NotNull String rawBody, boolean replacePlaceholdersInBody) {
        String body = rawBody.replace("\\n", "\n");
        if (body.contains("$prop_line_break$") || body.contains("$prop_brackets_open$") || body.contains("$prop_brackets_close$")) {
            body = PropertiesParser.unstringify(body);
        }
        if (!replacePlaceholdersInBody) {
            body = body.replace(RAW_PLACEHOLDER_ESCAPE, "$$");
        }
        return body;
    }

    @NotNull
    private static String encodeBodyForValue(@NotNull String body, boolean replacePlaceholdersInBody) {
        if (replacePlaceholdersInBody) {
            return body;
        }
        String escaped = body.replace("$$", RAW_PLACEHOLDER_ESCAPE);
        return PropertiesParser.stringifyFancyString(escaped);
    }

    @NotNull
    private static String decodeBodyFromValue(@NotNull String body, boolean replacePlaceholdersInBody) {
        String decoded = body;
        if (decoded.contains("$prop_line_break$") || decoded.contains("$prop_brackets_open$") || decoded.contains("$prop_brackets_close$")) {
            decoded = PropertiesParser.unstringify(decoded);
        }
        if (!replacePlaceholdersInBody) {
            decoded = decoded.replace(RAW_PLACEHOLDER_ESCAPE, "$$");
        }
        return decoded;
    }

    @Nullable
    private static String buildDefaultBodyFor(@NotNull String elementType) {
        ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(elementType);
        if (builder == null) {
            return null;
        }
        AbstractElement element = builder.buildDefaultInstance();
        SerializedElement serialized = builder.serializeElementInternal(element);
        if (serialized == null) {
            return null;
        }
        String body = PropertiesParser.serializeContainerToFancyString(serialized);
        return body.replace("\n", "\\n");
    }

    public static final class Config {
        public String elementType = "";
        public String elementBody = "";
        public String elementIdentifier = "";
        public boolean replacePlaceholdersInBody = true;

        @NotNull
        public String serialize() {
            String body = encodeBodyForValue(this.elementBody, this.replacePlaceholdersInBody);
            return this.elementType + VALUE_SEPARATOR + body + VALUE_SEPARATOR + this.elementIdentifier + VALUE_SEPARATOR + this.replacePlaceholdersInBody;
        }

        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) {
                return null;
            }
            String[] parts = value.split("\\|\\|\\|", -1);
            Config config = new Config();
            if (parts.length >= 1) config.elementType = parts[0];
            if (parts.length >= 3) config.elementIdentifier = parts[2];
            if (parts.length >= 4) config.replacePlaceholdersInBody = Boolean.parseBoolean(parts[3]);
            if (parts.length >= 2) {
                config.elementBody = decodeBodyFromValue(parts[1], config.replacePlaceholdersInBody);
            }
            return config;
        }

        @NotNull
        public static Config createDefault() {
            Config config = new Config();
            List<ElementBuilder<?, ?>> builders = ElementRegistry.getBuilders();
            if (!builders.isEmpty()) {
                config.elementType = builders.getFirst().getIdentifier();
                String body = buildDefaultBodyFor(config.elementType);
                if (body != null) {
                    config.elementBody = body;
                }
            } else {
                config.elementType = "button";
            }
            config.elementIdentifier = "spawned_element";
            config.replacePlaceholdersInBody = true;
            return config;
        }

        public void applyDefaultsIfMissing() {
            if (this.elementType == null || this.elementType.isBlank()) {
                Config defaults = createDefault();
                this.elementType = defaults.elementType;
                if (this.elementBody == null || this.elementBody.isBlank()) {
                    this.elementBody = defaults.elementBody;
                }
            }
            if (this.elementBody == null || this.elementBody.isBlank()) {
                String body = buildDefaultBodyFor(this.elementType);
                if (body != null) {
                    this.elementBody = body;
                }
            }
        }
    }

    public static class SpawnElementInstanceActionValueScreen extends PiPCellWindowBody {

        protected Config config;
        protected Consumer<String> callback;
        protected TextInputCell elementBodyCell;

        protected SpawnElementInstanceActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.spawn_element_instance.edit_value"));
            this.callback = callback;
            this.config = Objects.requireNonNullElse(Config.parse(value), Config.createDefault());
            this.config.applyDefaultsIfMissing();
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            List<ElementBuilder<?, ?>> builders = ElementRegistry.getBuilders();
            List<String> elementTypes = new ArrayList<>();
            for (ElementBuilder<?, ?> builder : builders) {
                elementTypes.add(builder.getIdentifier());
            }
            if (elementTypes.isEmpty()) {
                this.addLabelCell(Component.translatable("fancymenu.actions.spawn_element_instance.edit.no_elements"));
                this.addStartEndSpacerCell();
                return;
            }

            if (!elementTypes.contains(this.config.elementType)) {
                this.config.elementType = elementTypes.getFirst();
                String body = buildDefaultBodyFor(this.config.elementType);
                if (body != null) {
                    this.config.elementBody = body;
                }
            }

            LocalizedGenericValueCycle<String> typeCycle;
            if (elementTypes.size() < 2) {
                typeCycle = LocalizedGenericValueCycle.of("fancymenu.actions.spawn_element_instance.edit.element_type", elementTypes.get(0), elementTypes.get(0));
            } else {
                typeCycle = LocalizedGenericValueCycle.of("fancymenu.actions.spawn_element_instance.edit.element_type", elementTypes.toArray(new String[0]));
            }
            typeCycle.setValueNameSupplier(id -> {
                ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(id);
                if (builder == null) {
                    return id;
                }
                return builder.getDisplayName(null).getString();
            });
            typeCycle.setCurrentValue(this.config.elementType, false);

            CycleButton<String> typeButton = new CycleButton<>(0, 0, 20, 20, typeCycle, (value, button) -> {
                this.config.elementType = value;
                String body = buildDefaultBodyFor(value);
                if (body != null) {
                    this.config.elementBody = body;
                    if (this.elementBodyCell != null) {
                        this.elementBodyCell.setText(this.config.elementBody);
                    }
                }
            });
            typeButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.actions.spawn_element_instance.edit.element_type.desc")));
            this.addWidgetCell(typeButton, true);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.spawn_element_instance.edit.element_identifier"));
            TextInputCell identifierCell = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.elementIdentifier = s)
                    .setText(this.config.elementIdentifier)
                    .setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.actions.spawn_element_instance.edit.element_identifier.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.spawn_element_instance.edit.element_body"));
            this.elementBodyCell = this.addTextInputCell(null, true, true)
                    .setEditorMultiLineMode(true)
                    .setEditListener(s -> this.config.elementBody = s.replace("\n", "\\n"))
                    .setText(this.config.elementBody)
                    .setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.actions.spawn_element_instance.edit.element_body.desc")));

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> placeholderButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.spawn_element_instance.edit.replace_placeholders", this.config.replacePlaceholdersInBody),
                    (val, button) -> this.config.replacePlaceholdersInBody = val.getAsBoolean()
            );
            placeholderButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.actions.spawn_element_instance.edit.replace_placeholders.desc")));
            this.addWidgetCell(placeholderButton, true);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return this.config.elementType != null && !this.config.elementType.isBlank();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.callback.accept(this.config.serialize());
        }

        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }
    }

}
