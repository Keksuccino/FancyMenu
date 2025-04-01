package de.keksuccino.fancymenu.customization.layout.editor.buddy.animation;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Represents an animation state for the TamagotchiBuddy.
 * Each state has specific properties that determine when and how it's displayed.
 */
public class AnimationState {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation TEXTURE_ATLAS = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/buddy_atlas.png");
    public static final int SPRITE_WIDTH = 32;
    public static final int SPRITE_HEIGHT = 32;
    public static final int ATLAS_COLUMNS = 4; // 4 animation frames per row
    public static final int ATLAS_ROWS = 17; // Different states (idle, happy, eating, etc.) - now 17 rows with sitting, waving, and yawning animations

    private final String name;
    private final int atlasIndex;
    private final AnimationSpeedSupplier animationSpeed;
    private final boolean allowsMovement;
    private final boolean allowsHopping;
    private final Predicate<TamagotchiBuddy> activationCondition;
    private final Predicate<TamagotchiBuddy> preventionCondition;
    private final int priority;
    private final boolean isTemporaryState;
    private final int minDuration;
    private final int maxDuration;
    private final DurationRandomizer durationRandomizer;
    @NotNull
    private final WalkingSpeedSupplier walkingSpeed;
    private final boolean lockStateUntilFinished;
    private final boolean ignoresLockedState;
    private final long cooldown;
    @Nullable
    private final StateEndAction onDeactivate;

    private long lastCountdownTriggerTime = -1L;

    /**
     * Creates a new animation state.
     *
     * @param name The unique name of the state for logging and identification
     * @param atlasIndex The row in the texture atlas containing this animation (first row is 0)
     * @param animationSpeed The speed of the animation in ticks per frame
     * @param allowsMovement Whether the buddy can move while in this state
     * @param allowsHopping Whether the buddy can hop while in this state
     * @param activationCondition Condition that must be met for this state to be activated
     * @param preventionCondition Condition that prevents this state from activating if true
     * @param priority Priority level - higher priority states override lower ones when multiple could apply
     * @param isTemporaryState Whether this state is temporary with a duration
     * @param minDuration The minimum duration
     * @param maxDuration The maximum duration
     * @param durationRandomizer The duration randomizer
     * @param walkingSpeed The speed buddy should walk at
     * @param lockStateUntilFinished Locks buddy into this state until the state finished playing
     * @param ignoresLockedState If this state should other locked states
     * @param cooldown The amount of time in MS the state should be on cooldown after getting activated
     */
    private AnimationState(@NotNull String name, int atlasIndex, @NotNull AnimationSpeedSupplier animationSpeed, boolean allowsMovement,
                           boolean allowsHopping, @NotNull Predicate<TamagotchiBuddy> activationCondition,
                           @NotNull Predicate<TamagotchiBuddy> preventionCondition, int priority,
                           boolean isTemporaryState, int minDuration, int maxDuration, @NotNull DurationRandomizer durationRandomizer, @NotNull WalkingSpeedSupplier walkingSpeed,
                           boolean lockStateUntilFinished, boolean ignoresLockedState, long cooldown, @Nullable StateEndAction onDeactivate) {
        this.name = name;
        this.atlasIndex = atlasIndex;
        this.animationSpeed = animationSpeed;
        this.allowsMovement = allowsMovement;
        this.allowsHopping = allowsHopping;
        this.activationCondition = activationCondition;
        this.preventionCondition = preventionCondition;
        this.priority = priority;
        this.isTemporaryState = isTemporaryState;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.durationRandomizer = durationRandomizer;
        this.walkingSpeed = walkingSpeed;
        this.lockStateUntilFinished = lockStateUntilFinished;
        this.ignoresLockedState = ignoresLockedState;
        this.cooldown = cooldown;
        this.onDeactivate = onDeactivate;
    }

    /**
     * Checks if this state can be activated for the given buddy.
     *
     * @param buddy The buddy to check
     * @return true if this state can be activated, false otherwise
     */
    public boolean canActivate(TamagotchiBuddy buddy) {
        // First check activation condition
        if (!activationCondition.test(buddy)) {
            return false;
        }
        // Then check prevention condition if present
        if (preventionCondition.test(buddy)) {
            return false;
        }
        // If state is on cooldown, don't activate
        long now = System.currentTimeMillis();
        if ((this.cooldown > 0) && ((this.lastCountdownTriggerTime + this.cooldown) > now)) {
            LOGGER.info("State on cooldown! Will not activate: " + this.getName());
            return false;
        }
        return true;
    }

    /**
     * Called when this state becomes active
     *
     * @param buddy The buddy instance
     */
    public void onActivate(TamagotchiBuddy buddy) {

        LOGGER.info("Activating state: {}", name);

        // Reset animation frame when changing state
        buddy.resetAnimationFrame();

        // Set activity duration for temporary states
        if (isTemporaryState) {
            buddy.setCurrentStateDuration(Math.min(this.maxDuration, Math.max(this.minDuration, this.getRandomizedDuration(buddy))));
        }

        this.lastCountdownTriggerTime = System.currentTimeMillis();

    }

    /**
     * Called when this state is deactivated
     *
     * @param buddy The buddy instance
     */
    public void onDeactivate(TamagotchiBuddy buddy) {
        LOGGER.info("Deactivating state: {}", name);
        if (this.onDeactivate != null) this.onDeactivate.onDeactivate(buddy, this);
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getAtlasIndex() {
        return atlasIndex;
    }

    public int getAnimationSpeed(@NotNull TamagotchiBuddy buddy) {
        return animationSpeed.speed(buddy, this);
    }

    public boolean allowsMovement() {
        return allowsMovement;
    }

    public boolean allowsHopping() {
        return allowsHopping;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isTemporaryState() {
        return isTemporaryState;
    }

    public int getMinDuration() {
        return minDuration;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public int getRandomizedDuration(TamagotchiBuddy buddy) {
        return this.durationRandomizer.randomize(buddy, this);
    }

    public int getCurrentWalkingSpeed(@NotNull TamagotchiBuddy buddy) {
        return this.walkingSpeed.speed(buddy, this);
    }

    public boolean shouldLockStateUntilFinished() {
        return this.lockStateUntilFinished;
    }

    public boolean shouldIgnoreLockedState() {
        return this.ignoresLockedState;
    }

    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public String toString() {
        return name;
    }

    @FunctionalInterface
    public interface DurationRandomizer {
        int randomize(TamagotchiBuddy buddy, AnimationState state);
    }

    @FunctionalInterface
    public interface WalkingSpeedSupplier {
        int speed(TamagotchiBuddy buddy, AnimationState state);
    }

    @FunctionalInterface
    public interface AnimationSpeedSupplier {
        int speed(TamagotchiBuddy buddy, AnimationState state);
    }

    @FunctionalInterface
    public interface StateEndAction {
        void onDeactivate(TamagotchiBuddy buddy, AnimationState state);
    }

    // Builder pattern for easier construction
    public static class Builder {

        private final String name;
        private final int atlasIndex;
        private AnimationSpeedSupplier animationSpeed = (buddy, state) -> 5; // Default animation speed
        private boolean allowsMovement = true;
        private boolean allowsHopping = true;
        private Predicate<TamagotchiBuddy> activationCondition = buddy -> true; // Default: always active
        private Predicate<TamagotchiBuddy> preventionCondition = buddy -> false; // Default: no prevention
        private int priority = 0;
        private boolean isTemporaryState = false;
        private int minDuration = 60;
        private int maxDuration = 60;
        private DurationRandomizer durationRandomizer = (buddy, state) -> MathUtils.getRandomNumberInRange(state.minDuration, state.maxDuration);
        private WalkingSpeedSupplier walkingSpeed = (buddy, state) -> 2; // Default walking speed
        private boolean lockStateUntilFinished = false;
        private boolean ignoresLockedState = false;
        private long cooldown = 0L;
        @Nullable
        private StateEndAction onDeactivate = null;

        public Builder(String name, int atlasIndex) {
            this.name = name;
            this.atlasIndex = atlasIndex;
        }

        /**
         * @param animationSpeed Higher value means slower animation. Default speed is 5.
         */
        public Builder animationSpeed(@NotNull AnimationSpeedSupplier animationSpeed) {
            this.animationSpeed = animationSpeed;
            return this;
        }

        /**
         * If buddy should be able to move while this state is active. Default is true.
         */
        public Builder allowsMovement(boolean allowsMovement) {
            this.allowsMovement = allowsMovement;
            return this;
        }

        /**
         * If buddy should be able to hop while in this state. Default is true.
         */
        public Builder allowsHopping(boolean allowsHopping) {
            this.allowsHopping = allowsHopping;
            return this;
        }

        public Builder activationCondition(@NotNull Predicate<TamagotchiBuddy> activationCondition) {
            this.activationCondition = activationCondition;
            return this;
        }

        public Builder preventionCondition(@NotNull Predicate<TamagotchiBuddy> preventionCondition) {
            this.preventionCondition = preventionCondition;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @param isTemporary If the state should automatically end after its max duration or if it should play endlessly until ended manually. Default is false.
         */
        public Builder temporaryState(boolean isTemporary) {
            this.isTemporaryState = isTemporary;
            return this;
        }

        /**
         * Sets the duration for temporary states.
         *
         * @param minDuration The min duration in render ticks that the state should be active
         * @param maxDuration The max duration in render ticks that the state should be active
         */
        public Builder duration(int minDuration, int maxDuration) {
            this.minDuration = Math.max(0, minDuration);
            this.maxDuration = Math.max(0, maxDuration);
            if (this.minDuration > this.maxDuration) this.minDuration = this.maxDuration;
            return this;
        }

        public Builder durationRandomizer(@NotNull DurationRandomizer durationRandomizer) {
            this.durationRandomizer = durationRandomizer;
            return this;
        }

        /**
         * @param walkingSpeed The walking speed. Higher values mean faster moving speed. Default speed is 2.
         */
        public Builder walkingSpeed(@NotNull WalkingSpeedSupplier walkingSpeed) {
            this.walkingSpeed = walkingSpeed;
            return this;
        }

        /**
         * Locks buddy in this state until the state finished playing. Default is false.
         */
        public Builder lockStateUntilFinished(boolean lock) {
            this.lockStateUntilFinished = lock;
            return this;
        }

        /**
         * If this state can ignore states that are locked.
         */
        public Builder ignoresLockedState(boolean ignoresLockedState) {
            this.ignoresLockedState = ignoresLockedState;
            return this;
        }

        /**
         * If there should be a cooldown in MS after the state got successfully activated. Default is no cooldown (0).
         */
        public Builder cooldown(long cooldownMillis) {
            this.cooldown = cooldownMillis;
            return this;
        }

        public Builder onDeactivate(StateEndAction onDeactivate) {
            this.onDeactivate = onDeactivate;
            return this;
        }

        public AnimationState build() {
            return new AnimationState(
                    name, atlasIndex, animationSpeed, allowsMovement, allowsHopping,
                    activationCondition, preventionCondition, priority, isTemporaryState,
                    minDuration, maxDuration, durationRandomizer, walkingSpeed,
                    lockStateUntilFinished, ignoresLockedState, cooldown, onDeactivate
            );
        }

    }

}