package de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a stat attribute for the buddy.
 * Each attribute can be improved through leveling and affects different aspects of the buddy's behavior.
 */
public class BuddyAttribute {

    // The maximum value for any attribute
    public static final int MAX_ATTRIBUTE_VALUE = 100;
    
    private final AttributeType type;
    private int baseValue;
    private int bonusPoints;
    
    /**
     * Creates a new buddy attribute.
     *
     * @param type The attribute type
     * @param baseValue The base value for this attribute (1-100)
     */
    public BuddyAttribute(@NotNull AttributeType type, int baseValue) {
        this.type = type;
        this.baseValue = Math.max(1, Math.min(MAX_ATTRIBUTE_VALUE, baseValue));
        this.bonusPoints = 0;
    }
    
    /**
     * @return The type of this attribute
     */
    public AttributeType getType() {
        return type;
    }
    
    /**
     * @return The base value of this attribute
     */
    public int getBaseValue() {
        return baseValue;
    }
    
    /**
     * @return The additional points added to this attribute
     */
    public int getBonusPoints() {
        return bonusPoints;
    }
    
    /**
     * @return The total effective value of this attribute (base + bonus)
     */
    public int getTotalValue() {
        return Math.min(MAX_ATTRIBUTE_VALUE, baseValue + bonusPoints);
    }
    
    /**
     * Adds bonus points to this attribute.
     *
     * @param points The number of points to add
     * @return The actual number of points added (may be less if hitting max value)
     */
    public int addBonusPoints(int points) {
        int currentTotal = getTotalValue();
        int maxPointsToAdd = MAX_ATTRIBUTE_VALUE - currentTotal;
        int pointsToAdd = Math.min(points, maxPointsToAdd);
        
        if (pointsToAdd > 0) {
            this.bonusPoints += pointsToAdd;
            return pointsToAdd;
        }
        return 0;
    }
    
    /**
     * Improves the base value of this attribute.
     *
     * @param amount The amount to improve by
     * @return The actual amount improved (may be less if hitting max value)
     */
    public int improveBaseValue(int amount) {
        int maxImprovement = MAX_ATTRIBUTE_VALUE - baseValue;
        int actualImprovement = Math.min(amount, maxImprovement);
        
        if (actualImprovement > 0) {
            this.baseValue += actualImprovement;
            return actualImprovement;
        }
        return 0;
    }
    
    /**
     * Resets all bonus points for this attribute.
     */
    public void resetBonusPoints() {
        this.bonusPoints = 0;
    }
    
    /**
     * The different types of attributes that a buddy can have.
     */
    public enum AttributeType {
        // Physical stats
        VITALITY("Vitality", "Affects health recovery and resistance to exhaustion", 
                "Higher vitality makes your buddy less likely to get tired or sick, " +
                "and improves health regeneration rate."),
                
        AGILITY("Agility", "Affects movement speed and animation fluidity", 
                "Higher agility allows your buddy to move more quickly and smoothly, " +
                "with more dynamic animations."),
                
        // Mental stats
        INTELLIGENCE("Intelligence", "Affects learning rate and problem-solving", 
                "Higher intelligence means faster leveling up, better suggestions, " +
                "and more effective assistance with complex tasks."),
                
        CREATIVITY("Creativity", "Affects unique interactions and idea generation", 
                "Higher creativity enables your buddy to suggest more innovative layout ideas " +
                "and increases the chance of discovering rare behaviors."),
                
        // Social stats
        CHARISMA("Charisma", "Affects interaction quality and animation expressiveness", 
                "Higher charisma makes your buddy's animations more expressive and " +
                "interactions more engaging. Improves happiness gains from petting."),
                
        EMPATHY("Empathy", "Affects mood understanding and happiness sharing", 
                "Higher empathy lets your buddy better understand your design intent, " +
                "offer more relevant suggestions, and share happiness with you."),
                
        // Special stats
        LUCK("Luck", "Affects random events and rare find chances", 
                "Higher luck increases the chances of rare animations, special events, " +
                "and valuable discoveries during play."),
                
        WISDOM("Wisdom", "Affects advice quality and experience gains", 
                "Higher wisdom improves the quality of suggestions, increases experience " +
                "gains, and enhances the buddy's ability to recognize patterns in your work.");
        
        private final String name;
        private final String shortDescription;
        private final String detailedDescription;
        
        AttributeType(String name, String shortDescription, String detailedDescription) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.detailedDescription = detailedDescription;
        }
        
        public String getName() {
            return name;
        }
        
        public String getShortDescription() {
            return shortDescription;
        }
        
        public String getDetailedDescription() {
            return detailedDescription;
        }
    }
}