package de.keksuccino.fancymenu.commands.client;

import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VariableCommand implements ICommand {

    protected static GuiScreen lastScreen = null;
    protected static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(new VariableCommand());
            initialized = true;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {

        GuiScreen s = Minecraft.getMinecraft().currentScreen;
        if ((s instanceof GuiChat) && ((lastScreen == null) || (lastScreen != s))) {
            VariableCommandSuggestionsPacketMessage msg = new VariableCommandSuggestionsPacketMessage();
            msg.direction = "server";
            msg.variableNameSuggestions.addAll(getVariableNameSuggestions());
            PacketHandler.sendToServer(msg);
        }
        lastScreen = s;

    }

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
                String s = VariableHandler.getVariable(variableName);
                if (s != null) {
                    sender.sendMessage(new TextComponentString(Locals.localize("fancymenu.commands.variable.get.success", s)));
                } else {
                    sender.sendMessage(new TextComponentString(Locals.localize("fancymenu.commands.variable.not_found")));
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
                VariableHandler.setVariable(variableName, setToValue);
                if (sendFeedback) {
                    sender.sendMessage(new TextComponentString(Locals.localize("fancymenu.commands.variable.set.success", setToValue)));
                }
            }
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

    private static List<String> getVariableNameSuggestions() {
        List<String> l = VariableHandler.getVariableNames();
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
            l.addAll(getVariableNameSuggestions());
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
