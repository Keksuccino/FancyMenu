package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.level.raid.ActiveRaidClientState;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class IsInActiveRaidAreaRequirement extends Requirement {

    public IsInActiveRaidAreaRequirement() {
        super("is_in_active_raid_area");
    }

    @Override
    public boolean canRunAsync() {
        // The raid state is an immutable snapshot, so it's safe to read from any thread
        return true;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        return ActiveRaidClientState.INSTANCE.isInActiveRaidArea();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.world.is_in_active_raid_area");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.world.is_in_active_raid_area.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public Component getValueDisplayName() {
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
