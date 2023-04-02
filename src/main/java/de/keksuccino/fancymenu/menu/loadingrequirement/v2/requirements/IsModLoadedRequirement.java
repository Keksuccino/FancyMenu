package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.fabricmc.loader.api.FabricLoader;

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
                        if (!FabricLoader.getInstance().isModLoaded(s)) {
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
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.valuename");
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
