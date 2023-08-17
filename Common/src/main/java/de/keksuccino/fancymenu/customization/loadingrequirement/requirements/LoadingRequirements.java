package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.realtime.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.FileExistsRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsLinuxRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsMacOSRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.IsOsWindowsRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.window.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsWorldLoadedRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsAdventureRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsCreativeRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSpectatorRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSurvivalRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsMultiplayerRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsSingleplayerRequirement;

public class LoadingRequirements {

    public static final IsElementHoveredRequirement IS_ELEMENT_HOVERED = new IsElementHoveredRequirement();
    public static final IsAnyElementHoveredRequirement IS_ANY_ELEMENT_HOVERED = new IsAnyElementHoveredRequirement();
    public static final IsAnyButtonHoveredRequirement IS_ANY_BUTTON_HOVERED = new IsAnyButtonHoveredRequirement();
    public static final IsLayoutEnabledRequirement IS_LAYOUT_ENABLED = new IsLayoutEnabledRequirement();
    public static final IsGuiScaleRequirement IS_GUI_SCALE = new IsGuiScaleRequirement();
    public static final IsButtonActiveRequirement IS_BUTTON_ACTIVE = new IsButtonActiveRequirement();
    public static final IsRealTimeDayRequirement IS_REAL_TIME_DAY = new IsRealTimeDayRequirement();
    public static final IsRealTimeHourRequirement IS_REAL_TIME_HOUR = new IsRealTimeHourRequirement();
    public static final IsRealTimeMinuteRequirement IS_REAL_TIME_MINUTE = new IsRealTimeMinuteRequirement();
    public static final IsRealTimeMonthRequirement IS_REAL_TIME_MONTH = new IsRealTimeMonthRequirement();
    public static final IsRealTimeSecondRequirement IS_REAL_TIME_SECOND = new IsRealTimeSecondRequirement();
    public static final IsRealTimeWeekDayRequirement IS_REAL_TIME_WEEK_DAY = new IsRealTimeWeekDayRequirement();
    public static final IsRealTimeYearRequirement IS_REAL_TIME_YEAR = new IsRealTimeYearRequirement();
    public static final FileExistsRequirement FILE_EXISTS = new FileExistsRequirement();
    public static final IsOsLinuxRequirement IS_OS_LINUX = new IsOsLinuxRequirement();
    public static final IsOsMacOSRequirement IS_OS_MAC_OS = new IsOsMacOSRequirement();
    public static final IsOsWindowsRequirement IS_OS_WINDOWS = new IsOsWindowsRequirement();
    public static final IsFullscreenRequirement IS_FULLSCREEN = new IsFullscreenRequirement();
    public static final IsWindowWidthRequirement IS_WINDOW_WIDTH = new IsWindowWidthRequirement();
    public static final IsWindowHeightRequirement IS_WINDOW_HEIGHT = new IsWindowHeightRequirement();
    public static final IsWindowWidthBiggerThanRequirement IS_WINDOW_WIDTH_BIGGER_THAN = new IsWindowWidthBiggerThanRequirement();
    public static final IsWindowHeightBiggerThanRequirement IS_WINDOW_HEIGHT_BIGGER_THAN = new IsWindowHeightBiggerThanRequirement();
    public static final IsAdventureRequirement IS_ADVENTURE = new IsAdventureRequirement();
    public static final IsCreativeRequirement IS_CREATIVE = new IsCreativeRequirement();
    public static final IsSpectatorRequirement IS_SPECTATOR = new IsSpectatorRequirement();
    public static final IsSurvivalRequirement IS_SURVIVAL = new IsSurvivalRequirement();
    public static final IsMultiplayerRequirement IS_MULTIPLAYER = new IsMultiplayerRequirement();
    public static final IsSingleplayerRequirement IS_SINGLEPLAYER = new IsSingleplayerRequirement();
    public static final IsWorldLoadedRequirement IS_WORLD_LOADED = new IsWorldLoadedRequirement();
    public static final IsLanguageRequirement IS_LANGUAGE = new IsLanguageRequirement();
    public static final IsModLoadedRequirement IS_MOD_LOADED = new IsModLoadedRequirement();
    public static final IsNumberRequirement IS_NUMBER = new IsNumberRequirement();
    public static final IsTextRequirement IS_TEXT = new IsTextRequirement();
    public static final IsServerIpRequirement IS_SERVER_IP = new IsServerIpRequirement();
    public static final IsServerOnlineRequirement IS_SERVER_ONLINE = new IsServerOnlineRequirement();
    public static final IsVariableValueRequirement IS_VARIABLE_VALUE = new IsVariableValueRequirement();

    public static void registerAll() {

        LoadingRequirementRegistry.register(IS_ELEMENT_HOVERED);
        LoadingRequirementRegistry.register(IS_ANY_ELEMENT_HOVERED);
        LoadingRequirementRegistry.register(IS_ANY_BUTTON_HOVERED);
        LoadingRequirementRegistry.register(IS_LAYOUT_ENABLED);
        LoadingRequirementRegistry.register(IS_GUI_SCALE);
        LoadingRequirementRegistry.register(IS_BUTTON_ACTIVE);

        LoadingRequirementRegistry.register(IS_REAL_TIME_DAY);
        LoadingRequirementRegistry.register(IS_REAL_TIME_HOUR);
        LoadingRequirementRegistry.register(IS_REAL_TIME_MINUTE);
        LoadingRequirementRegistry.register(IS_REAL_TIME_MONTH);
        LoadingRequirementRegistry.register(IS_REAL_TIME_SECOND);
        LoadingRequirementRegistry.register(IS_REAL_TIME_WEEK_DAY);
        LoadingRequirementRegistry.register(IS_REAL_TIME_YEAR);

        LoadingRequirementRegistry.register(FILE_EXISTS);
        LoadingRequirementRegistry.register(IS_OS_LINUX);
        LoadingRequirementRegistry.register(IS_OS_MAC_OS);
        LoadingRequirementRegistry.register(IS_OS_WINDOWS);

        LoadingRequirementRegistry.register(IS_FULLSCREEN);
        LoadingRequirementRegistry.register(IS_WINDOW_WIDTH);
        LoadingRequirementRegistry.register(IS_WINDOW_HEIGHT);
        LoadingRequirementRegistry.register(IS_WINDOW_WIDTH_BIGGER_THAN);
        LoadingRequirementRegistry.register(IS_WINDOW_HEIGHT_BIGGER_THAN);

        LoadingRequirementRegistry.register(IS_ADVENTURE);
        LoadingRequirementRegistry.register(IS_CREATIVE);
        LoadingRequirementRegistry.register(IS_SPECTATOR);
        LoadingRequirementRegistry.register(IS_SURVIVAL);

        LoadingRequirementRegistry.register(IS_MULTIPLAYER);
        LoadingRequirementRegistry.register(IS_SINGLEPLAYER);
        LoadingRequirementRegistry.register(IS_WORLD_LOADED);

        LoadingRequirementRegistry.register(IS_LANGUAGE);
        LoadingRequirementRegistry.register(IS_MOD_LOADED);
        LoadingRequirementRegistry.register(IS_NUMBER);
        LoadingRequirementRegistry.register(IS_TEXT);
        LoadingRequirementRegistry.register(IS_SERVER_IP);
        LoadingRequirementRegistry.register(IS_SERVER_ONLINE);
        LoadingRequirementRegistry.register(IS_VARIABLE_VALUE);

    }

}
