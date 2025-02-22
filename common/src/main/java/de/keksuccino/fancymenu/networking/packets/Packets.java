package de.keksuccino.fancymenu.networking.packets;

import de.keksuccino.fancymenu.networking.PacketRegistry;
import de.keksuccino.fancymenu.networking.packets.commands.closegui.CloseGuiCommandPacketCodec;
import de.keksuccino.fancymenu.networking.packets.commands.layout.command.LayoutCommandPacketCodec;
import de.keksuccino.fancymenu.networking.packets.commands.layout.suggestions.LayoutCommandSuggestionsPacketCodec;
import de.keksuccino.fancymenu.networking.packets.commands.opengui.OpenGuiCommandPacketCodec;
import de.keksuccino.fancymenu.networking.packets.commands.variable.command.VariableCommandPacketCodec;
import de.keksuccino.fancymenu.networking.packets.commands.variable.suggestions.VariableCommandSuggestionsPacketCodec;

public class Packets {

    public static final CloseGuiCommandPacketCodec CLOSE_GUI_COMMAND_PACKET_CODEC = new CloseGuiCommandPacketCodec();
    public static final OpenGuiCommandPacketCodec OPEN_GUI_COMMAND_PACKET_CODEC = new OpenGuiCommandPacketCodec();
    public static final VariableCommandPacketCodec VARIABLE_COMMAND_PACKET_CODEC = new VariableCommandPacketCodec();
    public static final VariableCommandSuggestionsPacketCodec VARIABLE_COMMAND_SUGGESTIONS_PACKET_CODEC = new VariableCommandSuggestionsPacketCodec();
    public static final LayoutCommandSuggestionsPacketCodec LAYOUT_COMMAND_SUGGESTIONS_PACKET_CODEC = new LayoutCommandSuggestionsPacketCodec();
    public static final LayoutCommandPacketCodec LAYOUT_COMMAND_PACKET_CODEC = new LayoutCommandPacketCodec();

    public static void registerAll() {

        PacketRegistry.register(CLOSE_GUI_COMMAND_PACKET_CODEC);
        PacketRegistry.register(OPEN_GUI_COMMAND_PACKET_CODEC);
        PacketRegistry.register(VARIABLE_COMMAND_PACKET_CODEC);
        PacketRegistry.register(VARIABLE_COMMAND_SUGGESTIONS_PACKET_CODEC);
        PacketRegistry.register(LAYOUT_COMMAND_PACKET_CODEC);
        PacketRegistry.register(LAYOUT_COMMAND_SUGGESTIONS_PACKET_CODEC);

    }

}
