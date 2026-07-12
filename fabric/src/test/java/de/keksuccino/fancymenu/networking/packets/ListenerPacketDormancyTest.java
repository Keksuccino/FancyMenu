package de.keksuccino.fancymenu.networking.packets;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.networking.packets.entities.EntityEventPacket;
import de.keksuccino.fancymenu.networking.packets.fmdata.FmDataToClientPacket;
import de.keksuccino.fancymenu.networking.packets.structure.clientstructures.StructureEventPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerPacketDormancyTest {

    private static final List<AbstractListener> RELEVANT_PROVIDERS = List.of(Listeners.ON_ENTITY_SPAWNED, Listeners.ON_ENTITY_DIED, Listeners.ON_FM_DATA_RECEIVED, Listeners.ON_ENTER_STRUCTURE, Listeners.ON_LEAVE_STRUCTURE, Listeners.ON_ENTER_STRUCTURE_HIGH_PRECISION, Listeners.ON_LEAVE_STRUCTURE_HIGH_PRECISION);

    @BeforeEach
    void clearRelevantProvidersBeforeTest() {
        clearRelevantProviders();
    }

    @AfterEach
    void clearRelevantProvidersAfterTest() {
        clearRelevantProviders();
    }

    @Test
    void validDormantEntityPacketsAreHandledBeforePayloadParsing() {
        for (EntityEventPacket.EntityEventType eventType : EntityEventPacket.EntityEventType.values()) {
            EntityEventPacket packet = new EntityEventPacket();
            packet.event_type = eventType;
            packet.entity_uuid = "not-a-uuid";

            assertTrue(packet.processPacket(null));
        }
    }

    @Test
    void dormantFmDataPacketIsHandledWithoutNormalizingItsPayload() {
        FmDataToClientPacket packet = new FmDataToClientPacket();

        assertTrue(packet.processPacket(null));
    }

    @Test
    void validDormantStructurePacketsAreHandled() {
        for (StructureEventPacket.StructureEventType eventType : StructureEventPacket.StructureEventType.values()) {
            StructureEventPacket packet = new StructureEventPacket();
            packet.event_type = eventType;
            packet.structure_identifier = "minecraft:village";

            assertTrue(packet.processPacket(null));
        }
    }

    @Test
    void invalidEntityAndStructureDiscriminatorsRemainUnhandled() {
        EntityEventPacket entityPacket = new EntityEventPacket();
        StructureEventPacket structurePacket = new StructureEventPacket();
        structurePacket.structure_identifier = "minecraft:village";

        assertFalse(entityPacket.processPacket(null));
        assertFalse(structurePacket.processPacket(null));
    }

    @Test
    void invalidStructureIdentifierRemainsUnhandledWhileDormant() {
        StructureEventPacket packet = new StructureEventPacket();
        packet.event_type = StructureEventPacket.StructureEventType.ENTER;
        packet.structure_identifier = " ";

        assertFalse(packet.processPacket(null));
    }

    private static void clearRelevantProviders() {
        for (AbstractListener provider : RELEVANT_PROVIDERS) {
            provider.replaceInstances(List.of());
            assertFalse(provider.hasInstancesListening());
        }
    }
}
