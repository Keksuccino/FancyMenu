
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders;

import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderRegistry;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.advanced.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.client.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.gui.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.other.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.other.ram.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.player.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.realtime.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.server.*;

public class Placeholders {

    public static void registerAll() {

//        PlaceholderRegistry.registerPlaceholder(new TestPlaceholder1());
//        PlaceholderRegistry.registerPlaceholder(new TestPlaceholder2());
//        PlaceholderRegistry.registerPlaceholder(new TestPlaceholder3());
//        PlaceholderRegistry.registerPlaceholder(new TestPlaceholder4());

        //Client
        PlaceholderRegistry.registerPlaceholder(new MinecraftVersionPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ModVersionPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new LoadedModsPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new TotalModsPlaceholder());

        //GUI
        PlaceholderRegistry.registerPlaceholder(new ScreenWidthPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ScreenHeightPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ElementWidthPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ElementHeightPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ElementPosXPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ElementPosYPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new MousePosXPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new MousePosYPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new GuiScalePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new VanillaButtonLabelPlaceholder());

        //Player
        PlaceholderRegistry.registerPlaceholder(new PlayerNamePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new PlayerUuidPlaceholder());

        //Server
        PlaceholderRegistry.registerPlaceholder(new ServerMotdPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ServerPingPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ServerVersionPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ServerPlayerCountPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ServerStatusPlaceholder());

        //Realtime
        PlaceholderRegistry.registerPlaceholder(new RealtimeYearPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RealtimeMonthPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RealtimeDayPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RealtimeHourPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RealtimeMinutePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RealtimeSecondPlaceholder());

        //Advanced
        PlaceholderRegistry.registerPlaceholder(new StringifyPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new JsonPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new GetVariablePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new LocalizationPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new CalculatorPlaceholder());

        //Other
        PlaceholderRegistry.registerPlaceholder(new PercentRamPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new UsedRamPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new MaxRamPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new RandomTextPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new WebTextPlaceholder());

    }

}
