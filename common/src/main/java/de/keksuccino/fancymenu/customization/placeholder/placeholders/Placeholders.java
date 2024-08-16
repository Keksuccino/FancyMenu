package de.keksuccino.fancymenu.customization.placeholder.placeholders;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.audio.AudioElementVolumePlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.client.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.gui.*;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.cpu.CpuInfoPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.cpu.JvmCpuUsagePlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.cpu.OsCpuUsagePlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.server.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.MaxRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.PercentRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.UsedRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.player.PlayerNamePlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.player.PlayerUuidPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.world.*;

public class Placeholders {

    public static final MinecraftVersionPlaceholder MINECRAFT_VERSION = new MinecraftVersionPlaceholder();
    public static final ModLoaderVersionPlaceholder MOD_LOADER_VERSION = new ModLoaderVersionPlaceholder();
    public static final ModLoaderNamePlaceholder MOD_LOADER_NAME = new ModLoaderNamePlaceholder();
    public static final ModVersionPlaceholder MOD_VERSION = new ModVersionPlaceholder();
    public static final LoadedModsPlaceholder LOADED_MODS = new LoadedModsPlaceholder();
    public static final TotalModsPlaceholder TOTAL_MODS = new TotalModsPlaceholder();
    public static final WorldLoadProgressPlaceholder WORLD_LOAD_PROGRESS = new WorldLoadProgressPlaceholder();
    public static final MinecraftOptionValuePlaceholder MINECRAFT_OPTION_VALUE = new MinecraftOptionValuePlaceholder();
    public static final ScreenWidthPlaceholder SCREEN_WIDTH = new ScreenWidthPlaceholder();
    public static final ScreenHeightPlaceholder SCREEN_HEIGHT = new ScreenHeightPlaceholder();
    public static final ScreenIdentifierPlaceholder SCREEN_IDENTIFIER = new ScreenIdentifierPlaceholder();
    public static final ElementWidthPlaceholder ELEMENT_WIDTH = new ElementWidthPlaceholder();
    public static final ElementHeightPlaceholder ELEMENT_HEIGHT = new ElementHeightPlaceholder();
    public static final ElementPosXPlaceholder ELEMENT_POS_X = new ElementPosXPlaceholder();
    public static final ElementPosYPlaceholder ELEMENT_POS_Y = new ElementPosYPlaceholder();
    public static final MousePosXPlaceholder MOUSE_POS_X = new MousePosXPlaceholder();
    public static final MousePosYPlaceholder MOUSE_POS_Y = new MousePosYPlaceholder();
    public static final GuiScalePlaceholder GUI_SCALE = new GuiScalePlaceholder();
    public static final VanillaButtonLabelPlaceholder VANILLA_BUTTON_LABEL = new VanillaButtonLabelPlaceholder();
    public static final PlayerNamePlaceholder PLAYER_NAME = new PlayerNamePlaceholder();
    public static final PlayerUuidPlaceholder PLAYER_UUID = new PlayerUuidPlaceholder();
    public static final ServerMotdPlaceholder SERVER_MOTD = new ServerMotdPlaceholder();
    public static final ServerPingPlaceholder SERVER_PING = new ServerPingPlaceholder();
    public static final ServerVersionPlaceholder SERVER_VERSION = new ServerVersionPlaceholder();
    public static final ServerPlayerCountPlaceholder SERVER_PLAYER_COUNT = new ServerPlayerCountPlaceholder();
    public static final ServerStatusPlaceholder SERVER_STATUS = new ServerStatusPlaceholder();
    public static final RealtimeYearPlaceholder REALTIME_YEAR = new RealtimeYearPlaceholder();
    public static final RealtimeMonthPlaceholder REALTIME_MONTH = new RealtimeMonthPlaceholder();
    public static final RealtimeDayPlaceholder REALTIME_DAY = new RealtimeDayPlaceholder();
    public static final RealtimeHourPlaceholder REALTIME_HOUR = new RealtimeHourPlaceholder();
    public static final RealtimeMinutePlaceholder REALTIME_MINUTE = new RealtimeMinutePlaceholder();
    public static final RealtimeSecondPlaceholder REALTIME_SECOND = new RealtimeSecondPlaceholder();
    public static final UnixTimestampPlaceholder UNIX_TIMESTAMP = new UnixTimestampPlaceholder();
    public static final StringifyPlaceholder STRINGIFY = new StringifyPlaceholder();
    public static final JsonPlaceholder JSON = new JsonPlaceholder();
    public static final GetVariablePlaceholder GET_VARIABLE = new GetVariablePlaceholder();
    public static final LocalizationPlaceholder LOCALIZATION = new LocalizationPlaceholder();
    public static final CalculatorPlaceholder CALCULATOR = new CalculatorPlaceholder();
    public static final RandomNumberPlaceholder RANDOM_NUMBER = new RandomNumberPlaceholder();
    public static final MaxNumberPlaceholder MAX_NUMBER = new MaxNumberPlaceholder();
    public static final MinNumberPlaceholder MIN_NUMBER = new MinNumberPlaceholder();
    public static final AbsoluteNumberPlaceholder ABSOLUTE_NUMBER = new AbsoluteNumberPlaceholder();
    public static final NegateNumberPlaceholder NEGATE_NUMBER = new NegateNumberPlaceholder();
    public static final MathPiPlaceholder MATH_PI = new MathPiPlaceholder();
    public static final MathSinPlaceholder MATH_SIN = new MathSinPlaceholder();
    public static final MathSinhPlaceholder MATH_SINH = new MathSinhPlaceholder();
    public static final MathCosPlaceholder MATH_COS = new MathCosPlaceholder();
    public static final MathCoshPlaceholder MATH_COSH = new MathCoshPlaceholder();
    public static final MathTanPlaceholder MATH_TAN = new MathTanPlaceholder();
    public static final MathTanhPlaceholder MATH_TANH = new MathTanhPlaceholder();
    public static final PercentRamPlaceholder PERCENT_RAM = new PercentRamPlaceholder();
    public static final UsedRamPlaceholder USED_RAM = new UsedRamPlaceholder();
    public static final MaxRamPlaceholder MAX_RAM = new MaxRamPlaceholder();
    public static final RandomTextPlaceholder RANDOM_TEXT = new RandomTextPlaceholder();
    public static final WebTextPlaceholder WEB_TEXT = new WebTextPlaceholder();
    public static final AbsolutePathPlaceholder ABSOLUTE_PATH = new AbsolutePathPlaceholder();
    public static final JvmCpuUsagePlaceholder JVM_CPU_USAGE = new JvmCpuUsagePlaceholder();
    public static final OsCpuUsagePlaceholder OS_CPU_USAGE = new OsCpuUsagePlaceholder();
    public static final CpuInfoPlaceholder CPU_INFO = new CpuInfoPlaceholder();
    public static final FpsPlaceholder FPS = new FpsPlaceholder();
    public static final GpuInfoPlaceholder GPU_INFO = new GpuInfoPlaceholder();
    public static final JavaVersionPlaceholder JAVA_VERSION = new JavaVersionPlaceholder();
    public static final JvmNamePlaceholder JVM_NAME = new JvmNamePlaceholder();
    public static final OpenGLVersionPlaceholder OPEN_GL_VERSION = new OpenGLVersionPlaceholder();
    public static final OSNamePlaceholder OS_NAME = new OSNamePlaceholder();
    public static final ActiveHotbarSlotPlaceholder ACTIVE_HOTBAR_SLOT = new ActiveHotbarSlotPlaceholder();
    public static final CurrentPlayerHealthPlaceholder CURRENT_PLAYER_HEALTH = new CurrentPlayerHealthPlaceholder();
    public static final GameTimePlaceholder GAME_TIME = new GameTimePlaceholder();
    public static final SlotItemPlaceholder SLOT_ITEM = new SlotItemPlaceholder();
    public static final WorldDayTimePlaceholder WORLD_DAY_TIME = new WorldDayTimePlaceholder();
    public static final WorldDayTimeHourPlaceholder WORLD_DAY_TIME_HOUR = new WorldDayTimeHourPlaceholder();
    public static final WorldDayTimeMinutePlaceholder WORLD_DAY_TIME_MINUTE = new WorldDayTimeMinutePlaceholder();
    public static final WorldDifficultyPlaceholder WORLD_DIFFICULTY = new WorldDifficultyPlaceholder();
    public static final MaxPlayerHealthPlaceholder MAX_PLAYER_HEALTH = new MaxPlayerHealthPlaceholder();
    public static final CurrentPlayerHealthPercentagePlaceholder CURRENT_PLAYER_HEALTH_PERCENTAGE = new CurrentPlayerHealthPercentagePlaceholder();
    public static final CurrentPlayerAbsorptionHealthPlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH = new CurrentPlayerAbsorptionHealthPlaceholder();
    public static final MaxPlayerAbsorptionHealthPlaceholder MAX_PLAYER_ABSORPTION_HEALTH = new MaxPlayerAbsorptionHealthPlaceholder();
    public static final CurrentPlayerAbsorptionHealthPercentagePlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH_PERCENTAGE = new CurrentPlayerAbsorptionHealthPercentagePlaceholder();
    public static final CurrentPlayerHungerPlaceholder CURRENT_PLAYER_HUNGER = new CurrentPlayerHungerPlaceholder();
    public static final MaxPlayerHungerPlaceholder MAX_PLAYER_HUNGER = new MaxPlayerHungerPlaceholder();
    public static final CurrentPlayerHungerPercentagePlaceholder CURRENT_PLAYER_HUNGER_PERCENTAGE = new CurrentPlayerHungerPercentagePlaceholder();
    public static final CurrentPlayerArmorPlaceholder CURRENT_PLAYER_ARMOR = new CurrentPlayerArmorPlaceholder();
    public static final MaxPlayerArmorPlaceholder MAX_PLAYER_ARMOR = new MaxPlayerArmorPlaceholder();
    public static final CurrentPlayerArmorPercentagePlaceholder CURRENT_PLAYER_ARMOR_PERCENTAGE = new CurrentPlayerArmorPercentagePlaceholder();
    public static final CurrentPlayerExpProgressPlaceholder CURRENT_PLAYER_EXP_PROGRESS = new CurrentPlayerExpProgressPlaceholder();
    public static final CurrentPlayerExperiencePlaceholder CURRENT_PLAYER_EXPERIENCE = new CurrentPlayerExperiencePlaceholder();
    public static final CurrentPlayerLevelPlaceholder CURRENT_PLAYER_LEVEL = new CurrentPlayerLevelPlaceholder();
    public static final CurrentMountHealthPlaceholder CURRENT_MOUNT_HEALTH = new CurrentMountHealthPlaceholder();
    public static final MaxMountHealthPlaceholder MAX_MOUNT_HEALTH = new MaxMountHealthPlaceholder();
    public static final CurrentMountHealthPercentagePlaceholder CURRENT_MOUNT_HEALTH_PERCENTAGE = new CurrentMountHealthPercentagePlaceholder();
    public static final CurrentMountJumpMeterPlaceholder CURRENT_MOUNT_JUMP_METER = new CurrentMountJumpMeterPlaceholder();
    public static final CurrentBossHealthPlaceholder CURRENT_BOSS_HEALTH = new CurrentBossHealthPlaceholder();
    public static final BossNamePlaceholder BOSS_NAME = new BossNamePlaceholder();
    public static final BossCountPlaceholder BOSS_COUNT = new BossCountPlaceholder();
    public static final ActiveEffectsCountPlaceholder ACTIVE_EFFECTS_COUNT = new ActiveEffectsCountPlaceholder();
    public static final ActiveEffectPlaceholder ACTIVE_EFFECT = new ActiveEffectPlaceholder();
    public static final CurrentTitlePlaceholder CURRENT_TITLE = new CurrentTitlePlaceholder();
    public static final PlayerXCoordinatePlaceholder PLAYER_X_COORDINATE = new PlayerXCoordinatePlaceholder();
    public static final PlayerYCoordinatePlaceholder PLAYER_Y_COORDINATE = new PlayerYCoordinatePlaceholder();
    public static final PlayerZCoordinatePlaceholder PLAYER_Z_COORDINATE = new PlayerZCoordinatePlaceholder();
    public static final CurrentServerIpPlaceholder CURRENT_SERVER_IP = new CurrentServerIpPlaceholder();
    public static final PlayerAttackStrengthPercentagePlaceholder PLAYER_ATTACK_STRENGTH_PERCENTAGE = new PlayerAttackStrengthPercentagePlaceholder();
    public static final PlayerGamemodePlaceholder PLAYER_GAMEMODE = new PlayerGamemodePlaceholder();
    public static final PlayerViewDirectionPlaceholder PLAYER_VIEW_DIRECTION = new PlayerViewDirectionPlaceholder();
    //TODO übernehmen
    public static final AudioElementVolumePlaceholder AUDIO_ELEMENT_VOLUME = new AudioElementVolumePlaceholder();

    public static void registerAll() {

        //Client
        PlaceholderRegistry.register(MINECRAFT_VERSION);
        PlaceholderRegistry.register(MOD_LOADER_VERSION);
        PlaceholderRegistry.register(MOD_LOADER_NAME);
        PlaceholderRegistry.register(MOD_VERSION);
        PlaceholderRegistry.register(LOADED_MODS);
        PlaceholderRegistry.register(TOTAL_MODS);
        PlaceholderRegistry.register(WORLD_LOAD_PROGRESS);
        PlaceholderRegistry.register(MINECRAFT_OPTION_VALUE);

        //GUI
        PlaceholderRegistry.register(SCREEN_WIDTH);
        PlaceholderRegistry.register(SCREEN_HEIGHT);
        PlaceholderRegistry.register(SCREEN_IDENTIFIER);
        PlaceholderRegistry.register(ELEMENT_WIDTH);
        PlaceholderRegistry.register(ELEMENT_HEIGHT);
        PlaceholderRegistry.register(ELEMENT_POS_X);
        PlaceholderRegistry.register(ELEMENT_POS_Y);
        PlaceholderRegistry.register(MOUSE_POS_X);
        PlaceholderRegistry.register(MOUSE_POS_Y);
        PlaceholderRegistry.register(GUI_SCALE);
        PlaceholderRegistry.register(VANILLA_BUTTON_LABEL);

        //Player
        PlaceholderRegistry.register(PLAYER_NAME);
        PlaceholderRegistry.register(PLAYER_UUID);

        //TODO übernehmen
        //World
        PlaceholderRegistry.register(ACTIVE_HOTBAR_SLOT);
        PlaceholderRegistry.register(CURRENT_PLAYER_HEALTH);
        PlaceholderRegistry.register(MAX_PLAYER_HEALTH);
        PlaceholderRegistry.register(CURRENT_PLAYER_HEALTH_PERCENTAGE);
        PlaceholderRegistry.register(CURRENT_PLAYER_ABSORPTION_HEALTH);
        PlaceholderRegistry.register(MAX_PLAYER_ABSORPTION_HEALTH);
        PlaceholderRegistry.register(CURRENT_PLAYER_ABSORPTION_HEALTH_PERCENTAGE);
        PlaceholderRegistry.register(CURRENT_PLAYER_HUNGER);
        PlaceholderRegistry.register(MAX_PLAYER_HUNGER);
        PlaceholderRegistry.register(CURRENT_PLAYER_HUNGER_PERCENTAGE);
        PlaceholderRegistry.register(CURRENT_PLAYER_ARMOR);
        PlaceholderRegistry.register(MAX_PLAYER_ARMOR);
        PlaceholderRegistry.register(CURRENT_PLAYER_ARMOR_PERCENTAGE);
        PlaceholderRegistry.register(CURRENT_PLAYER_EXP_PROGRESS);
        PlaceholderRegistry.register(CURRENT_PLAYER_EXPERIENCE);
        PlaceholderRegistry.register(CURRENT_PLAYER_LEVEL);
        PlaceholderRegistry.register(CURRENT_MOUNT_HEALTH);
        PlaceholderRegistry.register(MAX_MOUNT_HEALTH);
        PlaceholderRegistry.register(CURRENT_MOUNT_HEALTH_PERCENTAGE);
        PlaceholderRegistry.register(CURRENT_MOUNT_JUMP_METER);
        PlaceholderRegistry.register(GAME_TIME);
        PlaceholderRegistry.register(SLOT_ITEM);
        PlaceholderRegistry.register(WORLD_DAY_TIME);
        PlaceholderRegistry.register(WORLD_DAY_TIME_HOUR);
        PlaceholderRegistry.register(WORLD_DAY_TIME_MINUTE);
        PlaceholderRegistry.register(WORLD_DIFFICULTY);
        PlaceholderRegistry.register(CURRENT_BOSS_HEALTH);
        PlaceholderRegistry.register(BOSS_NAME);
        PlaceholderRegistry.register(BOSS_COUNT);
        PlaceholderRegistry.register(ACTIVE_EFFECTS_COUNT);
        PlaceholderRegistry.register(ACTIVE_EFFECT);
        PlaceholderRegistry.register(CURRENT_TITLE);
        PlaceholderRegistry.register(PLAYER_X_COORDINATE);
        PlaceholderRegistry.register(PLAYER_Y_COORDINATE);
        PlaceholderRegistry.register(PLAYER_Z_COORDINATE);
        PlaceholderRegistry.register(CURRENT_SERVER_IP);
        PlaceholderRegistry.register(PLAYER_ATTACK_STRENGTH_PERCENTAGE);
        PlaceholderRegistry.register(PLAYER_GAMEMODE);
        PlaceholderRegistry.register(PLAYER_VIEW_DIRECTION);
        //----------------------------

        //Server
        PlaceholderRegistry.register(SERVER_MOTD);
        PlaceholderRegistry.register(SERVER_PING);
        PlaceholderRegistry.register(SERVER_VERSION);
        PlaceholderRegistry.register(SERVER_PLAYER_COUNT);
        PlaceholderRegistry.register(SERVER_STATUS);

        //Realtime
        PlaceholderRegistry.register(REALTIME_YEAR);
        PlaceholderRegistry.register(REALTIME_MONTH);
        PlaceholderRegistry.register(REALTIME_DAY);
        PlaceholderRegistry.register(REALTIME_HOUR);
        PlaceholderRegistry.register(REALTIME_MINUTE);
        PlaceholderRegistry.register(REALTIME_SECOND);
        PlaceholderRegistry.register(UNIX_TIMESTAMP);

        //Advanced
        PlaceholderRegistry.register(STRINGIFY);
        PlaceholderRegistry.register(JSON);
        PlaceholderRegistry.register(GET_VARIABLE);
        PlaceholderRegistry.register(LOCALIZATION);
        PlaceholderRegistry.register(CALCULATOR);
        PlaceholderRegistry.register(RANDOM_NUMBER);
        PlaceholderRegistry.register(MAX_NUMBER);
        PlaceholderRegistry.register(MIN_NUMBER);
        PlaceholderRegistry.register(ABSOLUTE_NUMBER);
        PlaceholderRegistry.register(NEGATE_NUMBER);
        PlaceholderRegistry.register(MATH_PI);
        PlaceholderRegistry.register(MATH_SIN);
        PlaceholderRegistry.register(MATH_SINH);
        PlaceholderRegistry.register(MATH_COS);
        PlaceholderRegistry.register(MATH_COSH);
        PlaceholderRegistry.register(MATH_TAN);
        PlaceholderRegistry.register(MATH_TANH);

        //TODO übernehmen
        //Audio
        PlaceholderRegistry.register(AUDIO_ELEMENT_VOLUME);
        //----------------------

        //Other
        PlaceholderRegistry.register(PERCENT_RAM);
        PlaceholderRegistry.register(USED_RAM);
        PlaceholderRegistry.register(MAX_RAM);
        PlaceholderRegistry.register(RANDOM_TEXT);
        PlaceholderRegistry.register(WEB_TEXT);
        PlaceholderRegistry.register(ABSOLUTE_PATH);
        PlaceholderRegistry.register(JVM_CPU_USAGE);
        PlaceholderRegistry.register(OS_CPU_USAGE);
        PlaceholderRegistry.register(CPU_INFO);
        PlaceholderRegistry.register(GPU_INFO);
        PlaceholderRegistry.register(FPS);
        PlaceholderRegistry.register(JAVA_VERSION);
        PlaceholderRegistry.register(JVM_NAME);
        PlaceholderRegistry.register(OPEN_GL_VERSION);
        PlaceholderRegistry.register(OS_NAME);

    }

}
