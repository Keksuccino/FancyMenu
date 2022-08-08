//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.commands.variable;

import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ServerboundVariableCommandSuggestionsPacketHandler {

    public static final ResourceLocation PACKET_ID = new ResourceLocation("fancymenu", "variable_cmd_sugg");

    public static void handle(VariableCommandSuggestionsPacketMessage msg, ServerPlayer sender) {

        if (sender != null) {
            sender.getServer().execute(() -> {
                String uuid = sender.getUUID().toString();
                ServerVariableCommand.cachedVariableArguments.put(uuid, msg.variableNameSuggestions);
            });
        }

    }

    public static FriendlyByteBuf writeToByteBuf(VariableCommandSuggestionsPacketMessage msg) {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeUtf(msg.direction);
        String s = "";
        for (String s2 : msg.variableNameSuggestions) {
            if (s2.length() > 0) {
                s += s2 + ";";
            }
        }
        buf.writeUtf(s);

        return buf;

    }

    public static VariableCommandSuggestionsPacketMessage readFromByteBuf(FriendlyByteBuf buf) {

        VariableCommandSuggestionsPacketMessage msg = new VariableCommandSuggestionsPacketMessage();

        msg.direction = buf.readUtf();
        String s = buf.readUtf();
        if ((s != null) && (s.contains(";"))) {
            for (String s2 : s.split("[;]")) {
                msg.variableNameSuggestions.add(s2);
            }
        }

        return msg;

    }

}
