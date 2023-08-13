
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.commands.client.CommandUtils;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerVariableCommandOLD {

    public static volatile Map<String, List<String>> cachedVariableArguments = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fmvariable")
                .then(Commands.argument("action", StringArgumentType.string())
                        .suggests(((context, builder) -> {
                            return CommandUtils.getStringSuggestions(builder, "get", "set");
                        }))
                        .then(Commands.argument("variable_name", StringArgumentType.string())
                                .executes((stack) -> {
                                    return getVariable(stack.getSource(), StringArgumentType.getString(stack, "action"), StringArgumentType.getString(stack, "variable_name"));
                                })
                                .suggests(((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions(context.getSource().getPlayerOrException()));
                                }))
                                .then(Commands.argument("set_to_value", StringArgumentType.greedyString())
                                        .suggests(((context, builder) -> {
                                            if (StringArgumentType.getString(context, "action").equalsIgnoreCase("set")) {
                                                return CommandUtils.getStringSuggestions(builder, "<set_to_value>");
                                            } else {
                                                return CommandUtils.getStringSuggestions(builder, new String[0]);
                                            }
                                        }))
                                        .then(Commands.argument("send_chat_feedback", BoolArgumentType.bool())
                                                .executes((stack) -> {
                                                    return setVariable(stack.getSource(), StringArgumentType.getString(stack, "action"), StringArgumentType.getString(stack, "variable_name"), StringArgumentType.getString(stack, "set_to_value"), BoolArgumentType.getBool(stack, "send_chat_feedback"));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static String[] getVariableNameSuggestions(ServerPlayer sender) {
        List<String> l = cachedVariableArguments.get(sender.getUUID().toString());
        if (l.isEmpty()) {
            l.add("<no_variables_found>");
        }
        return l.toArray(new String[0]);
    }

    private static int getVariable(CommandSourceStack stack, String getOrSet, String variableName) {
        try {
            if (getOrSet.equalsIgnoreCase("get")) {
                if (variableName != null) {
                    ServerPlayer sender = stack.getPlayerOrException();
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.direction = "client";
                    msg.command = "/fmvariable get " + variableName;
                    PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

    private static int setVariable(CommandSourceStack stack, String getOrSet, String variableName, String setToValue, boolean sendFeedback) {
        try {
            if (getOrSet.equalsIgnoreCase("set")) {
                if ((variableName != null) && (setToValue != null)) {
                    ServerPlayer sender = stack.getPlayerOrException();
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.direction = "client";
                    msg.command = "/fmvariable set " + variableName + " " + setToValue + " " + sendFeedback;
                    PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}