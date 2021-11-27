package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class OpenGuiScreenCommand extends CommandBase {

    @Override
    public String getName() {
        return "openguiscreen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1) {
            //TODO remove debug
            System.out.println("MENU IDENTIFIER: " + args[0]);
            openGui(sender, args[0]);
        }
    }

    private static int openGui(ICommandSender sender, String menuIdentifierOrCustomGuiName) {
        try {
            if (CustomGuiLoader.guiExists(menuIdentifierOrCustomGuiName)) {
                Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(menuIdentifierOrCustomGuiName, Minecraft.getMinecraft().currentScreen, null));
            } else {
                GuiScreen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(menuIdentifierOrCustomGuiName));
                if (s != null) {
                    Minecraft.getMinecraft().displayGuiScreen(s);
                } else {
                    sender.sendMessage(new TextComponentString("§c" + Locals.localize("fancymenu.commands.openguiscreen.cannotopen")));
                }
            }
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§c" + Locals.localize("fancymenu.commands.openguiscreen.error")));
            e.printStackTrace();
        }
        return 1;
    }

}
