//---
package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

public class VariableCommand {

    protected static Screen lastScreen = null;
    protected static boolean initialized = false;

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(new VariableCommand());
            initialized = true;
        }
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
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions());
                                }))
                                .then(Commands.argument("set_to_value", StringArgumentType.string())
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

    private static String[] getVariableNameSuggestions() {
        List<String> l = VariableHandler.getVariableNames();
        if (l.isEmpty()) {
            l.add("<no_variables_found>");
        }
        return l.toArray(new String[0]);
    }

    private static int getVariable(CommandSourceStack stack, String getOrSet, String variableName) {
        ClientExecutor.execute(() -> {
            try {
                if (getOrSet.equalsIgnoreCase("get")) {
                    String s = VariableHandler.getVariable(variableName);
                    if (s != null) {
                        stack.sendSuccess(new TextComponent(Locals.localize("fancymenu.commands.variable.get.success", s)), false);
                    } else {
                        stack.sendFailure(new TextComponent(Locals.localize("fancymenu.commands.variable.not_found")));
                    }
                }
            } catch (Exception e) {
                stack.sendFailure(new TextComponent("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

    private static int setVariable(CommandSourceStack stack, String getOrSet, String variableName, String setToValue, boolean sendFeedback) {
        ClientExecutor.execute(() -> {
            try {
                if (getOrSet.equalsIgnoreCase("set")) {
                    VariableHandler.setVariable(variableName, setToValue);
                    if (sendFeedback) {
                        stack.sendSuccess(new TextComponent(Locals.localize("fancymenu.commands.variable.set.success", setToValue)), false);
                    }
                }
            } catch (Exception e) {
                stack.sendFailure(new TextComponent("Error while executing command!"));
                e.printStackTrace();
            }
        });
        return 1;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {

        Screen s = Minecraft.getInstance().screen;
        if ((s instanceof ChatScreen) && ((lastScreen == null) || (lastScreen != s))) {
            VariableCommandSuggestionsPacketMessage msg = new VariableCommandSuggestionsPacketMessage();
            msg.direction = "server";
            msg.variableNameSuggestions.addAll(Arrays.asList(getVariableNameSuggestions()));
            PacketHandler.sendToServer(msg);
        }
        lastScreen = s;

    }

}