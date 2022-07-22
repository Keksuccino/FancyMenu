//TODO übernehmen
package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class CloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(CommandSource stack) {
        ClientExecutor.execute(() -> {
            try {
                Minecraft.getInstance().setScreen(null);
            } catch (Exception e) {
                stack.sendFailure(new StringTextComponent("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}