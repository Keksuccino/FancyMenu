package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class VariableCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fmvariable")
                .then(Commands.argument("get_or_set", StringArgumentType.string())
                .then(Commands.argument("variable_name", StringArgumentType.string())
                        .executes((stack) -> {
                            return getVariable(stack.getSource(), StringArgumentType.getString(stack, "get_or_set"), StringArgumentType.getString(stack, "variable_name"));
                        })
                        .then(Commands.argument("set_to_value", StringArgumentType.string())
                        .then(Commands.argument("send_chat_feedback", BoolArgumentType.bool())
                                .executes((stack) -> {
                                    return setVariable(stack.getSource(), StringArgumentType.getString(stack, "get_or_set"), StringArgumentType.getString(stack, "variable_name"), StringArgumentType.getString(stack, "set_to_value"), BoolArgumentType.getBool(stack, "send_chat_feedback"));
                                })
                        )
                        )
                )
                )
        );
    }

    private static int getVariable(CommandSourceStack stack, String getOrSet, String variableName) {
        Minecraft.getInstance().execute(() -> {
            try {
                if (getOrSet.equalsIgnoreCase("get")) {
                    String s = VariableHandler.getVariable(variableName);
                    if (s != null) {
                        stack.sendSuccess(Component.literal(Locals.localize("fancymenu.commands.variable.get.success", s)), false);
                    } else {
                        stack.sendFailure(Component.literal(Locals.localize("fancymenu.commands.variable.not_found")));
                    }
                }
            } catch (Exception e) {
                stack.sendFailure(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

    private static int setVariable(CommandSourceStack stack, String getOrSet, String variableName, String setToValue, boolean sendFeedback) {
        Minecraft.getInstance().execute(() -> {
            try {
                if (getOrSet.equalsIgnoreCase("set")) {
                    VariableHandler.setVariable(variableName, setToValue);
                    if (sendFeedback) {
                        stack.sendSuccess(Component.literal(Locals.localize("fancymenu.commands.variable.set.success", setToValue)), false);
                    }
                }
            } catch (Exception e) {
                stack.sendFailure(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

}