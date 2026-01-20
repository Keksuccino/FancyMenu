package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.leveling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.Buddy;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.BuddySaveFileNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Manages the leveling system for the buddy, including experience, levels, attributes, and skills.
 */
public class LevelingManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILENAME_PREFIX = "buddy_leveling_";
    private static final File BUDDY_DIR = new File(FancyMenu.INSTANCE_DATA_DIR, "buddy");

    // Constants for the leveling system
    private static final int MAX_LEVEL = 30;
    private static final int BASE_XP_PER_LEVEL = 100;
    private static final float LEVEL_SCALING_FACTOR = 1.5f;

    // Reference to the buddy
    private final Buddy buddy;

    // Leveling data
    private int currentLevel = 1;
    private int experience = 0;

    // Achievements
    private final Map<BuddyAchievement.AchievementType, BuddyAchievement> achievements = new EnumMap<>(BuddyAchievement.AchievementType.class);

    // Level definitions - simplified
    private final int[] levelExperience = new int[MAX_LEVEL + 1];

    // Activity tracking for achievements
    private int petCount = 0;
    private int feedCount = 0;
    private int playCount = 0;
    private int poopCleanCount = 0;
    private long totalSessionTime = 0;
    private long lastSessionStart = 0;
    private int layoutCreationCount = 0;

    /**
     * Creates a new leveling manager for the buddy.
     *
     * @param buddy The buddy to manage leveling for
     */
    public LevelingManager(@NotNull Buddy buddy) {
        this.buddy = buddy;
        
        // Attributes initialization removed
        
        // Initialize achievements
        initializeAchievements();
        
        // Initialize level experience requirements
        initializeLevelExperience();
        
        // Start session time tracking
        this.lastSessionStart = System.currentTimeMillis();
    }
    
    // Attributes initialization method removed
    
    /**
     * Initializes the level experience requirements.
     */
    private void initializeLevelExperience() {
        for (int i = 1; i <= MAX_LEVEL; i++) {
            levelExperience[i] = calculateExperienceForLevel(i);
        }
    }
    
    /**
     * Initializes the achievements with their rewards.
     */
    private void initializeAchievements() {
        // Basic achievements
        achievements.put(BuddyAchievement.AchievementType.FIRST_STEPS, new BuddyAchievement(
                BuddyAchievement.AchievementType.FIRST_STEPS,
                BuddyAchievement.AchievementType.FIRST_STEPS.getDescriptionKey(),
                50, null));
                
        achievements.put(BuddyAchievement.AchievementType.FRIENDLY_TOUCH, new BuddyAchievement(
                BuddyAchievement.AchievementType.FRIENDLY_TOUCH,
                BuddyAchievement.AchievementType.FRIENDLY_TOUCH.getDescriptionKey(),
                100, null));
                
        achievements.put(BuddyAchievement.AchievementType.CARETAKER, new BuddyAchievement(
                BuddyAchievement.AchievementType.CARETAKER,
                BuddyAchievement.AchievementType.CARETAKER.getDescriptionKey(),
                100, null));
                
        achievements.put(BuddyAchievement.AchievementType.PLAYFUL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.PLAYFUL_FRIEND,
                BuddyAchievement.AchievementType.PLAYFUL_FRIEND.getDescriptionKey(),
                75, null));
                
        achievements.put(BuddyAchievement.AchievementType.CLEANUP_CREW, new BuddyAchievement(
                BuddyAchievement.AchievementType.CLEANUP_CREW,
                BuddyAchievement.AchievementType.CLEANUP_CREW.getDescriptionKey(),
                75, null));
                
        // Intermediate achievements
        achievements.put(BuddyAchievement.AchievementType.BEST_BUDDIES, new BuddyAchievement(
                BuddyAchievement.AchievementType.BEST_BUDDIES,
                BuddyAchievement.AchievementType.BEST_BUDDIES.getDescriptionKey(),
                200, null));
                
        achievements.put(BuddyAchievement.AchievementType.WELL_FED, new BuddyAchievement(
                BuddyAchievement.AchievementType.WELL_FED,
                BuddyAchievement.AchievementType.WELL_FED.getDescriptionKey(),
                200, null));
                
        achievements.put(BuddyAchievement.AchievementType.ENERGIZER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ENERGIZER,
                BuddyAchievement.AchievementType.ENERGIZER.getDescriptionKey(),
                200, null));
                
        achievements.put(BuddyAchievement.AchievementType.FUN_TIMES, new BuddyAchievement(
                BuddyAchievement.AchievementType.FUN_TIMES,
                BuddyAchievement.AchievementType.FUN_TIMES.getDescriptionKey(),
                200, null));
                
        achievements.put(BuddyAchievement.AchievementType.MARATHON_SESSION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MARATHON_SESSION,
                BuddyAchievement.AchievementType.MARATHON_SESSION.getDescriptionKey(),
                300, null));
                
        // Advanced achievements
        achievements.put(BuddyAchievement.AchievementType.MASTER_TRAINER, new BuddyAchievement(
                BuddyAchievement.AchievementType.MASTER_TRAINER,
                BuddyAchievement.AchievementType.MASTER_TRAINER.getDescriptionKey(),
                500, null));
                
        achievements.put(BuddyAchievement.AchievementType.COMPLETION_COLLECTOR, new BuddyAchievement(
                BuddyAchievement.AchievementType.COMPLETION_COLLECTOR,
                BuddyAchievement.AchievementType.COMPLETION_COLLECTOR.getDescriptionKey(),
                750, null));
                
        // Special achievements
        achievements.put(BuddyAchievement.AchievementType.MIDNIGHT_COMPANION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MIDNIGHT_COMPANION,
                BuddyAchievement.AchievementType.MIDNIGHT_COMPANION.getDescriptionKey(),
                250, null));
                
        achievements.put(BuddyAchievement.AchievementType.DESIGN_MARATHON, new BuddyAchievement(
                BuddyAchievement.AchievementType.DESIGN_MARATHON,
                BuddyAchievement.AchievementType.DESIGN_MARATHON.getDescriptionKey(),
                500, null));
                
        achievements.put(BuddyAchievement.AchievementType.LOYAL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.LOYAL_FRIEND,
                BuddyAchievement.AchievementType.LOYAL_FRIEND.getDescriptionKey(),
                1000, null));
                
        achievements.put(BuddyAchievement.AchievementType.PERFECT_HARMONY, new BuddyAchievement(
                BuddyAchievement.AchievementType.PERFECT_HARMONY,
                BuddyAchievement.AchievementType.PERFECT_HARMONY.getDescriptionKey(),
                1500, null));
                
        achievements.put(BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER,
                BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER.getDescriptionKey(),
                5000, null));
    }
    
    // Level initialization moved to initializeLevelExperience
    
    /**
     * Calculates the total experience required to reach a specific level.
     *
     * @param level The level to calculate experience for
     * @return The total experience required to reach this level
     */
    private int calculateExperienceForLevel(int level) {
        if (level <= 1) return 0;
        
        int totalXp = 0;
        for (int i = 1; i < level; i++) {
            totalXp += (int)(BASE_XP_PER_LEVEL * Math.pow(i, LEVEL_SCALING_FACTOR));
        }
        return totalXp;
    }
    
    /**
     * Gets the experience required to reach the next level.
     *
     * @return The experience required for the next level, or 0 if at max level
     */
    public int getExperienceForNextLevel() {
        if (currentLevel >= MAX_LEVEL) return 0;
        return levelExperience[currentLevel + 1] - experience;
    }
    
    /**
     * Gets the percentage progress to the next level (0-100).
     *
     * @return The percentage progress to the next level, or 100 if at max level
     */
    public int getLevelProgressPercentage() {
        if (currentLevel >= MAX_LEVEL) return 100;
        
        int currentLevelXp = levelExperience[currentLevel];
        int nextLevelXp = levelExperience[currentLevel + 1];
        int levelXpRange = nextLevelXp - currentLevelXp;
        int currentLevelProgress = experience - currentLevelXp;
        
        return (levelXpRange > 0) ? (currentLevelProgress * 100 / levelXpRange) : 100;
    }
    
    /**
     * Adds experience to the buddy and handles level-ups.
     *
     * @param amount The amount of experience to add
     * @return A list of levels that were gained, empty if no level-up occurred
     */
    public List<Integer> addExperience(int amount) {
        if (amount <= 0 || currentLevel >= MAX_LEVEL) return Collections.emptyList();
        
        List<Integer> levelsGained = new ArrayList<>();
        int oldLevel = currentLevel;
        
        experience += amount;
        LOGGER.debug("Added {} XP to buddy, total XP: {}", amount, experience);
        
        // Check for level-ups
        while (currentLevel < MAX_LEVEL && experience >= levelExperience[currentLevel + 1]) {
            currentLevel++;
            levelsGained.add(currentLevel);
            
            // Attribute points award removed
            
            LOGGER.debug("Buddy leveled up to level {}!", currentLevel);
            
            // Check for level-based achievements
            checkLevelAchievements();
        }
        
        // Apply the effects of leveling up to the buddy
        if (oldLevel != currentLevel) {
            applyLevelStatBoosts();
        }
        
        return levelsGained;
    }
    
    /**
     * Applies statistical boosts based on the buddy's current level.
     */
    private void applyLevelStatBoosts() {
        // Every level improves the buddy's base stats slightly
        buddy.standChancePercentage = Math.min(2.0f, 1.0f + (currentLevel * 0.03f));
        buddy.hopChancePercentage = Math.min(1.0f, 0.3f + (currentLevel * 0.02f));
        buddy.lookChancePercentage = Math.min(0.8f, 0.2f + (currentLevel * 0.02f));
        buddy.stretchChancePercentage = Math.min(0.5f, 0.1f + (currentLevel * 0.01f));
        buddy.excitedChancePercentage = Math.min(0.5f, 0.1f + (currentLevel * 0.01f));
        buddy.poopingInterval = Math.max(4000, 6000 - (int)(200 * Math.min(10, currentLevel))); // Reduced from 1500-100 to 6000-200
        buddy.hopAnimationSpeed = 0.3f + (0.01f * Math.min(30, currentLevel));
        
        // Apply buddy effects based on level only
        applyLevelEffects();
    }
    
    /**
     * Applies effects to the buddy based on level instead of attributes.
     */
    public void applyLevelEffects() {
        // Calculate level-based effects (0.0 - 1.0 scale)
        float levelProgress = Math.min(currentLevel, MAX_LEVEL) / (float)MAX_LEVEL;
        
        // Derive effects from level
        float hungerMultiplier = 1.0f - (levelProgress * 0.5f); // Up to 50% slower hunger decrease
        float happinessMultiplier = 1.0f - (levelProgress * 0.5f); // Up to 50% slower happiness decrease
        float energyMultiplier = 1.0f - (levelProgress * 0.5f); // Up to 50% slower energy decrease
        float happinessGainMultiplier = 1.0f + (levelProgress * 0.5f); // Up to 50% more happiness gain
        float experienceMultiplier = 1.0f + (levelProgress * 0.3f); // Up to 30% more experience
        float needsUnderstandingBonus = levelProgress * 0.5f; // Up to 50% better needs understanding
        float luckBonus = levelProgress * 0.4f; // Up to 40% better luck
        
        // Store these effects for the TamagotchiBuddy to use
        buddy.setAttributeEffects(
                hungerMultiplier,
                happinessMultiplier,
                energyMultiplier,
                happinessGainMultiplier,
                experienceMultiplier,
                needsUnderstandingBonus,
                luckBonus
        );
    }
    
    /**
     * Updates the session time tracking and checks for time-based achievements.
     */
    public void updateSessionTime() {
        long currentTime = System.currentTimeMillis();
        long sessionDuration = (currentTime - lastSessionStart) / 1000; // in seconds
        totalSessionTime += sessionDuration;
        
        // Reset session start time
        lastSessionStart = currentTime;
        
        // Check for time-based achievements
        checkTimeAchievements(currentTime);
    }
    
    /**
     * Increments the pet count and checks for related achievements.
     */
    public void incrementPetCount() {
        petCount++;
        
        // First interaction achievement
        if (petCount == 1) {
            unlockAchievement(BuddyAchievement.AchievementType.FIRST_STEPS);
        }
        
        // Pet 10 times achievement
        if (petCount == 10) {
            unlockAchievement(BuddyAchievement.AchievementType.FRIENDLY_TOUCH);
        }
    }
    
    /**
     * Increments the feed count and checks for related achievements.
     */
    public void incrementFeedCount() {
        feedCount++;
        
        // Feed 5 times achievement
        if (feedCount == 5) {
            unlockAchievement(BuddyAchievement.AchievementType.CARETAKER);
        }
    }
    
    /**
     * Increments the play count and checks for related achievements.
     */
    public void incrementPlayCount() {
        playCount++;
        
        // First play achievement
        if (playCount == 1) {
            unlockAchievement(BuddyAchievement.AchievementType.PLAYFUL_FRIEND);
        }
    }
    
    /**
     * Increments the poop clean count and checks for related achievements.
     */
    public void incrementPoopCleanCount() {
        poopCleanCount++;
        
        // Clean 5 poops achievement
        if (poopCleanCount == 5) {
            unlockAchievement(BuddyAchievement.AchievementType.CLEANUP_CREW);
        }
    }
    
    /**
     * Increments the session time and checks for related achievements.
     * This method replaces the original layout creation counter with a session time threshold.
     */
    public void incrementLayoutCreationCount() {
        // Repurpose this counter to track extended sessions
        layoutCreationCount++;
        
        // Award achievement after sufficient interaction
        if (layoutCreationCount == 10 && totalSessionTime > 1800) { // 30 minutes of total session time
            unlockAchievement(BuddyAchievement.AchievementType.DESIGN_MARATHON);
        }
    }
    
    /**
     * Checks for level-based achievements.
     */
    private void checkLevelAchievements() {
        // Level 10 achievement
        if (currentLevel >= 10) {
            unlockAchievement(BuddyAchievement.AchievementType.MASTER_TRAINER);
        }
    }
    
    /**
     * Checks for time-based achievements.
     *
     * @param currentTime The current time in milliseconds
     */
    private void checkTimeAchievements(long currentTime) {
        // Check for midnight companion achievement
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 0 && hour < 4) { // Between midnight and 4 AM
            unlockAchievement(BuddyAchievement.AchievementType.MIDNIGHT_COMPANION);
        }
        
        // Check for marathon session achievement (1 hour = 3600 seconds)
        if (totalSessionTime >= 3600) {
            unlockAchievement(BuddyAchievement.AchievementType.MARATHON_SESSION);
        }
    }
    
    /**
     * Checks for stat-based achievements.
     */
    public void checkStatAchievements() {
        // Check for maximum happiness achievement
        if (buddy.getHappiness() >= 100) {
            unlockAchievement(BuddyAchievement.AchievementType.BEST_BUDDIES);
        }
        
        // Check for maximum fun level achievement
        if (buddy.getFunLevel() >= 100) {
            unlockAchievement(BuddyAchievement.AchievementType.FUN_TIMES);
        }
    }
    
    /**
     * Unlocks an achievement if it's not already unlocked.
     *
     * @param type The achievement type to unlock
     * @return True if the achievement was newly unlocked, false otherwise
     */
    public boolean unlockAchievement(BuddyAchievement.AchievementType type) {
        BuddyAchievement achievement = achievements.get(type);
        if (achievement == null || achievement.isUnlocked()) return false;
        
        boolean unlocked = achievement.unlock(this);
        
        if (unlocked) {
            LOGGER.debug("Unlocked achievement: {}", type.getName());
            
            // Check for completion collector achievement
            checkCompletionCollectorAchievement();
            
            // Check for achievement hunter achievement
            checkAchievementHunterAchievement();
        }
        
        return unlocked;
    }
    
    /**
     * Checks if the completion collector achievement should be unlocked.
     */
    private void checkCompletionCollectorAchievement() {
        // Check if all basic (tier 1) achievements are unlocked
        boolean allBasicUnlocked = true;
        for (BuddyAchievement achievement : achievements.values()) {
            if (achievement.getType().getTier() == 1 && !achievement.isUnlocked()) {
                allBasicUnlocked = false;
                break;
            }
        }
        
        if (allBasicUnlocked) {
            // Don't use the normal unlock method to avoid infinite recursion
            BuddyAchievement completionCollector = achievements.get(BuddyAchievement.AchievementType.COMPLETION_COLLECTOR);
            if (completionCollector != null && !completionCollector.isUnlocked()) {
                completionCollector.unlock(this);
                LOGGER.debug("Unlocked achievement: {}", BuddyAchievement.AchievementType.COMPLETION_COLLECTOR.getName());
            }
        }
    }
    
    /**
     * Checks if the achievement hunter achievement should be unlocked.
     */
    private void checkAchievementHunterAchievement() {
        // Check if all other achievements are unlocked
        boolean allOthersUnlocked = true;
        for (Map.Entry<BuddyAchievement.AchievementType, BuddyAchievement> entry : achievements.entrySet()) {
            if (entry.getKey() != BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER && !entry.getValue().isUnlocked()) {
                allOthersUnlocked = false;
                break;
            }
        }
        
        if (allOthersUnlocked) {
            // Don't use the normal unlock method to avoid infinite recursion
            BuddyAchievement achievementHunter = achievements.get(BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER);
            if (achievementHunter != null && !achievementHunter.isUnlocked()) {
                achievementHunter.unlock(this);
                LOGGER.debug("Unlocked achievement: {}", BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER.getName());
            }
        }
    }
    
    /**
     * Saves the leveling system state to a file.
     *
     * @return True if the save was successful, false otherwise
     */
    public boolean saveState() {
        try {
            if (!BUDDY_DIR.exists()) {
                BUDDY_DIR.mkdirs();
            }
            
            File saveFile = getSaveFile();
            JsonObject json = new JsonObject();
            
            // Save basic leveling data
            json.addProperty("level", currentLevel);
            json.addProperty("experience", experience);
            
            // Save achievements
            JsonObject achievementsObj = new JsonObject();
            for (Map.Entry<BuddyAchievement.AchievementType, BuddyAchievement> entry : achievements.entrySet()) {
                JsonObject achievementObj = new JsonObject();
                achievementObj.addProperty("unlocked", entry.getValue().isUnlocked());
                achievementObj.addProperty("unlockTimestamp", entry.getValue().getUnlockTimestamp());
                achievementsObj.add(entry.getKey().name(), achievementObj);
            }
            json.add("achievements", achievementsObj);
            
            // Save activity tracking
            JsonObject activityObj = new JsonObject();
            activityObj.addProperty("petCount", petCount);
            activityObj.addProperty("feedCount", feedCount);
            activityObj.addProperty("playCount", playCount);
            activityObj.addProperty("poopCleanCount", poopCleanCount);
            activityObj.addProperty("totalSessionTime", totalSessionTime);
            activityObj.addProperty("layoutCreationCount", layoutCreationCount);
            json.add("activity", activityObj);
            
            // Write to file
            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(json, writer);
            }
            
            LOGGER.debug("Saved buddy leveling data to {}", saveFile.getAbsolutePath());
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to save buddy leveling data", e);
            return false;
        }
    }
    
    /**
     * Loads the leveling system state from a file.
     *
     * @return True if the load was successful, false otherwise
     */
    public boolean loadState() {
        File saveFile = getSaveFile();
        if (!saveFile.exists()) {
            LOGGER.debug("No buddy leveling save file found at {}", saveFile.getAbsolutePath());
            return false;
        }
        
        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            // Load basic leveling data
            if (json.has("level")) {
                currentLevel = json.get("level").getAsInt();
                // Ensure level is within valid range
                currentLevel = Math.max(1, Math.min(MAX_LEVEL, currentLevel));
            }
            
            if (json.has("experience")) {
                experience = json.get("experience").getAsInt();
                // Ensure experience is consistent with level
                experience = Math.max(calculateExperienceForLevel(currentLevel), experience);
            }
            
            // Load achievements
            if (json.has("achievements")) {
                JsonObject achievementsObj = json.getAsJsonObject("achievements");
                for (BuddyAchievement.AchievementType type : BuddyAchievement.AchievementType.values()) {
                    if (achievementsObj.has(type.name())) {
                        JsonObject achievementObj = achievementsObj.getAsJsonObject(type.name());
                        BuddyAchievement achievement = achievements.get(type);
                        
                        if (achievement != null && achievementObj.has("unlocked") && achievementObj.get("unlocked").getAsBoolean()) {
                            // Just mark as unlocked without applying effects again
                            achievement.reset();
                            achievement.unlock(this);
                        }
                    }
                }
            }
            
            // Load activity tracking
            if (json.has("activity")) {
                JsonObject activityObj = json.getAsJsonObject("activity");
                
                if (activityObj.has("petCount")) {
                    petCount = activityObj.get("petCount").getAsInt();
                }
                
                if (activityObj.has("feedCount")) {
                    feedCount = activityObj.get("feedCount").getAsInt();
                }
                
                if (activityObj.has("playCount")) {
                    playCount = activityObj.get("playCount").getAsInt();
                }
                
                if (activityObj.has("poopCleanCount")) {
                    poopCleanCount = activityObj.get("poopCleanCount").getAsInt();
                }
                
                if (activityObj.has("totalSessionTime")) {
                    totalSessionTime = activityObj.get("totalSessionTime").getAsLong();
                }
                
                if (activityObj.has("layoutCreationCount")) {
                    layoutCreationCount = activityObj.get("layoutCreationCount").getAsInt();
                }
            }
            
            // Apply level stat boosts after loading
            applyLevelStatBoosts();
            
            LOGGER.debug("Loaded buddy leveling data from {}", saveFile.getAbsolutePath());
            LOGGER.debug("Current level: {}, Experience: {}",
                    currentLevel, experience);
            
            return true;
            
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            LOGGER.error("Failed to load buddy leveling data", e);
            return false;
        }
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public Map<BuddyAchievement.AchievementType, BuddyAchievement> getAchievements() {
        return new EnumMap<>(achievements);
    }
    
    public int getPetCount() {
        return petCount;
    }
    
    public int getFeedCount() {
        return feedCount;
    }
    
    public int getPlayCount() {
        return playCount;
    }
    
    public int getPoopCleanCount() {
        return poopCleanCount;
    }
    
    public long getTotalSessionTime() {
        return totalSessionTime;
    }
    
    public int getLayoutCreationCount() {
        return layoutCreationCount;
    }

    /**
     * Deletes the leveling save file for the given instance identifier, if it exists.
     *
     * @param instanceIdentifier the buddy instance id used for namespacing
     * @return true if the file was deleted or didn't exist; false if deletion failed
     */
    public static boolean deleteLevelingSave(@NotNull String instanceIdentifier) {
        File saveFile = new File(BUDDY_DIR, BuddySaveFileNames.buildSaveFileName(SAVE_FILENAME_PREFIX, instanceIdentifier));
        if (!saveFile.exists()) {
            return true;
        }
        boolean deleted = saveFile.delete();
        if (!deleted) {
            LOGGER.warn("Failed to delete buddy leveling save at {}", saveFile.getAbsolutePath());
        }
        return deleted;
    }

    @NotNull
    private File getSaveFile() {
        return new File(BUDDY_DIR, getSaveFileName());
    }

    @NotNull
    private String getSaveFileName() {
        return BuddySaveFileNames.buildSaveFileName(SAVE_FILENAME_PREFIX, this.buddy.getInstanceIdentifier());
    }

}
