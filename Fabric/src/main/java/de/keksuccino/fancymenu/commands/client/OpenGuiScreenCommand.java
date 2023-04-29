package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.fancymenu.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.command.CommandUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("openguiscreen").then(ClientCommandManager.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
        ));
    }

    private static int openGui(FabricClientCommandSource stack, String menuIdentifierOrCustomGuiName) {
        try {

            if (menuIdentifierOrCustomGuiName.equalsIgnoreCase(CreateWorldScreen.class.getName())) {
                CreateWorldScreen.openFresh(Minecraft.getInstance(), Minecraft.getInstance().screen);
                return 1;
            }
            //------------------
            if (CustomGuiLoader.guiExists(menuIdentifierOrCustomGuiName)) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(menuIdentifierOrCustomGuiName, Minecraft.getInstance().screen, null));
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                Screen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        Minecraft.getInstance().setScreen(s);
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                } else {
                    stack.sendError(Component.literal(Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            stack.sendError(Component.literal(Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}