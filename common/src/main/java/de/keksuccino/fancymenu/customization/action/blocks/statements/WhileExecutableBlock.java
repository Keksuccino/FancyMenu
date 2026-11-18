package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class WhileExecutableBlock extends AbstractExecutableBlock {

    private static final Logger LOGGER = LogManager.getLogger();
    static final long TIMEOUT_MILLIS = 3000; // 3 seconds timeout
    private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);

    @NotNull
    public RequirementContainer condition = new RequirementContainer().forceRequirementsMet(true);

    private boolean collapsed = false;

    /** Retained so copy() can preserve an injected clock. */
    @NotNull
    private final LongSupplier nanoTimeSupplier;

    public WhileExecutableBlock() {
        this(System::nanoTime);
    }

    public WhileExecutableBlock(@NotNull RequirementContainer condition) {
        this();
        this.condition = Objects.requireNonNull(condition);
    }

    WhileExecutableBlock(@NotNull LongSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;
    }

    @Override
    public String getBlockType() {
        return "while";
    }

    /**
     * Executes the block contents for as long as the condition is met, but stops after {@link #TIMEOUT_MILLIS},
     * so an endless loop can't freeze the game.<br>
     * The timeout only stops the run that hit it. Keep the start time local to each run, so every run gets the full timeout,
     * and a nested run of this same block can't reset the timer of the run around it. A nested run happens, for example, when the loop
     * is in the script of an On Variable Updated listener and sets a variable, because that runs the same script again right away.
     */
    @Override
    public void execute() {
        long loopStartTime = this.nanoTimeSupplier.getAsLong();
        while (this.check()) {
            if ((this.nanoTimeSupplier.getAsLong() - loopStartTime) >= TIMEOUT_NANOS) {
                LOGGER.warn("[FANCYMENU] WhileExecutableBlock loop timed out after {} milliseconds!", TIMEOUT_MILLIS);
                return;
            }
            super.execute();
        }
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        super.addValuePlaceholder(placeholder, replaceWithSupplier);
        this.condition.addValuePlaceholder(placeholder, replaceWithSupplier);
    }

    @Override
    public @NotNull WhileExecutableBlock copy(boolean unique) {
        WhileExecutableBlock b = new WhileExecutableBlock(this.nanoTimeSupplier);
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.condition = this.condition.copy(unique);
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.collapsed = this.collapsed;
        return b;
    }

    public boolean check() {
        return this.condition.requirementsMet();
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        String key = "[while_executable_block_body:" + this.getIdentifier() + "]";
        container.putProperty(key, this.condition.identifier);
        this.condition.serializeToExistingPropertyContainer(container);
        container.putProperty("[while_executable_block_collapsed:" + this.getIdentifier() + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static WhileExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        WhileExecutableBlock b = new WhileExecutableBlock();
        b.identifier = identifier;
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().equals("[while_executable_block_body:" + identifier + "]")) {
                RequirementContainer lrc = RequirementContainer.deserializeWithIdentifier(m.getValue(), serialized);
                if (lrc != null) {
                    b.condition = lrc;
                }
                break;
            }
        }
        String collapsedKey = "[while_executable_block_collapsed:" + identifier + "]";
        if (serialized.hasProperty(collapsedKey)) {
            b.collapsed = Boolean.parseBoolean(serialized.getValue(collapsedKey));
        }
        return b;
    }

}