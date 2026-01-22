package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.math.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ExecuteLaterExecutableBlock extends AbstractExecutableBlock {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String DEFAULT_DELAY_MS = "1000";

    @NotNull
    private String delayMs = DEFAULT_DELAY_MS;
    private boolean collapsed = false;

    public ExecuteLaterExecutableBlock() {
    }

    public ExecuteLaterExecutableBlock(@NotNull String delayMs) {
        this.setDelayMs(delayMs);
    }

    @Override
    public String getBlockType() {
        return "execute-later";
    }

    @Override
    public void execute() {
        long delay = this.resolveDelayMsOrFallback();
        ExecuteLaterExecutableBlock scheduledCopy = this.copy(true);
        TaskExecutor.schedule(future ->
                MainThreadTaskExecutor.executeInMainThread(scheduledCopy::executeScheduled, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK),
                delay, TimeUnit.MILLISECONDS, false);
    }

    private void executeScheduled() {
        super.execute();
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

    public long resolveDelayMsOrFallback() {
        String resolved = this.resolveDelayString();
        if (resolved == null) {
            this.logInvalidDelay(null);
            return Long.parseLong(DEFAULT_DELAY_MS);
        }
        String trimmed = resolved.trim();
        if (!MathUtils.isLong(trimmed)) {
            this.logInvalidDelay(resolved);
            return Long.parseLong(DEFAULT_DELAY_MS);
        }
        long value = Long.parseLong(trimmed);
        if (value < 0L) {
            this.logInvalidDelay(resolved);
            return Long.parseLong(DEFAULT_DELAY_MS);
        }
        return value;
    }

    private void logInvalidDelay(@Nullable String resolved) {
        LOGGER.error("[FANCYMENU] Invalid Execute Later delay value! Using fallback of {} ms. Raw: '{}' Resolved: '{}'", DEFAULT_DELAY_MS, this.delayMs, resolved);
    }

    @Nullable
    private String resolveDelayString() {
        String value = this.delayMs;
        if (value == null) {
            return null;
        }
        if (!this.valuePlaceholders.isEmpty()) {
            for (Map.Entry<String, Supplier<String>> entry : this.valuePlaceholders.entrySet()) {
                String replaceWith = entry.getValue().get();
                if (replaceWith == null) {
                    replaceWith = "";
                }
                value = value.replace(ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + entry.getKey(), replaceWith);
            }
        }
        return PlaceholderParser.replacePlaceholders(value);
    }

    @Override
    public @NotNull ExecuteLaterExecutableBlock copy(boolean unique) {
        ExecuteLaterExecutableBlock b = new ExecuteLaterExecutableBlock();
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
        container.putProperty("[execute_later_executable_block_value:" + this.identifier + "]", this.delayMs);
        container.putProperty("[execute_later_executable_block_collapsed:" + this.identifier + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static ExecuteLaterExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        ExecuteLaterExecutableBlock b = new ExecuteLaterExecutableBlock();
        b.identifier = identifier;
        String valueKey = "[execute_later_executable_block_value:" + identifier + "]";
        String collapsedKey = "[execute_later_executable_block_collapsed:" + identifier + "]";
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
