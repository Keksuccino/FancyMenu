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

    /**
     * Creates a new buddy level.
     *
     * @param level The numeric level (1-based)
     * @param experienceRequired The experience points required to reach this level
     * @param title The title for this level
     * @param description A description of what this level represents
     */
    public BuddyLevel(int level, int experienceRequired, @NotNull String title, @NotNull String description) {
        this.level = level;
        this.experienceRequired = experienceRequired;
        this.title = title;
        this.description = description;
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

}