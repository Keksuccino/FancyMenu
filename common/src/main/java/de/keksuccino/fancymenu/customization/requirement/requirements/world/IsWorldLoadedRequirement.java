package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsWorldLoadedRequirement extends Requirement {

    public IsWorldLoadedRequirement() {
        super("fancymenu_loading_requirement_is_world_loaded");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        return Minecraft.getInstance().level != null;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.worldloaded");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.worldloaded.desc");
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
