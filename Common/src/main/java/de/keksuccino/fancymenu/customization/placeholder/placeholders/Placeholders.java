package de.keksuccino.fancymenu.customization.placeholder.placeholders;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.gui.*;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.AbsolutePathPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.server.*;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.client.LoadedModsPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.client.MinecraftVersionPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.client.ModVersionPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.client.TotalModsPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.RandomTextPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.WebTextPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.MaxRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.PercentRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.ram.UsedRamPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.player.PlayerNamePlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.player.PlayerUuidPlaceholder;

public class Placeholders {

    public static final MinecraftVersionPlaceholder MINECRAFT_VERSION = new MinecraftVersionPlaceholder();
    public static final ModVersionPlaceholder MOD_VERSION = new ModVersionPlaceholder();
    public static final LoadedModsPlaceholder LOADED_MODS = new LoadedModsPlaceholder();
    public static final TotalModsPlaceholder TOTAL_MODS = new TotalModsPlaceholder();
    public static final ScreenWidthPlaceholder SCREEN_WIDTH = new ScreenWidthPlaceholder();
    public static final ScreenHeightPlaceholder SCREEN_HEIGHT = new ScreenHeightPlaceholder();
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
    public static final StringifyPlaceholder STRINGIFY = new StringifyPlaceholder();
    public static final JsonPlaceholder JSON = new JsonPlaceholder();
    public static final GetVariablePlaceholder GET_VARIABLE = new GetVariablePlaceholder();
    public static final LocalizationPlaceholder LOCALIZATION = new LocalizationPlaceholder();
    public static final CalculatorPlaceholder CALCULATOR = new CalculatorPlaceholder();
    public static final RandomNumberPlaceholder RANDOM_NUMBER = new RandomNumberPlaceholder();
    public static final PercentRamPlaceholder PERCENT_RAM = new PercentRamPlaceholder();
    public static final UsedRamPlaceholder USED_RAM = new UsedRamPlaceholder();
    public static final MaxRamPlaceholder MAX_RAM = new MaxRamPlaceholder();
    public static final RandomTextPlaceholder RANDOM_TEXT = new RandomTextPlaceholder();
    public static final WebTextPlaceholder WEB_TEXT = new WebTextPlaceholder();
    public static final AbsolutePathPlaceholder ABSOLUTE_PATH = new AbsolutePathPlaceholder();

    public static void registerAll() {

        //Client
        PlaceholderRegistry.register(MINECRAFT_VERSION);
        PlaceholderRegistry.register(MOD_VERSION);
        PlaceholderRegistry.register(LOADED_MODS);
        PlaceholderRegistry.register(TOTAL_MODS);

        //GUI
        PlaceholderRegistry.register(SCREEN_WIDTH);
        PlaceholderRegistry.register(SCREEN_HEIGHT);
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

        //Advanced
        PlaceholderRegistry.register(STRINGIFY);
        PlaceholderRegistry.register(JSON);
        PlaceholderRegistry.register(GET_VARIABLE);
        PlaceholderRegistry.register(LOCALIZATION);
        PlaceholderRegistry.register(CALCULATOR);
        PlaceholderRegistry.register(RANDOM_NUMBER);

        //Other
        PlaceholderRegistry.register(PERCENT_RAM);
        PlaceholderRegistry.register(USED_RAM);
        PlaceholderRegistry.register(MAX_RAM);
        PlaceholderRegistry.register(RANDOM_TEXT);
        PlaceholderRegistry.register(WEB_TEXT);
        PlaceholderRegistry.register(ABSOLUTE_PATH);

    }

}
