
package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.Packets;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.suggestions.ServerboundVariableCommandSuggestionsPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.suggestions.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

//TODO übernehmen
public class PacketsForge {

    public static void init() {

        Packets.registerAll();



    }

    public static void registerAll() {

        //EXECUTE COMMAND
        PacketHandlerForge.registerMessage(ExecuteCommandPacketMessage.class, (msg, buf) -> {

            //Write data from message to byte buf
            buf.writeUtf(msg.direction);
            buf.writeUtf(msg.command);

        }, (buf) -> {

            //Write data from byte buf to msg
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.direction = buf.readUtf();
            msg.command = buf.readUtf();
            return msg;

        }, (msg, context) -> {

            //Handle packet
            context.get().enqueueWork(() -> {
                //Handle on client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    //Handle both sides on client, because integrated server needs handling too
                    if (msg.direction.equals("server")) { //Will never happen for this packet
//                        ServerboundPacketHandler.handle(msg, context.get().getSender());
                    } else if (msg.direction.equals("client")) {
                        ClientboundExecuteCommandPacketHandler.handle(msg);
                    }
                });
                //Handle on server (Will never happen for this packet)
                DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                    if (msg.direction.equals("server")) {
//                        ServerboundPacketHandler.handle(msg, context.get().getSender());
                    }
                });
            });
            context.get().setPacketHandled(true);

        });

        //VARIABLE COMMAND SUGGESTIONS HANDLING
        PacketHandlerForge.registerMessage(VariableCommandSuggestionsPacketMessage.class, (msg, buf) -> {

            //Write data from message to byte buf
            buf.writeUtf(msg.direction);
            String suggestionsRaw = "";
            for (String s : msg.variableNameSuggestions) {
                suggestionsRaw += s + ";";
            }
            buf.writeUtf(suggestionsRaw);

        }, (buf) -> {

            //Write data from byte buf to msg
            VariableCommandSuggestionsPacketMessage msg = new VariableCommandSuggestionsPacketMessage();
            msg.direction = buf.readUtf();
            String suggestionsRaw = buf.readUtf();
            if (suggestionsRaw.contains(";")) {
                for (String s : suggestionsRaw.split("[;]")) {
                    if (s.length() > 0) {
                        msg.variableNameSuggestions.add(s);
                    }
                }
            }
            return msg;

        }, (msg, context) -> {

            //Handle packet
            context.get().enqueueWork(() -> {
                //Handle on client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    //Handle both sides on client, because integrated server needs handling too
                    if (msg.direction.equals("server")) {
                        ServerboundVariableCommandSuggestionsPacketHandler.handle(msg, context.get().getSender());
                    } else if (msg.direction.equals("client")) { //Will never happen for this packet
//                        ClientboundPacketHandler.handle(msg);
                    }
                });
                //Handle on server
                DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                    if (msg.direction.equals("server")) {
                        ServerboundVariableCommandSuggestionsPacketHandler.handle(msg, context.get().getSender());
                    }
                });
            });
            context.get().setPacketHandled(true);

        });

    }

}
