package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an achievement that the buddy can earn through various actions.
 * Achievements provide rewards like experience points, attribute bonuses, or special unlocks.
 */
public class BuddyAchievement {
    
    private final AchievementType type;
    private final String description;
    private final int experienceReward;
    private final BuddyAttribute.AttributeType attributeRewardType;
    private final int attributeRewardPoints;
    private final BuddyLevel.BuddyUnlock unlockReward;
    private final Consumer<LevelingManager> customRewardAction;
    private boolean unlocked;
    private long unlockTimestamp;
    
    /**
     * Creates a new buddy achievement.
     *
     * @param type The achievement type
     * @param description A description of the achievement
     * @param experienceReward The experience points awarded for completing this achievement
     * @param attributeRewardType The attribute type to reward (can be null)
     * @param attributeRewardPoints The number of attribute points to award
     * @param unlockReward A special unlock awarded for completing this achievement (can be null)
     * @param customRewardAction A custom action to perform when the achievement is completed (can be null)
     */
    public BuddyAchievement(@NotNull AchievementType type, @NotNull String description, int experienceReward,
                           @Nullable BuddyAttribute.AttributeType attributeRewardType, int attributeRewardPoints,
                           @Nullable BuddyLevel.BuddyUnlock unlockReward,
                           @Nullable Consumer<LevelingManager> customRewardAction) {
        this.type = type;
        this.description = description;
        this.experienceReward = Math.max(0, experienceReward);
        this.attributeRewardType = attributeRewardType;
        this.attributeRewardPoints = Math.max(0, attributeRewardPoints);
        this.unlockReward = unlockReward;
        this.customRewardAction = customRewardAction;
        this.unlocked = false;
        this.unlockTimestamp = 0;
    }
    
    /**
     * @return The achievement type
     */
    public AchievementType getType() {
        return type;
    }
    
    /**
     * @return A description of the achievement
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return The experience points awarded for completing this achievement
     */
    public int getExperienceReward() {
        return experienceReward;
    }
    
    /**
     * @return The attribute type to reward (can be null)
     */
    public BuddyAttribute.AttributeType getAttributeRewardType() {
        return attributeRewardType;
    }
    
    /**
     * @return The number of attribute points to award
     */
    public int getAttributeRewardPoints() {
        return attributeRewardPoints;
    }
    
    /**
     * @return A special unlock awarded for completing this achievement (can be null)
     */
    public BuddyLevel.BuddyUnlock getUnlockReward() {
        return unlockReward;
    }
    
    /**
     * @return Whether this achievement has been unlocked
     */
    public boolean isUnlocked() {
        return unlocked;
    }
    
    /**
     * @return The timestamp when this achievement was unlocked (0 if not unlocked)
     */
    public long getUnlockTimestamp() {
        return unlockTimestamp;
    }
    
    /**
     * Unlocks this achievement and applies its rewards to the leveling manager.
     *
     * @param levelingManager The leveling manager to apply rewards to
     * @return True if the achievement was newly unlocked, false if already unlocked
     */
    public boolean unlock(@NotNull LevelingManager levelingManager) {
        if (unlocked) {
            return false;
        }
        
        unlocked = true;
        unlockTimestamp = System.currentTimeMillis();
        
        // Apply experience reward
        if (experienceReward > 0) {
            levelingManager.addExperience(experienceReward);
        }
        
        // Apply attribute reward
        if (attributeRewardType != null && attributeRewardPoints > 0) {
            levelingManager.addAttributePoints(attributeRewardType, attributeRewardPoints);
        }
        
        // Apply unlock reward
        if (unlockReward != null) {
            levelingManager.addUnlock(unlockReward);
        }
        
        // Apply custom reward action
        if (customRewardAction != null) {
            customRewardAction.accept(levelingManager);
        }
        
        return true;
    }
    
    /**
     * Resets this achievement to the unlocked state.
     */
    public void reset() {
        this.unlocked = false;
        this.unlockTimestamp = 0;
    }
    
    /**
     * The different types of achievements that a buddy can earn.
     */
    public enum AchievementType {
        // Basic achievements
        FIRST_STEPS("First Steps", "First interaction with your buddy", 1),
        FRIENDLY_TOUCH("Friendly Touch", "Pet your buddy 10 times", 1),
        CARETAKER("Caretaker", "Feed your buddy 5 times", 1),
        PLAYFUL_FRIEND("Playful Friend", "Play with your buddy for the first time", 1),
        CLEANUP_CREW("Cleanup Crew", "Clean up 5 poops", 1),
        
        // Intermediate achievements
        BEST_BUDDIES("Best Buddies", "Reach maximum happiness with your buddy", 2),
        WELL_FED("Well Fed", "Keep your buddy perfectly fed for a full session", 2),
        ENERGIZER("Energizer", "Keep your buddy's energy above 80% for an entire session", 2),
        FUN_TIMES("Fun Times", "Reach maximum fun level with your buddy", 2),
        MARATHON_SESSION("Marathon Session", "Use the layout editor with your buddy for over an hour", 2),
        
        // Advanced achievements
        MASTER_TRAINER("Master Trainer", "Reach level 10 with your buddy", 3),
        ATTRIBUTE_EXPERT("Attribute Expert", "Get one attribute to maximum level", 3),
        SKILL_SPECIALIST("Skill Specialist", "Master any skill to its highest level", 3),
        COMPLETION_COLLECTOR("Completion Collector", "Unlock all basic achievements", 3),
        
        // Special achievements
        MIDNIGHT_COMPANION("Midnight Companion", "Work with your buddy after midnight", 4),
        DESIGN_MARATHON("Design Marathon", "Create 10 layouts with your buddy present", 4),
        LOYAL_FRIEND("Loyal Friend", "Interact with your buddy every day for a week", 4),
        SECRET_DANCE("Secret Dance", "Discover the buddy's secret dance animation", 4),
        TELEPATHIC_BOND("Telepathic Bond", "Get your buddy to predict your actions correctly 5 times", 4),
        
        // Master achievements
        BUDDY_WHISPERER("Buddy Whisperer", "Max out all attributes", 5),
        SKILL_MASTER("Skill Master", "Unlock all skills", 5),
        PERFECT_HARMONY("Perfect Harmony", "Keep all buddy stats above 90% for an entire session", 5),
        ACHIEVEMENT_HUNTER("Achievement Hunter", "Unlock all other achievements", 5);
        
        private final String name;
        private final String defaultDescription;
        private final int tier; // 1-5, with 5 being the hardest/rarest
        
        AchievementType(String name, String defaultDescription, int tier) {
            this.name = name;
            this.defaultDescription = defaultDescription;
            this.tier = tier;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDefaultDescription() {
            return defaultDescription;
        }
        
        public int getTier() {
            return tier;
        }
    }
}