package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import de.keksuccino.fancymenu.networking.Packets;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.KeyMapping;

public class FancyMenuFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        FancyMenu.init();

        if (Services.PLATFORM.isOnClient()) {

            this.registerClientCommands();

            Konkrete.addPostLoadingEvent(FancyMenu.MOD_ID, FancyMenu::onClientSetup);

        }

        Packets.registerAll();

        this.registerServerCommands();

        this.registerKeyMappings();

    }

    public void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            OpenGuiScreenCommand.register(dispatcher);
            CloseGuiScreenCommand.register(dispatcher);
            VariableCommand.register(dispatcher);
        });
    }

    public void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
            ServerOpenGuiScreenCommand.register(dispatcher);
            ServerCloseGuiScreenCommand.register(dispatcher);
            ServerVariableCommand.register(dispatcher);
        });
    }

    public void registerKeyMappings() {
        if (FancyMenu.getConfig().getOrDefault("enablehotkeys", true)) {
            for (KeyMapping m : KeyMappings.KEY_MAPPINGS) {
                KeyBindingHelper.registerKeyBinding(m);
            }
            KeyMappings.init();
        }
    }

}
