package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.SharedConstants;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySightTransitionBaselineTest {

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reactivationSeedsCurrentEntitiesWithoutSyntheticStartEvents() {
        OnEntityStopsBeingInSightListener stopListener = new OnEntityStopsBeingInSightListener();
        OnEntityStartsBeingInSightListener startListener = new OnEntityStartsBeingInSightListener(stopListener);
        AtomicInteger starts = new AtomicInteger();
        ListenerInstance instance = startListener.createFreshInstance();
        instance.getActionScript().addExecutable(new RunnableExecutable(starts::incrementAndGet));
        TestEntity existingEntity = new TestEntity();

        assertFalse(startListener.onRenderFrameStart());
        startListener.registerInstance(instance);
        assertTrue(startListener.onRenderFrameStart());
        startListener.onEntityVisible(existingEntity, 4.0D);
        startListener.onRenderFrameEnd();
        assertEquals(0, starts.get());

        TestEntity newlyVisibleEntity = new TestEntity();
        assertTrue(startListener.onRenderFrameStart());
        startListener.onEntityVisible(existingEntity, 4.0D);
        startListener.onEntityVisible(newlyVisibleEntity, 5.0D);
        startListener.onRenderFrameEnd();
        assertEquals(1, starts.get());

        startListener.unregisterInstance(instance);
        assertFalse(startListener.onRenderFrameStart());
        startListener.registerInstance(instance);
        assertTrue(startListener.onRenderFrameStart());
        startListener.onEntityVisible(newlyVisibleEntity, 5.0D);
        startListener.onRenderFrameEnd();
        assertEquals(1, starts.get());
    }

    private static final class TestEntity extends Entity {

        private TestEntity() {
            super(EntityType.PIG, null);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
            return false;
        }
    }

    private static final class RunnableExecutable implements Executable {

        private final String identifier = UUID.randomUUID().toString();
        private final Runnable runnable;

        private RunnableExecutable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void execute() {
            this.runnable.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new RunnableExecutable(this.runnable);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test_executable");
        }
    }
}
