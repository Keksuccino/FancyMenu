package de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.requirements.window;

import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class IsWindowWidthBiggerThanRequirement extends LoadingRequirement {

    public IsWindowWidthBiggerThanRequirement() {
        super("fancymenu_loading_requirement_is_window_width_bigger_than");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value != null) {
            value = value.replace(" ", "");
            if (MathUtils.isDouble(value)) {
                return Minecraft.getInstance().getWindow().getWidth() > (int)Double.parseDouble(value);
            }
        }
        return false;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.desc", "" + Minecraft.getInstance().getWindow().getWidth(), "" + Minecraft.getInstance().getWindow().getHeight()), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.window");
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.valuename");
    }

    @Override
    public String getValuePreset() {
        return "1920";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
