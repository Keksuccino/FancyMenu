package de.keksuccino.fancymenu.customization.layout.editor.buddy.animation;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy.MAX_POOPS_BEFORE_SAD;

public class AnimationStates {

    private static final Map<String, AnimationState> STATE_MAP = new HashMap<>();

    private static final int ATLAS_INDEX_IDLE_WALK = 0; // currently used for walking and chasing ball
    private static final int ATLAS_INDEX_BEING_PET = 1;
    private static final int ATLAS_INDEX_SAD_WALK = 2;
    private static final int ATLAS_INDEX_EATING_STAND = 3;
    private static final int ATLAS_INDEX_PLAYING_WITH_BALL_STAND = 4;
    private static final int ATLAS_INDEX_SLEEPING = 5;
    private static final int ATLAS_INDEX_IDLE_STAND = 6;
    private static final int ATLAS_INDEX_SAD_STAND = 7;
    private static final int ATLAS_INDEX_STRETCHING_STANDING = 8;
    private static final int ATLAS_INDEX_EXCITED_WALKING = 9;
    private static final int ATLAS_INDEX_POOPING_STANDING = 10;
    private static final int ATLAS_INDEX_LOOKING_AROUND_STANDING = 11;
    private static final int ATLAS_INDEX_GRUMPY_STAND = 12;
    private static final int ATLAS_INDEX_SLEEPY_WALKING = 13;
    private static final int ATLAS_INDEX_SITTING = 14;
    private static final int ATLAS_INDEX_WAVING = 15;
    private static final int ATLAS_INDEX_YAWNING = 16;
    private static final int ATLAS_INDEX_PEEKING = 17;

    public static final AnimationState PEEKING = registerState(new AnimationState.Builder("PEEKING", ATLAS_INDEX_PEEKING)
            .priority(110) // Highest priority
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 20) // Slow animation for peeking
            .activationCondition(buddy -> buddy.isPeeking && buddy.isActivelyPeeking)
            .preventionCondition(buddy -> !buddy.hasBeenAwakened ? false : (buddy.needsFood || buddy.needsPet || buddy.needsPlay || buddy.isSleepy))
            .ignoresLockedState(true)
            .build());

    public static final AnimationState POOPING_STANDING = registerState(new AnimationState.Builder("POOPING_STANDING", ATLAS_INDEX_POOPING_STANDING)
            .priority(100)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isPooping)
            .temporaryState(true)
            .duration(60, 60) // 3 seconds for pooping
            .ignoresLockedState(true)
            .build());

    public static final AnimationState SLEEPING = registerState(new AnimationState.Builder("SLEEPING", ATLAS_INDEX_SLEEPING)
            .priority(90)
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 15) // Slower breathing animation
            .activationCondition(buddy -> buddy.isSleeping)
            .ignoresLockedState(true)
            .build());

    public static final AnimationState GRUMPY_STANDING = registerState(new AnimationState.Builder("GRUMPY_STANDING", ATLAS_INDEX_GRUMPY_STAND)
            .priority(80)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isGrumpy)
            .temporaryState(true)
            .duration(60, 60) // 3 seconds for grumpy
            .build());

    public static final AnimationState EATING_STANDING = registerState(new AnimationState.Builder("EATING_STANDING", ATLAS_INDEX_EATING_STAND)
            .priority(70)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isEating)
            .temporaryState(true)
            .duration(60, 60) // 3 seconds for eating
            .ignoresLockedState(true)
            .build());

    public static final AnimationState BEING_PET = registerState(new AnimationState.Builder("BEING_PET", ATLAS_INDEX_BEING_PET)
            .priority(70)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isBeingPet)
            .temporaryState(true)
            .duration(40, 40) // 2 seconds for being pet
            .ignoresLockedState(true)
            .build());

    public static final AnimationState PLAYING_WITH_BALL_STANDING = registerState(new AnimationState.Builder("PLAYING_WITH_BALL_STANDING", ATLAS_INDEX_PLAYING_WITH_BALL_STAND)
            .priority(60)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isPlaying && buddy.isHoldingBall)
            .ignoresLockedState(true)
            .build());

    public static final AnimationState CHASING_BALL = registerState(new AnimationState.Builder("CHASING_BALL", ATLAS_INDEX_IDLE_WALK)
            .priority(59)
            .walkingSpeed((buddy, state) -> 5)
            .activationCondition(buddy -> buddy.isChasingBall)
            .ignoresLockedState(true)
            .build());

    public static final AnimationState YAWNING_STANDING = registerState(new AnimationState.Builder("YAWNING_STANDING", ATLAS_INDEX_YAWNING)
            .priority(40)
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 25)
            .activationCondition(buddy -> buddy.isYawning)
            .temporaryState(true)
            .duration(50, 70) // 2.5-3.5 seconds at 20 ticks per second
            .build());

    public static final AnimationState WAVING_STANDING = registerState(new AnimationState.Builder("WAVING_STANDING", ATLAS_INDEX_WAVING)
            .priority(55)
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 5)
            .activationCondition(buddy -> buddy.isWaving)
            .preventionCondition(buddy -> buddy.isSad() || buddy.isSleepy)
            .temporaryState(true)
            .duration(40, 60) // 2-3 seconds at 20 ticks per second
            .build());

    public static final AnimationState SITTING = registerState(new AnimationState.Builder("SITTING", ATLAS_INDEX_SITTING)
            .priority(30)
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 10) // Moderate animation speed for sitting
            .activationCondition(buddy -> buddy.isSitting)
            .temporaryState(true)
            .duration(80, 240) // 4-12 seconds at 20 ticks per second
            .build());

    public static final AnimationState LOOKING_AROUND_STANDING = registerState(new AnimationState.Builder("LOOKING_AROUND_STANDING", ATLAS_INDEX_LOOKING_AROUND_STANDING)
            .priority(29)
            .allowsMovement(false)
            .allowsHopping(false)
            .animationSpeed((buddy, state) -> 80)
            .activationCondition(buddy -> buddy.isLookingAround)
            .preventionCondition(buddy -> buddy.isSad() || buddy.isSleepy)
            .temporaryState(true)
            .duration(100, 160)
            .build());

    public static final AnimationState STANDING_SAD = registerState(new AnimationState.Builder("STANDING_SAD", ATLAS_INDEX_SAD_STAND)
            .priority(21)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isStanding && (
                    buddy.needsFood || buddy.needsPet ||
                            (buddy.needsPlay && !buddy.isPlaying) ||
                            buddy.poops.size() >= MAX_POOPS_BEFORE_SAD))
            .preventionCondition(buddy -> buddy.isSleepy || buddy.isOffScreen)
            .temporaryState(true)
            .duration(70, 150)
            .build());

    public static final AnimationState STRETCHING_STANDING = registerState(new AnimationState.Builder("STRETCHING_STANDING", ATLAS_INDEX_STRETCHING_STANDING)
            .priority(20)
            .allowsMovement(false)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isStretching)
            .preventionCondition(buddy -> buddy.isSleepy)
            .temporaryState(true)
            .duration(50, 60)
            .build());

    public static final AnimationState STANDING = registerState(new AnimationState.Builder("STANDING", ATLAS_INDEX_IDLE_STAND)
            .priority(19)
            .allowsMovement(false)
            .activationCondition(buddy -> buddy.isStanding)
            .preventionCondition(buddy -> buddy.isSleepy || buddy.isOffScreen)
            .temporaryState(true)
            .duration(50, 100)
            .build());

    public static final AnimationState WALKING_SAD = registerState(new AnimationState.Builder("WALKING_SAD", ATLAS_INDEX_SAD_WALK)
            .priority(10)
            .allowsHopping(false)
            .walkingSpeed((buddy, state) -> 1)
            .activationCondition(buddy -> buddy.isSad() && !buddy.isPlaying && !buddy.isChasingBall)
            .build());

    public static final AnimationState WALKING_SLEEPY = registerState(new AnimationState.Builder("WALKING_SLEEPY", ATLAS_INDEX_SLEEPY_WALKING)
            .priority(9)
            .animationSpeed((buddy, state) -> 25)
            .walkingSpeed((buddy, state) -> 1)
            .allowsHopping(false)
            .activationCondition(buddy -> buddy.isSleepy)
            .build());

    public static final AnimationState WALKING_EXCITED = registerState(new AnimationState.Builder("WALKING_EXCITED", ATLAS_INDEX_EXCITED_WALKING)
            .priority(8)
            .walkingSpeed((buddy, state) -> 4)
            .activationCondition(buddy -> buddy.isExcited)
            .preventionCondition(buddy -> buddy.isSleepy)
            .temporaryState(true)
            .duration(70, 100)
            .build());

    public static final AnimationState RUNNING = registerState(new AnimationState.Builder("RUNNING", ATLAS_INDEX_IDLE_WALK)
            .priority(1)
            .animationSpeed((buddy, state) -> 3)
            .walkingSpeed((buddy, state) -> 7)
            .activationCondition(buddy -> (buddy.energy >= 50) && buddy.chanceCheck(0.02f)) // Adjusted from 70 to 50 for new energy scale
            .preventionCondition(buddy -> buddy.isSad() || buddy.isSleepy)
            .temporaryState(true)
            .duration(30, 50)
            .lockStateUntilFinished(true)
            .cooldown(60000) // After activating, go on cooldown for 60 seconds
            .build());

    public static final AnimationState WALKING = registerState(new AnimationState.Builder("WALKING", ATLAS_INDEX_IDLE_WALK)
            .priority(0)
            .walkingSpeed((buddy, state) -> (buddy.energy < 10) ? 1 : 2) // Walks slower when very tired (adjusted from 45)
            .activationCondition(buddy -> true) // Always valid if no other state applies
            .preventionCondition(buddy -> buddy.isSleepy)
            .build());

    private static List<AnimationState> cachedSortedStates = null;

    @NotNull
    public static AnimationState registerState(@NotNull AnimationState state) {
        STATE_MAP.put(state.getName(), state);
        cachedSortedStates = null;
        return state;
    }

    @Nullable
    public static AnimationState getStateByName(@NotNull String name) {
        return STATE_MAP.get(name);
    }

    @NotNull
    public static List<AnimationState> getStates() {
        if (cachedSortedStates == null) {
            List<AnimationState> l = new ArrayList<>(STATE_MAP.values());
            l.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            cachedSortedStates = l;
        }
        return cachedSortedStates;
    }

    @NotNull
    public static AnimationState findFirstValidStateFor(@NotNull TamagotchiBuddy buddy) {
        AnimationState selectedState = null;
        for (AnimationState state : AnimationStates.getStates()) {
            if (state.canActivate(buddy)) {
                selectedState = state;
                break;
            }
        }
        if (selectedState == null) selectedState = WALKING;
        return selectedState;
    }

}