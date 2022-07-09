package de.keksuccino.fancymenu.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CloseGuiScreenCommand extends CommandBase {

    @Override
    public String getName() {
        return "closeguiscreen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        closeGui(sender);
    }

    private static int closeGui(ICommandSender sender) {
        try {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
