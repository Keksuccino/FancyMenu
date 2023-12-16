package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("screen_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "screen_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<screen_identifier>");
                })
        ));
    }

    private static int openGui(CommandSourceStack stack, String screenIdentifierOrCustomGuiName) {
        try {
            if (CustomGuiHandler.guiExists(screenIdentifierOrCustomGuiName)) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    Screen custom = CustomGuiHandler.constructInstance(screenIdentifierOrCustomGuiName, Minecraft.getInstance().screen, null);
                    if (custom != null) Minecraft.getInstance().setScreen(custom);
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                Screen s = ScreenInstanceFactory.tryConstruct(ScreenIdentifierHandler.getBestIdentifier(screenIdentifierOrCustomGuiName));
                if (s != null) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        Minecraft.getInstance().setScreen(s);
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                } else {
                    stack.sendFailure(Components.translatable("fancymenu.commmands.openguiscreen.unable_to_open_gui", screenIdentifierOrCustomGuiName));
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Components.translatable("fancymenu.commands.openguiscreen.error"));
            e.printStackTrace();
        }
        return 1;
    }

}