package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsModLoadedRequirement extends LoadingRequirement {

    public IsModLoadedRequirement() {
        super("fancymenu_loading_requirement_is_mod_loaded");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            List<String> l = this.parseStrings(value);
            if (!l.isEmpty()) {
                for (String s : l) {
                    if (s.equalsIgnoreCase("optifine")) {
                        if (!Konkrete.isOptifineLoaded) {
                            return false;
                        }
                    } else {
                        if (!Services.PLATFORM.isModLoaded(s)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        return false;

    }

    protected List<String> parseStrings(String value) {
        List<String> l = new ArrayList<>();
        if (value != null) {
            if (value.contains(",")) {
                for (String s : value.replace(" ", "").split("[,]")) {
                    l.add(s);
                }
            } else {
                l.add(value.replace(" ", ""));
            }
        }
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.modloaded");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.modloaded.desc")));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.modloaded.valuename");
    }

    @Override
    public String getValuePreset() {
        return "optifine";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
