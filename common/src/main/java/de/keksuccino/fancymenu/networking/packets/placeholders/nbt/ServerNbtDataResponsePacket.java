package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class ServerNbtDataResponsePacket extends Packet {

    public enum ResultType {
        SUCCESS,
        EMPTY
    }

    public String placeholder;
    public String data;
    public ResultType resultType;

    public ServerNbtDataResponsePacket() {
    }

    public ServerNbtDataResponsePacket(String placeholder, String data, ResultType resultType) {
        this.placeholder = placeholder;
        this.data = data;
        this.resultType = resultType;
    }

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return ClientSideServerNbtDataResponsePacketLogic.handle(this);
    }

}
