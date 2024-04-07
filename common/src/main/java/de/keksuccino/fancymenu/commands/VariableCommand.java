package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.commands.variable.command.VariableCommandPacket;
import de.keksuccino.konkrete.command.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class VariableCommand {

    public static final Map<String, List<String>> CACHED_VARIABLE_SUGGESTIONS = Collections.synchronizedMap(new HashMap<>());

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fmvariable")
                .then(Commands.literal("get")
                        .then(Commands.argument("variable_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions(context.getSource().getPlayerOrException()));
                                })
                                .executes((stack) -> {
                                    return getVariable(stack.getSource(), StringArgumentType.getString(stack, "variable_name"));
                                })
                        )
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("variable_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions(context.getSource().getPlayerOrException()));
                                })
                                .then(Commands.argument("send_chat_feedback", BoolArgumentType.bool())
                                        .then(Commands.argument("set_to_value", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    return CommandUtils.getStringSuggestions(builder, "<set_to_value>");
                                                })
                                                .executes((stack) -> {
                                                    return setVariable(stack.getSource(), StringArgumentType.getString(stack, "variable_name"), StringArgumentType.getString(stack, "set_to_value"), BoolArgumentType.getBool(stack, "send_chat_feedback"));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static String[] getVariableNameSuggestions(ServerPlayer sender) {
        List<String> l = new ArrayList<>(Objects.requireNonNullElse(CACHED_VARIABLE_SUGGESTIONS.get(sender.getUUID().toString()), new ArrayList<>()));
        if (l.isEmpty()) {
            l.add("<no_variables_found>");
        }
        return l.toArray(new String[0]);
    }

    private static int getVariable(CommandSourceStack stack, String variableName) {
        try {
            if (variableName != null) {
                ServerPlayer sender = stack.getPlayerOrException();
                VariableCommandPacket packet = new VariableCommandPacket();
                packet.set = false;
                packet.variable_name = variableName;
                PacketHandler.sendToClient(sender, packet);
            }
        } catch (Exception ex) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            ex.printStackTrace();
        }
        return 1;
    }

    private static int setVariable(CommandSourceStack stack, String variableName, String setToValue, boolean sendFeedback) {
        try {
            if ((variableName != null) && (setToValue != null)) {
                ServerPlayer sender = stack.getPlayerOrException();
                VariableCommandPacket packet = new VariableCommandPacket();
                packet.set = true;
                packet.variable_name = variableName;
                packet.set_to_value = setToValue;
                packet.feedback = sendFeedback;
                PacketHandler.sendToClient(sender, packet);
            }
        } catch (Exception ex) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            ex.printStackTrace();
        }
        return 1;
    }

}