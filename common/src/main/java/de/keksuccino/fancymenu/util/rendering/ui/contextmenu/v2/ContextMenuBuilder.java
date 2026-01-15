package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
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
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.RangeSliderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder and helper contract for creating {@link ContextMenu} entries and wiring up stacked (multi-select) behavior.
 * <p>
 * A "stack" is created when multiple context menus are combined via
 * {@link ContextMenu#stackContextMenus(ContextMenu...)}. Every entry that is marked stackable
 * becomes a linked chain (see {@link ContextMenu.ContextMenuStackMeta#getNextInStack()}).
 * The first entry in that chain is the one rendered in the stacked menu; subsequent entries
 * are accessible through the stack meta and are used to apply changes to all selected objects.
 * <p>
 * This builder abstracts that pattern and provides:
 * <ul>
 *     <li>stack-safe click handling for multi-select</li>
 *     <li>stack-wide value application via {@link #applyStackAppliers(ContextMenu.ContextMenuEntry, Object)}</li>
 *     <li>stack-wide value inspection via {@link #resolveStackValue(ContextMenu.ContextMenuEntry)}</li>
 *     <li>common UI flows (resource chooser, text input, cycle/toggle entries, etc.)</li>
 * </ul>
 *
 * <p><b>Minimal example (boolean toggle with stacking)</b>
 * <pre>{@code
 * ContextMenuBuilder<MyObject> builder = ...;
 * ContextMenu menu = new ContextMenu();
 *
 * builder.addGenericToggleContextMenuEntryTo(
 *     menu,
 *     "affects_overlay",
 *     obj -> obj instanceof MyObject,
 *     MyObject::isAffectedByDecorations,
 *     MyObject::setAffectedByDecorations,
 *     "fancymenu.context.affects_overlay.display_name"
 * );
 * }</pre>
 *
 * <p>For boolean/toggle entries, the localization value for the key base must include a
 * {@code %s} placeholder (for example {@code "Affects Overlay: %s"}). The mod replaces
 * {@code %s} dynamically with the current state text (Enabled/Disabled).
 *
 * <p><b>Minimal example (resource chooser with stacking)</b>
 * <pre>{@code
 * builder.addImageResourceChooserContextMenuEntryTo(
 *     menu,
 *     "background_texture",
 *     obj -> obj instanceof MyObject,
 *     ResourceSupplier.image("textures/gui/default.png"),
 *     MyObject::getBackgroundTexture,
 *     MyObject::setBackgroundTexture,
 *     Component.translatable("fancymenu.context.background_texture"),
 *     true,
 *     null,
 *     true, true, false
 * );
 * }</pre>
 *
 * <p>Notes:
 * <ul>
 *     <li>All "build..." methods return an entry; all "add..." methods add the entry to the menu.</li>
 *     <li>All stack-aware entries should call {@link ContextMenu.ContextMenuEntry#setStackable(boolean)}.</li>
 *     <li>Stacked operations should use {@link #applyStackAppliers(ContextMenu.ContextMenuEntry, Object)}
 *     rather than directly mutating {@link #self()}.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public interface ContextMenuBuilder<O> {

    /**
     * Internal runtime flag used by {@link #runStackedClickActions(ContextMenu.ClickableContextMenuEntry)}
     * to prevent re-entrant click handling while iterating a stack.
     */
    String STACK_ACTION_IN_PROGRESS_KEY = "stack_action_in_progress";
    /**
     * Internal runtime flag that temporarily forces {@link StackContext#isPrimary()} to return true
     * for all entries in the stack while a stacked click action is running.
     */
    String STACK_FORCE_APPLY_KEY = "stack_force_apply";
    /**
     * Internal runtime flag for callers that want to record snapshot creation during stacked actions.
     */
    String STACK_SNAPSHOT_TAKEN_KEY = "stack_snapshot_taken";

    /**
     * Processor invoked before a context menu entry opens a {@link Screen} via {@link Minecraft#setScreen(Screen)}.
     */
    @FunctionalInterface
    interface ContextMenuScreenOpenProcessor {
        void beforeOpen(@NotNull Screen screen);
    }

    /**
     * Returns the mutable processor list backing this builder.
     */
    @NotNull
    List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList();

    /**
     * Adds a screen open processor that will run before any context menu entry opens a screen.
     */
    default void addContextMenuScreenOpenProcessor(@NotNull ContextMenuScreenOpenProcessor processor) {
        Objects.requireNonNull(processor, "processor");
        Objects.requireNonNull(this.getContextMenuScreenOpenProcessorList(), "contextMenuScreenOpenProcessorList").add(processor);
    }

    /**
     * Removes a previously added screen open processor.
     *
     * @return true if a processor was removed
     */
    default boolean removeContextMenuScreenOpenProcessor(@NotNull ContextMenuScreenOpenProcessor processor) {
        Objects.requireNonNull(processor, "processor");
        return Objects.requireNonNull(this.getContextMenuScreenOpenProcessorList(), "contextMenuScreenOpenProcessorList").remove(processor);
    }

    /**
     * Returns all registered screen open processors.
     */
    @NotNull
    default List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessors() {
        List<ContextMenuScreenOpenProcessor> processors = Objects.requireNonNull(this.getContextMenuScreenOpenProcessorList(), "contextMenuScreenOpenProcessorList");
        return processors.isEmpty() ? List.of() : List.copyOf(processors);
    }

    /**
     * Opens a screen from a context menu entry, running any registered processors first.
     */
    default void openContextMenuScreen(@Nullable Screen screen) {
        if (screen != null) {
            for (ContextMenuScreenOpenProcessor processor : this.getContextMenuScreenOpenProcessors()) {
                processor.beforeOpen(screen);
            }
        }
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * Creates a standalone builder wrapper by providing the required callbacks.
     * <p>
     * This is useful in UI code that does not already implement {@link ContextMenuBuilder} directly.
     *
     * @param callbackScreenSupplier screen to return to after input/resource screens close
     * @param stackableObjectsListSupplier provides the full list of stackable objects, optionally filtered
     * @param selfSupplier supplies the "self" object for this menu instance
     * @param saveSnapshotVoidTask optional snapshot hook for history/undo
     */
    static <O> ContextMenuBuilder<O> createStandalone(@NotNull Supplier<Screen> callbackScreenSupplier, @NotNull ConsumingSupplier<ConsumingSupplier<O, Boolean>, List<O>> stackableObjectsListSupplier, @NotNull Supplier<O> selfSupplier, @Nullable Runnable saveSnapshotVoidTask, @Nullable Consumer<Object> saveSnapshotObjectTask, @Nullable Supplier<Object> createSnapshotSupplier) {
        return new ContextMenuBuilder<>() {
            private final List<ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessorList = new ArrayList<>();

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
                if (saveSnapshotVoidTask == null) return;
                saveSnapshotVoidTask.run();
            }
            @Override
            public void saveSnapshot(@NotNull Object snapshot) {
                if (saveSnapshotObjectTask == null) return;
                saveSnapshotObjectTask.accept(snapshot);
            }
            @Override
            public @Nullable Object createSnapshot() {
                if (createSnapshotSupplier == null) return null;
                return createSnapshotSupplier.get();
            }
            @Override
            public @NotNull List<O> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<O, Boolean> filter) {
                if (filter == null) filter = consumes -> true;
                return stackableObjectsListSupplier.get(filter);
            }
            @Override
            public @NotNull List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
                return this.contextMenuScreenOpenProcessorList;
            }
        };
    }

    /**
     * Returns the {@link Screen} {@link ContextMenu}s should come back to after doing things in other {@link Screen}s, like text input screens or similar.
     *
     * @return the screen that should be restored after a modal UI (text editor, resource chooser) closes.
     */
    @Nullable
    Screen getContextMenuCallbackScreen();

    /**
     * Used to get the self-instance object for building {@link ContextMenu}s.
     *
     * @return the object that this builder is currently editing (the "primary" selection).
     * <p>
     * In a stacked menu, this is still the local object's instance; use stack helpers to touch all selections.
     */
    @NotNull
    O self();

    /**
     * Saves a snapshot to the history/undo stack if supported.
     * <p>
     * Call this right before applying a value across a stack to ensure a single undo step.
     */
    void saveSnapshot();

    void saveSnapshot(@NotNull Object snapshot);

    @Nullable
    Object createSnapshot();

    /**
     * Returns the list of stackable objects for multi-selection.
     *
     * @param filter optional filter (may be null) to include only a subset
     * @return list of stackable objects, never null
     */
    @NotNull
    List<O> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<O, Boolean> filter);

    /**
     * Creates a {@link StackContext} for the given entry and filter.
     * <p>
     * This is the standard way to access the current selection and to perform stacked actions safely.
     *
     * @param entry the entry that initiated the action
     * @param filter optional filter for the selectable objects
     */
    default StackContext<O> stack(@NotNull ContextMenu.ContextMenuEntry<?> entry, @Nullable ConsumingSupplier<O, Boolean> filter) {
        return new StackContext<>(entry, this.getFilteredStackableObjectsList(filter));
    }

    /**
     * Runs click actions for every entry in a stack, but only once per logical group.
     * <p>
     * The entry must be the first entry in the stack; otherwise this returns false.
     * Grouping is controlled by {@link ContextMenu.ContextMenuEntry#getStackGroupKey()}.
     * If no group key is set, the entry instance is used as its own key.
     *
     * @param entry the entry being clicked
     * @return true if the stack was handled here, false if the caller should run its own click action
     */
    default boolean runStackedClickActions(@NotNull ContextMenu.ClickableContextMenuEntry<?> entry) {
        ContextMenu.ContextMenuStackMeta stackMeta = entry.getStackMeta();
        if (!stackMeta.isPartOfStack() || !stackMeta.isFirstInStack()) {
            return false;
        }
        if (Boolean.TRUE.equals(stackMeta.getProperties().getBooleanProperty(STACK_ACTION_IN_PROGRESS_KEY))) {
            return false;
        }
        stackMeta.getProperties().putProperty(STACK_ACTION_IN_PROGRESS_KEY, true);
        stackMeta.getProperties().putProperty(STACK_FORCE_APPLY_KEY, true);
        ContextMenu.ContextMenuEntry<?> current = entry;
        java.util.HashSet<Object> handledGroups = new java.util.HashSet<>();
        while (current != null) {
            if (current instanceof ContextMenu.ClickableContextMenuEntry<?> clickable) {
                Object groupKey = current.getStackGroupKey();
                if (groupKey == null) {
                    groupKey = current;
                }
                if (handledGroups.add(groupKey)) {
                    clickable.runClickAction();
                }
            }
            current = current.getStackMeta().getNextInStack();
        }
        stackMeta.getProperties().removeProperty(STACK_FORCE_APPLY_KEY);
        stackMeta.getProperties().removeProperty(STACK_ACTION_IN_PROGRESS_KEY);
        stackMeta.getProperties().removeProperty(STACK_SNAPSHOT_TAKEN_KEY);
        return true;
    }

    /**
     * Applies a value to all entries in a stack by invoking their {@link ContextMenu.StackApplier}.
     * <p>
     * Each stacked entry may have its own applier. This method is the canonical way to apply
     * values from menu actions (toggles, cycles, input, resource chooser, etc.).
     *
     * @param entry the first (visible) entry in the stack
     * @param value the value to apply (type is defined by the entry's applier)
     */
    default void applyStackAppliers(@NotNull ContextMenu.ContextMenuEntry<?> entry, @Nullable Object value) {
        ContextMenu.ContextMenuEntry<?> current = entry;
        while (current != null) {
            ContextMenu.StackApplier applier = current.getStackApplier();
            if (applier != null) {
                applier.apply(current, value);
            }
            current = current.getStackMeta().getNextInStack();
        }
    }

    /**
     * Resolves the current value of a stack by querying each entry's {@link ContextMenu.StackValueSupplier}.
     * <p>
     * This is used to detect "mixed" state. When not all values are equal, the returned
     * {@link StackValue} is marked as mixed.
     *
     * @param entry the first (visible) entry in the stack
     * @param <V>   the value type
     * @return a {@link StackValue} describing the stack state
     */
    default <V> StackValue<V> resolveStackValue(@NotNull ContextMenu.ContextMenuEntry<?> entry) {
        List<V> values = new ArrayList<>();
        ContextMenu.ContextMenuEntry<?> current = entry;
        while (current != null) {
            ContextMenu.StackValueSupplier supplier = current.getStackValueSupplier();
            if (supplier != null) {
                @SuppressWarnings("unchecked")
                V value = (V) supplier.get(current);
                values.add(value);
            }
            current = current.getStackMeta().getNextInStack();
        }
        if (values.isEmpty()) {
            return StackValue.empty();
        }
        V first = values.get(0);
        boolean mixed = false;
        for (int i = 1; i < values.size(); i++) {
            if (!Objects.equals(first, values.get(i))) {
                mixed = true;
                break;
            }
        }
        return new StackValue<>(false, mixed, first);
    }

    /**
     * Convenience wrapper around a selection and its stack metadata.
     * <p>
     * Instances are created via {@link #stack(ContextMenu.ContextMenuEntry, ConsumingSupplier)}.
     */
    final class StackContext<O> {

        private final ContextMenu.ContextMenuEntry<?> entry;
        private final List<O> objects;

        private StackContext(@NotNull ContextMenu.ContextMenuEntry<?> entry, @NotNull List<O> objects) {
            this.entry = entry;
            this.objects = objects;
        }

        /**
         * @return true if the entry is part of a stacked menu.
         */
        public boolean isStacked() {
            return this.entry.getStackMeta().isPartOfStack();
        }

        /**
         * Whether this entry should perform the primary action.
         * <p>
         * Only the first entry in a stack should perform actions, unless a stack action is
         * already in progress and {@link #STACK_FORCE_APPLY_KEY} is set to force all entries.
         */
        public boolean isPrimary() {
            if (Boolean.TRUE.equals(this.entry.getStackMeta().getProperties().getBooleanProperty(STACK_FORCE_APPLY_KEY))) {
                return true;
            }
            return !this.isStacked() || this.entry.getStackMeta().isFirstInStack();
        }

        /**
         * @return true if the selection is empty.
         */
        public boolean isEmpty() {
            return this.objects.isEmpty();
        }

        /**
         * @return the currently selected objects (filtered).
         */
        @NotNull
        public List<O> getObjects() {
            return this.objects;
        }

        /**
         * @return stack meta of the entry that created this context.
         */
        @NotNull
        public ContextMenu.ContextMenuStackMeta getStackMeta() {
            return this.entry.getStackMeta();
        }

        /**
         * Applies an action to all selected objects, but only if this context is primary.
         * <p>
         * This mirrors {@link #applyStackAppliers(ContextMenu.ContextMenuEntry, Object)}, but runs
         * on actual selected objects rather than entry appliers.
         */
        public void apply(@NotNull Consumer<O> action) {
            if (!this.isPrimary() || this.objects.isEmpty()) {
                return;
            }
            for (O object : this.objects) {
                action.accept(object);
            }
        }

        /**
         * Resolves a stack value directly from selected objects using a getter.
         * <p>
         * This is useful for mixed-state detection when you do not want to rely on
         * {@link ContextMenu.StackValueSupplier} on entries.
         */
        @NotNull
        public <V> StackValue<V> resolveValue(@NotNull ConsumingSupplier<O, V> getter) {
            if (this.objects.isEmpty()) {
                return StackValue.empty();
            }
            V first = getter.get(this.objects.get(0));
            boolean mixed = false;
            for (int i = 1; i < this.objects.size(); i++) {
                V current = getter.get(this.objects.get(i));
                if (!Objects.equals(first, current)) {
                    mixed = true;
                    break;
                }
            }
            return new StackValue<>(false, mixed, first);
        }

    }

    /**
     * Result type returned by stack value resolution.
     * <p>
     * A stack can be:
     * <ul>
     *     <li>empty: no objects selected</li>
     *     <li>mixed: values differ across selection</li>
     *     <li>common: all values equal</li>
     * </ul>
     */
    final class StackValue<V> {

        private final boolean empty;
        private final boolean mixed;
        @Nullable
        private final V value;

        private StackValue(boolean empty, boolean mixed, @Nullable V value) {
            this.empty = empty;
            this.mixed = mixed;
            this.value = value;
        }

        /**
         * @return a marker value indicating an empty stack.
         */
        @NotNull
        public static <V> StackValue<V> empty() {
            return new StackValue<>(true, false, null);
        }

        /**
         * @return true if the stack is empty.
         */
        public boolean isEmpty() {
            return this.empty;
        }

        /**
         * @return true if the stack has differing values.
         */
        public boolean isMixed() {
            return this.mixed;
        }

        /**
         * @return true if the stack has a common value (not empty and not mixed).
         */
        public boolean hasCommonValue() {
            return !this.empty && !this.mixed;
        }

        /**
         * @return the common value, or null if empty/mixed.
         */
        @Nullable
        public V getValue() {
            return this.value;
        }

    }

    /**
     * Builds a resource chooser entry for image resources.
     * <p>
     * The entry opens a {@link ResourceChooserScreen} and applies the selected resource
     * to the current selection using stack appliers.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildImageResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<ITexture>> getter = (ConsumingSupplier<O, ResourceSupplier<ITexture>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<ITexture>> setter = (BiConsumer<O, ResourceSupplier<ITexture>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter, () -> ResourceChooserScreen.image(null, file -> {}), ResourceSupplier::image, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.IMAGE_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds an image resource chooser entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildImageResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds an image resource chooser entry for a specific object type.
     * @see #buildImageResourceChooserContextMenuEntry(ContextMenu, String, ConsumingSupplier, ResourceSupplier, ConsumingSupplier, BiConsumer, Component, boolean, FileFilter, boolean, boolean, boolean)
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildImageResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return buildImageResourceChooserContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds an image resource chooser entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildImageResourceChooserContextMenuEntry(addTo, entryIdentifier, objectType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a resource chooser entry for audio resources.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildAudioResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<IAudio>> getter = (ConsumingSupplier<O, ResourceSupplier<IAudio>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<IAudio>> setter = (BiConsumer<O, ResourceSupplier<IAudio>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter, () -> ResourceChooserScreen.audio(null, file -> {}), ResourceSupplier::audio, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.AUDIO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds an audio resource chooser entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildAudioResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds an audio resource chooser entry for a specific object type.
     * @see #buildAudioResourceChooserContextMenuEntry(ContextMenu, String, ConsumingSupplier, ResourceSupplier, ConsumingSupplier, BiConsumer, Component, boolean, FileFilter, boolean, boolean, boolean)
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildAudioResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return buildAudioResourceChooserContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds an audio resource chooser entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildAudioResourceChooserContextMenuEntry(addTo, entryIdentifier, objectType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a resource chooser entry for video resources.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildVideoResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<IVideo>> getter = (ConsumingSupplier<O, ResourceSupplier<IVideo>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<IVideo>> setter = (BiConsumer<O, ResourceSupplier<IVideo>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter, () -> ResourceChooserScreen.video(null, file -> {}), ResourceSupplier::video, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.VIDEO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds a video resource chooser entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildVideoResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a video resource chooser entry for a specific object type.
     * @see #buildVideoResourceChooserContextMenuEntry(ContextMenu, String, ConsumingSupplier, ResourceSupplier, ConsumingSupplier, BiConsumer, Component, boolean, FileFilter, boolean, boolean, boolean)
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildVideoResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return buildVideoResourceChooserContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds a video resource chooser entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildVideoResourceChooserContextMenuEntry(addTo, entryIdentifier, objectType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a resource chooser entry for text resources.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildTextResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter, () -> ResourceChooserScreen.text(null, file -> {}), ResourceSupplier::text, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.TEXT_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds a text resource chooser entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildTextResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a text resource chooser entry for a specific object type.
     * @see #buildTextResourceChooserContextMenuEntry(ContextMenu, String, ConsumingSupplier, ResourceSupplier, ConsumingSupplier, BiConsumer, Component, boolean, FileFilter, boolean, boolean, boolean)
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildTextResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return buildTextResourceChooserContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds a text resource chooser entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildTextResourceChooserContextMenuEntry(addTo, entryIdentifier, objectType, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a generic resource chooser entry for a specific object type.
     * @see #buildGenericResourceChooserContextMenuEntry(ContextMenu, String, ConsumingSupplier, Supplier, ConsumingSupplier, ResourceSupplier, ConsumingSupplier, BiConsumer, Component, boolean, FileTypeGroup, FileFilter, boolean, boolean, boolean)
     */
    @SuppressWarnings("all")
    default <R extends Resource, F extends FileType<R>, E extends O> ContextMenu.ClickableContextMenuEntry<?> buildGenericResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        ConsumingSupplier<O, ResourceSupplier<R>> getter = (ConsumingSupplier<O, ResourceSupplier<R>>) targetFieldGetter;
        BiConsumer<O, ResourceSupplier<R>> setter = (BiConsumer<O, ResourceSupplier<R>>) targetFieldSetter;
        return buildGenericResourceChooserContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> objectType.isAssignableFrom(consumes.getClass()), resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, getter, setter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /**
     * Adds a generic resource chooser entry for a specific object type.
     */
    default <R extends Resource, F extends FileType<R>, E extends O> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier <O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer <O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildGenericResourceChooserContextMenuEntry(addTo, entryIdentifier, objectType, resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a generic resource chooser entry for any resource type.
     * <p>
     * The entry spawns a {@link ResourceChooserScreen}, applies the selected resource to the
     * stack via {@link #applyStackAppliers(ContextMenu.ContextMenuEntry, Object)}, and optionally
     * adds a "reset to default" action.
     *
     * @param selectedObjectsFilter optional filter for selection
     * @param resourceChooserScreenBuilder builder for the chooser UI
     * @param resourceSupplierBuilder creates a {@link ResourceSupplier} from a source string
     * @param targetFieldGetter getter for the field on a single object
     * @param targetFieldSetter setter for the field on a single object
     */
    default <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> buildGenericResourceChooserContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("fancymenu.ui.resources.choose"),
                        (menu, entry) -> {
                            StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                            if (!stack.isPrimary() || stack.isEmpty()) {
                                return;
                            }
                            List<O> selectedObjects = stack.getObjects();
                            String preSelectedSource = null;
                            List<String> allPaths = ObjectUtils.getOfAll(String.class, selectedObjects, consumes -> {
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
                                    this.applyStackAppliers(entry, source);
                                }
                                this.openContextMenuScreen(this.getContextMenuCallbackScreen());
                            });
                            this.openContextMenuScreen(chooserScreen);
                        }).setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("file"))
                .setStackApplier((stackEntry, value) -> {
                    if (!(value instanceof String source)) {
                        return;
                    }
                    targetFieldSetter.accept(this.self(), resourceSupplierBuilder.get(source));
                });

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.ui.resources.reset"),
                            (menu, entry) -> {
                                StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                this.saveSnapshot();
                                this.applyStackAppliers(entry, null);
                            }).setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("undo"))
                    .setStackApplier((stackEntry, value) -> {
                        targetFieldSetter.accept(this.self(), defaultValue);
                    });
        }

        subMenu.addSeparatorEntry("separator_before_current_value_display")
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1);
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> {
                    List<O> selectedObjects = this.stack(entry, selectedObjectsFilter).getObjects();
                    if (selectedObjects.size() == 1) {
                        Component valueComponent;
                        ResourceSupplier<R> supplier = targetFieldGetter.get(selectedObjects.get(0));
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
                })
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subMenu).setStackable(true);

    }

    /**
     * Adds a generic resource chooser entry to the given menu.
     */
    default <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<O, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<O, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addTo.addEntry(buildGenericResourceChooserContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb));
    }

    /**
     * Builds a range slider input entry that opens {@link RangeSliderScreen}.
     * <p>
     * This entry is stack-aware and applies the slider value to all selected objects.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildRangeSliderInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, double minSliderValue, double maxSliderValue, @NotNull ConsumingSupplier<Double, Component> sliderLabelSupplier) {
        ContextMenu subMenu = new ContextMenu();
        subMenu.addClickableEntry("input_value", Component.translatable("fancymenu.common_components.set"), (menu, entry) -> {
                    StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                    if (!stack.isPrimary() || stack.isEmpty()) {
                        return;
                    }
                    Double presetValue = targetFieldGetter.get(this.self());
                    menu.closeMenuChain();
                    RangeSliderScreen sliderScreen = new RangeSliderScreen(minSliderValue, maxSliderValue, Objects.requireNonNullElse(presetValue, 0.0D), sliderLabelSupplier,
                            value -> this.applyStackAppliers(entry, value),
                            value -> {
                                this.saveSnapshot();
                                this.applyStackAppliers(entry, value);
                            },
                            value -> this.applyStackAppliers(entry, value));
                    PiPWindow window = new PiPWindow(label)
                            .setScreen(sliderScreen)
                            .setForceFancyMenuUiScale(true)
                            .setMinSize(RangeSliderScreen.PIP_WINDOW_WIDTH, RangeSliderScreen.PIP_WINDOW_HEIGHT)
                            .setSize(RangeSliderScreen.PIP_WINDOW_WIDTH, RangeSliderScreen.PIP_WINDOW_HEIGHT);
                    PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
                }).setStackable(true)
                .setStackApplier((stackEntry, value) -> {
                    if (!(value instanceof Number number)) {
                        return;
                    }
                    targetFieldSetter.accept(this.self(), number.doubleValue());
                });

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"), (menu, entry) -> {
                        StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                        if (!stack.isPrimary() || stack.isEmpty()) {
                            return;
                        }
                        this.saveSnapshot();
                        this.applyStackAppliers(entry, null);
                    }).setStackable(true)
                    .setStackApplier((stackEntry, value) -> targetFieldSetter.accept(this.self(), defaultValue))
                    .setIcon(ContextMenu.IconFactory.getIcon("undo"));
        }

        subMenu.addSeparatorEntry("separator_before_current_value_display")
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1);
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> {
                    List<O> selectedObjects = this.stack(entry, selectedObjectsFilter).getObjects();
                    if (selectedObjects.size() == 1) {
                        Component valueComponent;
                        Double val = targetFieldGetter.get(selectedObjects.get(0));
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
                    }
                    return Component.empty();
                })
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subMenu).setStackable(true);
    }

    /**
     * Adds a range slider input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addRangeSliderInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, double minSliderValue, double maxSliderValue, @NotNull ConsumingSupplier<Double, Component> sliderLabelSupplier) {
        return addTo.addEntry(buildRangeSliderInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, minSliderValue, maxSliderValue, sliderLabelSupplier));
    }

    /**
     * Builds a text input entry that opens {@link TextInputScreen} or {@link TextEditorScreen}.
     * <p>
     * This entry is stack-aware: it uses {@link #applyStackAppliers(ContextMenu.ContextMenuEntry, Object)}
     * to apply the resulting text to all selected objects.
     *
     * @param inputCharacterFilter optional per-character filter
     * @param multiLineInput true to use multiline text editor
     * @param allowPlaceholders true to allow placeholder tokens
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ContextMenu subMenu = new ContextMenu();
        ContextMenu.ClickableContextMenuEntry<?> inputEntry = subMenu.addClickableEntry("input_value", Component.translatable("fancymenu.common_components.set"), (menu, entry) ->
                {
                    StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                    if (!stack.isPrimary() || stack.isEmpty()) {
                        return;
                    }
                    List<O> selectedObjects = stack.getObjects();
                    String defaultText = null;
                    List<String> targetValuesOfSelected = new ArrayList<>();
                    for (O e : selectedObjects) {
                        targetValuesOfSelected.add(targetFieldGetter.get(e));
                    }
                    if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                        defaultText = targetFieldGetter.get(this.self());
                    }
                    if (!multiLineInput && !allowPlaceholders) {
                        menu.closeMenuChain();
                        TextInputScreen s = new TextInputScreen(inputCharacterFilter, call -> {
                            if (call != null) {
                                this.saveSnapshot();
                                this.applyStackAppliers(entry, call);
                            }
                        }).setText(defaultText);
                        if (textValidator != null) {
                            s.setTextValidator(consumes -> {
                                if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                                return textValidator.get(consumes.getText());
                            });
                        }
                        Dialogs.openGeneric(s, label, ContextMenu.IconFactory.getIcon("text"), TextInputScreen.PIP_WINDOW_WIDTH, TextInputScreen.PIP_WINDOW_HEIGHT);
                    } else {
                        menu.closeMenuChain();
                        TextEditorScreen s = new TextEditorScreen(label, (inputCharacterFilter != null) ? inputCharacterFilter.convertToLegacyFilter() : null, (call) -> {
                            if (call != null) {
                                this.saveSnapshot();
                                this.applyStackAppliers(entry, call);
                            }
                        }).setText(defaultText)
                                .setMultilineMode(multiLineInput)
                                .setPlaceholdersAllowed(allowPlaceholders);
                        if (textValidator != null) {
                            s.setTextValidator(consumes -> {
                                if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                                return textValidator.get(consumes.getText());
                            });
                        }
                        Dialogs.openGeneric(s, label, ContextMenu.IconFactory.getIcon("text"), TextEditorScreen.PIP_WINDOW_WIDTH, TextEditorScreen.PIP_WINDOW_HEIGHT);
                    }
                }).setStackable(true)
                .setStackApplier((stackEntry, value) -> {
                    if (!(value instanceof String call)) {
                        return;
                    }
                    targetFieldSetter.accept(this.self(), call);
                })
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"), (menu, entry) -> {
                        StackContext<O> stack = this.stack(entry, selectedObjectsFilter);
                        if (!stack.isPrimary() || stack.isEmpty()) {
                            return;
                        }
                        this.saveSnapshot();
                        this.applyStackAppliers(entry, null);
                    }).setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        targetFieldSetter.accept(this.self(), defaultValue);
                    })
                    .setIcon(ContextMenu.IconFactory.getIcon("undo"));
        }

        subMenu.addSeparatorEntry("separator_before_current_value_display")
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1);
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> {
                    List<O> selectedObjects = this.stack(entry, selectedObjectsFilter).getObjects();
                    if (selectedObjects.size() == 1) {
                        Component valueComponent;
                        String val = targetFieldGetter.get(selectedObjects.get(0));
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
                })
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .addIsVisibleSupplier((menu, entry) -> this.stack(entry, selectedObjectsFilter).getObjects().size() == 1)
                .setIcon(ContextMenu.IconFactory.getIcon("info"));

        return new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subMenu).setStackable(true);

    }

    /**
     * Adds a text input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a text input entry for a specific object type.
     * @see #buildInputContextMenuEntry(ContextMenu, String, ConsumingSupplier, ConsumingSupplier, BiConsumer, CharacterFilter, boolean, boolean, Component, boolean, String, ConsumingSupplier, ConsumingSupplier)
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, String> getter = (ConsumingSupplier<O, String>) targetFieldGetter;
        BiConsumer<O, String> setter = (BiConsumer<O, String>) targetFieldSetter;
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, (consumes) -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a text input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a generic string input entry.
     * <p>
     * This is a convenience alias for {@link #buildInputContextMenuEntry}.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericStringInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a generic string input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, String> targetFieldGetter, @NotNull BiConsumer<O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericStringInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a string input entry for a specific object type.
     * @see #buildGenericStringInputContextMenuEntry(ContextMenu, String, ConsumingSupplier, ConsumingSupplier, BiConsumer, CharacterFilter, boolean, boolean, Component, boolean, String, ConsumingSupplier, ConsumingSupplier)
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildStringInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, String> getter = (ConsumingSupplier<O, String>) targetFieldGetter;
        BiConsumer<O, String> setter = (BiConsumer<O, String>) targetFieldSetter;
        return buildGenericStringInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a string input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, String> targetFieldGetter, @NotNull BiConsumer <O, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildStringInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds an integer input entry.
     * <p>
     * This is a convenience wrapper around {@link #buildInputContextMenuEntry} that
     * converts between text and integer values.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericIntegerInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Integer> targetFieldGetter, @NotNull BiConsumer<O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter,
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

    /**
     * Adds an integer input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Integer> targetFieldGetter, @NotNull BiConsumer<O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericIntegerInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds an integer input entry for a specific object type.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildIntegerInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Integer> targetFieldGetter, @NotNull BiConsumer <O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Integer> getter = (ConsumingSupplier<O, Integer>) targetFieldGetter;
        BiConsumer<O, Integer> setter = (BiConsumer<O, Integer>) targetFieldSetter;
        return buildGenericIntegerInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds an integer input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Integer> targetFieldGetter, @NotNull BiConsumer <O, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildIntegerInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a long input entry.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericLongInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Long> targetFieldGetter, @NotNull BiConsumer<O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter,
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

    /**
     * Adds a long input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Long> targetFieldGetter, @NotNull BiConsumer<O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericLongInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a long input entry for a specific object type.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildLongInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Long> targetFieldGetter, @NotNull BiConsumer <O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Long> getter = (ConsumingSupplier<O, Long>) targetFieldGetter;
        BiConsumer<O, Long> setter = (BiConsumer<O, Long>) targetFieldSetter;
        return buildGenericLongInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a long input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Long> targetFieldGetter, @NotNull BiConsumer <O, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildLongInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a double input entry.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericDoubleInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter,
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

    /**
     * Adds a double input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Double> targetFieldGetter, @NotNull BiConsumer<O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericDoubleInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a double input entry for a specific object type.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildDoubleInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Double> targetFieldGetter, @NotNull BiConsumer <O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Double> getter = (ConsumingSupplier<O, Double>) targetFieldGetter;
        BiConsumer<O, Double> setter = (BiConsumer<O, Double>) targetFieldSetter;
        return buildGenericDoubleInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a double input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Double> targetFieldGetter, @NotNull BiConsumer <O, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildDoubleInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a float input entry.
     */
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericFloatInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Float> targetFieldGetter, @NotNull BiConsumer<O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
        return buildInputContextMenuEntry(parentMenu, entryIdentifier, selectedObjectsFilter,
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

    /**
     * Adds a float input entry to the given menu.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, Float> targetFieldGetter, @NotNull BiConsumer<O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildGenericFloatInputContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a float input entry for a specific object type.
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildFloatInputContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Float> targetFieldGetter, @NotNull BiConsumer <O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        ConsumingSupplier<O, Float> getter = (ConsumingSupplier<O, Float>) targetFieldGetter;
        BiConsumer<O, Float> setter = (BiConsumer<O, Float>) targetFieldSetter;
        return buildGenericFloatInputContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
    }

    /**
     * Adds a float input entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Float> targetFieldGetter, @NotNull BiConsumer <O, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addTo.addEntry(buildFloatInputContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback));
    }

    /**
     * Builds a click-to-cycle entry that is aware of stacked menus.
     * <p>
     * The current value is resolved using {@link #resolveStackValue(ContextMenu.ContextMenuEntry)}.
     * If the stack is mixed, the cycle starts from its default (first) value.
     *
     * <p><b>Example</b>
     * <pre>{@code
     * builder.addGenericCycleContextMenuEntryTo(
     *     menu,
     *     "h_align",
     *     ListUtils.of(Alignment.LEFT, Alignment.CENTER, Alignment.RIGHT),
     *     obj -> obj instanceof MyObject,
     *     MyObject::getHorizontalAlignment,
     *     MyObject::setHorizontalAlignment,
     *     (m, e, v) -> Component.translatable(
     *         "fancymenu.align.display_name",
     *         Component.translatable("fancymenu.align." + v.name().toLowerCase())
     *     )
     * );
     * }</pre>
     *
     * <p><b>Localization format</b>
     * If you use a localization key base for cycle entries, the localization value should include
     * a {@code %s} placeholder (for example {@code "Display Name: %s"}). The label supplier should
     * pass the current cycle value's display name as the argument so {@code %s} gets replaced.
     */
    default <V> ContextMenu.ClickableContextMenuEntry<?> buildGenericCycleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, V> targetFieldGetter, @NotNull BiConsumer<O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        ContextMenu.ClickableContextMenuEntry<?> entry = new ContextMenu.ClickableContextMenuEntry<>(entryIdentifier, parentMenu, Component.literal(""), (menu, entryRef) -> {
            if (entryRef.getStackMeta().isPartOfStack() && !entryRef.getStackMeta().isFirstInStack()) {
                return;
            }
            StackValue<V> currentValue = this.resolveStackValue(entryRef);
            if (currentValue.isEmpty()) {
                return;
            }
            ValueCycle<V> cycle = ValueCycle.fromList(switcherValues);
            if (currentValue.hasCommonValue()) {
                cycle.setCurrentValue(currentValue.getValue(), false);
            }
            V next = cycle.next();
            this.saveSnapshot();
            this.applyStackAppliers(entryRef, next);
        });
        entry.setLabelSupplier((menu, entryRef) -> {
            StackValue<V> currentValue = this.resolveStackValue(entryRef);
            ValueCycle<V> cycle = ValueCycle.fromList(switcherValues);
            if (currentValue.hasCommonValue()) {
                cycle.setCurrentValue(currentValue.getValue(), false);
            }
            return labelSupplier.get(menu, (ContextMenu.ClickableContextMenuEntry<?>) entryRef, cycle.current());
        });
        entry.setStackApplier((stackEntry, value) -> {
            @SuppressWarnings("unchecked")
            V casted = (V) value;
            targetFieldSetter.accept(this.self(), casted);
        });
        entry.setStackValueSupplier(stackEntry -> targetFieldGetter.get(this.self()));
        entry.setStackable(true);
        entry.setStackGroupKey(this.self().getClass());
        return entry;
    }

    /**
     * Adds a generic cycle entry to the given menu.
     * <p>
     * If you use a localization key base for cycle entries, the localization value should include
     * a {@code %s} placeholder (for example {@code "Display Name: %s"}). The label supplier should
     * pass the current cycle value's display name as the argument so {@code %s} gets replaced.
     * </p>
     */
    default <V> ContextMenu.ClickableContextMenuEntry<?> addGenericCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier<O, V> targetFieldGetter, @NotNull BiConsumer<O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        return addTo.addEntry(buildGenericCycleContextMenuEntry(addTo, entryIdentifier, switcherValues, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, labelSupplier));
    }

    /**
     * Builds a cycle entry for a specific object type.
     * <p>
     * If you use a localization key base for cycle entries, the localization value should include
     * a {@code %s} placeholder (for example {@code "Display Name: %s"}). The label supplier should
     * pass the current cycle value's display name as the argument so {@code %s} gets replaced.
     * </p>
     */
    @SuppressWarnings("all")
    default <V, E extends O> ContextMenu.ClickableContextMenuEntry<?> buildCycleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, List<V> switcherValues, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, V> targetFieldGetter, @NotNull BiConsumer <O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        ConsumingSupplier<O, V> getter = (ConsumingSupplier<O, V>) targetFieldGetter;
        BiConsumer<O, V> setter = (BiConsumer<O, V>) targetFieldSetter;
        return buildGenericCycleContextMenuEntry(parentMenu, entryIdentifier, switcherValues, consumes -> objectType.isAssignableFrom(consumes.getClass()), getter, setter, labelSupplier);
    }

    /**
     * Adds a cycle entry for a specific object type.
     */
    default <V, E extends O> ContextMenu.ClickableContextMenuEntry<?> addCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, V> targetFieldGetter, @NotNull BiConsumer <O, V> targetFieldSetter, @NotNull ContextMenuBuilder.CycleContextMenuEntryLabelSupplier<V> labelSupplier) {
        return addTo.addEntry(buildCycleContextMenuEntry(addTo, entryIdentifier, switcherValues, objectType, targetFieldGetter, targetFieldSetter, labelSupplier));
    }

    /**
     * Builds a boolean toggle entry (false/true cycle) with standard FancyMenu labels.
     * <p>
     * The localization value for {@code labelLocalizationKeyBase} must include a {@code %s}
     * placeholder (for example {@code "Display Name: %s"}). The mod replaces {@code %s}
     * dynamically with the current state text (Enabled/Disabled).
     * </p>
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildGenericToggleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        ConsumingSupplier<O, Boolean> getter = (ConsumingSupplier<O, Boolean>) targetFieldGetter;
        BiConsumer<O, Boolean> setter = (BiConsumer<O, Boolean>) targetFieldSetter;
        return buildGenericCycleContextMenuEntry(parentMenu, entryIdentifier, ListUtils.of(false, true), selectedObjectsFilter, getter, setter, (menu, entry, switcherValue) -> {
            if (switcherValue && entry.isActive()) {
                MutableComponent enabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.enabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
                return Component.translatable(labelLocalizationKeyBase, enabled);
            }
            MutableComponent disabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.disabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            return Component.translatable(labelLocalizationKeyBase, disabled);
        });
    }

    /**
     * Adds a toggle entry to the given menu.
     * <p>
     * The localization value for {@code labelLocalizationKeyBase} must include a {@code %s}
     * placeholder (for example {@code "Display Name: %s"}). The mod replaces {@code %s}
     * dynamically with the current state text (Enabled/Disabled).
     * </p>
     */
    default ContextMenu.ClickableContextMenuEntry<?> addGenericToggleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<O, Boolean> selectedObjectsFilter, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return addTo.addEntry(buildGenericToggleContextMenuEntry(addTo, entryIdentifier, selectedObjectsFilter, targetFieldGetter, targetFieldSetter, labelLocalizationKeyBase));
    }

    /**
     * Builds a toggle entry for a specific object type.
     * <p>
     * The localization value for {@code labelLocalizationKeyBase} must include a {@code %s}
     * placeholder (for example {@code "Display Name: %s"}). The mod replaces {@code %s}
     * dynamically with the current state text (Enabled/Disabled).
     * </p>
     */
    @SuppressWarnings("all")
    default ContextMenu.ClickableContextMenuEntry<?> buildToggleContextMenuEntry(@NotNull ContextMenu parentMenu, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return buildGenericToggleContextMenuEntry(parentMenu, entryIdentifier, consumes -> objectType.isAssignableFrom(consumes.getClass()), targetFieldGetter, targetFieldSetter, labelLocalizationKeyBase);
    }

    /**
     * Adds a toggle entry for a specific object type.
     */
    default ContextMenu.ClickableContextMenuEntry<?> addToggleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<? extends O> objectType, @NotNull ConsumingSupplier <O, Boolean> targetFieldGetter, @NotNull BiConsumer <O, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return addTo.addEntry(buildToggleContextMenuEntry(addTo, entryIdentifier, objectType, targetFieldGetter, targetFieldSetter, labelLocalizationKeyBase));
    }

    /**
     * Label supplier for cycle/toggle entries.
     * <p>
     * The current cycle value is provided so the label can reflect it.
     * <p>
     * When using localization keys for cycle labels, prefer a value that includes a {@code %s}
     * placeholder (for example {@code "Display Name: %s"}), and pass the current cycle value's
     * display name as the argument to {@link Component#translatable(String, Object...)}.
     */
    @FunctionalInterface
    interface CycleContextMenuEntryLabelSupplier<V> {
        Component get(ContextMenu menu, ContextMenu.ClickableContextMenuEntry<?> entry, V switcherValue);
    }

}
