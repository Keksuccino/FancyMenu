package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.enums.LocalizedEnum;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ManageResourcePackAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";

    public ManageResourcePackAction() {
        super("manage_resource_pack");
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
        if ((value == null) || value.isEmpty()) {
            LOGGER.error("[FANCYMENU] ManageResourcePackAction: No value provided!");
            return;
        }

        try {
            ResourcePackConfig config = ResourcePackConfig.parse(value);
            if (config == null) {
                LOGGER.error("[FANCYMENU] ManageResourcePackAction: Failed to parse configuration: {}", value);
                return;
            }

            String targetName = config.packName.trim();
            if (targetName.isEmpty()) {
                LOGGER.error("[FANCYMENU] ManageResourcePackAction: Pack name is empty!");
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            PackRepository repository = minecraft.getResourcePackRepository();
            Pack pack = findPack(repository, targetName);
            if (pack == null) {
                LOGGER.warn("[FANCYMENU] ManageResourcePackAction: Unable to find resource pack with display name '{}'.", targetName);
                return;
            }

            List<Pack> workingSelection = new ArrayList<>(repository.getSelectedPacks());
            boolean modified = config.mode.apply(workingSelection, pack);
            if (!modified) {
                return;
            }

            List<String> selectedIds = new ArrayList<>();
            for (Pack entry : workingSelection) {
                selectedIds.add(entry.getId());
            }
            repository.setSelected(selectedIds);

            refreshOptions(minecraft.options, repository);
            updateHighContrastOption(minecraft, repository);

            if (config.reloadOnChange) {
                minecraft.reloadResourcePacks();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] ManageResourcePackAction failed to change resource pack state!", ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.manage_resource_pack");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.manage_resource_pack.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return "Programmer Art|||TOGGLE|||true";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        ManageResourcePackActionValueScreen screen = new ManageResourcePackActionValueScreen(
                Objects.requireNonNullElse(instance.value, this.getValueExample()),
                editedValue -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (editedValue != null) {
                        instance.value = editedValue;
                        onEditingCompleted.accept(instance, oldValue, editedValue);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                    PiPWindow window = windowHolder[0];
                    if (window != null) {
                        window.close();
                    }
                }
        );
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
        window.addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    private void refreshOptions(@NotNull Options options, @NotNull PackRepository repository) {
        List<String> previous = new ArrayList<>(options.resourcePacks);
        List<String> previousIncompatible = new ArrayList<>(options.incompatibleResourcePacks);

        options.resourcePacks.clear();
        options.incompatibleResourcePacks.clear();
        for (Pack selected : repository.getSelectedPacks()) {
            if (!selected.isFixedPosition()) {
                options.resourcePacks.add(selected.getId());
                if (!selected.getCompatibility().isCompatible()) {
                    options.incompatibleResourcePacks.add(selected.getId());
                }
            }
        }

        boolean changed = !previous.equals(options.resourcePacks) || !previousIncompatible.equals(options.incompatibleResourcePacks);
        if (changed) {
            options.save();
        }
    }

    private void updateHighContrastOption(@NotNull Minecraft minecraft, @NotNull PackRepository repository) {
        boolean highContrastEnabled = false;
        for (Pack selected : repository.getSelectedPacks()) {
            if ("high_contrast".equals(selected.getId())) {
                highContrastEnabled = true;
                break;
            }
        }
        minecraft.options.highContrast().set(highContrastEnabled);
    }

    @Nullable
    private Pack findPack(@NotNull PackRepository repository, @NotNull String search) {
        String trimmed = search.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (Pack pack : repository.getSelectedPacks()) {
            if (matchesPack(pack, trimmed)) {
                return pack;
            }
        }
        for (Pack pack : repository.getAvailablePacks()) {
            if (matchesPack(pack, trimmed)) {
                return pack;
            }
        }
        return null;
    }

    private boolean matchesPack(@NotNull Pack pack, @NotNull String search) {
        String title = pack.getTitle().getString();
        if (title.equalsIgnoreCase(search)) {
            return true;
        }
        return pack.getId().equalsIgnoreCase(search);
    }

    protected static class ResourcePackConfig {

        @NotNull
        protected String packName = "";
        @NotNull
        protected ResourcePackMode mode = ResourcePackMode.TOGGLE;
        protected boolean reloadOnChange = true;

        @NotNull
        public String serialize() {
            return sanitize(this.packName) + VALUE_SEPARATOR + this.mode.getName() + VALUE_SEPARATOR + this.reloadOnChange;
        }

        @Nullable
        public static ResourcePackConfig parse(@NotNull String value) {
            ResourcePackConfig config = new ResourcePackConfig();
            String[] parts = value.split("\\|\\|\\|", -1);
            if (parts.length >= 1) {
                config.packName = parts[0];
            }
            if (parts.length >= 2) {
                config.mode = ResourcePackMode.byName(parts[1]);
            }
            if (parts.length >= 3) {
                config.reloadOnChange = Boolean.parseBoolean(parts[2]);
            }
            return config;
        }

        @NotNull
        private static String sanitize(@NotNull String input) {
            return input.replace(VALUE_SEPARATOR, "").trim();
        }

    }

    public static class ManageResourcePackActionValueScreen extends StringBuilderScreen {

        protected ResourcePackConfig config;

        protected ManageResourcePackActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.manage_resource_pack.edit_value"), callback);
            this.config = ResourcePackConfig.parse(value);
            if (this.config == null) {
                this.config = new ResourcePackConfig();
            }
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.manage_resource_pack.edit.pack_name"));
            TextInputCell nameCell = this.addTextInputCell(null, true, true)
                    .setEditListener(text -> this.config.packName = text)
                    .setText(this.config.packName);
            nameCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.manage_resource_pack.edit.pack_name.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.manage_resource_pack.edit.mode.label"));
            LocalizedEnumValueCycle<ResourcePackMode> modeCycle = LocalizedEnumValueCycle.ofArray(
                    "fancymenu.actions.manage_resource_pack.edit.mode",
                    ResourcePackMode.values()
            );
            modeCycle.setCurrentValue(this.config.mode, false);
            CycleButton<ResourcePackMode> modeButton = new CycleButton<>(0, 0, 20, 20, modeCycle, (selectedMode, button) -> this.config.mode = selectedMode);
            modeButton.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.manage_resource_pack.edit.mode.desc")));
            modeButton.setSelectedValue(this.config.mode);
            this.addWidgetCell(modeButton, true);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.manage_resource_pack.edit.reload.label"));
            CycleButton<CommonCycles.CycleEnabledDisabled> reloadButton = new CycleButton<>(
                    0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.manage_resource_pack.edit.reload", this.config.reloadOnChange),
                    (state, button) -> this.config.reloadOnChange = state.getAsBoolean()
            );
            reloadButton.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.manage_resource_pack.edit.reload.desc")));
            reloadButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.config.reloadOnChange));
            this.addWidgetCell(reloadButton, true);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.config.packName.trim().isEmpty();
        }

        @Override
        public @NotNull String buildString() {
            this.config.packName = this.config.packName.trim();
            return this.config.serialize();
        }

        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }

    }

    public enum ResourcePackMode implements LocalizedEnum<ResourcePackMode> {

        ENABLE("enable", LocalizedEnum.SUCCESS_TEXT_STYLE) {
            @Override
            boolean apply(@NotNull List<Pack> selection, @NotNull Pack pack) {
                if (selection.contains(pack)) {
                    return false;
                }
                pack.getDefaultPosition().insert(selection, pack, Pack::selectionConfig, false);
                return true;
            }
        },
        DISABLE("disable", LocalizedEnum.ERROR_TEXT_STYLE) {
            @Override
            boolean apply(@NotNull List<Pack> selection, @NotNull Pack pack) {
                if (pack.isRequired()) {
                    LOGGER.warn("[FANCYMENU] ManageResourcePackAction: Tried to disable required resource pack '{}'.", pack.getId());
                    return false;
                }
                return selection.remove(pack);
            }
        },
        TOGGLE("toggle", LocalizedEnum.WARNING_TEXT_STYLE) {
            @Override
            boolean apply(@NotNull List<Pack> selection, @NotNull Pack pack) {
                if (selection.contains(pack)) {
                    return DISABLE.apply(selection, pack);
                }
                return ENABLE.apply(selection, pack);
            }
        };

        private final String name;
        private final Supplier<Style> styleSupplier;

        ResourcePackMode(@NotNull String name, @NotNull Supplier<Style> styleSupplier) {
            this.name = name;
            this.styleSupplier = styleSupplier;
        }

        abstract boolean apply(@NotNull List<Pack> selection, @NotNull Pack pack);

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.actions.manage_resource_pack.mode";
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull ResourcePackMode[] getValues() {
            return values();
        }

        @Override
        public @Nullable ResourcePackMode getByNameInternal(@NotNull String name) {
            return byName(name);
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.styleSupplier.get();
        }

        public static ResourcePackMode byName(@Nullable String name) {
            if (name != null) {
                for (ResourcePackMode mode : values()) {
                    if (mode.getName().equalsIgnoreCase(name)) {
                        return mode;
                    }
                }
            }
            return TOGGLE;
        }

    }

}

