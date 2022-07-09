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
        Minecraft.getInstance().execute(() -> {
            try {
                Minecraft.getInstance().displayGuiScreen(null);
            } catch (Exception e) {
                stack.sendErrorMessage(new StringTextComponent("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}