package de.keksuccino.fancymenu.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.ServerboundVariableCommandSuggestionsPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.command.CommandUtils;
import net.minecraft.client.resources.language.I18n;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VariableCommand {

    protected static Screen lastScreen = null;
    protected static boolean initialized = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        if (!initialized) {
            EventHandler.INSTANCE.registerListenersOf(new VariableCommand());
            initialized = true;
        }
        d.register(ClientCommandManager.literal("fmvariable")
                .then(ClientCommandManager.literal("get")
                        .then(ClientCommandManager.argument("variable_name", StringArgumentType.string())
                                .suggests(((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions());
                                }))
                                .executes((stack) -> {
                                    return getVariable(stack.getSource(), StringArgumentType.getString(stack, "variable_name"));
                                })
                        )
                )
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("variable_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    return CommandUtils.getStringSuggestions(builder, getVariableNameSuggestions());
                                })
                                .then(ClientCommandManager.argument("send_chat_feedback", BoolArgumentType.bool())
                                        .then(ClientCommandManager.argument("set_to_value", StringArgumentType.greedyString())
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

    private static int getVariable(FabricClientCommandSource stack, String variableName) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                String s = VariableHandler.variableExists(variableName) ? Objects.requireNonNull(VariableHandler.getVariable(variableName)).getValue() : null;
                if (s != null) {
                    stack.sendFeedback(Component.literal(I18n.get("fancymenu.commands.variable.get.success", s)));
                } else {
                    stack.sendError(Component.literal(I18n.get("fancymenu.commands.variable.not_found")));
                }
            } catch (Exception e) {
                stack.sendError(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return 1;
    }

    private static int setVariable(FabricClientCommandSource stack, String variableName, String setToValue, boolean sendFeedback) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                VariableHandler.setVariable(variableName, setToValue);
                if (sendFeedback) {
                    stack.sendFeedback(Component.literal(I18n.get("fancymenu.commands.variable.set.success", setToValue)));
                }
            } catch (Exception e) {
                stack.sendError(Component.literal("Error while executing command!"));
                e.printStackTrace();
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return 1;
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {

        Screen s = Minecraft.getInstance().screen;
        if ((s instanceof ChatScreen) && ((lastScreen == null) || (lastScreen != s))) {
            VariableCommandSuggestionsPacketMessage msg = new VariableCommandSuggestionsPacketMessage();
            msg.direction = "server";
            msg.variableNameSuggestions.addAll(Arrays.asList(getVariableNameSuggestions()));
            ClientPlayNetworking.send(ServerboundVariableCommandSuggestionsPacketHandler.PACKET_ID, ServerboundVariableCommandSuggestionsPacketHandler.writeToByteBuf(msg));
        }
        lastScreen = s;

    }

}