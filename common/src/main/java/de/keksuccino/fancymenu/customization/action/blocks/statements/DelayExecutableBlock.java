package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.LongSupplier;

public class DelayExecutableBlock extends AbstractExecutableBlock {

    public static final String DEFAULT_DELAY_MS = "1000";

    @NotNull
    private String delayMs = DEFAULT_DELAY_MS;
    private boolean collapsed = false;

    /** Retained so copy() can preserve an injected clock while deliberately creating fresh, closed gate state. */
    @NotNull
    private final LongSupplier currentTimeMillisSupplier;
    @NotNull
    private final OneTimeDelayGate delayGate;

    public DelayExecutableBlock() {
        this(System::currentTimeMillis);
    }

    public DelayExecutableBlock(@NotNull String delayMs) {
        this();
        this.setDelayMs(delayMs);
    }

    DelayExecutableBlock(@NotNull LongSupplier currentTimeMillisSupplier) {
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
        this.delayGate = new OneTimeDelayGate(currentTimeMillisSupplier);
    }

    @Override
    public String getBlockType() {
        return "delay";
    }

    @Override
    public void execute() {
        long delay = this.resolveDelayMs();
        if (this.delayGate.shouldExecute(delay)) {
            super.execute();
        }
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @NotNull
    public String getDelayMsRaw() {
        return this.delayMs;
    }

    public void setDelayMs(@Nullable String delayMs) {
        String normalized = (delayMs != null) ? delayMs.trim() : "";
        if (normalized.isEmpty()) {
            normalized = DEFAULT_DELAY_MS;
        }
        this.delayMs = normalized;
    }

    public long resolveDelayMs() {
        String resolved = this.resolveDelayString();
        if (resolved == null) {
            return 0L;
        }
        String trimmed = resolved.trim();
        if (!MathUtils.isLong(trimmed)) {
            return 0L;
        }
        long value = Long.parseLong(trimmed);
        return Math.max(0L, value);
    }

    @Nullable
    private String resolveDelayString() {
        String value = this.delayMs;
        if (value == null) {
            return null;
        }
        if (!this.valuePlaceholders.isEmpty()) {
            value = ValuePlaceholderHolder.applyValuePlaceholders(value, this.valuePlaceholders);
        }
        return PlaceholderParser.replacePlaceholders(value);
    }

    @Override
    public @NotNull DelayExecutableBlock copy(boolean unique) {
        DelayExecutableBlock b = new DelayExecutableBlock(this.currentTimeMillisSupplier);
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.delayMs = this.delayMs;
        b.collapsed = this.collapsed;
        return b;
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        container.putProperty("[delay_executable_block_value:" + this.identifier + "]", this.delayMs);
        container.putProperty("[delay_executable_block_collapsed:" + this.identifier + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static DelayExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        DelayExecutableBlock b = new DelayExecutableBlock();
        b.identifier = identifier;
        String valueKey = "[delay_executable_block_value:" + identifier + "]";
        String collapsedKey = "[delay_executable_block_collapsed:" + identifier + "]";
        if (serialized.hasProperty(valueKey)) {
            String storedDelay = serialized.getValue(valueKey);
            if (storedDelay != null && !storedDelay.isEmpty()) {
                b.delayMs = storedDelay;
            }
        }
        if (serialized.hasProperty(collapsedKey)) {
            b.collapsed = Boolean.parseBoolean(serialized.getValue(collapsedKey));
        }
        return b;
    }

}
