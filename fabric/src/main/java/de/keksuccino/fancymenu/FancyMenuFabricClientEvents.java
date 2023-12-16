package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientCommandRegistrationEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class FancyMenuFabricClientEvents {

    public static void registerAll() {

        Konkrete.getEventHandler().registerEventsFrom(new FancyMenuFabricClientEvents());

        registerKeyMappings();

        registerScreenEvents();

    }

    @SubscribeEvent
    public void onRegisterCommands(ClientCommandRegistrationEvent e) {

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

            OpenGuiScreenCommand.register(e.dispatcher);
            CloseGuiScreenCommand.register(e.dispatcher);
            VariableCommand.register(e.dispatcher);

        }

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

                ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(screen1, key, scancode, modifiers);
                EventHandler.INSTANCE.postEvent(event);

                if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(key, scancode, modifiers);

            });
        });
    }

}
