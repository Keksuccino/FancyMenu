package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.commands.layout.command.LayoutCommandPacket;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.konkrete.command.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class LayoutCommand {

    public static final Map<String, List<String>> CACHED_LAYOUT_SUGGESTIONS = Collections.synchronizedMap(new HashMap<>());

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fmlayout").then(Commands.argument("layout_name", StringArgumentType.string())
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, getLayoutNameSuggestions(context.getSource().getPlayerOrException()));
                })
                .then(Commands.argument("is_layout_enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            return setLayoutState(context.getSource(), StringArgumentType.getString(context, "layout_name"), BoolArgumentType.getBool(context, "is_layout_enabled"), null);
                        })
                        .then(Commands.argument("target_players", EntityArgument.players())
                                .requires(context -> context.hasPermission(2))
                                .executes(context -> {
                                    return setLayoutState(context.getSource(), StringArgumentType.getString(context, "layout_name"), BoolArgumentType.getBool(context, "is_layout_enabled"), EntityArgument.getPlayers(context, "target_players"));
                                })))
        ));
    }

    private static String[] getLayoutNameSuggestions(ServerPlayer sender) {
        List<String> l = new ArrayList<>(Objects.requireNonNullElse(CACHED_LAYOUT_SUGGESTIONS.get(sender.getUUID().toString()), new ArrayList<>()));
        if (l.isEmpty()) {
            l.add("<no_layouts_found>");
        }
        return l.toArray(new String[0]);
    }

    private static int setLayoutState(CommandSourceStack stack, String layoutName, boolean enabled, @Nullable Collection<ServerPlayer> targets) {
        try {
            if (targets == null) {
                ServerPlayer sender = stack.getPlayerOrException();
                LayoutCommandPacket packet = new LayoutCommandPacket();
                packet.layout_name = layoutName;
                packet.enabled = enabled;
                PacketHandler.sendToClient(sender, packet);
            } else {
                for (ServerPlayer target : targets) {
                    LayoutCommandPacket packet = new LayoutCommandPacket();
                    packet.layout_name = layoutName;
                    packet.enabled = enabled;
                    PacketHandler.sendToClient(target, packet);
                }
            }
        } catch (Exception ex) {
            stack.sendFailure(Components.literal("Error while executing /fmlayout command!"));
            ex.printStackTrace();
        }
        return 1;
    }

}