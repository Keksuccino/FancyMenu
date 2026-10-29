package de.keksuccino.fancymenu.networking.packets.raid;

import de.keksuccino.fancymenu.networking.Packet;
import de.keksuccino.fancymenu.util.level.raid.RaidAudienceEvent;
import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tells a client about a change of a raid it sees the raid bar of. Only gets sent from the server to the client.
 */
public class ActiveRaidPacket extends Packet {

    public RaidAudienceEvent event_type;
    public int raid_id;
    public int total_waves;
    public int current_wave;
    public List<String> raiders;
    public boolean victory;

    @NotNull
    public static ActiveRaidPacket create(int raidId, @NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory) {
        ActiveRaidPacket packet = new ActiveRaidPacket();
        packet.event_type = Objects.requireNonNull(event);
        packet.raid_id = raidId;
        if (snapshot != null) {
            packet.total_waves = snapshot.totalWaves();
            packet.current_wave = snapshot.currentWave();
            packet.raiders = snapshot.raiders();
        }
        packet.victory = victory;
        return packet;
    }

    /**
     * @return The raid state of this packet, or NULL if its event does not carry one.
     */
    @Nullable
    public RaidSnapshot toSnapshot() {
        if ((this.event_type == null) || !this.event_type.carriesSnapshot()) return null;
        List<String> validRaiders = new ArrayList<>();
        if (this.raiders != null) {
            for (String raider : this.raiders) {
                if ((raider != null) && !raider.isBlank()) validRaiders.add(raider);
            }
        }
        return new RaidSnapshot(this.total_waves, this.current_wave, validRaiders);
    }

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        // Clientbound only, so clients can't send it to the server
        return false;
    }

    @Override
    public boolean processClientPacket(@NotNull Connection connection) {
        return ClientSideActiveRaidPacketLogic.handle(this, connection);
    }

}
