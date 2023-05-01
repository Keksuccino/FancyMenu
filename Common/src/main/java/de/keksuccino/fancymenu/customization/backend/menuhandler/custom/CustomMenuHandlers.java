package de.keksuccino.fancymenu.customization.backend.menuhandler.custom;

import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerRegistry;

public class CustomMenuHandlers {

    public static void registerAll() {

        MenuHandlerRegistry.registerHandler(new MainMenuHandler());
        MenuHandlerRegistry.registerHandler(new MoreRefinedStorageMainHandler());
        MenuHandlerRegistry.registerHandler(new DummyCoreMainHandler());
        MenuHandlerRegistry.registerHandler(new WorldLoadingScreenHandler());
        MenuHandlerRegistry.registerHandler(new PauseScreenHandler());

    }

}
