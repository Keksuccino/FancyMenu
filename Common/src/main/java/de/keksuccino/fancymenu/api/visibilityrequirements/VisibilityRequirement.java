
package de.keksuccino.fancymenu.api.visibilityrequirements;

import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;

import javax.annotation.Nullable;
import java.util.List;

/**
 * DEPRECATED! Use {@link LoadingRequirement} instead!
 */
@Deprecated
public abstract class VisibilityRequirement {

    protected final String identifier;

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public VisibilityRequirement(String uniqueRequirementIdentifier) {
        this.identifier = uniqueRequirementIdentifier;
    }

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract boolean hasValue();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract boolean isRequirementMet(@Nullable String value);

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract String getDisplayName();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract List<String> getDescription();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract String getValueDisplayName();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract String getValuePreset();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public abstract CharacterFilter getValueInputFieldFilter();

    /**
     * DEPRECATED! Use {@link LoadingRequirement} instead!
     */
    @Deprecated
    public String getIdentifier() {
        return this.identifier;
    }

}
