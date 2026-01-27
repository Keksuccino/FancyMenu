package de.keksuccino.fancymenu.customization.scheduler;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchedulerInstance {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected String schedulerIdentifier;
    @NotNull
    protected GenericExecutableBlock actionScript;
    @Nullable
    protected String displayName = null;
    protected boolean startOnLaunch = false;
    protected long startDelayMs = 0;
    protected long tickDelayMs = 0;
    protected long ticksToRun = 0;

    public SchedulerInstance(@NotNull String schedulerIdentifier) {
        this.schedulerIdentifier = schedulerIdentifier;
        this.actionScript = new GenericExecutableBlock();
    }

    public @NotNull String getIdentifier() {
        return schedulerIdentifier;
    }

    public void setIdentifier(@NotNull String schedulerIdentifier) {
        this.schedulerIdentifier = schedulerIdentifier;
    }

    public @NotNull GenericExecutableBlock getActionScript() {
        return actionScript;
    }

    public void setActionScript(@NotNull GenericExecutableBlock actionScript) {
        this.actionScript = actionScript;
    }

    public @Nullable String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    public boolean isStartOnLaunch() {
        return startOnLaunch;
    }

    public void setStartOnLaunch(boolean startOnLaunch) {
        this.startOnLaunch = startOnLaunch;
    }

    public long getStartDelayMs() {
        return startDelayMs;
    }

    public void setStartDelayMs(long startDelayMs) {
        this.startDelayMs = startDelayMs;
    }

    public long getTickDelayMs() {
        return tickDelayMs;
    }

    public void setTickDelayMs(long tickDelayMs) {
        this.tickDelayMs = tickDelayMs;
    }

    public long getTicksToRun() {
        return ticksToRun;
    }

    public void setTicksToRun(long ticksToRun) {
        this.ticksToRun = ticksToRun;
    }

    public @NotNull PropertyContainer serialize() {

        PropertyContainer serialized = new PropertyContainer("scheduler_instance");

        serialized.putProperty("scheduler_identifier", this.schedulerIdentifier);
        serialized.putProperty("scheduler_action_script_identifier", this.actionScript.getIdentifier());
        if (this.displayName != null) serialized.putProperty("scheduler_display_name", this.displayName);
        serialized.putProperty("scheduler_start_on_launch", "" + this.startOnLaunch);
        serialized.putProperty("scheduler_start_delay_ms", "" + this.startDelayMs);
        serialized.putProperty("scheduler_tick_delay_ms", "" + this.tickDelayMs);
        serialized.putProperty("scheduler_ticks_to_run", "" + this.ticksToRun);

        this.actionScript.serializeToExistingPropertyContainer(serialized);

        return serialized;

    }

    @Nullable
    public static SchedulerInstance deserialize(@NotNull PropertyContainer serialized) {

        if (!"scheduler_instance".equals(serialized.getType())) {
            LOGGER.error("[FANCYMENU] Failed to deserialize scheduler instance! Provided PropertyContainer does not hold a valid serialized scheduler instance! Wrong type: " + serialized.getType());
            return null;
        }

        String identifier = serialized.getValue("scheduler_identifier");
        if (identifier == null) {
            identifier = SchedulerHandler.generateUniqueIdentifier();
        }

        SchedulerInstance instance = new SchedulerInstance(identifier);

        instance.displayName = serialized.getValue("scheduler_display_name");

        String startOnLaunch = serialized.getValue("scheduler_start_on_launch");
        if ((startOnLaunch != null) && startOnLaunch.equalsIgnoreCase("true")) {
            instance.startOnLaunch = true;
        }

        String startDelay = serialized.getValue("scheduler_start_delay_ms");
        if (startDelay != null) {
            try {
                instance.startDelayMs = Long.parseLong(startDelay);
            } catch (Exception ignored) {
            }
        }

        String tickDelay = serialized.getValue("scheduler_tick_delay_ms");
        if (tickDelay != null) {
            try {
                instance.tickDelayMs = Long.parseLong(tickDelay);
            } catch (Exception ignored) {
            }
        }

        String ticksToRun = serialized.getValue("scheduler_ticks_to_run");
        if (ticksToRun != null) {
            try {
                instance.ticksToRun = Long.parseLong(ticksToRun);
            } catch (Exception ignored) {
            }
        }

        String actionScriptIdentifier = serialized.getValue("scheduler_action_script_identifier");
        if (actionScriptIdentifier == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize scheduler instance! Action script identifier was NULL for scheduler: " + identifier, new NullPointerException("Action script identifier was NULL"));
            return null;
        }

        AbstractExecutableBlock executableBlock = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, actionScriptIdentifier);
        if (executableBlock == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize scheduler instance! Action script failed to get deserialized and was NULL for scheduler: " + identifier, new NullPointerException("Action script was NULL"));
            return null;
        } else if (executableBlock instanceof GenericExecutableBlock g) {
            instance.setActionScript(g);
        } else {
            LOGGER.error("[FANCYMENU] Failed to deserialize scheduler instance! Action script is not a GenericExecutableBlock for scheduler: " + identifier, new ClassCastException("Block is not a GenericExecutableBlock"));
            return null;
        }

        return instance;

    }

}
