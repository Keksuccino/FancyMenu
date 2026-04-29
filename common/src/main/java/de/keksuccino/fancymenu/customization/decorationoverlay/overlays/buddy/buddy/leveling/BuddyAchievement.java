package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.leveling;

import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents an achievement that the buddy can earn through various actions.
 * Achievements provide rewards like experience points, attribute bonuses, or special unlocks.
 */
public class BuddyAchievement {
    
    private final AchievementType type;
    private final String descriptionKey;
    private final int experienceReward;
    private final Consumer<LevelingManager> customRewardAction;
    private boolean unlocked;
    private long unlockTimestamp;
    
    /**
     * Creates a new buddy achievement.
     *
     * @param type The achievement type
     * @param descriptionKey A translation key for the achievement description
     * @param experienceReward The experience points awarded for completing this achievement
     * @param customRewardAction A custom action to perform when the achievement is completed (can be null)
     */
    public BuddyAchievement(@NotNull AchievementType type, @NotNull String descriptionKey, int experienceReward,
                           @Nullable Consumer<LevelingManager> customRewardAction) {
        this.type = type;
        this.descriptionKey = descriptionKey;
        this.experienceReward = Math.max(0, experienceReward);
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
     * @return A localized description of the achievement
     */
    public String getDescription() {
        return I18n.get(descriptionKey);
    }

    /**
     * @return The translation key for this achievement's description
     */
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * @return The experience points awarded for completing this achievement
     */
    public int getExperienceReward() {
        return experienceReward;
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
        FIRST_STEPS("fancymenu.buddy.achievement.first_steps.name", "fancymenu.buddy.achievement.first_steps.desc", 1),
        FRIENDLY_TOUCH("fancymenu.buddy.achievement.friendly_touch.name", "fancymenu.buddy.achievement.friendly_touch.desc", 1),
        CARETAKER("fancymenu.buddy.achievement.caretaker.name", "fancymenu.buddy.achievement.caretaker.desc", 1),
        PLAYFUL_FRIEND("fancymenu.buddy.achievement.playful_friend.name", "fancymenu.buddy.achievement.playful_friend.desc", 1),
        CLEANUP_CREW("fancymenu.buddy.achievement.cleanup_crew.name", "fancymenu.buddy.achievement.cleanup_crew.desc", 1),
        
        // Intermediate achievements
        BEST_BUDDIES("fancymenu.buddy.achievement.best_buddies.name", "fancymenu.buddy.achievement.best_buddies.desc", 2),
        WELL_FED("fancymenu.buddy.achievement.well_fed.name", "fancymenu.buddy.achievement.well_fed.desc", 2),
        ENERGIZER("fancymenu.buddy.achievement.energizer.name", "fancymenu.buddy.achievement.energizer.desc", 2),
        FUN_TIMES("fancymenu.buddy.achievement.fun_times.name", "fancymenu.buddy.achievement.fun_times.desc", 2),
        MARATHON_SESSION("fancymenu.buddy.achievement.marathon_session.name", "fancymenu.buddy.achievement.marathon_session.desc", 2),
        
        // Advanced achievements
        MASTER_TRAINER("fancymenu.buddy.achievement.master_trainer.name", "fancymenu.buddy.achievement.master_trainer.desc", 3),
        COMPLETION_COLLECTOR("fancymenu.buddy.achievement.completion_collector.name", "fancymenu.buddy.achievement.completion_collector.desc", 3),
        
        // Special achievements
        MIDNIGHT_COMPANION("fancymenu.buddy.achievement.midnight_companion.name", "fancymenu.buddy.achievement.midnight_companion.desc", 4),
        DESIGN_MARATHON("fancymenu.buddy.achievement.design_marathon.name", "fancymenu.buddy.achievement.design_marathon.desc", 4),
        LOYAL_FRIEND("fancymenu.buddy.achievement.loyal_friend.name", "fancymenu.buddy.achievement.loyal_friend.desc", 4),
        
        // Master achievements
        PERFECT_HARMONY("fancymenu.buddy.achievement.perfect_harmony.name", "fancymenu.buddy.achievement.perfect_harmony.desc", 5),
        ACHIEVEMENT_HUNTER("fancymenu.buddy.achievement.achievement_hunter.name", "fancymenu.buddy.achievement.achievement_hunter.desc", 5);
        
        private final String nameKey;
        private final String descriptionKey;
        private final int tier; // 1-5, with 5 being the hardest/rarest
        
        AchievementType(String nameKey, String descriptionKey, int tier) {
            this.nameKey = nameKey;
            this.descriptionKey = descriptionKey;
            this.tier = tier;
        }
        
        public String getName() {
            return I18n.get(nameKey);
        }

        public String getNameKey() {
            return nameKey;
        }
        
        public String getDefaultDescription() {
            return I18n.get(descriptionKey);
        }

        public String getDescriptionKey() {
            return descriptionKey;
        }
        
        public int getTier() {
            return tier;
        }

    }

}
