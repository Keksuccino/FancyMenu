package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("openguiscreen").then(CommandManager.argument("identifier", StringArgumentType.string()).executes((stack) -> {
            return openGui(stack.getSource(), StringArgumentType.getString(stack, "identifier"));
        })));
    }

    private static int openGui(ServerCommandSource stack, String menuIdentifierOrCustomGuiName) {
        try {
            if (CustomGuiLoader.guiExists(menuIdentifierOrCustomGuiName)) {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().openScreen(CustomGuiLoader.getGui(menuIdentifierOrCustomGuiName, MinecraftClient.getInstance().currentScreen, null));
                });
            } else {
                Screen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().openScreen(s);
                    });
                } else {
                    stack.sendError(new LiteralText(Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            stack.sendError(new LiteralText(Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}