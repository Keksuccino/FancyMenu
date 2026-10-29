package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Finds the achievements (advancements) the local player unlocks, based on the advancement updates the client receives from the server.<br><br>
 *
 * Vanilla treats every done progress in a non-reset update as a new unlock, but the server also resends the progress of advancements that were already done,
 * like when a command grants one of their remaining criteria. To report every unlock exactly once, the tracker remembers which achievements are done,
 * so every {@link net.minecraft.client.multiplayer.ClientAdvancements} instance needs its own tracker that sees all of its updates.
 */
public class AdvancementUnlockTracker {

    private final Set<Identifier> doneAchievements = new HashSet<>();
    @NotNull
    private final BiConsumer<Identifier, DisplayInfo> unlockHandler;

    public AdvancementUnlockTracker() {
        this(Listeners.ON_ACHIEVEMENT_UNLOCKED::onAchievementUnlocked);
    }

    AdvancementUnlockTracker(@NotNull BiConsumer<Identifier, DisplayInfo> unlockHandler) {
        this.unlockHandler = unlockHandler;
    }

    /**
     * Has to get called at the end of {@link net.minecraft.client.multiplayer.ClientAdvancements#update(ClientboundUpdateAdvancementsPacket)},
     * because it needs the tree with the advancements of the packet and the done state vanilla computes for the progress of the packet.
     */
    public void onAdvancementsUpdated(@NotNull ClientboundUpdateAdvancementsPacket packet, @NotNull AdvancementTree tree) {
        boolean reset = packet.shouldReset();
        // Reset updates sync the full state (like when joining a world or after reloading data packs), so their done achievements are no new unlocks
        if (reset) {
            this.doneAchievements.clear();
        }
        // The server removes hidden achievements from the client when they get revoked, without sending their progress
        this.doneAchievements.removeAll(packet.getRemoved());
        for (Map.Entry<Identifier, AdvancementProgress> entry : packet.getProgress().entrySet()) {
            Identifier id = entry.getKey();
            AdvancementNode node = tree.get(id);
            // Vanilla skips the progress of unknown advancements, so their done state never got computed.
            // Advancements without display info are technical ones (like recipe unlocks) that are invisible to the player.
            Optional<DisplayInfo> display = (node != null) ? node.advancement().display() : Optional.empty();
            if (display.isEmpty()) {
                continue;
            }
            if (!entry.getValue().isDone()) {
                this.doneAchievements.remove(id);
            } else if (this.doneAchievements.add(id) && !reset) {
                this.unlockHandler.accept(id, display.get());
            }
        }
    }

}
