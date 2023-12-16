package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.command.CommandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName) {
        try {
            if (CustomGuiHandler.guiExists(menuIdentifierOrCustomGuiName)) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    Minecraft.getInstance().setScreen(CustomGuiHandler.constructInstance(menuIdentifierOrCustomGuiName, Minecraft.getInstance().screen, null));
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                Screen s = ScreenInstanceFactory.tryConstruct(ScreenIdentifierHandler.getBestIdentifier(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        Minecraft.getInstance().setScreen(s);
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                } else {
                    stack.sendFailure(Components.translatable("fancymenu.commmands.openguiscreen.unable_to_open_gui", menuIdentifierOrCustomGuiName));
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Components.translatable("fancymenu.commands.openguiscreen.error"));
            e.printStackTrace();
        }
        return 1;
    }

}