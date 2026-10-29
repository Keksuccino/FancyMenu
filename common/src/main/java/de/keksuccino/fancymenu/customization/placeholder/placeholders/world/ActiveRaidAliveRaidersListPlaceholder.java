package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActiveRaidAliveRaidersListPlaceholder extends AbstractActiveRaidPlaceholder {

    public ActiveRaidAliveRaidersListPlaceholder() {
        super("active_raid_alive_raiders_list");
    }

    @Override
    protected @NotNull String getReplacementFor(@Nullable RaidSnapshot activeRaid) {
        return (activeRaid != null) ? activeRaid.joinRaiders() : "";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.active_raid_alive_raiders_list";
    }

}
