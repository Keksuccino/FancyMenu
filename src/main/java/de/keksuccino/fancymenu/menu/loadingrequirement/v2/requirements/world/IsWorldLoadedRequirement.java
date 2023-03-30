package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsWorldLoadedRequirement extends LoadingRequirement {

    public IsWorldLoadedRequirement() {
        super("fancymenu_loading_requirement_is_world_loaded");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet( String value) {

        return Minecraft.getInstance().level != null;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.world");
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
