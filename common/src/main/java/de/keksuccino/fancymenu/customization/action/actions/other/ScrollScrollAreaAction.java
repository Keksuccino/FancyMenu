package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaScrolling;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scrolls the content of a Scrollable Area element: to its start, to its end, to a position or to one of the elements in it.
 */
public class ScrollScrollAreaAction extends Action {

    private static final String VALUE_SEPARATOR = "|||";
    private static final String LOCALIZATION_KEY = "fancymenu.actions.scroll_scroll_area";

    @NotNull
    private final Function<String, AbstractElement> elementLookup;

    public ScrollScrollAreaAction() {
        this(AbstractElement::getElementByInstanceIdentifier);
    }

    ScrollScrollAreaAction(@NotNull Function<String, AbstractElement> elementLookup) {
        super("scroll_scroll_area");
        this.elementLookup = Objects.requireNonNull(elementLookup);
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
        Config config = Config.parse(value);
        if ((config == null) || config.areaIdentifier.isBlank()) return;
        if (!(this.elementLookup.apply(config.areaIdentifier.trim()) instanceof ScrollAreaElement area)) return;
        ScrollAreaScrolling scrolling = area.getScrolling();
        switch (config.target) {
            case START -> scrolling.scrollToEdge(false, config.smooth);
            case END -> scrolling.scrollToEdge(true, config.smooth);
            case POSITION -> {
                // An axis without a number stays where it is
                Double positionX = parsePosition(config.positionX);
                Double positionY = parsePosition(config.positionY);
                if (positionX != null) scrolling.scrollToPosition(true, positionX, config.smooth);
                if (positionY != null) scrolling.scrollToPosition(false, positionY, config.smooth);
            }
            case ELEMENT -> {
                AbstractElement element = config.elementIdentifier.isBlank() ? null : this.elementLookup.apply(config.elementIdentifier.trim());
                if ((element != null) && ElementContainerHierarchy.isInContainer(element, area)) scrolling.scrollToChild(element, config.smooth);
            }
        }
    }

    @Nullable
    private static Double parsePosition(@NotNull String position) {
        if (position.isBlank()) return null;
        try {
            return Double.parseDouble(position.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(LOCALIZATION_KEY);
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable(LOCALIZATION_KEY + ".desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        // The value gets edited in the window of the action
        return Component.empty();
    }

    @Override
    public @Nullable String getValuePreset() {
        Config config = new Config();
        config.areaIdentifier = "some.element.id";
        return config.serialize();
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        PiPWindow[] windowHolder = new PiPWindow[1];
        ScrollScrollAreaActionValueScreen screen = new ScrollScrollAreaActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValuePreset()), value -> {
            if (handled[0]) return;
            handled[0] = true;
            if (value != null) {
                instance.value = value;
                onEditingCompleted.accept(instance, oldValue, value);
            } else {
                onEditingCanceled.accept(instance);
            }
            if (windowHolder[0] != null) windowHolder[0].close();
        });
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        windowHolder[0] = window;
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        // A window that gets closed in any other way than with its buttons cancels the editing
        window.addCloseCallback(() -> {
            if (handled[0]) return;
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    /**
     * Where the action scrolls the content to.
     */
    public enum Target {

        START,
        END,
        POSITION,
        ELEMENT;

        /** The name the target gets saved with. Changing it breaks saved layouts. */
        private final String serializedName = this.name().toLowerCase(Locale.ROOT);

        @NotNull
        private static Target parse(@NotNull String serializedName) {
            for (Target target : values()) {
                if (target.serializedName.equals(serializedName)) return target;
            }
            return START;
        }

    }

    /**
     * What the action does, which is what its value holds.
     */
    public static final class Config {

        @NotNull
        public String areaIdentifier = "";
        @NotNull
        public Target target = Target.START;
        /** The positions stay text until the action runs, so they can be placeholders. */
        @NotNull
        public String positionX = "";
        @NotNull
        public String positionY = "";
        @NotNull
        public String elementIdentifier = "";
        public boolean smooth = true;

        @NotNull
        public String serialize() {
            return this.areaIdentifier + VALUE_SEPARATOR + this.target.serializedName + VALUE_SEPARATOR + this.positionX + VALUE_SEPARATOR + this.positionY + VALUE_SEPARATOR + this.elementIdentifier + VALUE_SEPARATOR + this.smooth;
        }

        /**
         * Reads a value. Parts a value does not have keep what a new action starts with.
         */
        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = value.split("\\|\\|\\|", -1);
            Config config = new Config();
            if (parts.length >= 1) config.areaIdentifier = parts[0];
            if (parts.length >= 2) config.target = Target.parse(parts[1]);
            if (parts.length >= 3) config.positionX = parts[2];
            if (parts.length >= 4) config.positionY = parts[3];
            if (parts.length >= 5) config.elementIdentifier = parts[4];
            if (parts.length >= 6) config.smooth = Boolean.parseBoolean(parts[5]);
            return config;
        }

    }

    /**
     * The window the value of the action gets edited in. It only shows the inputs the chosen target needs.
     */
    public static class ScrollScrollAreaActionValueScreen extends PiPCellWindowBody {

        @NotNull
        protected final Config config;
        @NotNull
        protected final Consumer<String> callback;

        protected ScrollScrollAreaActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"));
            this.callback = callback;
            this.config = Objects.requireNonNullElse(Config.parse(value), new Config());
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.area_identifier"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.areaIdentifier = s)
                    .setText(this.config.areaIdentifier)
                    .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.area_identifier.desc")));

            this.addCellGroupEndSpacerCell();

            LocalizedGenericValueCycle<Target> targetCycle = LocalizedGenericValueCycle.of(LOCALIZATION_KEY + ".edit.target", Target.values());
            targetCycle.setValueNameSupplier(target -> Component.translatable(LOCALIZATION_KEY + ".edit.target." + target.serializedName).getString());
            targetCycle.setValueComponentStyleSupplier(target -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            targetCycle.setCurrentValue(this.config.target);
            this.addCycleButtonCell(targetCycle, true, (target, button) -> {
                this.config.target = target;
                // Every target has its own inputs
                this.rebuild();
            });

            if (this.config.target == Target.POSITION) {
                this.addCellGroupEndSpacerCell();
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.position_y"));
                this.addTextInputCell(null, true, true)
                        .setEditListener(s -> this.config.positionY = s)
                        .setText(this.config.positionY)
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.position_y.desc")));
                this.addCellGroupEndSpacerCell();
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.position_x"));
                this.addTextInputCell(null, true, true)
                        .setEditListener(s -> this.config.positionX = s)
                        .setText(this.config.positionX)
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.position_x.desc")));
            } else if (this.config.target == Target.ELEMENT) {
                this.addCellGroupEndSpacerCell();
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.element_identifier"));
                this.addTextInputCell(null, true, true)
                        .setEditListener(s -> this.config.elementIdentifier = s)
                        .setText(this.config.elementIdentifier)
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.element_identifier.desc")));
            }

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> smoothButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled(LOCALIZATION_KEY + ".edit.smooth", this.config.smooth), (value, button) -> this.config.smooth = value.getAsBoolean());
            smoothButton.setUITooltip(UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.smooth.desc")));
            this.addWidgetCell(smoothButton, true);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            if (this.config.areaIdentifier.isBlank()) return false;
            return (this.config.target != Target.ELEMENT) || !this.config.elementIdentifier.isBlank();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.callback.accept(this.config.serialize());
        }

        /** The window keeps the scale it gets from FancyMenu's UI. */
        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }

    }

}
