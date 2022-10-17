package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.command.ClientExecutor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CloseGuiScreenCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(FabricClientCommandSource stack) {
        ClientExecutor.execute(() -> {
            try {
                Minecraft.getInstance().setScreen(null);
            } catch (Exception e) {
                stack.sendError(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}