package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class ServerGameruleValueResponsePacket extends Packet {

    public String placeholder;
    public String data;

    public ServerGameruleValueResponsePacket() {
    }

    public ServerGameruleValueResponsePacket(String placeholder, String data) {
        this.placeholder = placeholder;
        this.data = data;
    }

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return ClientSideServerGameruleValueResponsePacketLogic.handle(this);
    }

}
