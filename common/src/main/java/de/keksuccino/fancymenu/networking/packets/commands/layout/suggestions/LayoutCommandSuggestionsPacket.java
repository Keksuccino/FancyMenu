package de.keksuccino.fancymenu.networking.packets.commands.layout.suggestions;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class LayoutCommandSuggestionsPacket extends Packet {

    public List<String> layout_suggestions;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) return false;
        return ServerSideLayoutCommandSuggestionsPacketLogic.handle(sender, this);
    }

}