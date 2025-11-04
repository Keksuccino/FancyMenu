package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.realtime.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.window.*;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsWorldLoadedRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsAdventureRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsCreativeRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSpectatorRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode.IsSurvivalRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsMultiplayerRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.IsSingleplayerRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player.*;

public class LoadingRequirements {

    public static final IsElementHoveredRequirement IS_ELEMENT_HOVERED = new IsElementHoveredRequirement();
    public static final IsElementFocusedRequirement IS_ELEMENT_FOCUSED = new IsElementFocusedRequirement();
    public static final IsAnyElementHoveredRequirement IS_ANY_ELEMENT_HOVERED = new IsAnyElementHoveredRequirement();
    public static final IsAnyButtonHoveredRequirement IS_ANY_BUTTON_HOVERED = new IsAnyButtonHoveredRequirement();
    public static final IsLayoutEnabledRequirement IS_LAYOUT_ENABLED = new IsLayoutEnabledRequirement();
    public static final IsGuiScaleRequirement IS_GUI_SCALE = new IsGuiScaleRequirement();
    public static final IsButtonActiveRequirement IS_BUTTON_ACTIVE = new IsButtonActiveRequirement();
    public static final IsMenuTitleRequirement IS_MENU_TITLE = new IsMenuTitleRequirement();
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
    public static final IsResourcePackEnabledRequirement IS_RESOURCE_PACK_ENABLED = new IsResourcePackEnabledRequirement();
    public static final HasPlayerPermissionLevelRequirement HAS_PLAYER_PERMISSION_LEVEL = new HasPlayerPermissionLevelRequirement();
    public static final IsVariableValueRequirement IS_VARIABLE_VALUE = new IsVariableValueRequirement();
    public static final IsPlayerRunningRequirement IS_PLAYER_RUNNING = new IsPlayerRunningRequirement();
    public static final IsPlayerSneakingRequirement IS_PLAYER_SNEAKING = new IsPlayerSneakingRequirement();
    public static final IsPlayerSwimmingRequirement IS_PLAYER_SWIMMING = new IsPlayerSwimmingRequirement();
    public static final IsPlayerJumpingRequirement IS_PLAYER_JUMPING = new IsPlayerJumpingRequirement();
    public static final IsPlayerUnderWaterRequirement IS_PLAYER_UNDER_WATER = new IsPlayerUnderWaterRequirement();
    public static final IsPlayerInWaterRequirement IS_PLAYER_IN_WATER = new IsPlayerInWaterRequirement();
    public static final IsPlayerInLavaRequirement IS_PLAYER_IN_LAVA = new IsPlayerInLavaRequirement();
    public static final IsPlayerInFluidRequirement IS_PLAYER_IN_FLUID = new IsPlayerInFluidRequirement();
    public static final IsPlayerRidingEntityRequirement IS_PLAYER_RIDING_ENTITY = new IsPlayerRidingEntityRequirement();
    public static final IsPlayerRidingJumpableEntityRequirement IS_PLAYER_RIDING_JUMPABLE_ENTITY = new IsPlayerRidingJumpableEntityRequirement();
    public static final IsPlayerRidingEntityWithHealthRequirement IS_PLAYER_RIDING_ENTITY_WITH_HEALTH = new IsPlayerRidingEntityWithHealthRequirement();
    public static final IsPlayerInPowderSnowRequirement IS_PLAYER_IN_POWDER_SNOW = new IsPlayerInPowderSnowRequirement();
    public static final WasPlayerInPowderSnowRequirement WAS_PLAYER_IN_POWDER_SNOW = new WasPlayerInPowderSnowRequirement();
    public static final IsPlayerWearingPumpkinRequirement IS_PLAYER_WEARING_PUMPKIN = new IsPlayerWearingPumpkinRequirement();
    public static final IsPlayerFlyingWithElytraRequirement IS_PLAYER_FLYING_WITH_ELYTRA = new IsPlayerFlyingWithElytraRequirement();
    public static final IsPlayerCreativeFlyingRequirement IS_PLAYER_CREATIVE_FLYING = new IsPlayerCreativeFlyingRequirement();
    public static final HasPlayerAbsorptionHeartsRequirement HAS_PLAYER_ABSORPTION_HEARTS = new HasPlayerAbsorptionHeartsRequirement();
    public static final IsPlayerWitheredRequirement IS_PLAYER_WITHERED = new IsPlayerWitheredRequirement();
    public static final IsPlayerFullyFrozenRequirement IS_PLAYER_FULLY_FROZEN = new IsPlayerFullyFrozenRequirement();
    public static final IsPlayerPoisonedRequirement IS_PLAYER_POISONED = new IsPlayerPoisonedRequirement();
    public static final IsPlayerInBiomeRequirement IS_PLAYER_IN_BIOME = new IsPlayerInBiomeRequirement();
    public static final IsPlayerInDimensionRequirement IS_PLAYER_IN_DIMENSION = new IsPlayerInDimensionRequirement();
    public static final IsEntityNearbyRequirement IS_ENTITY_NEARBY = new IsEntityNearbyRequirement();
    public static final IsEffectActiveRequirement IS_EFFECT_ACTIVE = new IsEffectActiveRequirement();
    public static final IsAnyEffectActiveRequirement IS_ANY_EFFECT_ACTIVE = new IsAnyEffectActiveRequirement();
    public static final IsGameModeRequirement IS_GAME_MODE = new IsGameModeRequirement();
    public static final IsDifficultyRequirement IS_DIFFICULTY = new IsDifficultyRequirement();
    public static final IsRainingRequirement IS_RAINING = new IsRainingRequirement();
    public static final IsThunderingRequirement IS_THUNDERING = new IsThunderingRequirement();
    public static final IsClearWeatherRequirement IS_CLEAR_WEATHER = new IsClearWeatherRequirement();
    public static final IsSnowingRequirement IS_SNOWING = new IsSnowingRequirement();
    public static final IsPlayerLeftHandedRequirement IS_PLAYER_LEFT_HANDED = new IsPlayerLeftHandedRequirement();
    public static final IsInventorySlotFilledRequirement IS_INVENTORY_SLOT_FILLED = new IsInventorySlotFilledRequirement();
    public static final IsHotbarSlotActiveRequirement IS_HOTBAR_SLOT_ACTIVE = new IsHotbarSlotActiveRequirement();
    public static final IsAttackStrengthWeakenedRequirement IS_ATTACK_STRENGTH_WEAKENED = new IsAttackStrengthWeakenedRequirement();
    public static final IsKeyPressedRequirement IS_KEY_PRESSED = new IsKeyPressedRequirement();
    public static final OncePerSessionRequirement ONLY_ONCE_PER_SESSION = new OncePerSessionRequirement();
    public static final MouseClickedRequirement MOUSE_CLICKED = new MouseClickedRequirement();
    public static final IsInternetConnectionAvailableRequirement IS_INTERNET_CONNECTION_AVAILABLE = new IsInternetConnectionAvailableRequirement();
    public static final IsAnyScreenOpenRequirement IS_ANY_SCREEN_OPEN = new IsAnyScreenOpenRequirement();

    public static void registerAll() {

        LoadingRequirementRegistry.register(IS_ELEMENT_HOVERED);
        LoadingRequirementRegistry.register(IS_ELEMENT_FOCUSED);
        LoadingRequirementRegistry.register(IS_ANY_ELEMENT_HOVERED);
        LoadingRequirementRegistry.register(IS_ANY_BUTTON_HOVERED);
        LoadingRequirementRegistry.register(IS_LAYOUT_ENABLED);
        LoadingRequirementRegistry.register(IS_GUI_SCALE);
        LoadingRequirementRegistry.register(IS_BUTTON_ACTIVE);
        LoadingRequirementRegistry.register(IS_MENU_TITLE);
        LoadingRequirementRegistry.register(IS_KEY_PRESSED);
        LoadingRequirementRegistry.register(IS_ANY_SCREEN_OPEN);

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
        LoadingRequirementRegistry.register(IS_INTERNET_CONNECTION_AVAILABLE);

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

        LoadingRequirementRegistry.register(IS_PLAYER_RUNNING);
        LoadingRequirementRegistry.register(IS_PLAYER_SNEAKING);
        LoadingRequirementRegistry.register(IS_PLAYER_SWIMMING);
        LoadingRequirementRegistry.register(IS_PLAYER_JUMPING);
        LoadingRequirementRegistry.register(IS_PLAYER_UNDER_WATER);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_WATER);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_LAVA);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_FLUID);
        LoadingRequirementRegistry.register(IS_PLAYER_RIDING_ENTITY);
        LoadingRequirementRegistry.register(IS_PLAYER_RIDING_JUMPABLE_ENTITY);
        LoadingRequirementRegistry.register(IS_PLAYER_RIDING_ENTITY_WITH_HEALTH);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_POWDER_SNOW);
        LoadingRequirementRegistry.register(WAS_PLAYER_IN_POWDER_SNOW);
        LoadingRequirementRegistry.register(IS_PLAYER_WEARING_PUMPKIN);
        LoadingRequirementRegistry.register(IS_PLAYER_FLYING_WITH_ELYTRA);
        LoadingRequirementRegistry.register(IS_PLAYER_CREATIVE_FLYING);
        LoadingRequirementRegistry.register(HAS_PLAYER_ABSORPTION_HEARTS);
        LoadingRequirementRegistry.register(IS_PLAYER_WITHERED);
        LoadingRequirementRegistry.register(IS_PLAYER_FULLY_FROZEN);
        LoadingRequirementRegistry.register(IS_PLAYER_POISONED);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_BIOME);
        LoadingRequirementRegistry.register(IS_PLAYER_IN_DIMENSION);
        LoadingRequirementRegistry.register(IS_ENTITY_NEARBY);
        LoadingRequirementRegistry.register(IS_EFFECT_ACTIVE);
        LoadingRequirementRegistry.register(IS_ANY_EFFECT_ACTIVE);
        LoadingRequirementRegistry.register(IS_GAME_MODE);
        LoadingRequirementRegistry.register(IS_DIFFICULTY);
        LoadingRequirementRegistry.register(IS_RAINING);
        LoadingRequirementRegistry.register(IS_THUNDERING);
        LoadingRequirementRegistry.register(IS_CLEAR_WEATHER);
        LoadingRequirementRegistry.register(IS_SNOWING);
        LoadingRequirementRegistry.register(HAS_PLAYER_PERMISSION_LEVEL);
        LoadingRequirementRegistry.register(IS_PLAYER_LEFT_HANDED);
        LoadingRequirementRegistry.register(IS_INVENTORY_SLOT_FILLED);
        LoadingRequirementRegistry.register(IS_HOTBAR_SLOT_ACTIVE);
        LoadingRequirementRegistry.register(IS_ATTACK_STRENGTH_WEAKENED);

        LoadingRequirementRegistry.register(IS_LANGUAGE);
        LoadingRequirementRegistry.register(IS_MOD_LOADED);
        LoadingRequirementRegistry.register(IS_NUMBER);
        LoadingRequirementRegistry.register(IS_TEXT);
        LoadingRequirementRegistry.register(IS_SERVER_IP);
        LoadingRequirementRegistry.register(IS_SERVER_ONLINE);
        LoadingRequirementRegistry.register(IS_RESOURCE_PACK_ENABLED);
        LoadingRequirementRegistry.register(IS_VARIABLE_VALUE);
        LoadingRequirementRegistry.register(ONLY_ONCE_PER_SESSION);
        LoadingRequirementRegistry.register(MOUSE_CLICKED);

    }

}





