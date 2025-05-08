package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a level in the buddy leveling system.
 * Each level requires more experience to achieve and unlocks new abilities.
 */
public class BuddyLevel {

    private final int level;
    private final int experienceRequired;
    private final String title;
    private final String description;
    private final BuddyUnlock[] unlocks;

    /**
     * Creates a new buddy level.
     *
     * @param level The numeric level (1-based)
     * @param experienceRequired The experience points required to reach this level
     * @param title The title for this level
     * @param description A description of what this level represents
     * @param unlocks The gameplay elements that get unlocked at this level
     */
    public BuddyLevel(int level, int experienceRequired, @NotNull String title, @NotNull String description, BuddyUnlock... unlocks) {
        this.level = level;
        this.experienceRequired = experienceRequired;
        this.title = title;
        this.description = description;
        this.unlocks = unlocks;
    }

    /**
     * @return The numeric level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return The experience points required to reach this level
     */
    public int getExperienceRequired() {
        return experienceRequired;
    }

    /**
     * @return The title for this level
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return A description of what this level represents
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The unlocks available at this level
     */
    public BuddyUnlock[] getUnlocks() {
        return unlocks;
    }

    /**
     * Represents something that gets unlocked when a buddy reaches a specific level.
     */
    public enum BuddyUnlock {
        // Basic abilities
        BASIC_TRICKS("Basic Tricks", "Unlocks simple tricks like spinning and jumping"),
        ADVANCED_TRICKS("Advanced Tricks", "Unlocks complex tricks like backflips and dancing"),
        
        // Skills
        CLEANUP_SKILL("Auto-Cleanup", "Buddy will automatically clean up its messes"),
        MOOD_BOOST_SKILL("Mood Booster", "Buddy can improve your mood with jokes and fun facts"),
        
        // Cosmetic
        COLOR_CUSTOMIZATION("Color Customization", "Customize your buddy's colors"),
        ACCESSORY_SLOT("Accessory Slot", "Add accessories to your buddy"),
        PARTICLE_EFFECTS("Special Effects", "Add particle effects to your buddy"),
        CUSTOM_ANIMATIONS("Custom Animations", "Create or import custom animations"),
        
        // Mini-games
        DANCE_GAME("Dance Game", "Play a dancing game with your buddy"),
        FOLLOW_ME("Follow Me", "Buddy follows your cursor around the screen"),
        TRICK_PERFORMANCE("Trick Performance", "Watch your buddy perform a sequence of tricks"),
        MOOD_GAME("Mood Game", "A game where you need to keep your buddy happy"),
        
        // Stat boosts
        ENERGY_BOOST("Energy Boost", "Increases maximum energy by 25%"),
        HAPPINESS_BOOST("Happiness Boost", "Happiness decreases 30% slower"),
        HUNGER_EFFICIENCY("Efficient Metabolism", "Food provides 25% more satisfaction"),
        LEARNING_BOOST("Quick Learner", "Gain 25% more experience from activities"),
        
        // Advanced features
        CUSTOM_BEHAVIOR("Behavior Programming", "Program custom behaviors for your buddy"),
        
        // Special abilities
        SOUND_EFFECTS("Sound Effects", "Add sound effects to your buddy's actions"),
        SEASONAL_OUTFITS("Seasonal Outfits", "Buddy changes appearance with the seasons"),
        DAY_NIGHT_CYCLE("Day-Night Cycle", "Buddy follows day and night cycle"),
        WEATHER_REACTIONS("Weather Reactions", "Buddy reacts to real-world weather");
        
        private final String name;
        private final String description;
        
        BuddyUnlock(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
}