package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resources.Resource;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.text.IText;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class NonStackableOverlayUI {

    public static ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<ITexture> defaultValue, @NotNull Supplier<ResourceSupplier<ITexture>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserScreen.image(null, file -> {}), ResourceSupplier::image, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.IMAGE_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    public static ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IAudio> defaultValue, @NotNull Supplier<ResourceSupplier<IAudio>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserScreen.audio(null, file -> {}), ResourceSupplier::audio, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.AUDIO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    public static ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IVideo> defaultValue, @NotNull Supplier<ResourceSupplier<IVideo>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserScreen.video(null, file -> {}), ResourceSupplier::video, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.VIDEO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    public static ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IText> defaultValue, @NotNull Supplier<ResourceSupplier<IText>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserScreen.text(null, file -> {}), ResourceSupplier::text, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.TEXT_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    public static <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull Supplier<ResourceSupplier<R>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("fancymenu.ui.resources.choose"),
                (menu, entry) -> {
                    Screen current = Minecraft.getInstance().screen;
                    ResourceSupplier<R> supplier = targetFieldGetter.get();
                    String preSelectedSource = (supplier != null) ? supplier.getSourceWithPrefix() : null;
                    ResourceChooserScreen<R,F> chooserScreen = resourceChooserScreenBuilder.get();
                    chooserScreen.setFileFilter(fileFilter);
                    chooserScreen.setAllowedFileTypes(fileTypes);
                    chooserScreen.setSource(preSelectedSource, false);
                    chooserScreen.setLocationSourceAllowed(allowLocation);
                    chooserScreen.setLocalSourceAllowed(allowLocal);
                    chooserScreen.setWebSourceAllowed(allowWeb);
                    chooserScreen.setResourceSourceCallback(source -> {
                        if (source != null) {
                            targetFieldSetter.accept(resourceSupplierBuilder.get(source));
                        }
                        Minecraft.getInstance().setScreen(current);
                    });
                    Minecraft.getInstance().setScreen(chooserScreen);
                }).setStackable(false);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.ui.resources.reset"),
                    (menu, entry) -> {
                        targetFieldSetter.accept(defaultValue);
                    }).setStackable(false);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            ResourceSupplier<R> supplier = targetFieldGetter.get();
            String val = (supplier != null) ? supplier.getSourceWithoutPrefix() : null;
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            } else {
                val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
                if (Minecraft.getInstance().font.width(val) > 150) {
                    val = new StringBuilder(val).reverse().toString();
                    val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
                    val = new StringBuilder(val).reverse().toString();
                    val = ".." + val;
                }
                valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
            }
            return Component.translatable("fancymenu.ui.resources.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return addTo.addSubMenuEntry(entryIdentifier, label, subMenu).setStackable(true);

    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter) {
        return addFileChooserContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, fileFilter, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<?> fileTypes) {
        return addFileChooserContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, fileFilter, fileTypes, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<?> fileTypes, @Nullable BiConsumer<Screen, File> onCloseFileChooser) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("fancymenu.ui.filechooser.choose.file"), (menu, entry) -> {
            File startDir = LayoutHandler.ASSETS_DIR;
            String path = getter.get();
            if (path != null) {
                startDir = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path)).getParentFile();
                if (startDir == null) startDir = LayoutHandler.ASSETS_DIR;
            }
            Screen current = Minecraft.getInstance().screen;
            ChooseFileScreen fileChooser = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, startDir, (call) -> {
                if (call != null) {
                    setter.accept(call.getPath());
                }
                menu.closeMenu();
                if (onCloseFileChooser != null) {
                    onCloseFileChooser.accept(current, call);
                } else {
                    Minecraft.getInstance().setScreen(current);
                }
            });
            fileChooser.setVisibleDirectoryLevelsAboveRoot(2);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileTypes(fileTypes);
            Minecraft.getInstance().setScreen(fileChooser);
        });

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.editor.filechooser.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            });
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            String val = getter.get();
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            } else {
                val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
                if (Minecraft.getInstance().font.width(val) > 150) {
                    val = new StringBuilder(val).reverse().toString();
                    val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
                    val = new StringBuilder(val).reverse().toString();
                    val = ".." + val;
                }
                valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
            }
            return Component.translatable("fancymenu.context_menu.entries.choose_or_set.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return addTo.addSubMenuEntry(entryIdentifier, label, subMenu);
    }

    @NotNull
    public static <T> ContextMenu.ClickableContextMenuEntry<?> addGenericInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, boolean addResetOption, T defaultValue, @NotNull Consumer<Consumer<T>> inputLogic) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("input_value", Component.translatable("fancymenu.guicomponents.set"), (menu, entry) -> {
            menu.closeMenu();
            inputLogic.accept(setter);
        });

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.guicomponents.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            });
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            T val = getter.get();
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            } else {
                String valString = val.toString();
                if (Minecraft.getInstance().font.width(valString) > 150) {
                    valString = new StringBuilder(valString).reverse().toString();
                    valString = Minecraft.getInstance().font.plainSubstrByWidth(valString, 150);
                    valString = new StringBuilder(valString).reverse().toString();
                    valString = ".." + valString;
                }
                valueComponent = Component.literal(valString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
            }
            return Component.translatable("fancymenu.context_menu.entries.choose_or_set.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return addTo.addSubMenuEntry(entryIdentifier, label, subMenu);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, inputCharacterFilter, multiLineInput, allowPlaceholders, textValidator, textValidatorUserFeedback, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        return addGenericInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, valueSetter -> {
            Screen current = Minecraft.getInstance().screen;
            Screen inputScreen;
            if (!multiLineInput && !allowPlaceholders) {
                TextInputScreen s = TextInputScreen.build(label, inputCharacterFilter, call -> {
                    if (call != null) {
                        valueSetter.accept(call);
                    }
                    if (onCloseEditor != null) {
                        onCloseEditor.accept(current, call);
                    } else {
                        Minecraft.getInstance().setScreen(current);
                    }
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
                        valueSetter.accept(call);
                    }
                    if (onCloseEditor != null) {
                        onCloseEditor.accept(current, call);
                    } else {
                        Minecraft.getInstance().setScreen(current);
                    }
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
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Integer> getter, @NotNull Consumer<Integer> setter, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addIntegerInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Integer> getter, @NotNull Consumer<Integer> setter, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isInteger(s)) setter.accept(Integer.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerFiler(),
                false, false, (textValidator != null) ? textValidator : defaultIntegerValidator, textValidatorUserFeedback, onCloseEditor);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addDoubleInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isDouble(s)) setter.accept(Double.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDecimalFiler(),
                false, false, (textValidator != null) ? textValidator : defaultDoubleValidator, textValidatorUserFeedback, onCloseEditor);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Float> getter, @NotNull Consumer<Float> setter, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addFloatInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Float> getter, @NotNull Consumer<Float> setter, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isFloat(s)) setter.accept(Float.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDecimalFiler(),
                false, false, (textValidator != null) ? textValidator : defaultFloatValidator, textValidatorUserFeedback, onCloseEditor);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Long> getter, @NotNull Consumer<Long> setter, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addLongInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Long> getter, @NotNull Consumer<Long> setter, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isLong(s)) setter.accept(Long.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerFiler(),
                false, false, (textValidator != null) ? textValidator : defaultLongValidator, textValidatorUserFeedback, onCloseEditor);
    }

}
