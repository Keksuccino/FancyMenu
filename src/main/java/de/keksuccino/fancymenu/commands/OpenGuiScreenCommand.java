package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.command.CommandUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

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
            if (menuIdentifierOrCustomGuiName.equalsIgnoreCase(CreateWorldScreen.class.getName())) {
                Minecraft.getInstance().setScreen(CreateWorldScreen.createFresh(Minecraft.getInstance().screen));
                return 1;
            }
            if (CustomGuiLoader.guiExists(menuIdentifierOrCustomGuiName)) {
                ClientExecutor.execute(() -> {
                    Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(menuIdentifierOrCustomGuiName, Minecraft.getInstance().screen, null));
                });
            } else {
                Screen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    ClientExecutor.execute(() -> {
                        Minecraft.getInstance().setScreen(s);
                    });
                } else {
                    stack.sendFailure(new TextComponent(Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            stack.sendFailure(new TextComponent(Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}