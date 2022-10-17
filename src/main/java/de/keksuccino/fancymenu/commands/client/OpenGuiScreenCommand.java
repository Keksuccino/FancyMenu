package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
        ));
    }

    private static int openGui(CommandSource stack, String menuIdentifierOrCustomGuiName) {
        try {
            if (menuIdentifierOrCustomGuiName.equalsIgnoreCase(CreateWorldScreen.class.getName())) {
                Minecraft.getInstance().setScreen(CreateWorldScreen.create(Minecraft.getInstance().screen));
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
                    stack.sendFailure(new StringTextComponent(Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            stack.sendFailure(new StringTextComponent(Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}