package de.keksuccino.fancymenu.customization.action.actions.other;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.rendering.ui.toast.SimpleToast;
import de.keksuccino.fancymenu.util.rendering.ui.toast.ToastHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class ShowToastAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 320;
    private static final long MIN_DURATION_MS = 1000L;
    private static final long MAX_DURATION_MS = 600000L;

    public ShowToastAction() {
        super("show_toast");
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
        ToastConfig config = ToastConfig.parse(value);
        if (config == null) {
            LOGGER.error("[FANCYMENU] ShowToastAction: Failed to parse toast configuration!");
            return;
        }

        Component titleComponent = this.parseComponent(config.title, Component.literal(""));
        Component messageComponent = this.parseOptionalComponent(config.message);

        ResourceSupplier<ITexture> iconSupplier = config.iconSource.isBlank()
                ? ResourceSupplier.empty(ITexture.class, FileMediaType.IMAGE)
                : ResourceSupplier.image(config.iconSource);
        SimpleToast.Icon icon = new SimpleToast.Icon(iconSupplier);

        SimpleToast toast = new SimpleToast(icon, titleComponent, messageComponent, false)
                .setWidth(config.width);

        if (!config.backgroundSource.isBlank()) {
            toast.setCustomBackground(ResourceSupplier.image(config.backgroundSource));
        }

        ToastHandler.showToast(toast, config.durationMs);
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.show_toast");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.show_toast.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return ToastConfig.defaultConfig().serialize();
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        ToastConfig config = ToastConfig.parse(instance.value);
        if (config == null) {
            config = ToastConfig.defaultConfig();
        }
        ShowToastActionValueScreen s = new ShowToastActionValueScreen(config, value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    private Component parseOptionalComponent(@Nullable String raw) {
        if ((raw == null) || raw.isBlank()) {
            return null;
        }
        return this.parseComponent(raw, null);
    }

    private Component parseComponent(@Nullable String raw, @Nullable Component fallback) {
        if ((raw == null) || raw.isBlank()) {
            return fallback != null ? fallback : Component.empty();
        }
        try {
            return ComponentParser.fromJsonOrPlainText(raw);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] ShowToastAction: Failed to parse component: {}", raw, ex);
            return fallback != null ? fallback : Component.empty();
        }
    }

    public static class ShowToastActionValueScreen extends CellScreen {

        private final Consumer<String> callback;
        private ToastConfig config;
        private TextInputCell iconSourceCell;
        private TextInputCell backgroundSourceCell;

        protected ShowToastActionValueScreen(@NotNull ToastConfig config, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.show_toast.edit.title"));
            this.config = Objects.requireNonNull(config);
            this.callback = Objects.requireNonNull(callback);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.width"));
            TextInputCell widthCell = this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false)
                    .setEditListener(s -> this.config.width = parseInteger(s, this.config.width, MIN_WIDTH, MAX_WIDTH))
                    .setText(String.valueOf(this.config.width));
            widthCell.editBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("fancymenu.actions.show_toast.edit.width.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.duration"));
            TextInputCell durationCell = this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false)
                    .setEditListener(s -> this.config.durationMs = parseLong(s, this.config.durationMs, MIN_DURATION_MS, MAX_DURATION_MS))
                    .setText(String.valueOf(this.config.durationMs));
            durationCell.editBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("fancymenu.actions.show_toast.edit.duration.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.title_text"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.title = s.replace("\\n", "\n"))
                    .setText(this.config.title.replace("\n", "\\n"));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.message"));
            this.addTextInputCell(null, true, true)
                    .setEditorMultiLineMode(true)
                    .setEditListener(s -> this.config.message = s.replace("\\n", "\n"))
                    .setText(this.config.message.replace("\n", "\\n"));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.icon_source"));
            this.iconSourceCell = this.addTextInputCell(null, false, true)
                    .setEditListener(s -> this.config.iconSource = s.trim())
                    .setText(this.config.iconSource);
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.show_toast.edit.choose_icon"), button -> {
                ResourceChooserScreen<ITexture, ImageFileType> chooser = ResourceChooserScreen.image(null, source -> {
                    if (source != null) {
                        this.config.iconSource = source;
                        this.iconSourceCell.setText(source);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                chooser.setSource(this.config.iconSource.isBlank() ? null : this.config.iconSource, false);
                Minecraft.getInstance().setScreen(chooser);
            }), true);
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.show_toast.edit.clear_icon"), button -> {
                this.config.iconSource = "";
                this.iconSourceCell.setText("");
            }).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.show_toast.edit.clear_icon.desc"))), true);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.show_toast.edit.background_source"));
            this.backgroundSourceCell = this.addTextInputCell(null, false, true)
                    .setEditListener(s -> this.config.backgroundSource = s.trim())
                    .setText(this.config.backgroundSource);
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.show_toast.edit.choose_background"), button -> {
                ResourceChooserScreen<ITexture, ImageFileType> chooser = ResourceChooserScreen.image(null, source -> {
                    if (source != null) {
                        this.config.backgroundSource = source;
                        this.backgroundSourceCell.setText(source);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                chooser.setSource(this.config.backgroundSource.isBlank() ? null : this.config.backgroundSource, false);
                Minecraft.getInstance().setScreen(chooser);
            }), true);
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.show_toast.edit.clear_background"), button -> {
                this.config.backgroundSource = "";
                this.backgroundSourceCell.setText("");
            }).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.show_toast.edit.clear_background.desc"))), true);

            this.addStartEndSpacerCell();

        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.config.normalize();
            this.callback.accept(this.config.serialize());
        }

        @Override
        public boolean allowDone() {
            return true;
        }

        private static int parseInteger(@Nullable String raw, int fallback, int min, int max) {
            if ((raw == null) || raw.isBlank()) {
                return fallback;
            }
            try {
                return Mth.clamp(Integer.parseInt(raw.trim()), min, max);
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

        private static long parseLong(@Nullable String raw, long fallback, long min, long max) {
            if ((raw == null) || raw.isBlank()) {
                return fallback;
            }
            try {
                long parsed = Long.parseLong(raw.trim());
                return Mth.clamp(parsed, min, max);
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

    }

    public static class ToastConfig {

        public int width = 160;
        public long durationMs = 5000L;
        public String title = Component.translatable("fancymenu.actions.show_toast.default_title").getString();
        public String message = Component.translatable("fancymenu.actions.show_toast.default_message").getString();
        public String iconSource = "";
        public String backgroundSource = "";

        public static ToastConfig defaultConfig() {
            ToastConfig config = new ToastConfig();
            config.normalize();
            return config;
        }

        @Nullable
        public static ToastConfig parse(@Nullable String value) {
            if ((value == null) || value.isBlank()) {
                return defaultConfig();
            }
            try {
                ToastConfig config = GSON.fromJson(value, ToastConfig.class);
                if (config == null) {
                    return defaultConfig();
                }
                config.normalize();
                return config;
            } catch (JsonSyntaxException ex) {
                LOGGER.error("[FANCYMENU] ShowToastAction: Invalid toast configuration JSON!", ex);
                return null;
            }
        }

        public void normalize() {
            this.width = Mth.clamp(this.width, MIN_WIDTH, MAX_WIDTH);
            this.durationMs = Mth.clamp(this.durationMs, MIN_DURATION_MS, MAX_DURATION_MS);
            if (this.title == null) this.title = "";
            if (this.message == null) this.message = "";
            if (this.iconSource == null) this.iconSource = "";
            if (this.backgroundSource == null) this.backgroundSource = "";
        }

        @NotNull
        public String serialize() {
            return GSON.toJson(this);
        }
    }

}
