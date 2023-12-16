package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(CommandSourceStack stack) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                Minecraft.getInstance().setScreen(null);
            } catch (Exception e) {
                stack.sendFailure(Components.literal("Error while executing command!"));
                e.printStackTrace();
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return 1;
    }

}