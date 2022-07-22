//TODO übernehmen
package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.command.ClientExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class CloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(CommandSourceStack stack) {
        ClientExecutor.execute(() -> {
            try {
                Minecraft.getInstance().setScreen(null);
            } catch (Exception e) {
                stack.sendFailure(new TextComponent("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}