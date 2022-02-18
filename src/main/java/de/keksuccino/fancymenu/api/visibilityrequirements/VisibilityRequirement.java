//TODO übernehmen
package de.keksuccino.fancymenu.api.visibilityrequirements;

import de.keksuccino.konkrete.input.CharacterFilter;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A visibility requirement.<br><br>
 *
 * Needs to be registered to the {@link VisibilityRequirementRegistry} at mod init.
 */
public abstract class VisibilityRequirement {

    protected final String identifier;

    /**
     * The identifier needs to be unique! It's not possible to register two requirements with the same identifier.
     */
    public VisibilityRequirement(String uniqueRequirementIdentifier) {
        this.identifier = uniqueRequirementIdentifier;
    }

    /**
     * If the requirement has a value or not.<br>
     * A text field to input the value is shown in the visibility requirement options of the layout editor, if this is enabled.
     */
    public abstract boolean hasValue();

    /**
     * This is where you return if the requirement is met (using the given requirement value, if this requirement has one).<br><br>
     *
     * For example: If your requirement checks for fullscreen and the window IS in fullscreen, return TRUE here.
     *
     * @param value The value of the requirement, if it has one. This param is NULL if the requirement has no value!
     */
    public abstract boolean isRequirementMet(@Nullable String value);

    /**
     * The display name of the requirement.<br>
     * Is shown in the requirement options of the layout editor.
     */
    public abstract String getDisplayName();

    /**
     * The description of the requirement.<br>
     * Is shown in the requirement options of the layout editor, when hovering over the toggle requirement button.
     */
    public abstract List<String> getDescription();

    /**
     * The display name of the VALUE of the requirement, if it has one.<br>
     * Is shown in the requirement options of the layout editor.<br><br>
     *
     * Just return NULL here if the requirement has no value.
     */
    public abstract String getValueDisplayName();

    /**
     * The preset of the value, if it has one.<br>
     * Is shown in the value input field of the requirement options of the layout editor.<br><br>
     *
     * Just return NULL here if the requirement has no value.
     */
    public abstract String getValuePreset();

    /**
     * The character filter of the value, if it has one.<br>
     * This limits what characters the user can type into the value input field of the requirement options.<br><br>
     *
     * Return NULL here if you want no filter (all chars allowed) or if the requirement has no value.
     */
    public abstract CharacterFilter getValueInputFieldFilter();

    /**
     * The identifier of the requirement.
     */
    public String getIdentifier() {
        return this.identifier;
    }

}
