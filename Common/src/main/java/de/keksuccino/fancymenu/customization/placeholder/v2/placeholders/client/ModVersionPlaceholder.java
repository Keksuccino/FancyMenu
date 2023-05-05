package de.keksuccino.fancymenu.customization.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.v2.Placeholder;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModVersionPlaceholder extends Placeholder {

    public ModVersionPlaceholder() {
        super("modversion");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (!dps.values.containsKey("modid")) {
            return null;
        }
        return getModVersion(dps.values.get("modid"));
    }

    private String getModVersion(String modid) {
        return Services.PLATFORM.getModVersion(modid);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("modid");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("modid", "some_mod_id");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
