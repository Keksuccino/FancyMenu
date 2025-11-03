package de.keksuccino.fancymenu.networking.packets.entities;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class EntityEventPacket extends Packet {

    public EntityEventType event_type;
    public String entity_key;
    public String entity_uuid;
    public String killer_name;
    public String killer_key;
    public String killer_uuid;
    public String damage_type;
    public double pos_x;
    public double pos_y;
    public double pos_z;
    public String level_identifier;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender != null) {
            return false;
        }
        return ClientSideEntityEventPacketLogic.handle(this);
    }

    public enum EntityEventType {
        SPAWN,
        DEATH
    }
}
