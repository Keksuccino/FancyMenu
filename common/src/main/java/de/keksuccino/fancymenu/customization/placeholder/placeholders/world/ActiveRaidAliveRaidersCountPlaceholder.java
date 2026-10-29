package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActiveRaidAliveRaidersCountPlaceholder extends AbstractActiveRaidPlaceholder {

    public ActiveRaidAliveRaidersCountPlaceholder() {
        super("active_raid_alive_raiders_count");
    }

    @Override
    protected @NotNull String getReplacementFor(@Nullable RaidSnapshot activeRaid) {
        return (activeRaid != null) ? String.valueOf(activeRaid.aliveRaidersCount()) : "0";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.active_raid_alive_raiders_count";
    }

}
