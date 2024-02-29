package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.neoforge.PacketSender;
import de.keksuccino.fancymenu.networking.neoforge.packets.variablesuggestions.ServerboundVariableSuggestionsPacketPayload;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VariableCommand {

    protected static Screen lastScreen = null;
    protected static boolean initialized = false;

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        if (!initialized) {
            EventHandler.INSTANCE.registerListenersOf(new VariableCommand());
            initialized = true;
        }
        d.register(Commands.literal("fmvariable")
                .then(Commands.literal("get")
                        .then(Commands.argument("variable_name", StringArgumentType.string())
                                .suggests(((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions());
                                }))
                                .executes((stack) -> {
                                    return getVariable(stack.getSource(), StringArgumentType.getString(stack, "variable_name"));
                                })
                        )
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("variable_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions());
                                })
                                .then(Commands.argument("send_chat_feedback", BoolArgumentType.bool())
                                        .then(Commands.argument("set_to_value", StringArgumentType.greedyString())
                                                .suggests(((context, builder) -> {
                                                    return CommandUtils.getStringSuggestions(builder, "<set_to_value>");
                                                }))
                                                .executes((stack) -> {
                                                    return setVariable(stack.getSource(), StringArgumentType.getString(stack, "variable_name"), StringArgumentType.getString(stack, "set_to_value"), BoolArgumentType.getBool(stack, "send_chat_feedback"));
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

    private static int getVariable(CommandSourceStack stack, String variableName) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                String s = VariableHandler.variableExists(variableName) ? Objects.requireNonNull(VariableHandler.getVariable(variableName)).getValue() : null;
                if (s != null) {
                    stack.sendSuccess(() -> Component.translatable("fancymenu.commands.variable.get.success", s), false);
                } else {
                    stack.sendFailure(Component.translatable("fancymenu.commands.variable.not_found"));
                }
            } catch (Exception e) {
                stack.sendFailure(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return 1;
    }

    private static int setVariable(CommandSourceStack stack, String variableName, String setToValue, boolean sendFeedback) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                VariableHandler.setVariable(variableName, setToValue);
                if (sendFeedback) {
                    stack.sendSuccess(() -> Component.translatable("fancymenu.commands.variable.set.success", setToValue), false);
                }
            } catch (Exception e) {
                stack.sendFailure(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return 1;
    }

    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {

        Screen s = Minecraft.getInstance().screen;
        if ((s instanceof ChatScreen) && ((lastScreen == null) || (lastScreen != s))) {
            PacketSender.sendToServer(new ServerboundVariableSuggestionsPacketPayload(Arrays.asList(getVariableNameSuggestions())));
        }
        lastScreen = s;

    }

}