package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a skill that the buddy can learn through leveling up.
 * Skills are organized in a tree-like structure with prerequisites.
 */
public class BuddySkill {
    
    private final SkillType type;
    private final int levelRequirement;
    private final BuddyAttribute.AttributeType primaryAttributeType;
    private final int primaryAttributeRequirement;
    private final BuddyAttribute.AttributeType secondaryAttributeType;
    private final int secondaryAttributeRequirement;
    private final List<SkillType> prerequisites = new ArrayList<>();
    private boolean unlocked = false;
    private int skillLevel = 0;
    private final int maxSkillLevel;
    
    /**
     * Creates a new buddy skill.
     *
     * @param type The skill type
     * @param levelRequirement The minimum buddy level required to unlock this skill
     * @param primaryAttributeType The primary attribute required for this skill
     * @param primaryAttributeRequirement The minimum value of the primary attribute required
     * @param secondaryAttributeType The secondary attribute required for this skill (can be null)
     * @param secondaryAttributeRequirement The minimum value of the secondary attribute required
     * @param maxSkillLevel The maximum level this skill can be trained to
     * @param prerequisites Prerequisites skills that must be unlocked before this one
     */
    public BuddySkill(@NotNull SkillType type, int levelRequirement, 
                      @NotNull BuddyAttribute.AttributeType primaryAttributeType, int primaryAttributeRequirement,
                      @Nullable BuddyAttribute.AttributeType secondaryAttributeType, int secondaryAttributeRequirement,
                      int maxSkillLevel, SkillType... prerequisites) {
        this.type = type;
        this.levelRequirement = Math.max(1, levelRequirement);
        this.primaryAttributeType = primaryAttributeType;
        this.primaryAttributeRequirement = Math.max(0, primaryAttributeRequirement);
        this.secondaryAttributeType = secondaryAttributeType;
        this.secondaryAttributeRequirement = Math.max(0, secondaryAttributeRequirement);
        this.maxSkillLevel = Math.max(1, maxSkillLevel);
        
        if (prerequisites != null) {
            this.prerequisites.addAll(Arrays.asList(prerequisites));
        }
    }
    
    /**
     * @return The type of this skill
     */
    public SkillType getType() {
        return type;
    }
    
    /**
     * @return The minimum buddy level required to unlock this skill
     */
    public int getLevelRequirement() {
        return levelRequirement;
    }
    
    /**
     * @return The primary attribute required for this skill
     */
    public BuddyAttribute.AttributeType getPrimaryAttributeType() {
        return primaryAttributeType;
    }
    
    /**
     * @return The minimum value of the primary attribute required
     */
    public int getPrimaryAttributeRequirement() {
        return primaryAttributeRequirement;
    }
    
    /**
     * @return The secondary attribute required for this skill (can be null)
     */
    public BuddyAttribute.AttributeType getSecondaryAttributeType() {
        return secondaryAttributeType;
    }
    
    /**
     * @return The minimum value of the secondary attribute required
     */
    public int getSecondaryAttributeRequirement() {
        return secondaryAttributeRequirement;
    }
    
    /**
     * @return The prerequisites skills that must be unlocked before this one
     */
    public List<SkillType> getPrerequisites() {
        return new ArrayList<>(prerequisites);
    }
    
    /**
     * @return Whether this skill is unlocked
     */
    public boolean isUnlocked() {
        return unlocked;
    }
    
    /**
     * Sets whether this skill is unlocked.
     *
     * @param unlocked The new unlocked state
     */
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
    
    /**
     * @return The current skill level
     */
    public int getSkillLevel() {
        return skillLevel;
    }
    
    /**
     * @return The maximum level this skill can be trained to
     */
    public int getMaxSkillLevel() {
        return maxSkillLevel;
    }
    
    /**
     * Increases the skill level by one if possible.
     *
     * @return True if the skill level was increased, false if already at max level
     */
    public boolean increaseSkillLevel() {
        if (unlocked && skillLevel < maxSkillLevel) {
            skillLevel++;
            return true;
        }
        return false;
    }
    
    /**
     * Resets the skill level to zero and locks the skill.
     */
    public void reset() {
        this.skillLevel = 0;
        this.unlocked = false;
    }
    
    /**
     * Gets the effectiveness of this skill based on the current skill level.
     *
     * @return A value between 0.0 and 1.0 indicating the skill's effectiveness
     */
    public float getEffectiveness() {
        if (!unlocked || maxSkillLevel <= 0) return 0.0f;
        return (float) skillLevel / maxSkillLevel;
    }
    
    /**
     * The different types of skills that a buddy can learn.
     */
    public enum SkillType {
        // Social skills
        MOOD_LIFTING("Mood Lifting", "Buddy can improve your mood", 
                "At max level, buddy can perform special animations and interactions to significantly boost your happiness."),
                
        ENCOURAGEMENT("Encouragement", "Buddy provides encouragement during long sessions", 
                "At max level, buddy can sense when you're frustrated and offer tailored encouragement."),
                
        CELEBRATION("Celebration", "Buddy celebrates your accomplishments", 
                "At max level, buddy performs elaborate celebration animations for major achievements."),
                
        COMPANIONSHIP("Companionship", "Buddy provides company", 
                "At max level, buddy develops unique interactions based on your working patterns."),
                
        // Display skills
        ANIMATED_TRICKS("Animated Tricks", "Buddy can perform additional animations", 
                "At max level, buddy can perform complex animation sequences on command."),
                
        PARTICLE_MASTERY("Particle Mastery", "Buddy can display particle effects", 
                "At max level, buddy can create spectacular particle shows."),
                
        DANCE_MOVES("Dance Moves", "Buddy can perform dance moves", 
                "At max level, buddy can perform choreographed dance routines."),
                
        EXPRESSION_RANGE("Expression Range", "Buddy has a wider range of expressions", 
                "At max level, buddy has an extensive collection of facial expressions."),
                
        // Behavior skills
        ADAPTIVE_BEHAVIOR("Adaptive Behavior", "Buddy adapts to your schedule", 
                "At max level, buddy synchronizes perfectly with your activities."),
                
        ENHANCED_MEMORY("Enhanced Memory", "Buddy remembers more about your interactions", 
                "At max level, buddy maintains a detailed history of your relationship."),
                
        ENERGY_MANAGEMENT("Energy Management", "Buddy manages energy more efficiently", 
                "At max level, buddy has significantly longer active periods."),
                
        MOOD_STABILITY("Mood Stability", "Buddy maintains positive moods longer", 
                "At max level, buddy rarely experiences negative mood states.");
        
        private final String name;
        private final String shortDescription;
        private final String maxLevelDescription;
        
        SkillType(String name, String shortDescription, String maxLevelDescription) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.maxLevelDescription = maxLevelDescription;
        }
        
        public String getName() {
            return name;
        }
        
        public String getShortDescription() {
            return shortDescription;
        }
        
        public String getMaxLevelDescription() {
            return maxLevelDescription;
        }
    }
}