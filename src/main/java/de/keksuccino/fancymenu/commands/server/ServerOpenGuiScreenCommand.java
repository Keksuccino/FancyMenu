package de.keksuccino.fancymenu.commands.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ServerOpenGuiScreenCommand extends CommandBase {

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
        Entity e = sender.getCommandSenderEntity();
        if ((e != null) && (e instanceof EntityPlayerMP)) {
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
            msg.commandStringLength = ((CharSequence)msg.command).length();
            PacketHandler.sendTo((EntityPlayerMP) e, msg);
        }
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
