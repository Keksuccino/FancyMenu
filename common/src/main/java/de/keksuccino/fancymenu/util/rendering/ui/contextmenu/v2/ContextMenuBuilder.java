package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.cycle.ValueCycle;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface ContextMenuBuilder<O> {

    static <O> ContextMenuBuilder<O> createStandalone(@NotNull Supplier<Screen> callbackScreenSupplier, @NotNull ConsumingSupplier<ConsumingSupplier<O, Boolean>, List<O>> stackableObjectsListSupplier, @NotNull Supplier<O> selfSupplier, @Nullable Runnable saveSnapshotTask) {
        return new ContextMenuBuilder<>() {
            @Override
            public @Nullable Screen getContextMenuCallbackScreen() {
                return callbackScreenSupplier.get();
            }
            @Override
            public @NotNull O self() {
                return selfSupplier.get();
            }
            @Override
            public void saveSnapshot() {
                if (saveSnapshotTask == null) return;
                saveSnapshotTask.run();
            }
            @Override
            public @NotNull List<O> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<O, Boolean> filter) {
                if (filter == null) filter = consumes -> true;
                return stackableObjectsListSupplier.get(filter);
            }
        };
    }

    /**
     * Returns the {@link Screen} {@link ContextMenu}s should come back to after doing things in other {@link Screen}s, like text input screens or similar.
     */
    @Nullable
    Screen getContextMenuCallbackScreen();

    /**
     * Used to get the self-instance object for building {@link ContextMenu}s.
     */
    @NotNull
    O self();

    /**
     * Saves a snapshot to the history, if one is available. Mostly called right before changes are made.
     */
    void saveSnapshot();

    /**
     * Filters the pool of all available stackable objects for {@link ContextMenu}s.<br>
     * Should return the full pool if {@code filter} is null.
     */
    @NotNull
    List<O> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<O, Boolean> filter);

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildImageResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<ITexture>> getter = (ConsumingSupplier<O, ResourceSupplier<ITexture>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<ITexture>> setter = (BiConsumer<O, ResourceSupplier<ITexture>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.image(null, file -> {}), ResourceSupplier::image, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.IMAGE_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildImageResourceChooserContextMenuEntry(addTo, entryIdentifier, elementType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildAudioResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<IAudio>> getter = (ConsumingSupplier<O, ResourceSupplier<IAudio>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<IAudio>> setter = (BiConsumer<O, ResourceSupplier<IAudio>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.audio(null, file -> {}), ResourceSupplier::audio, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.AUDIO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildAudioResourceChooserContextMenuEntry(addTo, entryIdentifier, elementType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildVideoResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<IVideo>> getter = (ConsumingSupplier<O, ResourceSupplier<IVideo>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<IVideo>> setter = (BiConsumer<O, ResourceSupplier<IVideo>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.video(null, file -> {}), ResourceSupplier::video, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.VIDEO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildVideoResourceChooserContextMenuEntry(addTo, entryIdentifier, elementType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildTextResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<IText>> getter = targetFieldGetter;
        BiConsumer<O, ResourceSupplier<IText>> setter = targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.text(null, file -> {}), ResourceSupplier::text, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.TEXT_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildTextResourceChooserContextMenuEntry(addTo, entryIdentifier, elementType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    @SuppressWarnings("all")
    default <R extends Resource, F extends FileType<R>, E extends O> ContextMenu.ClickableContextMenuEntry<?> buildGenericResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<R>> getter = (ConsumingSupplier<O, ResourceSupplier<R>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<R>> setter = (BiConsumer<O, ResourceSupplier<R>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, getter, setter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    default <R extends Resource, F extends FileType<R>, E extends O> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildGenericResourceChooserContextMenuEntry(addTo, entryIdentifier, elementType, resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    default <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> buildGenericResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("fancymenu.ui.resources.choose"),
                (menu, entry) -> {
                    List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                    if (entry.getStackMeta().isFirstInStack() && !selectedElements.isEmpty()) {
                        String preSelectedSource = null;
                        List<String> allPaths = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> {
                            ResourceSupplier<R> supplier = targetFieldGetter.get(consumes);
                            if (supplier != null) return supplier.getSourceWithPrefix();
                            return null;
                        });
                        if (!allPaths.isEmpty() && ListUtils.allInListEqual(allPaths)) {
                            preSelectedSource = allPaths.get(0);
                        }
                        ResourceChooserScreen<R,F> chooserScreen = resourceChooserScreenBuilder.get();
                        chooserScreen.setFileFilter(fileFilter);
                        chooserScreen.setAllowedFileTypes(fileTypes);
                        chooserScreen.setSource(preSelectedSource, false);
                        chooserScreen.setLocationSourceAllowed(allowLocation);
                        chooserScreen.setLocalSourceAllowed(allowLocal);
                        chooserScreen.setWebSourceAllowed(allowWeb);
                        chooserScreen.setResourceSourceCallback(source -> {
                            if (source != null) {
                                this.saveSnapshot();
                                for (O e : selectedElements) {
                                    targetFieldSetter.accept(e, resourceSupplierBuilder.get(source));
                                }
                            }
                            Minecraft.getInstance().setScreen(this.getContextMenuCallbackScreen());
                        });
                        Minecraft.getInstance().setScreen(chooserScreen);
                    }
                }).setStackable(true);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.ui.resources.reset"),
                    (menu, entry) -> {
                        if (entry.getStackMeta().isFirstInStack()) {
                            List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                            this.saveSnapshot();
                            for (O e : selectedElements) {
                                targetFieldSetter.accept(e, defaultValue);
                            }
                        }
                    }).setStackable(true);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
            if (selectedElements.size() == 1) {
                Component valueComponent;
                ResourceSupplier<R> supplier = targetFieldGetter.get(selectedElements.get(0));
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
            }
            return Component.empty();
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display")
                .addIsVisibleSupplier((menu, entry) -> this.getFilteredStackableObjectsList(selectedElementsFilter).size() == 1);
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .addIsVisibleSupplier((menu, entry) -> this.getFilteredStackableObjectsList(selectedElementsFilter).size() == 1)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subMenu).setStackable(true);

    }

    default <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildGenericResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ContextMenu subMenu = new ContextMenu();
        ContextMenu.ClickableContextMenuEntry<?> inputEntry = subMenu.addClickableEntry("input_value", Component.translatable("fancymenu.common_components.set"), (menu, entry) ->
        {
            if (entry.getStackMeta().isFirstInStack()) {
                List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                String defaultText = null;
                List<String> targetValuesOfSelected = new ArrayList<>();
                for (O e : selectedElements) {
                    targetValuesOfSelected.add(targetFieldGetter.get(e));
                }
                if (!entry.getStackMeta().isPartOfStack() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                    defaultText = targetFieldGetter.get(this.self());
                }
                Screen inputScreen;
                if (!multiLineInput && !allowPlaceholders) {
                    TextInputScreen s = TextInputScreen.build(label, inputCharacterFilter, call -> {
                        if (call != null) {
                            this.saveSnapshot();
                            for (O e : selectedElements) {
                                targetFieldSetter.accept(e, call);
                            }
                        }
                        menu.closeMenu();
                        Minecraft.getInstance().setScreen(this.getContextMenuCallbackScreen());
                    });
                    if (textValidator != null) {
                        s.setTextValidator(consumes -> {
                            if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                            return textValidator.get(consumes.getText());
                        });
                    }
                    s.setText(defaultText);
                    inputScreen = s;
                } else {
                    TextEditorScreen s = new TextEditorScreen(label, (inputCharacterFilter != null) ? inputCharacterFilter.convertToLegacyFilter() : null, (call) -> {
                        if (call != null) {
                            this.saveSnapshot();
                            for (O e : selectedElements) {
                                targetFieldSetter.accept(e, call);
                            }
                        }
                        menu.closeMenu();
                        Minecraft.getInstance().setScreen(this.getContextMenuCallbackScreen());
                    });
                    if (textValidator != null) {
                        s.setTextValidator(consumes -> {
                            if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                            return textValidator.get(consumes.getText());
                        });
                    }
                    s.setText(defaultText);
                    s.setMultilineMode(multiLineInput);
                    s.setPlaceholdersAllowed(allowPlaceholders);
                    inputScreen = s;
                }
                Minecraft.getInstance().setScreen(inputScreen);
            }
        }).setStackable(true);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"), (menu, entry) -> {
                if (entry.getStackMeta().isFirstInStack()) {
                    List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                    this.saveSnapshot();
                    for (O e : selectedElements) {
                        targetFieldSetter.accept(e, defaultValue);
                    }
                }
            }).setStackable(true);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
            if (selectedElements.size() == 1) {
                Component valueComponent;
                String val = targetFieldGetter.get(selectedElements.get(0));
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
            }
            return Component.empty();
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display")
                .addIsVisibleSupplier((menu, entry) -> this.getFilteredStackableObjectsList(selectedElementsFilter).size() == 1);
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .addIsVisibleSupplier((menu, entry) -> this.getFilteredStackableObjectsList(selectedElementsFilter).size() == 1)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subMenu).setStackable(true);

    }

    default ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, String> getter = (ConsumingSupplier<O, String>) targetFieldGetter;
        BiConsumer<O, String> setter = (BiConsumer<O, String>) targetFieldSetter;
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildGenericStringInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addGenericStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericStringInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildStringInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, String> getter = (ConsumingSupplier<O, String>) targetFieldGetter;
        BiConsumer<O, String> setter = (BiConsumer<O, String>) targetFieldSetter;
        return buildGenericStringInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildStringInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildGenericIntegerInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Integer> targetFieldGetter, @NotNull BiConsumer<O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedElementsFilter,
                consumes -> {
                    Integer i = targetFieldGetter.get(consumes);
                    if (i == null) i = 0;
                    return "" + i;
                },
                (e, s) -> {
                    if (MathUtils.isInteger(s)) targetFieldSetter.accept(e, Integer.valueOf(s));
                },
                CharacterFilter.buildIntegerFilter(), false, false, label, addResetOption, "" + defaultValue,
                (textValidator != null) ? textValidator : defaultIntegerValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addGenericIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Integer> targetFieldGetter, @NotNull BiConsumer<O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericIntegerInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildIntegerInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Integer> targetFieldGetter, @NotNull BiConsumer <O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Integer> getter = (ConsumingSupplier<O, Integer>) targetFieldGetter;
        BiConsumer<O, Integer> setter = (BiConsumer<O, Integer>) targetFieldSetter;
        return buildGenericIntegerInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Integer> targetFieldGetter, @NotNull BiConsumer <O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildIntegerInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildGenericLongInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Long> targetFieldGetter, @NotNull BiConsumer<O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedElementsFilter,
                consumes -> {
                    Long l = targetFieldGetter.get(consumes);
                    if (l == null) l = 0L;
                    return "" + l;
                },
                (e, s) -> {
                    if (MathUtils.isLong(s)) targetFieldSetter.accept(e, Long.valueOf(s));
                },
                CharacterFilter.buildIntegerFilter(), false, false, label, addResetOption, "" + defaultValue,
                (textValidator != null) ? textValidator : defaultLongValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addGenericLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Long> targetFieldGetter, @NotNull BiConsumer<O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericLongInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildLongInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Long> targetFieldGetter, @NotNull BiConsumer <O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Long> getter = (ConsumingSupplier<O, Long>) targetFieldGetter;
        BiConsumer<O, Long> setter = (BiConsumer<O, Long>) targetFieldSetter;
        return buildGenericLongInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Long> targetFieldGetter, @NotNull BiConsumer <O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildLongInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildGenericDoubleInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedElementsFilter,
                consumes -> {
                    Double d = targetFieldGetter.get(consumes);
                    if (d == null) d = 0D;
                    return "" + d;
                },
                (e, s) -> {
                    if (MathUtils.isDouble(s)) targetFieldSetter.accept(e, Double.valueOf(s));
                },
                CharacterFilter.buildDecimalFiler(), false, false, label, addResetOption, "" + defaultValue,
                (textValidator != null) ? textValidator : defaultDoubleValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addGenericDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericDoubleInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildDoubleInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Double> targetFieldGetter, @NotNull BiConsumer <O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Double> getter = (ConsumingSupplier<O, Double>) targetFieldGetter;
        BiConsumer<O, Double> setter = (BiConsumer<O, Double>) targetFieldSetter;
        return buildGenericDoubleInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Double> targetFieldGetter, @NotNull BiConsumer <O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildDoubleInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default ContextMenu.ClickableContextMenuEntry<?> buildGenericFloatInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Float> targetFieldGetter, @NotNull BiConsumer<O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedElementsFilter,
                consumes -> {
                    Float f = targetFieldGetter.get(consumes);
                    if (f == null) f = 0F;
                    return "" + f;
                },
                (e, s) -> {
                    if (MathUtils.isFloat(s)) targetFieldSetter.accept(e, Float.valueOf(s));
                },
                CharacterFilter.buildDecimalFiler(), false, false, label, addResetOption, "" + defaultValue,
                (textValidator != null) ? textValidator : defaultFloatValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addGenericFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Float> targetFieldGetter, @NotNull BiConsumer<O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericFloatInputContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildFloatInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Float> targetFieldGetter, @NotNull BiConsumer <O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Float> getter = (ConsumingSupplier<O, Float>) targetFieldGetter;
        BiConsumer<O, Float> setter = (BiConsumer<O, Float>) targetFieldSetter;
        return buildGenericFloatInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    default ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Float> targetFieldGetter, @NotNull BiConsumer <O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildFloatInputContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    default <V> ContextMenu.ClickableContextMenuEntry<?> buildGenericCycleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, V> targetFieldGetter, @NotNull BiConsumer<O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        return new ContextMenu.ClickableContextMenuEntry<>(entryIdentifier, parentMenu, Component.literal(""), (menu, entry) ->
                {
                    List<O> selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                    ValueCycle<V> cycle = this.setupValueCycle("switcher", ValueCycle.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
                    this.saveSnapshot();
                    if (!selectedElements.isEmpty() && entry.getStackMeta().isFirstInStack()) {
                        V next = cycle.next();
                        for (O e : selectedElements) {
                            targetFieldSetter.accept(e, next);
                        }
                    }
                })
                .setLabelSupplier((menu, entry) -> {
                    List<O> selectedElements = new ArrayList<>();
                    if (!entry.getStackMeta().getProperties().hasProperty("switcher")) {
                        selectedElements = this.getFilteredStackableObjectsList(selectedElementsFilter);
                    }
                    ValueCycle<V> switcher = this.setupValueCycle("switcher", ValueCycle.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
                    return labelSupplier.get(menu, (ContextMenu.ClickableContextMenuEntry<?>) entry, switcher.current());
                }).setStackable(true);
    }

    default <V> ContextMenu.ClickableContextMenuEntry<?> addGenericCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, V> targetFieldGetter, @NotNull BiConsumer<O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        return addTo.addEntry(buildGenericCycleContextMenuEntry(addTo, entryIdentifier, switcherValues, selectedElementsFilter, targetFieldGetter, targetFieldSetter, labelSupplier));
    }

    @SuppressWarnings("all")
    default <V, E extends O> ContextMenu.ClickableContextMenuEntry<?> buildCycleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, List<V> switcherValues, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, V> targetFieldGetter, @NotNull BiConsumer <O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        ConsumingSupplier<O, V> getter = (ConsumingSupplier<O, V>) targetFieldGetter;
        BiConsumer<O, V> setter = (BiConsumer<O, V>) targetFieldSetter;
        return buildGenericCycleContextMenuEntry(parentMenu, entryIdentifier, switcherValues, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, labelSupplier);
    }

    default <V, E extends O> ContextMenu.ClickableContextMenuEntry<?> addCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, V> targetFieldGetter, @NotNull BiConsumer <O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        return addTo.addEntry(buildCycleContextMenuEntry(addTo, entryIdentifier, switcherValues, elementType, targetFieldGetter, targetFieldSetter, labelSupplier));
    }

    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildToggleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        ConsumingSupplier<O, Boolean> getter = (ConsumingSupplier<O, Boolean>) targetFieldGetter;
        BiConsumer<O, Boolean> setter = (BiConsumer<O, Boolean>) targetFieldSetter;
        return buildGenericCycleContextMenuEntry(parentMenu, entryIdentifier, ListUtils.of(false, true), consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, (menu, entry, switcherValue) -> {
            if (switcherValue && entry.isActive()) {
                MutableComponent enabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.enabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
                return Component.translatable(labelLocalizationKeyBase, enabled);
            }
            MutableComponent disabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.disabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            return Component.translatable(labelLocalizationKeyBase, disabled);
        });
    }

    default ContextMenu.ClickableContextMenuEntry<?> addToggleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<O> elementType, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return addTo.addEntry(buildToggleContextMenuEntry(addTo, entryIdentifier, elementType, targetFieldGetter, targetFieldSetter, labelLocalizationKeyBase));
    }

    /**
     * Only supports old (legacy) toggle localization keys (format = localization.key.on / .off)!<br>
     * For newer localization keys, use <b>O#addToggleContextMenuEntryTo(...)</b> instead!
     */
    @Deprecated
    @Legacy("This is to be able to use old .on/.off localizations. Remove this in the future and update localizations.")
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericBooleanSwitcherContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Boolean> targetFieldGetter, @NotNull BiConsumer<O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return buildGenericCycleContextMenuEntry(parentMenu, entryIdentifier, ListUtils.of(false, true), selectedElementsFilter, targetFieldGetter, targetFieldSetter, (menu, entry, switcherValue) -> {
            if (switcherValue && entry.isActive()) {
                return Component.translatable(labelLocalizationKeyBase + ".on");
            }
            return Component.translatable(labelLocalizationKeyBase + ".off");
        });
    }

    @Deprecated
    @Legacy("This is to be able to use old .on/.off localizations. Remove this in the future and update localizations.")
    default ContextMenu.ClickableContextMenuEntry<?> addGenericBooleanSwitcherContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<O, Boolean> targetFieldGetter, @NotNull BiConsumer<O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return addTo.addEntry(buildGenericBooleanSwitcherContextMenuEntry(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, labelLocalizationKeyBase));
    }

    @SuppressWarnings("all")
    default <T> ValueCycle<T> setupValueCycle(String toggleIdentifier, ValueCycle<T> cycle, List<O> elements, ContextMenu.ContextMenuStackMeta stackMeta, ConsumingSupplier <O, T> defaultValue) {
        boolean hasProperty = stackMeta.getProperties().hasProperty(toggleIdentifier);
        ValueCycle<T> t = stackMeta.getProperties().putPropertyIfAbsentAndGet(toggleIdentifier, cycle);
        if (!elements.isEmpty()) {
            O firstElement = elements.get(0);
            if (!stackMeta.isPartOfStack()) {
                t.setCurrentValue(defaultValue.get(firstElement));
            } else if (!hasProperty) {
                if (ListUtils.allInListEqual(ObjectUtils.getOfAllUnsafe((List<Object>)((Object)elements), (ConsumingSupplier<Object,Object>)((Object)defaultValue)))) {
                    t.setCurrentValue(defaultValue.get(firstElement));
                }
            }
        }
        return t;
    }

    @FunctionalInterface
    interface CycleContextMenuEntryLabelSupplier<V> {
        Component get(ContextMenu menu, ContextMenu.ClickableContextMenuEntry<?> entry, V switcherValue);
    }
    
}
