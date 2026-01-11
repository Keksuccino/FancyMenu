package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy;

/**
 * Immutable tuning values for Buddy's stat changes.
 */
public class BuddyStatConfig {

    public final float hungerDecayPerTick;
    public final float happinessDecayPerTick;
    public final float funDecayPerTick;
    public final float energyDecayPerTick;
    public final float energySleepRegenPerTick;

    public final float playEnergyDrainPerTick;
    public final float playFunGainPerTick;
    public final float playHappinessGainPerTick;
    public final float playHungerDrainPerTick;

    public final float chaseEnergyDrainPerTick;
    public final float chaseHungerDrainPerTick;

    public final float hopEnergyDrainPerTick;
    public final float hopHungerDrainPerTick;

    public final float excitedEnergyDrainPerTick;
    public final float excitedHappinessGainPerTick;
    public final float excitedHungerDrainPerTick;

    public final float runningEnergyDrainPerTick;
    public final float runningHungerDrainPerTick;

    public final float foodHungerGain;
    public final float foodHappinessGain;
    public final float petHappinessGain;
    public final float wakeupHappinessPenalty;

    public BuddyStatConfig(float hungerDecayPerTick,
                           float happinessDecayPerTick,
                           float funDecayPerTick,
                           float energyDecayPerTick,
                           float energySleepRegenPerTick,
                           float playEnergyDrainPerTick,
                           float playFunGainPerTick,
                           float playHappinessGainPerTick,
                           float playHungerDrainPerTick,
                           float chaseEnergyDrainPerTick,
                           float chaseHungerDrainPerTick,
                           float hopEnergyDrainPerTick,
                           float hopHungerDrainPerTick,
                           float excitedEnergyDrainPerTick,
                           float excitedHappinessGainPerTick,
                           float excitedHungerDrainPerTick,
                           float runningEnergyDrainPerTick,
                           float runningHungerDrainPerTick,
                           float foodHungerGain,
                           float foodHappinessGain,
                           float petHappinessGain,
                           float wakeupHappinessPenalty) {
        this.hungerDecayPerTick = hungerDecayPerTick;
        this.happinessDecayPerTick = happinessDecayPerTick;
        this.funDecayPerTick = funDecayPerTick;
        this.energyDecayPerTick = energyDecayPerTick;
        this.energySleepRegenPerTick = energySleepRegenPerTick;
        this.playEnergyDrainPerTick = playEnergyDrainPerTick;
        this.playFunGainPerTick = playFunGainPerTick;
        this.playHappinessGainPerTick = playHappinessGainPerTick;
        this.playHungerDrainPerTick = playHungerDrainPerTick;
        this.chaseEnergyDrainPerTick = chaseEnergyDrainPerTick;
        this.chaseHungerDrainPerTick = chaseHungerDrainPerTick;
        this.hopEnergyDrainPerTick = hopEnergyDrainPerTick;
        this.hopHungerDrainPerTick = hopHungerDrainPerTick;
        this.excitedEnergyDrainPerTick = excitedEnergyDrainPerTick;
        this.excitedHappinessGainPerTick = excitedHappinessGainPerTick;
        this.excitedHungerDrainPerTick = excitedHungerDrainPerTick;
        this.runningEnergyDrainPerTick = runningEnergyDrainPerTick;
        this.runningHungerDrainPerTick = runningHungerDrainPerTick;
        this.foodHungerGain = foodHungerGain;
        this.foodHappinessGain = foodHappinessGain;
        this.petHappinessGain = petHappinessGain;
        this.wakeupHappinessPenalty = wakeupHappinessPenalty;
    }

    public static BuddyStatConfig defaults() {
        return new BuddyStatConfig(
                Buddy.DEFAULT_HUNGER_DECAY_PER_TICK,
                Buddy.DEFAULT_HAPPINESS_DECAY_PER_TICK,
                Buddy.DEFAULT_FUN_DECAY_PER_TICK,
                Buddy.DEFAULT_ENERGY_DECAY_PER_TICK,
                Buddy.DEFAULT_ENERGY_SLEEP_REGEN_PER_TICK,
                Buddy.DEFAULT_PLAY_ENERGY_DRAIN_PER_TICK,
                Buddy.DEFAULT_PLAY_FUN_GAIN_PER_TICK,
                Buddy.DEFAULT_PLAY_HAPPINESS_GAIN_PER_TICK,
                Buddy.DEFAULT_PLAY_HUNGER_DRAIN_PER_TICK,
                Buddy.DEFAULT_CHASE_ENERGY_DRAIN_PER_TICK,
                Buddy.DEFAULT_CHASE_HUNGER_DRAIN_PER_TICK,
                Buddy.DEFAULT_HOP_ENERGY_DRAIN_PER_TICK,
                Buddy.DEFAULT_HOP_HUNGER_DRAIN_PER_TICK,
                Buddy.DEFAULT_EXCITED_ENERGY_DRAIN_PER_TICK,
                Buddy.DEFAULT_EXCITED_HAPPINESS_GAIN_PER_TICK,
                Buddy.DEFAULT_EXCITED_HUNGER_DRAIN_PER_TICK,
                Buddy.DEFAULT_RUNNING_ENERGY_DRAIN_PER_TICK,
                Buddy.DEFAULT_RUNNING_HUNGER_DRAIN_PER_TICK,
                Buddy.DEFAULT_FOOD_HUNGER_GAIN,
                Buddy.DEFAULT_FOOD_HAPPINESS_GAIN,
                Buddy.DEFAULT_PET_HAPPINESS_GAIN,
                Buddy.DEFAULT_WAKEUP_HAPPINESS_PENALTY
        );
    }
}
