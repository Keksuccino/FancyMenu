package de.keksuccino.fancymenu.menu.loadingrequirement.v2;

import de.keksuccino.konkrete.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;

/**
 * A LoadingRequirement.<br><br>
 *
 * Needs to be registered to the {@link LoadingRequirementRegistry} on mod init.
 */
public abstract class LoadingRequirement {

    protected final String identifier;

    /**
     * The identifier needs to be unique! It is not possible to register two requirements with the same identifier.
     */
    public LoadingRequirement(@NotNull String uniqueRequirementIdentifier) {
        if (!CharacterFilter.getBasicFilenameCharacterFilter().isAllowed(uniqueRequirementIdentifier)) {
            throw new UnsupportedCharsetException("[FANCYMENU] Illegal characters in LoadingRequirement name: " + uniqueRequirementIdentifier);
        }
        this.identifier = Objects.requireNonNull(uniqueRequirementIdentifier);
    }

    /**
     * If the requirement has a value.<br><br>
     *
     * Keep in mind that the value string will be checked for placeholders, so don't mess with similar JSON-like parsing in it.
     */
    public abstract boolean hasValue();

    /**
     * The magic happens here. This is where you put the actual logic of the requirement.<br>
     * It checks if the requirement is met.<br><br>
     *
     * <b>For example:</b> If your requirement checks if the window is fullscreen, and it IS currently in fullscreen, then return TRUE here.<br><br>
     *
     * Keep in mind that the value string will be checked for placeholders, so don't mess with similar JSON-like parsing in it.
     *
     * @param value The value of the requirement, if it has one. This param is NULL if the requirement has no value!
     */
    public abstract boolean isRequirementMet(@Nullable String value);

    /**
     * The display name of the requirement.<br>
     * It is shown in the requirement options of the layout editor.
     */
    @NotNull
    public abstract String getDisplayName();

    /**
     * The description of the requirement.<br>
     * It is shown in the requirement options of the layout editor.<br><br>
     *
     * Every entry in the returned list counts as a text line.
     */
    @NotNull
    public abstract List<String> getDescription();

    /**
     * The display name of the VALUE of the requirement, if it has one.<br>
     * It is shown in the requirement options of the layout editor.<br><br>
     *
     * Return NULL here if the requirement has no value.
     */
    @Nullable
    public abstract String getValueDisplayName();

    /**
     * The preset/example of the value, if it has one.<br>
     * It is shown in the value input field of the requirement options of the layout editor.<br><br>
     *
     * Keep in mind that the value string will be checked for placeholders, so don't mess with similar JSON-like parsing here.<br><br>
     *
     * Return NULL here if the requirement has no value.
     */
    @Nullable
    public abstract String getValuePreset();

    /**
     * The identifier of the requirement.
     */
    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

}
