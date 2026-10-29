package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActiveRaidRemainingWavesPlaceholder extends AbstractActiveRaidPlaceholder {

    public ActiveRaidRemainingWavesPlaceholder() {
        super("active_raid_remaining_waves");
    }

    @Override
    protected @NotNull String getReplacementFor(@Nullable RaidSnapshot activeRaid) {
        return (activeRaid != null) ? String.valueOf(activeRaid.remainingWaves()) : "0";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.active_raid_remaining_waves";
    }

}
