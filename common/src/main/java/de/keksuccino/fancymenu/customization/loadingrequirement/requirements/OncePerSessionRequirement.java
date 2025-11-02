package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A requirement that returns true only once per game session.
 * Uses the LoadingRequirementInstance's instanceIdentifier to track if it already returned true.
 */
public class OncePerSessionRequirement extends LoadingRequirement {

    private static final Set<String> SEEN_REQUIREMENT_INSTANCES = new HashSet<>();

    public OncePerSessionRequirement() {
        super("once_per_session");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        String instanceId = this.getCurrentInstance().instanceIdentifier;

        // If we haven't seen this instance before, mark it as seen and return true
        if (!SEEN_REQUIREMENT_INSTANCES.contains(instanceId)) {
            SEEN_REQUIREMENT_INSTANCES.add(instanceId);
            return true;
        }

        // We've seen this instance before, return false
        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.once_per_session");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.once_per_session.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}