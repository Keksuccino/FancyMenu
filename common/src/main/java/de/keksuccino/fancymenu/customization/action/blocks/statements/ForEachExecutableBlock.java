package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Runs its contents once for every entry of a list variable, from the first entry to the last.
 *
 * <p>Everything inside the loop gets two value placeholders: {@code $$list_entry} (the current entry) and {@code $$list_index}
 * (its index, starting at 0). The loop reads the entries once when it starts, so its contents can edit the list without changing
 * which entries the loop goes through.</p>
 *
 * <p><b>How the value placeholders work:</b> value placeholders get copied into every executable of a block when it gets added
 * (see {@link AbstractExecutableBlock#addValuePlaceholder(String, Supplier)}), but the action script editor moves executables between
 * blocks without that, and copies of executables keep the placeholders they had. So the suppliers of this block are the same objects
 * for every FOR EACH block and don't belong to one block: they read the values of the innermost loop that currently runs on the thread.
 * That keeps nested loops correct (the inner loop's values win for its contents, the outer loop's values are back after the inner loop
 * finished), works for scripts that run on other threads, and makes it harmless when an executable that got moved out of a loop still
 * has the suppliers. When no loop runs, they return their own reference text, so the reference stays as it is, like a reference without
 * any value placeholder (and resolves to a FancyMenu variable with that name, if there is one).</p>
 */
public class ForEachExecutableBlock extends AbstractExecutableBlock {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String LIST_ENTRY_PLACEHOLDER = "list_entry";
    public static final String LIST_INDEX_PLACEHOLDER = "list_index";
    private static final String LIST_ENTRY_REFERENCE = ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + LIST_ENTRY_PLACEHOLDER;
    private static final String LIST_INDEX_REFERENCE = ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + LIST_INDEX_PLACEHOLDER;
    private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(WhileExecutableBlock.TIMEOUT_MILLIS);

    /** The values of the innermost loop that runs on the thread, or nothing if no loop runs. */
    private static final ThreadLocal<LoopValues> CURRENT_LOOP_VALUES = new ThreadLocal<>();
    private static final Supplier<String> LIST_ENTRY_VALUE = () -> {
        LoopValues values = CURRENT_LOOP_VALUES.get();
        return (values != null) ? values.entry : LIST_ENTRY_REFERENCE;
    };
    private static final Supplier<String> LIST_INDEX_VALUE = () -> {
        LoopValues values = CURRENT_LOOP_VALUES.get();
        return (values != null) ? values.getIndexText() : LIST_INDEX_REFERENCE;
    };

    @NotNull
    private String listVariableName = "";
    private boolean collapsed = false;

    /** Retained so copy() can preserve an injected clock. */
    @NotNull
    private final LongSupplier nanoTimeSupplier;
    /** Returns the entries of a list variable, or {@code null} if it doesn't exist or is a text variable. Retained so copy() can preserve it. */
    @NotNull
    private final Function<String, List<String>> listEntriesGetter;

    public ForEachExecutableBlock() {
        this(System::nanoTime, VariableHandler::getListEntries);
    }

    public ForEachExecutableBlock(@NotNull String listVariableName) {
        this();
        this.setListVariableName(listVariableName);
    }

    ForEachExecutableBlock(@NotNull LongSupplier nanoTimeSupplier, @NotNull Function<String, List<String>> listEntriesGetter) {
        this.nanoTimeSupplier = Objects.requireNonNull(nanoTimeSupplier);
        this.listEntriesGetter = Objects.requireNonNull(listEntriesGetter);
        // Executables added later get these through addExecutable(), like all value placeholders of a block
        this.valuePlaceholders.put(LIST_ENTRY_PLACEHOLDER, LIST_ENTRY_VALUE);
        this.valuePlaceholders.put(LIST_INDEX_PLACEHOLDER, LIST_INDEX_VALUE);
    }

    @Override
    public String getBlockType() {
        return "for-each";
    }

    /**
     * Executes the block contents once for every entry the list has when the loop starts, but stops after
     * {@link WhileExecutableBlock#TIMEOUT_MILLIS}, like WHILE loops, so a loop over a huge list can't freeze the game.<br>
     * The start time is local to each run, so a nested run of this same block (like in the script of an On Variable Updated listener
     * that sets a variable) can't reset the timer of the run around it. The values of the loop around this one get restored when it
     * ends, also when its contents throw.
     */
    @Override
    public void execute() {
        String name = this.resolveListVariableName();
        if (name.isEmpty()) return;
        List<String> entries = this.listEntriesGetter.apply(name);
        if (entries == null) {
            LOGGER.debug("[FANCYMENU] FOR EACH loop skipped, because '{}' is no list variable.", name);
            return;
        }
        int size = entries.size();
        if (size == 0) return;
        long loopStartTime = this.nanoTimeSupplier.getAsLong();
        LoopValues outerValues = CURRENT_LOOP_VALUES.get();
        LoopValues values = new LoopValues();
        CURRENT_LOOP_VALUES.set(values);
        try {
            for (int i = 0; i < size; i++) {
                if ((this.nanoTimeSupplier.getAsLong() - loopStartTime) >= TIMEOUT_NANOS) {
                    LOGGER.warn("[FANCYMENU] ForEachExecutableBlock loop timed out after {} milliseconds! It stopped after {} of the {} entries of list variable '{}'.", WhileExecutableBlock.TIMEOUT_MILLIS, i, size, name);
                    return;
                }
                values.moveTo(entries.get(i), i);
                super.execute();
            }
        } finally {
            setCurrentLoopValues(outerValues);
        }
    }

    /**
     * Resolves the name of the list variable the loop goes through. The name can contain placeholders and {@code $$} references,
     * which get resolved with fresh values, because actions before this block can have just changed them, see {@link FreshValuesScope}.
     *
     * @return The name, or an empty text if there is none.
     */
    @NotNull
    public String resolveListVariableName() {
        String name = this.listVariableName;
        if (name.isEmpty()) return name;
        name = ValuePlaceholderHolder.applyValuePlaceholders(name, this.valuePlaceholders);
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            name = PlaceholderParser.replacePlaceholders(name);
        }
        return Variable.normalizeReferenceName(name);
    }

    /**
     * The values of the loop shadow value placeholders with the same names from outside, so these don't get passed on to its contents.
     */
    @Override
    public void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        if (LIST_ENTRY_PLACEHOLDER.equals(placeholder) || LIST_INDEX_PLACEHOLDER.equals(placeholder)) return;
        super.addValuePlaceholder(placeholder, replaceWithSupplier);
    }

    /**
     * @return The name of the list variable as it got typed in, which can contain placeholders.
     */
    @NotNull
    public String getListVariableName() {
        return this.listVariableName;
    }

    public void setListVariableName(@Nullable String listVariableName) {
        this.listVariableName = (listVariableName != null) ? listVariableName.strip() : "";
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * Captures the values of the innermost loop that runs on the thread right now, for code that runs contents of the loop later,
     * like Execute Later blocks, see {@link #runWithLoopValues(LoopValues, Runnable)}.
     *
     * @return The values, or {@code null} if no loop runs.
     */
    @Nullable
    public static LoopValues captureLoopValues() {
        LoopValues values = CURRENT_LOOP_VALUES.get();
        return (values != null) ? values.copy() : null;
    }

    /**
     * Runs the task with the captured values as the values of the innermost loop, and restores the values of the thread afterward.
     *
     * @param values The values of {@link #captureLoopValues()}. With {@code null}, the task runs as if no loop runs.
     */
    public static void runWithLoopValues(@Nullable LoopValues values, @NotNull Runnable task) {
        LoopValues previousValues = CURRENT_LOOP_VALUES.get();
        setCurrentLoopValues(values);
        try {
            task.run();
        } finally {
            setCurrentLoopValues(previousValues);
        }
    }

    private static void setCurrentLoopValues(@Nullable LoopValues values) {
        // Threads of pools outlive the scripts they run, so they don't keep values of finished loops
        if (values != null) {
            CURRENT_LOOP_VALUES.set(values);
        } else {
            CURRENT_LOOP_VALUES.remove();
        }
    }

    @Override
    public @NotNull ForEachExecutableBlock copy(boolean unique) {
        ForEachExecutableBlock b = new ForEachExecutableBlock(this.nanoTimeSupplier, this.listEntriesGetter);
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.listVariableName = this.listVariableName;
        b.collapsed = this.collapsed;
        return b;
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        container.putProperty("[for_each_executable_block_list:" + this.identifier + "]", this.listVariableName);
        container.putProperty("[for_each_executable_block_collapsed:" + this.identifier + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static ForEachExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        ForEachExecutableBlock b = new ForEachExecutableBlock();
        b.identifier = identifier;
        String listKey = "[for_each_executable_block_list:" + identifier + "]";
        String collapsedKey = "[for_each_executable_block_collapsed:" + identifier + "]";
        if (serialized.hasProperty(listKey)) {
            b.setListVariableName(serialized.getValue(listKey));
        }
        if (serialized.hasProperty(collapsedKey)) {
            b.collapsed = Boolean.parseBoolean(serialized.getValue(collapsedKey));
        }
        return b;
    }

    /**
     * The entry and index of a running FOR EACH loop, which the value placeholders of all loops read, see {@link ForEachExecutableBlock}.
     * Only the thread that runs the loop reads and writes them.
     */
    public static final class LoopValues {

        @NotNull
        private String entry = "";
        private int index;
        /** Built when a script reads the index for the first time in an iteration, so loops whose contents don't use it build nothing. */
        @Nullable
        private String indexText;

        private LoopValues() {
        }

        private void moveTo(@NotNull String entry, int index) {
            this.entry = entry;
            this.index = index;
            this.indexText = null;
        }

        @NotNull
        private String getIndexText() {
            String text = this.indexText;
            if (text == null) {
                text = Integer.toString(this.index);
                this.indexText = text;
            }
            return text;
        }

        @NotNull
        private LoopValues copy() {
            LoopValues copy = new LoopValues();
            copy.moveTo(this.entry, this.index);
            return copy;
        }

    }

}
