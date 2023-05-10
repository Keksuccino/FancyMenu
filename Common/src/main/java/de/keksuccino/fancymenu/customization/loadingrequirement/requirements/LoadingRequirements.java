
package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui.IsElementHoveredRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui.IsLayoutEnabledRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.realtime.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsLinuxRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsMacOSRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsWindowsRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.window.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsWorldLoadedRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsAdventureRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsCreativeRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSpectatorRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSurvivalRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui.IsGuiScaleRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsMultiplayerRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsSingleplayerRequirement;
import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.requirements.realtime.*;
import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.requirements.window.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.requirements.realtime.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.requirements.window.*;

public class LoadingRequirements {

    public static void registerAll() {

        LoadingRequirementRegistry.register(new IsElementHoveredRequirement());
        LoadingRequirementRegistry.register(new IsLayoutEnabledRequirement());
        LoadingRequirementRegistry.register(new IsGuiScaleRequirement());

        LoadingRequirementRegistry.register(new IsRealTimeDayRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeHourRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeMinuteRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeMonthRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeSecondRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeWeekDayRequirement());
        LoadingRequirementRegistry.register(new IsRealTimeYearRequirement());

        LoadingRequirementRegistry.register(new IsOsLinuxRequirement());
        LoadingRequirementRegistry.register(new IsOsMacOSRequirement());
        LoadingRequirementRegistry.register(new IsOsWindowsRequirement());

        LoadingRequirementRegistry.register(new IsFullscreenRequirement());
        LoadingRequirementRegistry.register(new IsWindowWidthRequirement());
        LoadingRequirementRegistry.register(new IsWindowHeightRequirement());
        LoadingRequirementRegistry.register(new IsWindowWidthBiggerThanRequirement());
        LoadingRequirementRegistry.register(new IsWindowHeightBiggerThanRequirement());

        LoadingRequirementRegistry.register(new IsAdventureRequirement());
        LoadingRequirementRegistry.register(new IsCreativeRequirement());
        LoadingRequirementRegistry.register(new IsSpectatorRequirement());
        LoadingRequirementRegistry.register(new IsSurvivalRequirement());

        LoadingRequirementRegistry.register(new IsMultiplayerRequirement());
        LoadingRequirementRegistry.register(new IsSingleplayerRequirement());
        LoadingRequirementRegistry.register(new IsWorldLoadedRequirement());

        LoadingRequirementRegistry.register(new IsLanguageRequirement());
        LoadingRequirementRegistry.register(new IsModLoadedRequirement());
        LoadingRequirementRegistry.register(new IsNumberRequirement());
        LoadingRequirementRegistry.register(new IsTextRequirement());
        LoadingRequirementRegistry.register(new IsServerIpRequirement());
        LoadingRequirementRegistry.register(new IsServerOnlineRequirement());
        LoadingRequirementRegistry.register(new IsVariableValueRequirement());

    }

}
