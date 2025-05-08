package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAttribute.AttributeType;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyLevel.BuddyUnlock;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddySkill.SkillType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private int unspentSkillPoints = 0;
    private final Set<BuddyUnlock> unlocks = new HashSet<>();

    // Attributes
    private final Map<AttributeType, BuddyAttribute> attributes = new EnumMap<>(AttributeType.class);

    // Skills
    private final Map<SkillType, BuddySkill> skills = new EnumMap<>(SkillType.class);

    // Achievements
    private final Map<BuddyAchievement.AchievementType, BuddyAchievement> achievements = new EnumMap<>(BuddyAchievement.AchievementType.class);

    // Level definitions
    private final List<BuddyLevel> levels = new ArrayList<>();

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
        
        // Initialize skills
        initializeSkills();
        
        // Initialize achievements
        initializeAchievements();
        
        // Initialize level definitions
        initializeLevels();
        
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
     * Initializes the skills with their prerequisites and requirements.
     */
    private void initializeSkills() {
        // Social skills
        skills.put(SkillType.MOOD_LIFTING, new BuddySkill(
                SkillType.MOOD_LIFTING, 2, 
                AttributeType.CHARISMA, 15, 
                null, 0, 
                3));
                
        skills.put(SkillType.ENCOURAGEMENT, new BuddySkill(
                SkillType.ENCOURAGEMENT, 5, 
                AttributeType.EMPATHY, 20, 
                AttributeType.CHARISMA, 15, 
                3, SkillType.MOOD_LIFTING));
                
        skills.put(SkillType.CELEBRATION, new BuddySkill(
                SkillType.CELEBRATION, 8, 
                AttributeType.CHARISMA, 25, 
                AttributeType.CREATIVITY, 20, 
                3));
                
        skills.put(SkillType.COMPANIONSHIP, new BuddySkill(
                SkillType.COMPANIONSHIP, 10, 
                AttributeType.EMPATHY, 30, 
                AttributeType.CHARISMA, 25, 
                4, SkillType.ENCOURAGEMENT, SkillType.CELEBRATION));
                
        // Display skills
        skills.put(SkillType.ANIMATED_TRICKS, new BuddySkill(
                SkillType.ANIMATED_TRICKS, 3, 
                AttributeType.AGILITY, 15, 
                AttributeType.CREATIVITY, 10, 
                3));
                
        skills.put(SkillType.PARTICLE_MASTERY, new BuddySkill(
                SkillType.PARTICLE_MASTERY, 7, 
                AttributeType.CREATIVITY, 25, 
                AttributeType.AGILITY, 15, 
                3, SkillType.ANIMATED_TRICKS));
                
        skills.put(SkillType.DANCE_MOVES, new BuddySkill(
                SkillType.DANCE_MOVES, 5, 
                AttributeType.AGILITY, 20, 
                AttributeType.CREATIVITY, 15, 
                3));
                
        skills.put(SkillType.EXPRESSION_RANGE, new BuddySkill(
                SkillType.EXPRESSION_RANGE, 4, 
                AttributeType.EMPATHY, 15, 
                AttributeType.CHARISMA, 10, 
                3));
                
        // Behavior skills
        skills.put(SkillType.ADAPTIVE_BEHAVIOR, new BuddySkill(
                SkillType.ADAPTIVE_BEHAVIOR, 6, 
                AttributeType.INTELLIGENCE, 20, 
                AttributeType.EMPATHY, 15, 
                3));
                
        skills.put(SkillType.ENHANCED_MEMORY, new BuddySkill(
                SkillType.ENHANCED_MEMORY, 8, 
                AttributeType.INTELLIGENCE, 25, 
                AttributeType.WISDOM, 20, 
                3));
                
        skills.put(SkillType.ENERGY_MANAGEMENT, new BuddySkill(
                SkillType.ENERGY_MANAGEMENT, 4, 
                AttributeType.VITALITY, 20, 
                AttributeType.WISDOM, 10, 
                3));
                
        skills.put(SkillType.MOOD_STABILITY, new BuddySkill(
                SkillType.MOOD_STABILITY, 6, 
                AttributeType.EMPATHY, 20, 
                AttributeType.VITALITY, 15, 
                3));
    }
    
    /**
     * Initializes the achievements with their rewards.
     */
    private void initializeAchievements() {
        // Basic achievements
        achievements.put(BuddyAchievement.AchievementType.FIRST_STEPS, new BuddyAchievement(
                BuddyAchievement.AchievementType.FIRST_STEPS,
                "Interact with your buddy for the first time",
                50, null, 0, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.FRIENDLY_TOUCH, new BuddyAchievement(
                BuddyAchievement.AchievementType.FRIENDLY_TOUCH,
                "Pet your buddy 10 times",
                100, AttributeType.CHARISMA, 1, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.CARETAKER, new BuddyAchievement(
                BuddyAchievement.AchievementType.CARETAKER,
                "Feed your buddy 5 times",
                100, AttributeType.EMPATHY, 1, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.PLAYFUL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.PLAYFUL_FRIEND,
                "Play with your buddy for the first time",
                75, AttributeType.CREATIVITY, 1, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.CLEANUP_CREW, new BuddyAchievement(
                BuddyAchievement.AchievementType.CLEANUP_CREW,
                "Clean up 5 poops",
                75, AttributeType.VITALITY, 1, null, null));
                
        // Intermediate achievements
        achievements.put(BuddyAchievement.AchievementType.BEST_BUDDIES, new BuddyAchievement(
                BuddyAchievement.AchievementType.BEST_BUDDIES,
                "Reach maximum happiness with your buddy",
                200, AttributeType.CHARISMA, 2, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.WELL_FED, new BuddyAchievement(
                BuddyAchievement.AchievementType.WELL_FED,
                "Keep your buddy perfectly fed for a full session",
                200, AttributeType.VITALITY, 2, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.ENERGIZER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ENERGIZER,
                "Keep your buddy's energy above 80% for an entire session",
                200, AttributeType.VITALITY, 2, BuddyUnlock.ENERGY_BOOST, null));
                
        achievements.put(BuddyAchievement.AchievementType.FUN_TIMES, new BuddyAchievement(
                BuddyAchievement.AchievementType.FUN_TIMES,
                "Reach maximum fun level with your buddy",
                200, AttributeType.CREATIVITY, 2, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.MARATHON_SESSION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MARATHON_SESSION,
                "Spend over an hour with your buddy in a single session",
                300, AttributeType.INTELLIGENCE, 2, null, null));
                
        // Advanced achievements
        achievements.put(BuddyAchievement.AchievementType.MASTER_TRAINER, new BuddyAchievement(
                BuddyAchievement.AchievementType.MASTER_TRAINER,
                "Reach level 10 with your buddy",
                500, null, 0, BuddyUnlock.ADVANCED_TRICKS, null));
                
        achievements.put(BuddyAchievement.AchievementType.ATTRIBUTE_EXPERT, new BuddyAchievement(
                BuddyAchievement.AchievementType.ATTRIBUTE_EXPERT,
                "Get one attribute to maximum level",
                500, null, 0, null, lm -> lm.unspentAttributePoints += 5));
                
        achievements.put(BuddyAchievement.AchievementType.SKILL_SPECIALIST, new BuddyAchievement(
                BuddyAchievement.AchievementType.SKILL_SPECIALIST,
                "Master any skill to its highest level",
                500, null, 0, null, lm -> lm.unspentSkillPoints += 3));
                
        achievements.put(BuddyAchievement.AchievementType.COMPLETION_COLLECTOR, new BuddyAchievement(
                BuddyAchievement.AchievementType.COMPLETION_COLLECTOR,
                "Unlock all basic achievements",
                750, AttributeType.WISDOM, 3, null, null));
                
        // Special achievements
        achievements.put(BuddyAchievement.AchievementType.MIDNIGHT_COMPANION, new BuddyAchievement(
                BuddyAchievement.AchievementType.MIDNIGHT_COMPANION,
                "Spend time with your buddy after midnight",
                250, AttributeType.WISDOM, 1, null, null));
                
        achievements.put(BuddyAchievement.AchievementType.DESIGN_MARATHON, new BuddyAchievement(
                BuddyAchievement.AchievementType.DESIGN_MARATHON,
                "Spend extended time with your buddy",
                500, AttributeType.CREATIVITY, 3, BuddyUnlock.SEASONAL_OUTFITS, null));
                
        achievements.put(BuddyAchievement.AchievementType.LOYAL_FRIEND, new BuddyAchievement(
                BuddyAchievement.AchievementType.LOYAL_FRIEND,
                "Interact with your buddy every day for a week",
                1000, AttributeType.CHARISMA, 3, BuddyUnlock.MOOD_BOOST_SKILL, null));
                
        achievements.put(BuddyAchievement.AchievementType.SECRET_DANCE, new BuddyAchievement(
                BuddyAchievement.AchievementType.SECRET_DANCE,
                "Discover the buddy's secret dance animation",
                300, AttributeType.LUCK, 2, BuddyUnlock.PARTICLE_EFFECTS, null));
                
        achievements.put(BuddyAchievement.AchievementType.TELEPATHIC_BOND, new BuddyAchievement(
                BuddyAchievement.AchievementType.TELEPATHIC_BOND,
                "Get your buddy to predict your actions correctly 5 times",
                500, AttributeType.EMPATHY, 3, null, null));
                
        // Master achievements
        achievements.put(BuddyAchievement.AchievementType.BUDDY_WHISPERER, new BuddyAchievement(
                BuddyAchievement.AchievementType.BUDDY_WHISPERER,
                "Max out all attributes",
                2000, null, 0, BuddyUnlock.CUSTOM_BEHAVIOR, null));
                
        achievements.put(BuddyAchievement.AchievementType.SKILL_MASTER, new BuddyAchievement(
                BuddyAchievement.AchievementType.SKILL_MASTER,
                "Unlock all skills",
                2000, null, 0, BuddyUnlock.WEATHER_REACTIONS, null));
                
        achievements.put(BuddyAchievement.AchievementType.PERFECT_HARMONY, new BuddyAchievement(
                BuddyAchievement.AchievementType.PERFECT_HARMONY,
                "Keep all buddy stats above 90% for an entire session",
                1500, null, 0, BuddyUnlock.DAY_NIGHT_CYCLE, null));
                
        achievements.put(BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER, new BuddyAchievement(
                BuddyAchievement.AchievementType.ACHIEVEMENT_HUNTER,
                "Unlock all other achievements",
                5000, null, 0, BuddyUnlock.CUSTOM_ANIMATIONS, null));
    }
    
    /**
     * Initializes the level definitions with their unlocks.
     */
    private void initializeLevels() {
        levels.add(new BuddyLevel(1, 0, "Newborn Buddy", "Your buddy has just hatched", 
                BuddyUnlock.BASIC_TRICKS));
                
        levels.add(new BuddyLevel(2, calculateExperienceForLevel(2), "Curious Buddy", "Your buddy is starting to explore the world",
                BuddyUnlock.COLOR_CUSTOMIZATION));
                
        levels.add(new BuddyLevel(3, calculateExperienceForLevel(3), "Playful Buddy", "Your buddy loves to play and learn",
                BuddyUnlock.FOLLOW_ME));
                
        levels.add(new BuddyLevel(5, calculateExperienceForLevel(5), "Happy Companion", "Your buddy enjoys spending time with you",
                BuddyUnlock.MOOD_BOOST_SKILL));
                
        levels.add(new BuddyLevel(7, calculateExperienceForLevel(7), "Loyal Companion", "Your buddy has formed a strong bond with you",
                BuddyUnlock.HAPPINESS_BOOST));
                
        levels.add(new BuddyLevel(10, calculateExperienceForLevel(10), "Skilled Buddy", "Your buddy has mastered many skills",
                BuddyUnlock.ADVANCED_TRICKS, BuddyUnlock.ACCESSORY_SLOT));
                
        levels.add(new BuddyLevel(12, calculateExperienceForLevel(12), "Expressive Partner", "Your buddy has a wide range of expressions",
                BuddyUnlock.DANCE_GAME));
                
        levels.add(new BuddyLevel(15, calculateExperienceForLevel(15), "Animated Friend", "Your buddy has mastered various animations",
                BuddyUnlock.TRICK_PERFORMANCE, BuddyUnlock.ENERGY_BOOST));
                
        levels.add(new BuddyLevel(20, calculateExperienceForLevel(20), "Clean Companion", "Your buddy is very clean and well-behaved",
                BuddyUnlock.CLEANUP_SKILL, BuddyUnlock.PARTICLE_EFFECTS, BuddyUnlock.HUNGER_EFFICIENCY));
                
        levels.add(new BuddyLevel(25, calculateExperienceForLevel(25), "Seasonal Friend", "Your buddy adapts to the changing seasons",
                BuddyUnlock.SEASONAL_OUTFITS, BuddyUnlock.MOOD_GAME));
                
        levels.add(new BuddyLevel(MAX_LEVEL, calculateExperienceForLevel(MAX_LEVEL), "Legendary Companion", "Your buddy has reached legendary status",
                BuddyUnlock.CUSTOM_ANIMATIONS, BuddyUnlock.CUSTOM_BEHAVIOR, BuddyUnlock.SOUND_EFFECTS));
    }
    
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
        return calculateExperienceForLevel(currentLevel + 1) - experience;
    }
    
    /**
     * Gets the percentage progress to the next level (0-100).
     *
     * @return The percentage progress to the next level, or 100 if at max level
     */
    public int getLevelProgressPercentage() {
        if (currentLevel >= MAX_LEVEL) return 100;
        
        int currentLevelXp = calculateExperienceForLevel(currentLevel);
        int nextLevelXp = calculateExperienceForLevel(currentLevel + 1);
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
        while (currentLevel < MAX_LEVEL && experience >= calculateExperienceForLevel(currentLevel + 1)) {
            currentLevel++;
            levelsGained.add(currentLevel);
            
            // Award attribute and skill points for leveling up
            unspentAttributePoints += ATTRIBUTE_POINTS_PER_LEVEL;
            unspentSkillPoints += 1;
            
            LOGGER.info("Buddy leveled up to level {}!", currentLevel);
            
            // Apply level-specific unlocks
            for (BuddyLevel level : levels) {
                if (level.getLevel() == currentLevel) {
                    for (BuddyUnlock unlock : level.getUnlocks()) {
                        addUnlock(unlock);
                    }
                    break;
                }
            }
            
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
    
    /**
     * Gets the effectiveness of a skill (0.0 - 1.0).
     *
     * @param type The skill type
     * @return The skill effectiveness, or 0.0 if not unlocked
     */
    public float getSkillEffectiveness(SkillType type) {
        BuddySkill skill = skills.get(type);
        if (skill == null || !skill.isUnlocked()) return 0.0f;
        
        float baseEffectiveness = skill.getEffectiveness();
        
        // Apply wisdom bonus to skill effectiveness
        float wisdomBonus = getAttributeEffectPercentage(AttributeType.WISDOM) * 0.3f; // Up to 30% bonus
        
        return Math.min(1.0f, baseEffectiveness * (1.0f + wisdomBonus));
    }
    
    /**
     * Checks if a skill can be unlocked with the current attributes and prerequisites.
     *
     * @param type The skill type to check
     * @return True if the skill can be unlocked, false otherwise
     */
    public boolean canUnlockSkill(SkillType type) {
        BuddySkill skill = skills.get(type);
        if (skill == null || skill.isUnlocked() || unspentSkillPoints <= 0) return false;
        
        // Check level requirement
        if (currentLevel < skill.getLevelRequirement()) return false;
        
        // Check primary attribute
        BuddyAttribute primaryAttribute = attributes.get(skill.getPrimaryAttributeType());
        if (primaryAttribute == null || primaryAttribute.getTotalValue() < skill.getPrimaryAttributeRequirement()) return false;
        
        // Check secondary attribute if present
        if (skill.getSecondaryAttributeType() != null) {
            BuddyAttribute secondaryAttribute = attributes.get(skill.getSecondaryAttributeType());
            if (secondaryAttribute == null || secondaryAttribute.getTotalValue() < skill.getSecondaryAttributeRequirement()) return false;
        }
        
        // Check prerequisites
        for (SkillType prerequisite : skill.getPrerequisites()) {
            BuddySkill prerequisiteSkill = skills.get(prerequisite);
            if (prerequisiteSkill == null || !prerequisiteSkill.isUnlocked()) return false;
        }
        
        return true;
    }
    
    /**
     * Attempts to unlock a skill.
     *
     * @param type The skill type to unlock
     * @return True if the skill was unlocked, false if it couldn't be
     */
    public boolean unlockSkill(SkillType type) {
        if (!canUnlockSkill(type)) return false;
        
        BuddySkill skill = skills.get(type);
        skill.setUnlocked(true);
        unspentSkillPoints--;
        
        LOGGER.info("Unlocked skill: {}", type.getName());
        
        // Check for skill-based achievements
        checkSkillAchievements();
        
        return true;
    }
    
    /**
     * Attempts to increase a skill's level.
     *
     * @param type The skill type to level up
     * @return True if the skill level was increased, false otherwise
     */
    public boolean increaseSkillLevel(SkillType type) {
        BuddySkill skill = skills.get(type);
        if (skill == null || !skill.isUnlocked() || unspentSkillPoints <= 0) return false;
        
        if (skill.increaseSkillLevel()) {
            unspentSkillPoints--;
            
            LOGGER.info("Increased skill level: {} to level {}", type.getName(), skill.getSkillLevel());
            
            // Check for skill mastery achievement
            if (skill.getSkillLevel() >= skill.getMaxSkillLevel()) {
                checkSkillAchievements();
            }
            
            return true;
        }
        
        return false;
    }
    
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
    
    /**
     * Adds an unlock to the buddy's unlocked features.
     *
     * @param unlock The unlock to add
     * @return True if the unlock was newly added, false if already unlocked
     */
    public boolean addUnlock(BuddyUnlock unlock) {
        if (unlocks.contains(unlock)) return false;
        
        unlocks.add(unlock);
        LOGGER.info("Unlocked new feature: {}", unlock.getName());
        return true;
    }
    
    /**
     * Checks if the buddy has a specific unlock.
     *
     * @param unlock The unlock to check
     * @return True if the buddy has the unlock, false otherwise
     */
    public boolean hasUnlock(BuddyUnlock unlock) {
        return unlocks.contains(unlock);
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
     * Checks for skill-based achievements.
     */
    private void checkSkillAchievements() {
        // Check for max skill level achievement
        for (BuddySkill skill : skills.values()) {
            if (skill.isUnlocked() && skill.getSkillLevel() >= skill.getMaxSkillLevel()) {
                unlockAchievement(BuddyAchievement.AchievementType.SKILL_SPECIALIST);
                break;
            }
        }
        
        // Check if all skills are unlocked
        boolean allUnlocked = true;
        for (BuddySkill skill : skills.values()) {
            if (!skill.isUnlocked()) {
                allUnlocked = false;
                break;
            }
        }
        
        if (allUnlocked) {
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
            json.addProperty("unspentSkillPoints", unspentSkillPoints);
            
            // Save unlocks
            JsonArray unlocksArray = new JsonArray();
            for (BuddyUnlock unlock : unlocks) {
                unlocksArray.add(unlock.name());
            }
            json.add("unlocks", unlocksArray);
            
            // Save attributes
            JsonObject attributesObj = new JsonObject();
            for (Map.Entry<AttributeType, BuddyAttribute> entry : attributes.entrySet()) {
                JsonObject attributeObj = new JsonObject();
                attributeObj.addProperty("baseValue", entry.getValue().getBaseValue());
                attributeObj.addProperty("bonusPoints", entry.getValue().getBonusPoints());
                attributesObj.add(entry.getKey().name(), attributeObj);
            }
            json.add("attributes", attributesObj);
            
            // Save skills
            JsonObject skillsObj = new JsonObject();
            for (Map.Entry<SkillType, BuddySkill> entry : skills.entrySet()) {
                JsonObject skillObj = new JsonObject();
                skillObj.addProperty("unlocked", entry.getValue().isUnlocked());
                skillObj.addProperty("level", entry.getValue().getSkillLevel());
                skillsObj.add(entry.getKey().name(), skillObj);
            }
            json.add("skills", skillsObj);
            
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
            
            if (json.has("unspentSkillPoints")) {
                unspentSkillPoints = json.get("unspentSkillPoints").getAsInt();
                unspentSkillPoints = Math.max(0, unspentSkillPoints);
            }
            
            // Load unlocks
            unlocks.clear();
            if (json.has("unlocks")) {
                JsonArray unlocksArray = json.getAsJsonArray("unlocks");
                for (int i = 0; i < unlocksArray.size(); i++) {
                    try {
                        String unlockName = unlocksArray.get(i).getAsString();
                        BuddyUnlock unlock = BuddyUnlock.valueOf(unlockName);
                        unlocks.add(unlock);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Unknown unlock found in save file: {}", e.getMessage());
                    }
                }
            }
            
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
            
            // Load skills
            if (json.has("skills")) {
                JsonObject skillsObj = json.getAsJsonObject("skills");
                for (SkillType type : SkillType.values()) {
                    if (skillsObj.has(type.name())) {
                        JsonObject skillObj = skillsObj.getAsJsonObject(type.name());
                        BuddySkill skill = skills.get(type);
                        
                        if (skill != null) {
                            if (skillObj.has("unlocked") && skillObj.get("unlocked").getAsBoolean()) {
                                skill.setUnlocked(true);
                                
                                if (skillObj.has("level")) {
                                    int level = skillObj.get("level").getAsInt();
                                    // Reset level to 0 and then add levels
                                    skill.reset();
                                    skill.setUnlocked(true);
                                    for (int i = 0; i < level; i++) {
                                        skill.increaseSkillLevel();
                                    }
                                }
                            }
                        }
                    }
                }
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
            
            LOGGER.info("Loaded buddy leveling data from {}", saveFile.getAbsolutePath());
            LOGGER.info("Current level: {}, Experience: {}, Attribute points: {}, Skill points: {}",
                    currentLevel, experience, unspentAttributePoints, unspentSkillPoints);
            
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
    
    public int getUnspentSkillPoints() {
        return unspentSkillPoints;
    }
    
    public Set<BuddyUnlock> getUnlocks() {
        return new HashSet<>(unlocks);
    }
    
    public Map<AttributeType, BuddyAttribute> getAttributes() {
        return new EnumMap<>(attributes);
    }
    
    public Map<SkillType, BuddySkill> getSkills() {
        return new EnumMap<>(skills);
    }
    
    public Map<BuddyAchievement.AchievementType, BuddyAchievement> getAchievements() {
        return new EnumMap<>(achievements);
    }
    
    public List<BuddyLevel> getLevels() {
        return new ArrayList<>(levels);
    }
    
    public BuddyLevel getCurrentLevelInfo() {
        for (BuddyLevel level : levels) {
            if (level.getLevel() == currentLevel) {
                return level;
            }
        }
        return levels.get(0); // Return level 1 as fallback
    }
    
    public BuddyLevel getNextLevelInfo() {
        if (currentLevel >= MAX_LEVEL) return null;
        
        for (BuddyLevel level : levels) {
            if (level.getLevel() > currentLevel) {
                return level;
            }
        }
        return null;
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