//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirementRegistry;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode.IsAdventureVisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode.IsCreativeVisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode.IsSpectatorVisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode.IsSurvivalVisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.realtime.*;

public class VisibilityRequirements {

    public static void registerAll() {

        VisibilityRequirementRegistry.registerRequirement(new IsVariableValueVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsServerIpVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsLayoutEnabledVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsSurvivalVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsCreativeVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsAdventureVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsSpectatorVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeHourVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeMinuteVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeSecondVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeDayVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeWeekDayVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeMonthVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsRealTimeYearVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsTextVisibilityRequirement());
        VisibilityRequirementRegistry.registerRequirement(new IsNumberVisibilityRequirement());

        VisibilityRequirementRegistry.registerRequirement(new IsElementHoveredVisibilityRequirement());

    }

}