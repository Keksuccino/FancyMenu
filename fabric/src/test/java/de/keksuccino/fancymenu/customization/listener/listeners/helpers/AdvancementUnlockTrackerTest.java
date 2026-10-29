package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AdvancementUnlockTrackerTest {

    private static final String CRITERION = "unlocked";
    private static final Identifier FIRST_ID = Identifier.parse("fancymenu:test/first");
    private static final Identifier SECOND_ID = Identifier.parse("fancymenu:test/second");

    private final AdvancementTree tree = new AdvancementTree();
    private final List<Unlock> unlocks = new ArrayList<>();
    private final AdvancementUnlockTracker tracker = new AdvancementUnlockTracker((id, display) -> this.unlocks.add(new Unlock(id, display)));

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void achievementsAlreadyDoneWhenSyncingAreNoUnlocks() {
        AdvancementHolder achievement = achievement(FIRST_ID);

        this.receive(true, List.of(achievement), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        assertEquals(List.of(), this.unlocks);
    }

    @Test
    void completedAchievementIsReportedOnceWithItsDisplayInfo() {
        AdvancementHolder achievement = achievement(FIRST_ID);
        this.receive(true, List.of(achievement), Set.of(), Map.of(FIRST_ID, progress(achievement, false)));

        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));
        // The server resends done progress, for example when a command grants one of the remaining criteria
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        assertEquals(1, this.unlocks.size());
        assertEquals(FIRST_ID, this.unlocks.get(0).id());
        assertSame(achievement.value().display().orElseThrow(), this.unlocks.get(0).display());
    }

    @Test
    void everyAchievementCompletedByOneUpdateIsReported() {
        AdvancementHolder first = achievement(FIRST_ID);
        AdvancementHolder second = achievement(SECOND_ID);
        this.receive(true, List.of(first, second), Set.of(), Map.of());

        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(first, true), SECOND_ID, progress(second, true)));

        assertEquals(Set.of(FIRST_ID, SECOND_ID), Set.copyOf(this.unlockedIds()));
        assertEquals(2, this.unlocks.size());
    }

    @Test
    void revokedAchievementIsReportedAgainWhenRegranted() {
        AdvancementHolder achievement = achievement(FIRST_ID);
        this.receive(true, List.of(achievement), Set.of(), Map.of());

        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, false)));
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        assertEquals(List.of(FIRST_ID, FIRST_ID), this.unlockedIds());
    }

    @Test
    void removedAchievementIsReportedAgainWhenAddedDoneAgain() {
        AdvancementHolder achievement = achievement(FIRST_ID);
        this.receive(true, List.of(), Set.of(), Map.of());

        this.receive(false, List.of(achievement), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));
        // Hidden achievements get removed without progress update when they get revoked
        this.receive(false, List.of(), Set.of(FIRST_ID), Map.of());
        this.receive(false, List.of(achievement), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        assertEquals(List.of(FIRST_ID, FIRST_ID), this.unlockedIds());
    }

    @Test
    void resetForgetsDoneAchievements() {
        AdvancementHolder achievement = achievement(FIRST_ID);
        this.receive(true, List.of(achievement), Set.of(), Map.of());
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        this.receive(true, List.of(achievement), Set.of(), Map.of());
        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(achievement, true)));

        assertEquals(List.of(FIRST_ID, FIRST_ID), this.unlockedIds());
    }

    @Test
    void advancementsWithoutDisplayInfoAreIgnored() {
        AdvancementHolder technical = Advancement.Builder.advancement().addCriterion(CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).build(FIRST_ID);
        this.receive(true, List.of(technical), Set.of(), Map.of());

        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(technical, true)));

        assertEquals(List.of(), this.unlocks);
    }

    @Test
    void progressOfUnknownAdvancementsIsIgnored() {
        AdvancementHolder unknown = achievement(FIRST_ID);
        this.receive(true, List.of(), Set.of(), Map.of());

        this.receive(false, List.of(), Set.of(), Map.of(FIRST_ID, progress(unknown, true)));

        assertEquals(List.of(), this.unlocks);
    }

    /**
     * Applies the update to the tree the same way vanilla's ClientAdvancements#update does it, before the tracker handles the update at the end of that method.
     */
    private void receive(boolean reset, Collection<AdvancementHolder> added, Set<Identifier> removed, Map<Identifier, AdvancementProgress> progress) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(reset, added.stream().map(advancement -> new ClientboundUpdateAdvancementsPacket.PositionedAdvancement(advancement, 0.0F, 0.0F)).toList(), removed, progress, true);
        if (packet.shouldReset()) {
            this.tree.clear();
        }
        this.tree.remove(packet.removed());
        this.tree.addAll(packet.added().stream().map(ClientboundUpdateAdvancementsPacket.PositionedAdvancement::advancement).toList());
        this.tracker.onAdvancementsUpdated(packet, this.tree);
    }

    private List<Identifier> unlockedIds() {
        return this.unlocks.stream().map(Unlock::id).toList();
    }

    private static AdvancementHolder achievement(Identifier id) {
        return Advancement.Builder.advancement().display(Items.DIAMOND, Component.literal("Title"), Component.literal("Description"), AdvancementType.TASK, true, true, false).addCriterion(CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).build(id);
    }

    /**
     * Builds the progress like the client gets it, including the requirements vanilla applies to it before computing its done state.
     */
    private static AdvancementProgress progress(AdvancementHolder advancement, boolean done) {
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(advancement.value().requirements());
        if (done) {
            progress.grantProgress(CRITERION);
        }
        return progress;
    }

    private record Unlock(Identifier id, DisplayInfo display) {}

}
