package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.window;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;

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
    public boolean isRequirementMet( String value) {
        if (value != null) {
            value = value.replace(" ", "");
            if (MathUtils.isDouble(value)) {
                return Minecraft.getMinecraft().displayWidth > (int)Double.parseDouble(value);
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
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.desc", "" + Minecraft.getMinecraft().displayWidth, "" + Minecraft.getMinecraft().displayHeight), "%n%"));
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
