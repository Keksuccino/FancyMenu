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
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuForge {
    
    public FancyMenuForge() {

        FancyMenu.init();

        if (Services.PLATFORM.isOnClient()) {
            Konkrete.addPostLoadingEvent(FancyMenu.MOD_ID, FancyMenu::onClientSetup);
        }

        Packets.registerAll();

        MinecraftForge.EVENT_BUS.register(this);
        
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent e) {
        OpenGuiScreenCommand.register(e.getDispatcher());
        CloseGuiScreenCommand.register(e.getDispatcher());
        VariableCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        ServerOpenGuiScreenCommand.register(e.getDispatcher());
        ServerCloseGuiScreenCommand.register(e.getDispatcher());
        ServerVariableCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void registerKeyMappings(RegisterKeyMappingsEvent e) {
//        for (KeyMapping m : KeyMappings.KEY_MAPPINGS) {
//            e.register(m);
//        }
//        KeyMappings.init();
    }

}