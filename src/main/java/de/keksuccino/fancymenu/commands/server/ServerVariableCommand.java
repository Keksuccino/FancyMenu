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
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerVariableCommand extends CommandBase {

    public static volatile Map<String, List<String>> cachedVariableArguments = new HashMap<>();

    @Override
    public String getName() {
        return "fmvariable";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public List<String> getAliases() {
        List<String> l = new ArrayList<>();
        l.add("/fmvariable");
        return l;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("get") && (args.length >= 2)) {
                getVariable(sender, "get", args[1]);
            }
            if (args[0].equalsIgnoreCase("set") && (args.length >= 4)) {
                boolean feedback = false;
                if (args[3].equalsIgnoreCase("true")) {
                    feedback = true;
                }
                setVariable(sender, "set", args[1], args[2], feedback);
            }
        }
    }

    private static int getVariable(ICommandSender sender, String getOrSet, String variableName) {
        try {
            if (getOrSet.equalsIgnoreCase("get")) {
                Entity e = sender.getCommandSenderEntity();
                if ((e != null) && (e instanceof EntityPlayerMP)) {
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.command = "/fmvariable get " + variableName;
                    msg.commandStringLength = ((CharSequence)msg.command).length();
                    PacketHandler.sendTo((EntityPlayerMP) e, msg);
                }
            }
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

    private static int setVariable(ICommandSender sender, String getOrSet, String variableName, String setToValue, boolean sendFeedback) {
        try {
            if (getOrSet.equalsIgnoreCase("set")) {
                Entity e = sender.getCommandSenderEntity();
                if ((e != null) && (e instanceof EntityPlayerMP)) {
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.command = "/fmvariable set " + variableName + " " + setToValue + " " + sendFeedback;
                    msg.commandStringLength = ((CharSequence)msg.command).length();
                    PacketHandler.sendTo((EntityPlayerMP) e, msg);
                }
            }
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

    private static List<String> getVariableNameSuggestions(ICommandSender sender) {
        List<String> l = new ArrayList<>();
        Entity e = sender.getCommandSenderEntity();
        if (e != null) {
            if (cachedVariableArguments.containsKey(e.getUniqueID().toString())) {
                l = cachedVariableArguments.get(e.getUniqueID().toString());
            }
        }
        if (l.isEmpty()) {
            l.add("<no_variables_found>");
        }
        return l;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> l = new ArrayList<>();
        if (args.length == 1) {
            l.add("get");
            l.add("set");
        } else if (args.length == 2) {
            l.addAll(getVariableNameSuggestions(sender));
        } else if (args.length == 3) {
            l.add("<set_to_value>");
        } else if (args.length == 4) {
            l.add("<send_chat_feedback>");
            l.add("true");
            l.add("false");
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
