package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import de.keksuccino.fancymenu.events.screen.KeyPressedScreenEvent;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class FancyMenuFabricEvents {

    public static void registerAll() {

        if (Services.PLATFORM.isOnClient()) {

            registerClientCommands();

            registerKeyMappings();

            registerScreenEvents();

        }

        registerServerCommands();

    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            OpenGuiScreenCommand.register(dispatcher);
            CloseGuiScreenCommand.register(dispatcher);
            VariableCommand.register(dispatcher);
        });
    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
            ServerOpenGuiScreenCommand.register(dispatcher);
            ServerCloseGuiScreenCommand.register(dispatcher);
            ServerVariableCommand.register(dispatcher);
        });
    }

    private static void registerKeyMappings() {
//        for (KeyMapping m : KeyMappings.KEY_MAPPINGS) {
//            KeyBindingHelper.registerKeyBinding(m);
//        }
//        KeyMappings.init();
    }

    private static void registerScreenEvents() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                KeyPressedScreenEvent event = new KeyPressedScreenEvent(screen1, key, scancode, modifiers);
                EventHandler.INSTANCE.postEvent(event);
            });
        });
    }

}
