package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAttribute.AttributeType;
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
    private static final String SAVE_FILENAME = "buddy_leveling.json";
    private static final File BUDDY_DIR = new File(FancyMenu.INSTANCE_DATA_DIR, "buddy");

    // Constants for the leveling system
    private static final int MAX_LEVEL = 30;
    private static final int BASE_XP_PER_LEVEL = 100;
    private static final float LEVEL_SCALING_FACTOR = 1.5f;
    private static final int ATTRIBUTE_POINTS_PER_LEVEL = 2;

    // Reference to the buddy
    private final TamagotchiBuddy buddy;

    // Leveling data
    private int currentLevel = 1;
    private int experience = 0;
    private int unspentAttributePoints = 0;
    // Removed skill points and unlocks

    // Attributes
    private final Map<AttributeType, BuddyAttribute> attributes = new EnumMap<>(AttributeType.class);

    // Skills - removed

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
    public LevelingManager(@NotNull TamagotchiBuddy buddy) {
        this.buddy = buddy;
        
        // Initialize attributes with default values
        initializeAttributes();
        
        // Initialize achievements
        initializeAchievements();
        
        // Initialize level experience requirements
        initializeLevelExperience();
        
        // Start session time tracking
        this.lastSessionStart = System.currentTimeMillis();
    }
    
    /**
     * Initializes the attributes with default values.
     */
    private void initializeAttributes() {
        attributes.put(AttributeType.VITALITY, new BuddyAttribute(AttributeType.VITALITY, 10));
        attributes.put(AttributeType.AGILITY, new BuddyAttribute(AttributeType.AGILITY, 10));
        attributes.put(AttributeType.INTELLIGENCE, new BuddyAttribute(AttributeType.INTELLIGENCE, 10));
        attributes.put(AttributeType.CREATIVITY, new BuddyAttribute(AttributeType.CREATIVITY, 10));
        attributes.put(AttributeType.CHARISMA, new BuddyAttribute(AttributeType.CHARISMA, 10));
        attributes.put(AttributeType.EMPATHY, new BuddyAttribute(AttributeType.EMPATHY, 10));
        attributes.put(AttributeType.LUCK, new BuddyAttribute(AttributeType.LUCK, 5));
        attributes.put(AttributeType.WISDOM, new BuddyAttribute(AttributeType.WISDOM, 5));
    }
    
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
                "Interact with your buddy for the first time",
                50, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.FRIENDLY_TOUCH, new BuddyAchievement(
                BuddyAchievement.AchievementType.FRIENDLY_TOUCH,
                "Pet your buddy 10 times",
                100, AttributeType.CHARISMA, 1, null));
                
        achievements.put(BuddyAchievement.AchievementType.CARETAKER, new BuddyAchievement(
                BuddyAchievement.AchievementType.CARETAKER,
                "Feed your buddy 5 times",
                100, AttributeType.EMPATHY, 1, null));
                
        achievements.put(BuddyAchievement.AchievementType.PLAYFUL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.PLAYFUL_FRIEND,
                "Play with your buddy for the first time",
                75, AttributeType.CREATIVITY, 1, null));
                
        achievements.put(BuddyAchievement.AchievementType.CLEANUP_CREW, new BuddyAchievement(
                BuddyAchievement.AchievementType.CLEANUP_CREW,
                "Clean up 5 poops",
                75, AttributeType.VITALITY, 1, null));
                
        // Intermediate achievements
        achievements.put(BuddyAchievement.AchievementType.BEST_BUDDIES, new BuddyAchievement(
                BuddyAchievement.AchievementType.BEST_BUDDIES,
                "Reach maximum happiness with your buddy",
                200, AttributeType.CHARISMA, 2, null));
                
        achievements.put(BuddyAchievement.AchievementType.WELL_FED, new BuddyAchievement(
                BuddyAchievement.AchievementType.WELL_FED,
                "Keep your buddy perfectly fed for a full session",
                200, AttributeType.VITALITY, 2, null));
                
        achievements.put(BuddyAchievement.AchievementType.ENERGIZER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ENERGIZER,
                "Keep your buddy's energy above 80% for an entire session",
                200, AttributeType.VITALITY, 2, null));
                
        achievements.put(BuddyAchievement.AchievementType.FUN_TIMES, new BuddyAchievement(
                BuddyAchievement.AchievementType.FUN_TIMES,
                "Reach maximum fun level with your buddy",
                200, AttributeType.CREATIVITY, 2, null));
                
        achievements.put(BuddyAchievement.AchievementType.MARATHON_SESSION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MARATHON_SESSION,
                "Spend over an hour with your buddy in a single session",
                300, AttributeType.INTELLIGENCE, 2, null));
                
        // Advanced achievements
        achievements.put(BuddyAchievement.AchievementType.MASTER_TRAINER, new BuddyAchievement(
                BuddyAchievement.AchievementType.MASTER_TRAINER,
                "Reach level 10 with your buddy",
                500, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.ATTRIBUTE_EXPERT, new BuddyAchievement(
                BuddyAchievement.AchievementType.ATTRIBUTE_EXPERT,
                "Get one attribute to maximum level",
                500, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.COMPLETION_COLLECTOR, new BuddyAchievement(
                BuddyAchievement.AchievementType.COMPLETION_COLLECTOR,
                "Unlock all basic achievements",
                750, AttributeType.WISDOM, 3, null));
                
        // Special achievements
        achievements.put(BuddyAchievement.AchievementType.MIDNIGHT_COMPANION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MIDNIGHT_COMPANION,
                "Spend time with your buddy after midnight",
                250, AttributeType.WISDOM, 1, null));
                
        achievements.put(BuddyAchievement.AchievementType.DESIGN_MARATHON, new BuddyAchievement(
                BuddyAchievement.AchievementType.DESIGN_MARATHON,
                "Spend extended time with your buddy",
                500, AttributeType.CREATIVITY, 3, null));
                
        achievements.put(BuddyAchievement.AchievementType.LOYAL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.LOYAL_FRIEND,
                "Interact with your buddy every day for a week",
                1000, AttributeType.CHARISMA, 3, null));
                
        achievements.put(BuddyAchievement.AchievementType.SECRET_DANCE, new BuddyAchievement(
                BuddyAchievement.AchievementType.SECRET_DANCE,
                "Discover the buddy's secret dance animation",
                300, AttributeType.LUCK, 2, null));
                
        achievements.put(BuddyAchievement.AchievementType.TELEPATHIC_BOND, new BuddyAchievement(
                BuddyAchievement.AchievementType.TELEPATHIC_BOND,
                "Get your buddy to predict your actions correctly 5 times",
                500, AttributeType.EMPATHY, 3, null));
                
        // Master achievements
        achievements.put(BuddyAchievement.AchievementType.BUDDY_WHISPERER, new BuddyAchievement(
                BuddyAchievement.AchievementType.BUDDY_WHISPERER,
                "Max out all attributes",
                2000, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.SKILL_MASTER, new BuddyAchievement(
                BuddyAchievement.AchievementType.SKILL_MASTER,
                "Unlock all skills",
                2000, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.PERFECT_HARMONY, new BuddyAchievement(
                BuddyAchievement.AchievementType.PERFECT_HARMONY,
                "Keep all buddy stats above 90% for an entire session",
                1500, null, 0, null));
                
        achievements.put(BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER,
                "Unlock all other achievements",
                5000, null, 0, null));
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
        LOGGER.info("Added {} XP to buddy, total XP: {}", amount, experience);
        
        // Check for level-ups
        while (currentLevel < MAX_LEVEL && experience >= levelExperience[currentLevel + 1]) {
            currentLevel++;
            levelsGained.add(currentLevel);
            
            // Award attribute points for leveling up
            unspentAttributePoints += ATTRIBUTE_POINTS_PER_LEVEL;
            
            LOGGER.info("Buddy leveled up to level {}!", currentLevel);
            
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
        
        // Vitality affects health-related stats
        float vitalityBonus = getAttributeEffectPercentage(AttributeType.VITALITY);
        buddy.poopingInterval = Math.max(1000, 1500 - (int)(500 * vitalityBonus));
        
        // Agility affects movement speed
        float agilityBonus = getAttributeEffectPercentage(AttributeType.AGILITY);
        buddy.hopAnimationSpeed = 0.3f + (0.3f * agilityBonus);
        
        // Apply other attribute effects when the buddy needs updating
        applyAttributeEffects();
    }
    
    /**
     * Applies effects from the current attribute values to the buddy.
     */
    public void applyAttributeEffects() {
        // Get the effect percentages for each attribute (0.0 - 1.0)
        float vitalityEffect = getAttributeEffectPercentage(AttributeType.VITALITY);
        float agilityEffect = getAttributeEffectPercentage(AttributeType.AGILITY);
        float intelligenceEffect = getAttributeEffectPercentage(AttributeType.INTELLIGENCE);
        float creativityEffect = getAttributeEffectPercentage(AttributeType.CREATIVITY);
        float charismaEffect = getAttributeEffectPercentage(AttributeType.CHARISMA);
        float empathyEffect = getAttributeEffectPercentage(AttributeType.EMPATHY);
        float luckEffect = getAttributeEffectPercentage(AttributeType.LUCK);
        float wisdomEffect = getAttributeEffectPercentage(AttributeType.WISDOM);
        
        // Apply effects to the buddy
        
        // Vitality effects
        // - Slower hunger decrease
        float hungerMultiplier = 1.0f - (vitalityEffect * 0.5f); // Up to 50% slower hunger decrease
        // - Slower energy decrease
        float energyMultiplier = 1.0f - (vitalityEffect * 0.5f); // Up to 50% slower energy decrease
        
        // Agility effects
        // - Faster movement
        float moveSpeedBonus = agilityEffect * 2.0f; // Up to +2 move speed
        
        // Intelligence effects
        // - Better memory (would be used in future features)
        // - Faster learning (bonus experience gain)
        float experienceMultiplier = 1.0f + (intelligenceEffect * 0.5f); // Up to 50% more experience
        
        // Creativity effects
        // - More varied animations
        float animationVarietyBonus = creativityEffect * 0.5f; // Up to 50% more animation variety
        
        // Charisma effects
        // - Slower happiness decrease
        float happinessMultiplier = 1.0f - (charismaEffect * 0.5f); // Up to 50% slower happiness decrease
        // - Faster happiness increase
        float happinessGainMultiplier = 1.0f + (charismaEffect * 0.5f); // Up to 50% more happiness gain
        
        // Empathy effects
        // - Better understanding of needs
        float needsUnderstandingBonus = empathyEffect * 0.5f; // Up to 50% better needs understanding
        
        // Luck effects
        // - Better random events
        float luckBonus = luckEffect * 0.5f; // Up to 50% better luck
        
        // Wisdom effects
        // - Better advice (would be used in future features)
        // - More efficient use of skills
        float skillEffectMultiplier = 1.0f + (wisdomEffect * 0.3f); // Up to 30% more effective skills
        
        // Store these effects temporarily for the TamagotchiBuddy to use
        buddy.setAttributeEffects(
                hungerMultiplier,
                happinessMultiplier,
                energyMultiplier,
                happinessGainMultiplier,
                moveSpeedBonus,
                experienceMultiplier,
                animationVarietyBonus,
                needsUnderstandingBonus,
                luckBonus,
                skillEffectMultiplier
        );
    }
    
    /**
     * Gets a percentage (0.0 - 1.0) representing the effectiveness of an attribute.
     *
     * @param type The attribute type
     * @return The effect percentage (0.0 - 1.0)
     */
    public float getAttributeEffectPercentage(AttributeType type) {
        BuddyAttribute attribute = attributes.get(type);
        if (attribute == null) return 0.0f;
        
        return (float) attribute.getTotalValue() / BuddyAttribute.MAX_ATTRIBUTE_VALUE;
    }
    
    // Skill-related methods removed
    
    /**
     * Adds attribute points to a specific attribute.
     *
     * @param type The attribute type to add points to
     * @param points The number of points to add
     * @return The actual number of points added
     */
    public int addAttributePoints(AttributeType type, int points) {
        BuddyAttribute attribute = attributes.get(type);
        if (attribute == null) return 0;
        
        int pointsToAdd = Math.min(points, unspentAttributePoints);
        int pointsAdded = attribute.addBonusPoints(pointsToAdd);
        
        unspentAttributePoints -= pointsAdded;
        
        LOGGER.info("Added {} points to attribute: {}, now at {}/{}", 
                pointsAdded, type.getName(), attribute.getTotalValue(), BuddyAttribute.MAX_ATTRIBUTE_VALUE);
        
        // After changing attributes, apply their effects
        applyAttributeEffects();
        
        // Check for attribute-based achievements
        checkAttributeAchievements();
        
        return pointsAdded;
    }
    
    // Unlock methods removed
    
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
     * Checks for attribute-based achievements.
     */
    private void checkAttributeAchievements() {
        // Check for max attribute achievement
        for (BuddyAttribute attribute : attributes.values()) {
            if (attribute.getTotalValue() >= BuddyAttribute.MAX_ATTRIBUTE_VALUE) {
                unlockAchievement(BuddyAchievement.AchievementType.ATTRIBUTE_EXPERT);
                break;
            }
        }
        
        // Check if all attributes are maxed
        boolean allMaxed = true;
        for (BuddyAttribute attribute : attributes.values()) {
            if (attribute.getTotalValue() < BuddyAttribute.MAX_ATTRIBUTE_VALUE) {
                allMaxed = false;
                break;
            }
        }
        
        if (allMaxed) {
            unlockAchievement(BuddyAchievement.AchievementType.BUDDY_WHISPERER);
        }
    }
    
    /**
     * Skills achievements have been simplified since skills were removed.
     * This method now awards achievements based on level instead.
     */
    private void checkSkillAchievements() {
        // Award achievement at level 15
        if (currentLevel >= 15) {
            unlockAchievement(BuddyAchievement.AchievementType.SKILL_SPECIALIST);
        }
        
        // Award achievement at level 20
        if (currentLevel >= 20) {
            unlockAchievement(BuddyAchievement.AchievementType.SKILL_MASTER);
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
            LOGGER.info("Unlocked achievement: {}", type.getName());
            
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
                LOGGER.info("Unlocked achievement: {}", BuddyAchievement.AchievementType.COMPLETION_COLLECTOR.getName());
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
                LOGGER.info("Unlocked achievement: {}", BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER.getName());
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
            
            File saveFile = new File(BUDDY_DIR, SAVE_FILENAME);
            JsonObject json = new JsonObject();
            
            // Save basic leveling data
            json.addProperty("level", currentLevel);
            json.addProperty("experience", experience);
            json.addProperty("unspentAttributePoints", unspentAttributePoints);
            
            // Save attributes
            JsonObject attributesObj = new JsonObject();
            for (Map.Entry<AttributeType, BuddyAttribute> entry : attributes.entrySet()) {
                JsonObject attributeObj = new JsonObject();
                attributeObj.addProperty("baseValue", entry.getValue().getBaseValue());
                attributeObj.addProperty("bonusPoints", entry.getValue().getBonusPoints());
                attributesObj.add(entry.getKey().name(), attributeObj);
            }
            json.add("attributes", attributesObj);
            
            // Skills removed
            
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
            
            LOGGER.info("Saved buddy leveling data to {}", saveFile.getAbsolutePath());
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
        File saveFile = new File(BUDDY_DIR, SAVE_FILENAME);
        if (!saveFile.exists()) {
            LOGGER.info("No buddy leveling save file found at {}", saveFile.getAbsolutePath());
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
            
            if (json.has("unspentAttributePoints")) {
                unspentAttributePoints = json.get("unspentAttributePoints").getAsInt();
                unspentAttributePoints = Math.max(0, unspentAttributePoints);
            }
            
            // Removed skill points and unlocks loading
            
            // Load attributes
            if (json.has("attributes")) {
                JsonObject attributesObj = json.getAsJsonObject("attributes");
                for (AttributeType type : AttributeType.values()) {
                    if (attributesObj.has(type.name())) {
                        JsonObject attributeObj = attributesObj.getAsJsonObject(type.name());
                        BuddyAttribute attribute = attributes.get(type);
                        
                        if (attribute != null) {
                            if (attributeObj.has("baseValue")) {
                                int baseValue = attributeObj.get("baseValue").getAsInt();
                                // Reset and set new base value
                                attribute = new BuddyAttribute(type, baseValue);
                            }
                            
                            if (attributeObj.has("bonusPoints")) {
                                int bonusPoints = attributeObj.get("bonusPoints").getAsInt();
                                attribute.addBonusPoints(bonusPoints);
                            }
                            
                            attributes.put(type, attribute);
                        }
                    }
                }
            }
            
            // Skills loading removed
            
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
            
            LOGGER.info("Loaded buddy leveling data from {}", saveFile.getAbsolutePath());
            LOGGER.info("Current level: {}, Experience: {}, Attribute points: {}",
                    currentLevel, experience, unspentAttributePoints);
            
            return true;
            
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            LOGGER.error("Failed to load buddy leveling data", e);
            return false;
        }
    }
    
    // Getters and setters
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public int getUnspentAttributePoints() {
        return unspentAttributePoints;
    }
    
    public Map<AttributeType, BuddyAttribute> getAttributes() {
        return new EnumMap<>(attributes);
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
}