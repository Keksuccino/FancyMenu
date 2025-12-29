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
import java.util.function.Supplier;

public class WhileExecutableBlock extends AbstractExecutableBlock {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long TIMEOUT_MILLIS = 3000; // 3 seconds timeout

    @NotNull
    public RequirementContainer condition = new RequirementContainer().forceRequirementsMet(true);

    private long loopStartTime = 0;
    private boolean hasTimedOut = false;
    private boolean collapsed = false;

    public WhileExecutableBlock() {
    }

    public WhileExecutableBlock(@NotNull RequirementContainer condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    @Override
    public String getBlockType() {
        return "while";
    }

    @Override
    public void execute() {

        // If we previously timed out, prevent execution
        if (this.hasTimedOut) {
            LOGGER.warn("[FANCYMENU] WhileExecutableBlock execution prevented - still in timeout state from previous timeout");
            return;
        }

        // Initialize loop start time
        this.loopStartTime = System.currentTimeMillis();

        // Keep executing block contents while condition is met and timeout hasn't occurred
        while (this.check() && !this.checkTimeout()) {
            super.execute();
        }

        // If the loop ended due to timeout
        if (this.checkTimeout()) {
            this.hasTimedOut = true;
            LOGGER.warn("[FANCYMENU] WhileExecutableBlock loop timed out after {} milliseconds!", TIMEOUT_MILLIS);
        } else {
            // Loop completed successfully - reset timeout state
            this.hasTimedOut = false;
        }

        // Reset loop start time
        this.loopStartTime = 0;

    }

    private boolean checkTimeout() {
        if (this.loopStartTime == 0) {
            return false;
        }
        return System.currentTimeMillis() - this.loopStartTime >= TIMEOUT_MILLIS;
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
        WhileExecutableBlock b = new WhileExecutableBlock();
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