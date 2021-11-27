package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("identifier", StringArgumentType.string()).executes((stack) -> {
            return openGui(stack.getSource(), StringArgumentType.getString(stack, "identifier"));
        })));
    }

    private static int openGui(CommandSource stack, String menuIdentifierOrCustomGuiName) {
        try {
            if (CustomGuiLoader.guiExists(menuIdentifierOrCustomGuiName)) {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().displayGuiScreen(CustomGuiLoader.getGui(menuIdentifierOrCustomGuiName, Minecraft.getInstance().currentScreen, null));
                });
            } else {
                Screen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().displayGuiScreen(s);
                    });
                } else {
                    stack.sendErrorMessage(new StringTextComponent(Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            stack.sendErrorMessage(new StringTextComponent(Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}