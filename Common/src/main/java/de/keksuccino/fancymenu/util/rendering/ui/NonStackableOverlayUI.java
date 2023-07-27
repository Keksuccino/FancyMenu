package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NonStackableOverlayUI {

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter) {
        ContextMenu subMenu = new ContextMenu();
        ContextMenu addToFinal = addResetOption ? subMenu : addTo;
        ContextMenu.ClickableContextMenuEntry<?> chooseEntry = addToFinal.addClickableEntry(addResetOption ? "choose_file" : entryIdentifier, addResetOption ? Component.translatable("fancymenu.ui.filechooser.choose.file") : label, (menu, entry) -> {
            File startDir = LayoutHandler.ASSETS_DIR;
            String path = getter.get();
            if (path != null) {
                startDir = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path)).getParentFile();
                if (startDir == null) startDir = LayoutHandler.ASSETS_DIR;
            }
            Screen current = Minecraft.getInstance().screen;
            ChooseFileScreen fileChooser = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, startDir, (call) -> {
                if (call != null) {
                    setter.accept(call.getPath());
                }
                menu.closeMenu();
                Minecraft.getInstance().setScreen(current);
            });
            fileChooser.setVisibleDirectoryLevelsAboveRoot(2);
            fileChooser.setFileFilter(fileFilter);
            Minecraft.getInstance().setScreen(fileChooser);
        });
        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.editor.filechooser.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            });
            return addTo.addSubMenuEntry(entryIdentifier, label, subMenu);
        }
        return chooseEntry;
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ContextMenu subMenu = new ContextMenu();
        ContextMenu addToFinal = addResetOption ? subMenu : addTo;
        ContextMenu.ClickableContextMenuEntry<?> inputEntry = addToFinal.addClickableEntry(addResetOption ? "input_value" : entryIdentifier, addResetOption ? Component.translatable("fancymenu.guicomponents.set") : label, (menu, entry) -> {
            Screen current = Minecraft.getInstance().screen;
            Screen inputScreen;
            if (!multiLineInput && !allowPlaceholders) {
                TextInputScreen s = TextInputScreen.build(label, inputCharacterFilter, call -> {
                    if (call != null) {
                        setter.accept(call);
                    }
                    menu.closeMenu();
                    Minecraft.getInstance().setScreen(current);
                });
                if (textValidator != null) {
                    s.setTextValidator(consumes -> {
                        if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                        return textValidator.get(consumes.getText());
                    });
                }
                s.setText(getter.get());
                inputScreen = s;
            } else {
                TextEditorScreen s = new TextEditorScreen(label, (inputCharacterFilter != null) ? inputCharacterFilter.convertToLegacyFilter() : null, (call) -> {
                    if (call != null) {
                        setter.accept(call);
                    }
                    menu.closeMenu();
                    Minecraft.getInstance().setScreen(current);
                });
                if (textValidator != null) {
                    s.setTextValidator(consumes -> {
                        if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                        return textValidator.get(consumes.getText());
                    });
                }
                s.setMultilineMode(multiLineInput);
                s.setPlaceholdersAllowed(allowPlaceholders);
                s.setText(getter.get());
                inputScreen = s;
            }
            Minecraft.getInstance().setScreen(inputScreen);
        });
        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.guicomponents.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            });
            return addTo.addSubMenuEntry(entryIdentifier, label, subMenu);
        }
        return inputEntry;
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Integer> getter, @NotNull Consumer<Integer> setter, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isInteger(s)) setter.accept(Integer.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerCharacterFiler(),
                false, false, (textValidator != null) ? textValidator : defaultIntegerValidator, textValidatorUserFeedback);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isDouble(s)) setter.accept(Double.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDoubleCharacterFiler(),
                false, false, (textValidator != null) ? textValidator : defaultDoubleValidator, textValidatorUserFeedback);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Float> getter, @NotNull Consumer<Float> setter, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isFloat(s)) setter.accept(Float.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDoubleCharacterFiler(),
                false, false, (textValidator != null) ? textValidator : defaultFloatValidator, textValidatorUserFeedback);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Long> getter, @NotNull Consumer<Long> setter, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isLong(s)) setter.accept(Long.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerCharacterFiler(),
                false, false, (textValidator != null) ? textValidator : defaultLongValidator, textValidatorUserFeedback);
    }

}
