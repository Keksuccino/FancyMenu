package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.util.level.raid.ActiveRaidClientState;
import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base for placeholders that return information about the raid whose raid bar the player currently sees.
 */
public abstract class AbstractActiveRaidPlaceholder extends AbstractWorldPlaceholder {

    public AbstractActiveRaidPlaceholder(@NotNull String identifier) {
        super(identifier);
    }

    /**
     * @param activeRaid The active raid, or NULL if the player is not in the area of a raid.
     */
    @NotNull
    protected abstract String getReplacementFor(@Nullable RaidSnapshot activeRaid);

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return this.getReplacementFor(ActiveRaidClientState.INSTANCE.getActiveRaid());
    }

    @Override
    public boolean canRunAsync() {
        // The raid state is an immutable snapshot, so it's safe to read from any thread
        return true;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
