package de.keksuccino.fancymenu.commands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CloseGuiScreenCommand implements ICommand {

    @Override
    public String getName() {
        return "closeguiscreen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public List<String> getAliases() {
        List<String> l = new ArrayList<>();
        l.add("/closeguiscreen");
        return l;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        closeGui(sender);
    }

    private static int closeGui(ICommandSender sender) {
        ClientExecutor.execute(() -> {
            try {
                Minecraft.getMinecraft().displayGuiScreen(null);
            } catch (Exception e) {
                sender.sendMessage(new TextComponentString("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return new ArrayList<>();
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }

}
