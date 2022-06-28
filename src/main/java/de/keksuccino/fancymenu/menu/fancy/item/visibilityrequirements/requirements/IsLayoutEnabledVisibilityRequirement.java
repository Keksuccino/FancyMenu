package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsLayoutEnabledVisibilityRequirement extends VisibilityRequirement {

    public IsLayoutEnabledVisibilityRequirement() {
        super("fancymenu_visibility_requirement_is_layout_enabled");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {

            for (PropertiesSet s : MenuCustomizationProperties.getProperties()) {
                List<PropertiesSection> l = s.getPropertiesOfType("customization-meta");
                if (!l.isEmpty()) {
                    PropertiesSection meta = l.get(0);
                    String path = meta.getEntryValue("path");
                    if (path != null) {
                        String name = Files.getNameWithoutExtension(path);
                        if (name.equals(value)) {
                            return true;
                        }
                    }
                }
            }

        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_layout_enabled");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.is_layout_enabled.desc"), "%n%"));
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_layout_enabled.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "my_cool_main_menu_layout";
    }

    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
