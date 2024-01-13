
package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.gui.IsElementHoveredRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.gui.IsGuiScaleRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.gui.IsLayoutEnabledRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.realtime.*;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.system.IsOsLinuxRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.system.IsOsMacOSRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.system.IsOsWindowsRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.window.*;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.IsMultiplayerRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.IsSingleplayerRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.IsWorldLoadedRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode.IsAdventureRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode.IsCreativeRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode.IsSpectatorRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode.IsSurvivalRequirement;

public class LoadingRequirements {

    public static void registerAll() {

        LoadingRequirementRegistry.registerRequirement(new IsElementHoveredRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsLayoutEnabledRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsGuiScaleRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsRealTimeDayRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeHourRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeMinuteRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeMonthRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeSecondRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeWeekDayRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsRealTimeYearRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsOsLinuxRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsOsMacOSRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsOsWindowsRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsFullscreenRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsWindowWidthRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsWindowHeightRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsWindowWidthBiggerThanRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsWindowHeightBiggerThanRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsAdventureRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsCreativeRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsSpectatorRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsSurvivalRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsMultiplayerRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsSingleplayerRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsWorldLoadedRequirement());

        LoadingRequirementRegistry.registerRequirement(new IsLanguageRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsModLoadedRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsNumberRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsTextRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsServerIpRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsServerOnlineRequirement());
        LoadingRequirementRegistry.registerRequirement(new IsVariableValueRequirement());

    }

}
