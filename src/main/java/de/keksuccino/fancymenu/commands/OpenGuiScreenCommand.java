package de.keksuccino.fancymenu.commands;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OpenGuiScreenCommand implements ICommand {

    @Override
    public String getName() {
        return "openguiscreen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public List<String> getAliases() {
        List<String> l = new ArrayList<>();
        l.add("/openguiscreen");
        return l;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1) {
            openGui(sender, args[0]);
        }
    }

    private static int openGui(ICommandSender sender, String menuIdentifierOrCustomGuiName) {
        ClientExecutor.execute(() -> {
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
        });
        return 1;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> l = new ArrayList<>();
        if (args.length == 1) {
            l.add("<menu_identifier>");
        }
        return l;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }
}
